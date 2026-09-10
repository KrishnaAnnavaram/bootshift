package com.bootshift.stages.stage10;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.Envelope;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.evidence.EvidenceManifest;
import com.bootshift.core.graph.ApplicationGraph;
import com.bootshift.core.graph.GraphNode;
import com.bootshift.core.graph.NodeType;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Ids;
import com.bootshift.core.util.Json;
import com.bootshift.stages.Stage;
import com.bootshift.stages.StageContext;
import com.bootshift.stages.StageSupport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent 10 - Characterization Engine (spec section 21).
 *
 * <p>Protects migration-sensitive behaviour <em>before</em> it is changed. For each important impact
 * the question is only ever "does adequate protection already exist?", and when it does not, a
 * harness-owned probe is scaffolded.
 *
 * <p>The critical rule: expected behaviour comes from observed original behaviour, an authoritative
 * specification, or an explicit human decision - never from invention. A probe whose expectation
 * cannot be filled from the sealed baseline is created in state
 * {@code AWAITING_OLD_OBSERVATION} and cannot be used as an oracle until it is executed against OLD.
 *
 * <p>Contracts are framework-neutral: they describe a scenario and its observed outcome, not a JUnit
 * test. That is what lets the test-infrastructure edge migrate without destroying the oracle.
 */
public final class CharacterizationStage implements Stage {

    public static final String OUTPUT_DIR = "10-characterization";

    /** Lifecycle of a characterization contract. */
    public enum ContractState {
        /** An existing application test already covers the behaviour. */
        MAPPED_TO_EXISTING_TEST,
        /** A probe exists but has not yet been executed against the original application. */
        AWAITING_OLD_OBSERVATION,
        /** Executed against OLD and the observed behaviour is frozen as the oracle. */
        FROZEN,
        /** Executed against OLD and rejected because it was not valid there. */
        REJECTED,
        /** The behaviour cannot be observed in this environment at all. */
        UNOBSERVABLE
    }

    private final AtomicLong sequence = new AtomicLong();

    @Override
    public String id() {
        return OUTPUT_DIR;
    }

    @Override
    public String outputDirectory() {
        return OUTPUT_DIR;
    }

    @Override
    public String purpose() {
        return "Establish behavioural contracts for migration-sensitive impacts before they change";
    }

    @Override
    public List<RunState> preconditions() {
        return List.of(RunState.IMPACT_ANALYZED);
    }

    @Override
    public RunState postcondition() {
        return RunState.CHARACTERIZATION_COMPLETE;
    }

    @Override
    public List<String> inputArtifacts() {
        return List.of("09-impact/impact-report.json", "03-graph/application-graph.json",
                "04-baseline/baseline-runtime.json", "04-baseline/baseline-tests.json");
    }

    @Override
    public List<String> outputArtifacts() {
        return List.of("characterization-report.json", "characterization-contracts.json",
                "characterization-gaps.json", "manifest.json");
    }

