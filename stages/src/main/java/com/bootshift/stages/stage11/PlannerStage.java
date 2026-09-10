package com.bootshift.stages.stage11;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.adapters.transform.ConfigurationPropertyTransformer;
import com.bootshift.adapters.transform.JakartaNamespaceTransformer;
import com.bootshift.adapters.transform.MavenPomTransformer;
import com.bootshift.adapters.transform.RemovedAnnotationTransformer;
import com.bootshift.adapters.transform.OpenRewriteCoreProbe;
import com.bootshift.adapters.transform.TestFrameworkTransformer;
import com.bootshift.core.domain.Envelope;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.HarnessException;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.evidence.EvidenceManifest;
import com.bootshift.core.policy.ValidationDepth;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Json;
import com.bootshift.ports.transformation.TransformationPort;
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

/**
 * Agent 11 - Migration Planner (spec section 22).
 *
 * <p>The Target Resolver said where to go. The Planner says exactly how, and freezes the answer.
 *
 * <p>Three things happen here that nothing else may do later:
 *
 * <ul>
 *   <li>the {@code TransformationCapabilityRegistry} is built from the tools actually present, so a
 *       missing recipe becomes measured residual instead of a silent hole (R31);</li>
 *   <li>validation depth is computed once and frozen into the edge plan (R16);</li>
 *   <li>composite transformations are reconciled against mandatory checkpoints, and a transformation
 *       that would skip one is decomposed, escalated, or blocked (R26).</li>
 * </ul>
 */
public final class PlannerStage implements Stage {

    public static final String OUTPUT_DIR = "11-plan";

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
        return "Plan and freeze how the frozen migration path will actually be executed";
    }

    @Override
    public List<RunState> preconditions() {
        return List.of(RunState.CHARACTERIZATION_COMPLETE);
    }

    @Override
    public RunState postcondition() {
        return RunState.PLAN_FROZEN;
    }

    @Override
    public List<String> inputArtifacts() {
        return List.of("06-target/migration-path.json", "08-knowledge/migration-knowledge.json",
                "09-impact/impact-report.json", "10-characterization/characterization-contracts.json");
    }

    @Override
    public List<String> outputArtifacts() {
        return List.of("migration-plan.json", "edge-plan.json",
                "transformation-capability-registry.json", "residual-report.json", "manifest.json");
    }

    @Override
    public StageResult execute(StageContext context) {
        JsonNode target = StageSupport.requireUpstream(context, "06-target", "target-state.json",
                "Run: harness resolve-target --target auto");
        JsonNode path = StageSupport.requireUpstream(context, "06-target", "migration-path.json",
                "Run: harness resolve-target --target auto");
        JsonNode knowledge = StageSupport.requireUpstream(context, "08-knowledge",
                "migration-knowledge.json", "Run: harness knowledge");
        JsonNode impact = StageSupport.requireUpstream(context, "09-impact", "impact-report.json",
                "Run: harness impact");
        JsonNode contracts = StageSupport.optionalUpstream(context, "10-characterization",
                "characterization-contracts.json");
        JsonNode lifecycle = StageSupport.optionalUpstream(context, "05-compatibility",
                "lifecycle-registry.json");

        // The Spring Cloud train is locked to the Boot line, so each edge needs the train that goes
        // with ITS target line. Using the landing train on a transit checkpoint installs a train from
        // the future onto an older Boot version, and types removed in between simply vanish.
        Map<String, String> cloudTrainByBootLine = new LinkedHashMap<>();
        Map<String, List<Integer>> javaMajorsByBootLine = new LinkedHashMap<>();
        if (lifecycle != null) {
            for (JsonNode release : lifecycle.path("releases")) {
                String line = release.path("line").asText();
                String train = release.path("spring_cloud_train").asText(null);
                if (train != null && !train.isBlank()) {
                    cloudTrainByBootLine.put(line, train);
                }
                List<Integer> majors = new ArrayList<>();
                release.path("supported_java_majors").forEach(n -> majors.add(n.asInt()));
                javaMajorsByBootLine.put(line, majors);
            }
        }

        // Java levels an installed toolchain can actually provide.
        List<Integer> installedJdks = new com.bootshift.adapters.build.ToolchainProbe().discover()
                .stream()
                .map(com.bootshift.adapters.build.ToolchainProbe.Jdk::major)
                .distinct().sorted().toList();

        OutputLayout.StageWriter writer = context.run().output().open(OUTPUT_DIR);
        Envelope envelope = StageSupport.envelope(context, OUTPUT_DIR);

        // ---------------------------------------------------------- capability registry (R31)
        String sourceVersion = target.path("source_version").asText();
        String targetVersion = target.path("landing_version").asText();
        List<TransformationPort> providers = List.of(
                new MavenPomTransformer(),
                new JakartaNamespaceTransformer(),
                new TestFrameworkTransformer(),
                new RemovedAnnotationTransformer(),
                ConfigurationPropertyTransformer.fromRuleFile(generatedRuleFile(context)),
                new OpenRewriteCoreProbe());

        List<TransformationPort.Capability> capabilities = new ArrayList<>();
        for (TransformationPort provider : providers) {
            capabilities.addAll(provider.capabilities(sourceVersion, targetVersion));
        }

        List<TransformationPort.Capability> availableCapabilities = capabilities.stream()
                .filter(c -> "AVAILABLE".equals(c.status()))
                .toList();
        Set<String> handledFactTypes = new LinkedHashSet<>();
        availableCapabilities.forEach(c -> handledFactTypes.addAll(c.handledFactTypes()));

        ObjectNode registryArtifact = Json.obj();
        registryArtifact.put("source_version", sourceVersion);
        registryArtifact.put("target_version", targetVersion);
        registryArtifact.put("capability_count", capabilities.size());
        registryArtifact.put("available_count",
                capabilities.stream().filter(c -> "AVAILABLE".equals(c.status())).count());
        registryArtifact.put("discovery_rule", "Capabilities are probed from the tools actually "
                + "present at planning time; nothing is assumed to exist for an edge (R31)");
        registryArtifact.set("capabilities", Json.toTree(capabilities));
        registryArtifact.set("handled_fact_types", Json.toTree(handledFactTypes));
        writer.write("transformation-capability-registry.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), registryArtifact));

        // ---------------------------------------------------------- deterministic coverage
        // Coverage is computed per FACT, not per fact type. A capability that handles JUnit 4
        // rewrites declares API_REMOVED, and counting the whole type as covered made 56 removed APIs
        // - @EnableEurekaClient among them - disappear from the residual while the edge failed to
        // compile. A capability now has to claim the fact's subject, not just its category.
        Map<String, Integer> factsByType = new java.util.TreeMap<>();
        Map<String, List<String>> factIdsByType = new LinkedHashMap<>();
        Map<String, Integer> coveredByType = new java.util.TreeMap<>();
        Map<String, List<String>> uncoveredIdsByType = new LinkedHashMap<>();
        Map<String, List<String>> uncoveredSubjectsByType = new LinkedHashMap<>();
        int verifiedFacts = 0;
        int covered = 0;
        for (JsonNode fact : knowledge.path("facts")) {
            if (!fact.path("authorizes_transformation").asBoolean(false)) {
                continue;
            }
            verifiedFacts++;
            String type = fact.path("type").asText();
            String subject = fact.path("subject").asText(null);
            factsByType.merge(type, 1, Integer::sum);
            factIdsByType.computeIfAbsent(type, k -> new ArrayList<>())
                    .add(fact.path("knowledge_id").asText());
            boolean handled = availableCapabilities.stream().anyMatch(c -> c.covers(type, subject));
            if (handled) {
                covered++;
                coveredByType.merge(type, 1, Integer::sum);
            } else {
                uncoveredIdsByType.computeIfAbsent(type, k -> new ArrayList<>())
                        .add(fact.path("knowledge_id").asText());
                List<String> subjects = uncoveredSubjectsByType
                        .computeIfAbsent(type, k -> new ArrayList<>());
                if (subject != null && subjects.size() < 25) {
                    subjects.add(subject);
                }
            }
        }
        List<ObjectNode> residual = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : uncoveredIdsByType.entrySet()) {
            ObjectNode item = Json.obj();
            item.put("fact_type", entry.getKey());
            item.put("fact_count", entry.getValue().size());
            item.set("knowledge_ids", Json.toTree(entry.getValue()));
            item.set("example_subjects", Json.toTree(uncoveredSubjectsByType.get(entry.getKey())));
            item.put("reason", handledFactTypes.contains(entry.getKey())
                    ? "A capability declares this fact type but none claims these subjects"
                    : "No AVAILABLE deterministic capability handles this fact type");
            item.put("consequence", "Counted as residual, which raises the planned validation "
                    + "depth for affected edges");
            residual.add(item);
        }
        double deterministicCoverage = verifiedFacts == 0 ? 1.0
                : Math.round((double) covered / verifiedFacts * 10000.0) / 10000.0;

        // A single coverage number over this fact set is dominated by property renames, which the
        // generated rules cover trivially. Reported alone it reads as "the migration is 99.8%
        // handled" when the four API facts that decide whether the edge compiles may all be
        // uncovered. Report the breakdown, and say what the number does not measure.
        ObjectNode coverageByType = Json.obj();
        for (Map.Entry<String, Integer> entry : factsByType.entrySet()) {
            int total = entry.getValue();
            int hit = coveredByType.getOrDefault(entry.getKey(), 0);
            ObjectNode row = Json.obj();
            row.put("fact_count", total);
            row.put("covered", hit);
            row.put("uncovered", total - hit);
            row.put("type_claimed_by_a_capability", handledFactTypes.contains(entry.getKey()));
            coverageByType.set(entry.getKey(), row);
        }

        ObjectNode residualArtifact = Json.obj();
        residualArtifact.put("verified_fact_count", verifiedFacts);
        residualArtifact.put("deterministically_covered", covered);
        residualArtifact.put("deterministic_coverage", deterministicCoverage);
        residualArtifact.set("coverage_by_fact_type", coverageByType);
        residualArtifact.put("metric_scope", "Deterministic coverage is the fraction of VERIFIED "
                + "facts for which an AVAILABLE transformer exists. It measures rule availability "
                + "for the facts the harness KNOWS about. It does not measure whether the fact set "
                + "is complete: a required change nobody discovered is not counted as uncovered, "
                + "it is not counted at all. Knowledge completeness is reported separately by the "
                + "artifact channel's api_diff coverage.");
        residualArtifact.put("policy_no_escalation_threshold",
                context.policy().residualNoEscalationThreshold());
        residualArtifact.put("policy_one_level_threshold", context.policy().residualOneLevelThreshold());
        residualArtifact.put("note", "Residual is not a failure. It is exactly what the bounded "
                + "compile-repair and differential layers exist to manage.");
        residualArtifact.set("residual", Json.toTree(residual));
        writer.write("residual-report.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), residualArtifact));

        // ---------------------------------------------------------- impact-driven requirements
        Set<String> requiredDimensions = new LinkedHashSet<>();
        Map<String, Set<String>> filesByModule = new LinkedHashMap<>();
        Set<String> allImpactedFileIds = new LinkedHashSet<>();
        Set<String> impactIds = new LinkedHashSet<>();
        boolean anyHighRisk = false;
        for (JsonNode finding : impact.path("findings")) {
            if ("UNAFFECTED_WITHIN_OBSERVED_COVERAGE".equals(finding.path("classification").asText())) {
                continue;
            }
            impactIds.add(finding.path("impact_id").asText());
            finding.path("required_validation_dimensions").forEach(d -> requiredDimensions.add(d.asText()));
            String fileId = finding.path("file_id").asText(null);
            if (fileId != null && !fileId.isBlank()) {
                allImpactedFileIds.add(fileId);
                filesByModule.computeIfAbsent(finding.path("module").asText("."),
                        k -> new LinkedHashSet<>()).add(fileId);
            }
            if ("HIGH".equals(finding.path("risk").asText())) {
                anyHighRisk = true;
            }
        }
        boolean impactRecallBelowFloor = impact.path("accuracy").path("evaluated").asBoolean(false)
                && impact.path("accuracy").path("recall").asDouble(1.0)
                < context.policy().impactRecallFloor();

        JsonNode buildModelNode = StageSupport.optionalUpstream(context, "02-build", "build-model.json");
        String currentJavaLevel = buildModelNode == null ? "17"
                : buildModelNode.path("frameworks").path("java").asText("17");

        // Test sources, needed as scope for the preparatory test-infrastructure edge.
        Set<String> testFileIds = new LinkedHashSet<>();
        com.bootshift.core.identity.FileRegistry registry =
                com.bootshift.stages.EdgeSupport.loadRegistry(context);
        registry.active().stream()
                .filter(r -> r.getRole() == com.bootshift.core.identity.FileRole.JAVA_TEST)
                .forEach(r -> testFileIds.add(r.getFileId()));

        // ---------------------------------------------------------- per-edge plan
        List<ObjectNode> edgePlans = new ArrayList<>();
        List<String> reconciliationDecisions = new ArrayList<>();
        int index = 0;
        for (JsonNode edge : path.path("edges")) {
            index++;
            ObjectNode plan = planEdge(context, edge, index, capabilities, handledFactTypes,
                    factIdsByType, deterministicCoverage, requiredDimensions, allImpactedFileIds,
                    impactIds, anyHighRisk, impactRecallBelowFloor, contracts, reconciliationDecisions,
                    testFileIds, cloudTrainByBootLine, javaMajorsByBootLine, installedJdks,
                    currentJavaLevel);
            edgePlans.add(plan);
        }

        // ---------------------------------------------------------- checkpoint reconciliation (R26)
        boolean blocked = reconciliationDecisions.stream().anyMatch(d -> d.startsWith("BLOCK"));
        if (blocked) {
            ObjectNode blockedPlan = Json.obj();
            blockedPlan.set("edges", Json.toTree(edgePlans));
            blockedPlan.set("reconciliation", Json.toTree(reconciliationDecisions));
            writer.write("edge-plan.json",
                    StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), blockedPlan));
            StageSupport.publish(context, writer);
            throw HarnessException.block("A mandatory migration checkpoint would be skipped and policy "
                    + "forbids collapsing it: " + reconciliationDecisions);
        }

        ObjectNode edgePlanArtifact = Json.obj();
        edgePlanArtifact.put("edge_count", edgePlans.size());
        edgePlanArtifact.set("edges", Json.toTree(edgePlans));
        edgePlanArtifact.set("reconciliation_decisions", Json.toTree(reconciliationDecisions));
        ObjectNode edgeArtifact = StageSupport.compose(
                StageSupport.envelope(context, OUTPUT_DIR), edgePlanArtifact);
        StageSupport.validate(context, writer, "migration-plan/edge-plan.schema.json",
                "edge-plan.json", edgeArtifact);
        writer.write("edge-plan.json", edgeArtifact);

        ObjectNode migrationPlan = Json.obj();
        migrationPlan.put("source_version", sourceVersion);
        migrationPlan.put("target_version", targetVersion);
        migrationPlan.put("edge_count", edgePlans.size());
        migrationPlan.put("deterministic_coverage", deterministicCoverage);
        migrationPlan.put("verified_facts", verifiedFacts);
        migrationPlan.put("impacted_files", allImpactedFileIds.size());
        migrationPlan.set("required_differential_dimensions", Json.toTree(requiredDimensions));
        migrationPlan.put("frozen", true);
        migrationPlan.put("freeze_rule", "Execution reads the planned depth; it never invents its own");
        migrationPlan.set("edge_summary", Json.toTree(edgePlans.stream().map(e -> Map.of(
                "edge_id", e.path("edge_id").asText(),
                "edge_class", e.path("edge_class").asText(),
                "validation_depth", e.path("frozen_validation_depth").asText(),
                "mandatory", e.path("mandatory_checkpoint").asBoolean())).toList()));
        envelope.stat("deterministic_coverage", deterministicCoverage)
                .stat("edges", edgePlans.size())
                .stat("residual_fact_types", residual.size());
        ObjectNode planArtifact = StageSupport.compose(envelope, migrationPlan);
        StageSupport.validate(context, writer, "migration-plan/migration-plan.schema.json",
                "migration-plan.json", planArtifact);
        writer.write("migration-plan.json", planArtifact);

        StageSupport.toEvidence(context, "migration-plan", planArtifact,
                EvidenceManifest.Classification.INTERNAL, "SEALED_EVIDENCE", OUTPUT_DIR);

        if (!writer.validationErrors().isEmpty()) {
            return StageResult.failure(OUTPUT_DIR, ExitCode.STAGE_FAILURE,
                    "Plan artifacts failed schema validation", writer.validationErrors());
        }

        String hash = StageSupport.publish(context, writer);
        context.stateMachine().transition(RunState.PLAN_FROZEN, edgePlans.size() + " edge(s) frozen");
        context.runStateStore().updateState(context.run().runId(), RunState.PLAN_FROZEN, "plan frozen");

        Map<String, Path> artifacts = new LinkedHashMap<>();
        outputArtifacts().forEach(name -> artifacts.put(name, writer.dir().resolve(name)));

        List<String> messages = new ArrayList<>(reconciliationDecisions);
        if (!residual.isEmpty()) {
            messages.add(residual.size() + " fact type(s) have no deterministic capability; "
                    + "deterministic coverage is " + deterministicCoverage);
        }

        return new StageResult(OUTPUT_DIR, ExitCode.SUCCESS,
                edgePlans.size() + " edge(s) frozen, deterministic coverage "
                        + deterministicCoverage + ", " + requiredDimensions.size()
                        + " differential dimension(s) required",
                messages, artifacts, hash);
    }

    // ------------------------------------------------------------------ edge planning

    private ObjectNode planEdge(StageContext context, JsonNode edge, int index,
                                List<TransformationPort.Capability> capabilities,
                                Set<String> handledFactTypes,
                                Map<String, List<String>> factIdsByType,
                                double deterministicCoverage, Set<String> requiredDimensions,
                                Set<String> impactedFileIds, Set<String> impactIds,
                                boolean anyHighRisk, boolean impactRecallBelowFloor,
                                JsonNode contracts, List<String> reconciliationDecisions,
                                Set<String> testFileIds, Map<String, String> cloudTrainByBootLine,
                                Map<String, List<Integer>> javaMajorsByBootLine,
                                List<Integer> installedJdks, String currentJava) {
        ObjectNode plan = Json.obj();
        String edgeId = edge.path("edgeId").asText("EDGE-" + index);
        String edgeClass = edge.path("edgeClass").asText("MINOR");
        boolean landing = edge.path("landing").asBoolean(false);
        boolean mandatory = edge.path("mandatory").asBoolean(false);

        plan.put("edge_id", edgeId);
        plan.put("edge_class", edgeClass);
        plan.put("source_state", edge.path("fromVersion").asText());
        plan.put("target_state", edge.path("toVersion").asText());
        plan.put("landing", landing);
        plan.put("transit", !landing);
        plan.put("mandatory_checkpoint", mandatory);
        plan.put("rationale", edge.path("rationale").asText());

        // The Spring Cloud train for THIS edge, not for the landing target.
        String edgeBootLine = lineOf(edge.path("toVersion").asText(""));
        String edgeCloudTrain = cloudTrainByBootLine.get(edgeBootLine);
        plan.put("spring_cloud_train", edgeCloudTrain);
        // The Java level for THIS edge: the highest an installed toolchain can provide that the
        // edge target line supports. Carrying the landing level backwards onto a transit checkpoint
        // would set a compiler target the intermediate Spring Boot line does not accept.
        List<Integer> supportedHere = javaMajorsByBootLine.getOrDefault(edgeBootLine, List.of());
        int sourceJavaLevel = parseJavaLevel(currentJava);
        int edgeJava = installedJdks.stream()
                .filter(jdk -> supportedHere.isEmpty() || supportedHere.contains(jdk))
                .filter(jdk -> jdk >= sourceJavaLevel)
                .max(Integer::compareTo)
                .orElse(sourceJavaLevel);
        plan.put("current_java", currentJava);
        plan.put("edge_java", String.valueOf(edgeJava));
        plan.put("edge_java_note", "Highest installed JDK that Boot " + edgeBootLine
                + " supports (" + supportedHere + ") and that is not below the project level "
                + currentJava);
        plan.put("spring_cloud_train_note", edgeCloudTrain == null
                ? "No GA Spring Cloud train targets Boot " + edgeBootLine + "; the managed-version "
                  + "transformation is omitted for this edge rather than installing a mismatched train"
                : "Spring Cloud " + edgeCloudTrain + " is the train published for Boot " + edgeBootLine);

        // ---- ordered transformations for this edge
        ArrayNode transformations = Json.arr();
        for (String recipeId : recipesFor(edgeClass, edge)) {
            if (MavenPomTransformer.RECIPE_MANAGED_VERSION.equals(recipeId) && edgeCloudTrain == null) {
                continue;
            }
            TransformationPort.Capability capability = capabilities.stream()
                    .filter(c -> c.handledFactTypes().stream().anyMatch(handledFactTypes::contains))
                    .filter(c -> "AVAILABLE".equals(c.status()))
                    .findFirst().orElse(null);
            ObjectNode transformation = Json.obj();
            transformation.put("recipe_id", recipeId);
            transformation.put("preferred_transformer", providerFor(recipeId));
            transformation.put("fallback_strategy", fallbackFor(recipeId));
            transformation.put("capability_id", capability == null ? null : capability.capabilityId());
            transformation.put("deterministic", true);
            transformations.add(transformation);
        }
        plan.set("ordered_transformations", transformations);

        // ---- affected scope
        //
        // Impact findings name the files a verified migration fact points at. A preparatory
        // test-infrastructure edge exists precisely to change files no framework fact mentions, so
        // it additionally owns the test sources; without this the gateway would correctly reject
        // every JUnit 4 rewrite as out of scope.
        java.util.Set<String> edgeScope = new LinkedHashSet<>(impactedFileIds);
        if ("PREPARATORY".equals(edgeClass)) {
            edgeScope.addAll(testFileIds);
        }
        plan.set("affected_file_ids", Json.toTree(edgeScope));
        plan.put("scope_note", "PREPARATORY".equals(edgeClass)
                ? "Impact-derived scope plus every test source, because this edge migrates test "
                  + "infrastructure rather than application behaviour"
                : "Impact-derived scope plus build descriptors");
        plan.set("impact_refs", Json.toTree(impactIds));
        List<String> knowledgeRefs = new ArrayList<>();
        factIdsByType.values().forEach(knowledgeRefs::addAll);
        plan.set("knowledge_refs", Json.toTree(knowledgeRefs));

        // ---- composite transformation reconciliation (R26)
        List<TransformationPort.Capability> spanning = capabilities.stream()
                .filter(c -> "AVAILABLE".equals(c.status()))
                .filter(c -> "MULTI_EDGE".equals(c.checkpointSpan()))
                .toList();
        if (!spanning.isEmpty()) {
            if (context.policy().allowCheckpointCollapse()) {
                plan.put("checkpoint_reconciliation", "COLLAPSE_WITH_ESCALATED_VALIDATION");
                reconciliationDecisions.add("COLLAPSE_WITH_ESCALATED_VALIDATION on " + edgeId
                        + ": composite capabilities " + spanning.stream()
                        .map(TransformationPort.Capability::capabilityId).toList()
                        + " span multiple checkpoints; depth raised to the maximum required");
            } else {
                plan.put("checkpoint_reconciliation", "BLOCK");
                reconciliationDecisions.add("BLOCK on " + edgeId
                        + ": a composite transformation would skip a mandatory checkpoint and policy "
                        + "forbids collapsing");
            }
        } else {
            plan.put("checkpoint_reconciliation", "DECOMPOSED");
        }

        // ---- validation depth: computed once, frozen (spec section 22)
        ValidationDepth classDepth = switch (edgeClass) {
            case "PATCH" -> ValidationDepth.BUILD_AND_TESTS;
            case "MINOR" -> ValidationDepth.BUILD_TESTS_RUNTIME;
            case "PREPARATORY" -> ValidationDepth.IMPACTED_DIFFERENTIAL;
            case "MAJOR_BOUNDARY" -> ValidationDepth.FULL_DIFFERENTIAL;
            default -> ValidationDepth.BUILD_TESTS_RUNTIME;
        };
        ValidationDepth residualDepth =
                context.policy().escalateForResidual(classDepth, deterministicCoverage);
        ValidationDepth impactDepth = requiredDimensions.isEmpty()
                ? ValidationDepth.BUILD_AND_TESTS
                : (anyHighRisk ? ValidationDepth.FULL_DIFFERENTIAL : ValidationDepth.IMPACTED_DIFFERENTIAL);
        ValidationDepth policyDepth = impactRecallBelowFloor
                ? ValidationDepth.FULL_DIFFERENTIAL : ValidationDepth.BUILD_ONLY;
        if ("COLLAPSE_WITH_ESCALATED_VALIDATION".equals(plan.path("checkpoint_reconciliation").asText())) {
            policyDepth = ValidationDepth.FULL_DIFFERENTIAL;
        }
        ValidationDepth depth = ValidationDepth.max(classDepth, residualDepth, impactDepth, policyDepth);

        ObjectNode depthCalculation = Json.obj();
        depthCalculation.put("class_depth", classDepth.name());
        depthCalculation.put("residual_depth", residualDepth.name());
        depthCalculation.put("impact_required_depth", impactDepth.name());
        depthCalculation.put("policy_required_depth", policyDepth.name());
        depthCalculation.put("formula", "MAX(class, residual, impact, policy)");
        plan.set("validation_depth_calculation", depthCalculation);
        plan.put("frozen_validation_depth", depth.name());

        // ---- obligations derived from the frozen depth
        plan.put("tests_required", depth.requiresTests());
        plan.put("runtime_required", depth.requiresRuntime());
        plan.put("differential_required", depth.requiresDifferential());
        plan.set("differential_dimensions", depth == ValidationDepth.FULL_DIFFERENTIAL
                ? Json.toTree(requiredDimensions)
                : Json.toTree(depth.requiresDifferential() ? requiredDimensions : Set.of()));

        ArrayNode contractRefs = Json.arr();
        if (contracts != null) {
            for (JsonNode contract : contracts.path("contracts")) {
                if (requiredDimensions.contains(contract.path("dimension").asText())) {
                    contractRefs.add(contract.path("scenario_id").asText());
                }
            }
        }
        plan.set("characterization_refs", contractRefs);

        plan.put("approval_required", "MAJOR_BOUNDARY".equals(edgeClass) || anyHighRisk);
        plan.set("checkpoint_requirements", Json.toTree(List.of(
                "mig/<run>/" + edgeId + "/start",
                "mig/<run>/" + edgeId + "/transformed",
                "mig/<run>/" + edgeId + "/compiled",
                "mig/<run>/" + edgeId + "/graph-verified",
                "mig/<run>/" + edgeId + "/tested",
                "mig/<run>/" + edgeId + "/runtime-validated",
                "mig/<run>/" + edgeId + "/differential-validated")));
        plan.put("expected_residual", 1.0 - deterministicCoverage);
        return plan;
    }

    /** The ordered recipe list for an edge class. Deliberately explicit rather than discovered. */
    public static List<String> recipesFor(String edgeClass, JsonNode edge) {
        return switch (edgeClass) {
            case "PREPARATORY" -> List.of(
                    TestFrameworkTransformer.RECIPE_JUNIT4_TO_JUPITER);
            case "MAJOR_BOUNDARY" -> List.of(
                    MavenPomTransformer.RECIPE_PARENT_VERSION,
                    MavenPomTransformer.RECIPE_PROPERTY,
                    MavenPomTransformer.RECIPE_MANAGED_VERSION,
                    JakartaNamespaceTransformer.RECIPE,
                    // The Spring Cloud train moves with the Boot major, and the train is where the
                    // opt-in annotations were deleted. Ordered after the namespace rewrite so a
                    // single file is only ever rewritten by one recipe at a time.
                    RemovedAnnotationTransformer.RECIPE_REMOVE_ANNOTATION,
                    ConfigurationPropertyTransformer.RECIPE);
            case "PATCH", "MINOR" -> List.of(
                    MavenPomTransformer.RECIPE_PARENT_VERSION,
                    MavenPomTransformer.RECIPE_MANAGED_VERSION,
                    RemovedAnnotationTransformer.RECIPE_REMOVE_ANNOTATION,
                    ConfigurationPropertyTransformer.RECIPE);
            default -> List.of(MavenPomTransformer.RECIPE_PARENT_VERSION);
        };
    }

    static int parseJavaLevel(String value) {
        if (value == null || value.isBlank()) {
            return 17;
        }
        String cleaned = value.startsWith("1.") ? value.substring(2) : value;
        try {
            return Integer.parseInt(cleaned.split("\\.")[0].replaceAll("[^0-9]", ""));
        } catch (RuntimeException e) {
            return 17;
        }
    }

    static String lineOf(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String[] parts = version.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : version;
    }

    static String providerFor(String recipeId) {
        if (recipeId.startsWith("maven.")) {
            return MavenPomTransformer.PROVIDER;
        }
        if (recipeId.startsWith("jakarta.")) {
            return JakartaNamespaceTransformer.PROVIDER;
        }
        if (recipeId.startsWith("test.")) {
            return TestFrameworkTransformer.PROVIDER;
        }
        if (recipeId.startsWith("config.")) {
            return ConfigurationPropertyTransformer.PROVIDER;
        }
        return OpenRewriteCoreProbe.PROVIDER;
    }

    static String fallbackFor(String recipeId) {
        if (recipeId.startsWith("jakarta.") || recipeId.startsWith("test.")) {
            return "Report residual and let the bounded compile-repair loop handle what remains";
        }
        return "Block the edge; a build descriptor change has no safe fallback";
    }

    static Path generatedRuleFile(StageContext context) {
        return context.migrationRules().resolve("generated-properties/property-migration-rules.json");
    }
}