    @Override
    public StageResult execute(StageContext context) {
        JsonNode impact = StageSupport.requireUpstream(context, "09-impact", "impact-report.json",
                "Run: harness impact");
        JsonNode graphNode = StageSupport.requireUpstream(context, "03-graph", "application-graph.json",
                "Run: harness graph --repo <path>");
        JsonNode baselineRuntime = StageSupport.optionalUpstream(context, "04-baseline",
                "baseline-runtime.json");
        JsonNode baselineTests = StageSupport.optionalUpstream(context, "04-baseline",
                "baseline-tests.json");

        ApplicationGraph graph = ApplicationGraph.fromNode(graphNode);
        OutputLayout.StageWriter writer = context.run().output().open(OUTPUT_DIR);
        Envelope envelope = StageSupport.envelope(context, OUTPUT_DIR);

        Set<String> passingTestClasses = passingTestClasses(baselineTests);
        Map<String, JsonNode> runtimeByModule = runtimeByModule(baselineRuntime);

        List<ObjectNode> contracts = new ArrayList<>();
        List<ObjectNode> gaps = new ArrayList<>();
        Map<String, Integer> byState = new java.util.TreeMap<>();

        for (JsonNode finding : impact.path("findings")) {
            String classification = finding.path("classification").asText();
            if ("UNAFFECTED_WITHIN_OBSERVED_COVERAGE".equals(classification)) {
                continue;
            }
            for (JsonNode dimensionNode : finding.path("required_validation_dimensions")) {
                String dimension = dimensionNode.asText();
                ObjectNode contract = characterize(context, finding, dimension, graph,
                        passingTestClasses, runtimeByModule);
                contracts.add(contract);
                byState.merge(contract.path("state").asText(), 1, Integer::sum);
                if (ContractState.UNOBSERVABLE.name().equals(contract.path("state").asText())) {
                    ObjectNode gap = Json.obj();
                    gap.put("gap_id", "GAP-CHAR-" + contract.path("scenario_id").asText());
                    gap.put("dimension", dimension);
                    gap.put("impact_id", finding.path("impact_id").asText());
                    gap.put("reason", contract.path("reason").asText());
                    gaps.add(gap);
                }
            }
        }

        // Endpoint characterization: every observed endpoint gets an HTTP contract, because HTTP
        // shape is the most commonly broken thing in a Spring Boot major upgrade.
        for (GraphNode endpoint : graph.nodesOfType(NodeType.ENDPOINT)) {
            ObjectNode contract = endpointContract(endpoint, runtimeByModule);
            contracts.add(contract);
            byState.merge(contract.path("state").asText(), 1, Integer::sum);
        }

        long frozen = byState.getOrDefault(ContractState.FROZEN.name(), 0);
        long awaiting = byState.getOrDefault(ContractState.AWAITING_OLD_OBSERVATION.name(), 0);
        long unobservable = byState.getOrDefault(ContractState.UNOBSERVABLE.name(), 0);
        long mapped = byState.getOrDefault(ContractState.MAPPED_TO_EXISTING_TEST.name(), 0);

        envelope.stat("contracts", contracts.size())
                .stat("frozen", frozen)
                .stat("awaiting_old_observation", awaiting)
                .stat("mapped_to_existing_tests", mapped)
                .stat("unobservable", unobservable);
        if (unobservable > 0) {
            envelope.blindSpot(new Envelope.BlindSpot("BS-CHAR-001", "CHARACTERIZATION",
                    unobservable + " behaviour(s) could not be characterized in this environment",
                    "Differential validation cannot reach E4 for those dimensions"));
        }

        ObjectNode report = Json.obj();
        report.put("contract_count", contracts.size());
        report.set("by_state", Json.toTree(byState));
        report.put("oracle_rule", "Expected behaviour comes from observed original behaviour, an "
                + "authoritative specification, or an explicit human decision. Never from invention.");
        report.put("framework_neutrality", "Contracts describe scenarios and observed outcomes rather "
                + "than JUnit tests, so a test-infrastructure migration cannot destroy the oracle.");
        ObjectNode reportArtifact = StageSupport.compose(envelope, report);
        StageSupport.validate(context, writer, "characterization/characterization-report.schema.json",
                "characterization-report.json", reportArtifact);
        writer.write("characterization-report.json", reportArtifact);

        ObjectNode contractArtifact = Json.obj();
        contractArtifact.put("count", contracts.size());
        contractArtifact.set("contracts", Json.toTree(contracts));
        writer.write("characterization-contracts.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), contractArtifact));

        ObjectNode gapArtifact = Json.obj();
        gapArtifact.put("count", gaps.size());
        // "gaps" is a reserved envelope key, so the payload names this collection explicitly.
        gapArtifact.set("characterization_gaps", Json.toTree(gaps));
        writer.write("characterization-gaps.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), gapArtifact));

        StageSupport.toEvidence(context, "characterization", reportArtifact,
                EvidenceManifest.Classification.INTERNAL, "SEALED_EVIDENCE", OUTPUT_DIR);

        if (!writer.validationErrors().isEmpty()) {
            return StageResult.failure(OUTPUT_DIR, ExitCode.STAGE_FAILURE,
                    "Characterization artifacts failed schema validation", writer.validationErrors());
        }

        String hash = StageSupport.publish(context, writer);
        context.stateMachine().transition(RunState.CHARACTERIZATION_COMPLETE,
                contracts.size() + " contract(s)");
        context.runStateStore().updateState(context.run().runId(),
                RunState.CHARACTERIZATION_COMPLETE, "characterization complete");

        Map<String, Path> artifacts = new LinkedHashMap<>();
        outputArtifacts().forEach(name -> artifacts.put(name, writer.dir().resolve(name)));

        List<String> messages = new ArrayList<>();
        if (awaiting > 0) {
            messages.add(awaiting + " probe(s) await execution against the original application; "
                    + "they cannot act as an oracle until then");
        }
        if (unobservable > 0) {
            messages.add(unobservable + " behaviour(s) are unobservable in this environment");
        }

        return new StageResult(OUTPUT_DIR, ExitCode.SUCCESS,
                contracts.size() + " characterization contract(s): " + frozen + " frozen, "
                        + mapped + " mapped to existing tests, " + awaiting + " awaiting OLD, "
                        + unobservable + " unobservable",
                messages, artifacts, hash);
    }

    // ------------------------------------------------------------------ contract construction

    private ObjectNode characterize(StageContext context, JsonNode finding, String dimension,
                                    ApplicationGraph graph, Set<String> passingTestClasses,
                                    Map<String, JsonNode> runtimeByModule) {
        ObjectNode contract = Json.obj();
        String scenarioId = Ids.scenarioId(sequence.incrementAndGet());
        contract.put("scenario_id", scenarioId);
        contract.put("impact_id", finding.path("impact_id").asText());
        contract.put("knowledge_id", finding.path("knowledge_id").asText());
        contract.put("dimension", dimension);
        contract.put("file_id", finding.path("file_id").asText(null));
        contract.put("node_id", finding.path("node_id").asText(null));
        contract.put("subject", finding.path("subject").asText());
        contract.put("module", finding.path("module").asText(null));

        // 1. does adequate protection already exist?
        ArrayNode covering = Json.arr();
        finding.path("covering_tests").forEach(t -> {
            if (passingTestClasses.contains(t.asText())) {
                covering.add(t.asText());
            }
        });
        if (covering.size() > 0) {
            contract.set("existing_tests", covering);
            contract.put("state", ContractState.MAPPED_TO_EXISTING_TEST.name());
            contract.put("oracle_source", "EXISTING_APPLICATION_TEST");
            contract.put("reason", "Behaviour is already protected by " + covering.size()
                    + " passing baseline test(s)");
            return contract;
        }

        // 2. can the behaviour be observed at all in this environment?
        String module = finding.path("module").asText(null);
        JsonNode runtime = module == null ? null : runtimeByModule.get(module);
        boolean runtimeObservable = runtime != null && runtime.path("started").asBoolean(false);

        boolean needsRuntime = switch (dimension) {
            case "HTTP_API", "SECURITY_AUTHORIZATION", "SERIALIZATION", "CONFIGURATION_BINDING",
                 "PERSISTENCE_STATE", "QUERY_RESULT", "TRANSACTION_EFFECT", "EVENT_MESSAGE",
                 "EXTERNAL_INTEGRATION", "BATCH_RESULT" -> true;
            default -> false;
        };

        if (needsRuntime && !runtimeObservable) {
            contract.put("state", ContractState.UNOBSERVABLE.name());
            contract.put("oracle_source", "NONE");
            contract.put("reason", "Dimension " + dimension + " requires a running application, and "
                    + (runtime == null ? "no runtime probe was attempted for module " + module
                    : "the module did not start: " + runtime.path("failure_reason").asText("unknown")));
            contract.set("probe", scaffold(dimension, finding, graph));
            return contract;
        }

        // 3. scaffold a probe whose expectation must still come from OLD.
        contract.put("state", ContractState.AWAITING_OLD_OBSERVATION.name());
        contract.put("oracle_source", "OBSERVED_ORIGINAL_BEHAVIOUR_PENDING");
        contract.put("reason", "No existing test covers this behaviour. A harness-owned probe was "
                + "scaffolded; its expected value will be frozen from the original application, never "
                + "invented.");
        contract.set("probe", scaffold(dimension, finding, graph));
        contract.put("ai_used", false);
        if (context.ai().enabled()) {
            contract.put("ai_used", true);
            contract.put("ai_role", "scaffolding only; the expected value still comes from OLD");
        }
        return contract;
    }

    /** Builds an executable probe description. Framework-neutral by construction. */
    private ObjectNode scaffold(String dimension, JsonNode finding, ApplicationGraph graph) {
        ObjectNode probe = Json.obj();
        probe.put("kind", dimension);
        probe.put("harness_owned", true);
        switch (dimension) {
            case "HTTP_API", "SECURITY_AUTHORIZATION", "SERIALIZATION" -> {
                probe.put("transport", "HTTP");
                probe.put("method", "GET");
                probe.put("path", "/actuator/health");
                probe.set("capture", Json.toTree(List.of("status_code", "headers", "body_shape")));
            }
            case "CONFIGURATION_BINDING" -> {
                probe.put("transport", "ACTUATOR");
                probe.put("path", "/actuator/configprops");
                probe.set("capture", Json.toTree(List.of("canonical_key", "bound", "defaulted",
                        "target_type", "source_type")));
            }
            case "PERSISTENCE_STATE", "QUERY_RESULT", "TRANSACTION_EFFECT" -> {
                probe.put("transport", "REPOSITORY_INVOCATION");
                probe.set("capture", Json.toTree(List.of("returned_entities", "database_state",
                        "transaction_outcome")));
                probe.put("note", "SQL text is diagnostic evidence only; the contract compares "
                        + "business-relevant semantics");
            }
            case "CONTEXT_CAPABILITY" -> {
                probe.put("transport", "ACTUATOR");
                probe.put("path", "/actuator/beans");
                probe.set("capture", Json.toTree(List.of("bean_names", "condition_evaluation")));
            }
            case "EVENT_MESSAGE" -> {
                probe.put("transport", "BROKER");
                probe.set("capture", Json.toTree(List.of("destination", "payload_shape", "headers")));
            }
            default -> {
                probe.put("transport", "IN_PROCESS");
                probe.set("capture", Json.toTree(List.of("observed_outcome")));
            }
        }
        graph.node(finding.path("node_id").asText("")).ifPresent(node -> {
            probe.put("target", node.getFqn() == null ? node.getName() : node.getFqn());
            if (node.getType() == NodeType.ENDPOINT) {
                probe.put("method", String.valueOf(node.getProperties().get("http_method")));
                probe.put("path", String.valueOf(node.getProperties().get("path")));
            }
        });
        return probe;
    }

    private ObjectNode endpointContract(GraphNode endpoint, Map<String, JsonNode> runtimeByModule) {
        ObjectNode contract = Json.obj();
        contract.put("scenario_id", Ids.scenarioId(sequence.incrementAndGet()));
        contract.put("dimension", "HTTP_API");
        contract.put("node_id", endpoint.getId());
        contract.put("module", endpoint.getModule());
        contract.put("subject", endpoint.getName());
        contract.put("file_id", endpoint.getFileId());

        JsonNode runtime = runtimeByModule.get(endpoint.getModule());
        boolean observable = runtime != null && runtime.path("started").asBoolean(false);
        ObjectNode probe = Json.obj();
        probe.put("kind", "HTTP_API");
        probe.put("harness_owned", true);
        probe.put("transport", "HTTP");
        probe.put("method", String.valueOf(endpoint.getProperties().get("http_method")));
        probe.put("path", String.valueOf(endpoint.getProperties().get("path")));
        probe.put("handler", String.valueOf(endpoint.getProperties().get("handler")));
        probe.set("capture", Json.toTree(List.of("status_code", "content_type", "body_shape",
                "error_payload_shape")));
        contract.set("probe", probe);

        if (observable) {
            contract.put("state", ContractState.AWAITING_OLD_OBSERVATION.name());
            contract.put("oracle_source", "OBSERVED_ORIGINAL_BEHAVIOUR_PENDING");
            contract.put("reason", "Endpoint is reachable in the baseline environment; its response "
                    + "shape will be frozen from the original application");
        } else {
            contract.put("state", ContractState.UNOBSERVABLE.name());
            contract.put("oracle_source", "NONE");
            contract.put("reason", "Module " + endpoint.getModule()
                    + " did not start during baseline capture, so this endpoint has no observed "
                    + "original behaviour to freeze");
        }
        return contract;
    }

    // ------------------------------------------------------------------ baseline lookups

    private Set<String> passingTestClasses(JsonNode baselineTests) {
        Set<String> classes = new LinkedHashSet<>();
        if (baselineTests == null) {
            return classes;
        }
        for (JsonNode module : baselineTests.path("modules")) {
            for (JsonNode testCase : module.path("cases")) {
                if ("PASSED".equals(testCase.path("outcome").asText())) {
                    classes.add(testCase.path("className").asText());
                }
            }
        }
        return classes;
    }

    private Map<String, JsonNode> runtimeByModule(JsonNode baselineRuntime) {
        Map<String, JsonNode> byModule = new LinkedHashMap<>();
        if (baselineRuntime == null) {
            return byModule;
        }
        for (JsonNode module : baselineRuntime.path("modules")) {
            byModule.put(module.path("module").asText(), module);
        }
        return byModule;
    }
}
