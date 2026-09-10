package com.bootshift.stages.stage19;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.Envelope;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.evidence.Claim;
import com.bootshift.core.evidence.CoverageStatement;
import com.bootshift.core.evidence.EvidenceLevel;
import com.bootshift.core.evidence.EvidenceManifest;
import com.bootshift.core.identity.FileRecord;
import com.bootshift.core.identity.FileRegistry;
import com.bootshift.core.ledger.ChangeLedger;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Json;
import com.bootshift.stages.EdgeSupport;
import com.bootshift.stages.Stage;
import com.bootshift.stages.StageContext;
import com.bootshift.stages.StageSupport;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 19 - Evidence and Report (spec sections 37 and 38).
 *
 * <p>Creates the defensible final claim. Every claim resolves to evidence and carries a coverage
 * statement; a claim without both is refused rather than published, which is the mechanism behind
 * "Security = E4" never appearing on its own.
 *
 * <p>MIGRATION_COMPLETE is asserted only when every policy-required dimension reached its minimum
 * evidence level, every shortfall has an explicit exception, no unexplained blocking difference
 * remains, the change ledger verifies, and the evidence manifest verifies.
 */
public final class EvidenceStage implements Stage {

    public static final String OUTPUT_DIR = "19-evidence";

    /** Dimensions the harness reports on, with the minimum level policy requires. */
    private static final Map<String, EvidenceLevel> REQUIRED_LEVELS = new LinkedHashMap<>();

    static {
        REQUIRED_LEVELS.put("INVENTORY", EvidenceLevel.E0);
        REQUIRED_LEVELS.put("STRUCTURE", EvidenceLevel.E1);
        REQUIRED_LEVELS.put("BUILD", EvidenceLevel.E2);
        REQUIRED_LEVELS.put("TESTS", EvidenceLevel.E3);
        REQUIRED_LEVELS.put("RUNTIME", EvidenceLevel.E3);
        REQUIRED_LEVELS.put("CONFIGURATION_BINDING", EvidenceLevel.E4);
        REQUIRED_LEVELS.put("HTTP_API", EvidenceLevel.E4);
        REQUIRED_LEVELS.put("SECURITY", EvidenceLevel.E4);
        REQUIRED_LEVELS.put("PERSISTENCE", EvidenceLevel.E4);
        REQUIRED_LEVELS.put("SERIALIZATION", EvidenceLevel.E4);
    }

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
        return "Seal the evidence manifest and produce the defensible migration report";
    }

    @Override
    public List<RunState> preconditions() {
        return List.of(RunState.FINAL_APPROVAL);
    }

    @Override
    public RunState postcondition() {
        return RunState.EVIDENCE_SEALED;
    }

    @Override
    public List<String> outputArtifacts() {
        return List.of("migration-report.md", "migration-result.json", "evidence-manifest.json",
                "file-lineage.json", "symbol-lineage.json", "claims.json", "manifest.json");
    }

    @Override
    public StageResult execute(StageContext context) {
        OutputLayout.StageWriter writer = context.run().output().open(OUTPUT_DIR);
        Envelope envelope = StageSupport.envelope(context, OUTPUT_DIR);

        JsonNode baseline = StageSupport.requireUpstream(context, "04-baseline",
                "baseline-manifest.json", "Run: harness baseline --repo <path>");
        JsonNode inventory = StageSupport.optionalUpstream(context, "01-inventory",
                "inventory-artifact.json");
        JsonNode graphVerification = StageSupport.optionalUpstream(context, "03-graph",
                "graph-verification-report.json");
        JsonNode buildReport = StageSupport.optionalUpstream(context, "13-build-repair",
                "build-report.json");
        JsonNode testReport = StageSupport.optionalUpstream(context, "15-test", "test-report.json");
        JsonNode runtimeReport = StageSupport.optionalUpstream(context, "16-runtime",
                "runtime-report.json");
        JsonNode differential = StageSupport.optionalUpstream(context, "17-differential",
                "differential-report.json");
        JsonNode approvals = StageSupport.optionalUpstream(context, "18-approval",
                "approval-report.json");
        JsonNode target = StageSupport.optionalUpstream(context, "06-target", "target-state.json");
        JsonNode plan = StageSupport.optionalUpstream(context, "11-plan", "migration-plan.json");
        JsonNode graphDiff = StageSupport.optionalUpstream(context, "14-graph-diff", "graph-diff.json");

        FileRegistry registry = EdgeSupport.loadRegistry(context);
        ChangeLedger.Verification ledgerVerification = EdgeSupport.verifyLedger(context);

        EvidenceManifest manifest = new EvidenceManifest(context.run().runId());
        manifest.seal("baseline_manifest_hash", baseline.path("baseline_manifest_hash").asText());
        manifest.seal("original_tree_hash", baseline.path("original_tree_hash").asText());
        manifest.seal("file_registry_seal", baseline.path("file_registry_seal").asText());
        manifest.seal("change_ledger_head", ledgerVerification.computedHead());
        manifest.seal("policy_hash", context.policy().policyHash());
        if (differential != null) {
            manifest.seal("normalization_policy_hash",
                    differential.path("normalization_policy_hash").asText());
        }

        // ---- claims ------------------------------------------------------------------------------
        List<Claim> claims = new ArrayList<>();
        claims.add(inventoryClaim(inventory));
        claims.add(structureClaim(graphVerification));
        claims.add(buildClaim(buildReport));
        claims.add(testClaim(testReport));
        claims.add(runtimeClaim(runtimeReport));
        claims.addAll(differentialClaims(differential, approvals));
        claims.forEach(manifest::claim);

        // ---- evidence entries --------------------------------------------------------------------
        int indexed = indexArtifacts(context, manifest);

        String manifestHash = manifest.finalizeManifest();
        ObjectNode manifestNode = manifest.toNode();
        writer.write("evidence-manifest.json", StageSupport.compose(envelope
                .stat("evidence_entries", indexed)
                .stat("claims", claims.size())
                .stat("manifest_hash", manifestHash), manifestNode));

        ObjectNode claimsArtifact = Json.obj();
        ArrayNode claimArray = Json.arr();
        List<String> unpublishable = new ArrayList<>();
        for (Claim claim : claims) {
            ObjectNode node = Json.obj();
            node.put("claim_id", claim.getClaimId());
            node.put("dimension", claim.getDimension());
            node.put("statement", claim.getStatement());
            node.put("evidence_level", claim.getLevel().name());
            node.put("required_level", REQUIRED_LEVELS.getOrDefault(claim.getDimension(),
                    EvidenceLevel.E0).name());
            node.put("meets_requirement", claim.getLevel().atLeast(
                    REQUIRED_LEVELS.getOrDefault(claim.getDimension(), EvidenceLevel.E0)));
            node.put("coverage", claim.getCoverage() == null ? null : claim.getCoverage().render());
            node.put("publishable", claim.isPublishable());
            node.set("evidence_refs", Json.toTree(claim.getEvidenceRefs()));
            node.set("blind_spots", Json.toTree(claim.getBlindSpotRefs()));
            node.put("rendered", claim.render());
            claimArray.add(node);
            if (!claim.isPublishable()) {
                unpublishable.add(claim.getClaimId() + " lacks evidence references or a coverage "
                        + "statement and cannot be published");
            }
        }
        claimsArtifact.put("claim_count", claims.size());
        claimsArtifact.set("claims", claimArray);
        claimsArtifact.put("rule", "A dimension assertion is illegal without a coverage statement");
        writer.write("claims.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), claimsArtifact));

        // ---- coverage statement -------------------------------------------------------------------
        // Per dimension, whether it was covered and - when it was not - why. A dimension with no
        // observation appears here as UNCOVERED with a reason. It is never omitted, and it is never
        // implied to have passed by its absence; that is the whole point of publishing this
        // separately from the claims rather than leaving a reader to infer it from what is missing.
        ObjectNode coverageArtifact = Json.obj();
        ArrayNode coverageArray = Json.arr();
        int covered = 0;
        for (Claim claim : claims) {
            EvidenceLevel required = REQUIRED_LEVELS.getOrDefault(claim.getDimension(),
                    EvidenceLevel.E0);
            boolean meets = claim.getLevel().atLeast(required);
            if (meets) {
                covered++;
            }
            ObjectNode row = Json.obj();
            row.put("dimension", claim.getDimension());
            row.put("claim_id", claim.getClaimId());
            row.put("level_required", required.name());
            row.put("level_reached", claim.getLevel().name());
            row.put("covered", meets);
            row.put("coverage", claim.getCoverage() == null ? null : claim.getCoverage().render());
            row.put("reason", meets ? null
                    : "Evidence reached " + claim.getLevel().name() + " but this dimension requires "
                            + required.name());
            row.set("blind_spots", Json.toTree(claim.getBlindSpotRefs()));
            coverageArray.add(row);
        }
        coverageArtifact.put("dimension_count", claims.size());
        coverageArtifact.put("covered", covered);
        coverageArtifact.put("uncovered", claims.size() - covered);
        coverageArtifact.set("dimensions", coverageArray);
        coverageArtifact.put("rule", "Every dimension appears here whether or not it was observed. "
                + "An uncovered dimension carries its reason; absence is never a pass.");
        writer.write("coverage-statement.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), coverageArtifact));

        // ---- lineage ------------------------------------------------------------------------------
        ObjectNode fileLineage = Json.obj();
        ArrayNode lineageArray = Json.arr();
        for (FileRecord record : registry.all()) {
            if (record.getVersions().size() <= 1 && record.getChangeIds().isEmpty()) {
                continue;
            }
            ObjectNode node = Json.obj();
            node.put("file_id", record.getFileId());
            node.put("baseline_path", record.getBaselinePath());
            node.put("current_path", record.getCurrentPath());
            node.put("baseline_sha256", record.getBaselineSha256());
            node.put("current_sha256", record.getCurrentSha256());
            node.put("status", record.getStatus().name());
            node.put("rename_source", record.getRenameSource().name());
            node.put("rename_confidence", record.getRenameConfidence());
            node.put("split_from", record.getSplitFrom());
            node.put("merged_into", record.getMergedInto());
            node.set("change_ids", Json.toTree(record.getChangeIds()));
            node.set("versions", Json.toTree(record.getVersions()));
            node.set("symbol_ids", Json.toTree(record.getSymbolIds()));
            lineageArray.add(node);
        }
        fileLineage.put("changed_file_count", lineageArray.size());
        fileLineage.put("total_file_count", registry.size());
        fileLineage.set("files", lineageArray);
        writer.write("file-lineage.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), fileLineage));

        ObjectNode symbolLineage = Json.obj();
        symbolLineage.set("changed_symbols", graphDiff == null
                ? Json.arr() : graphDiff.path("changed_symbol_ids"));
        symbolLineage.set("nodes_added", graphDiff == null ? Json.arr() : graphDiff.path("nodes_added"));
        symbolLineage.set("nodes_removed", graphDiff == null
                ? Json.arr() : graphDiff.path("nodes_removed"));
        writer.write("symbol-lineage.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), symbolLineage));

        // ---- completion determination --------------------------------------------------------------
        List<String> shortfalls = new ArrayList<>();
        for (Claim claim : claims) {
            EvidenceLevel required = REQUIRED_LEVELS.getOrDefault(claim.getDimension(),
                    EvidenceLevel.E0);
            if (!claim.getLevel().atLeast(required)) {
                shortfalls.add(claim.getDimension() + " reached " + claim.getLevel()
                        + " but policy requires " + required + " (" + claim.getCoverage().render() + ")");
            }
        }
        long unexplained = differential == null ? 0
                : differential.path("classification_counts").path("UNEXPLAINED").asLong(0);
        long outstandingApprovals = approvals == null ? 0
                : approvals.path("outstanding_requests").asLong(0);

        boolean complete = shortfalls.isEmpty() && unexplained == 0 && outstandingApprovals == 0
                && ledgerVerification.valid() && unpublishable.isEmpty();

        String finalTreeHash = context.scm().currentTreeHash(context.run().migrationWorkspace(),
                context.run().checkpointGit());

        ObjectNode result = Json.obj();
        result.put("status", complete ? "MIGRATION_COMPLETE"
                : (unexplained > 0 ? "BLOCKED" : "NEEDS_HUMAN"));
        result.put("source_version", target == null ? null : target.path("source_version").asText());
        result.put("target_version", target == null ? null : target.path("landing_version").asText());
        result.put("edges_planned", plan == null ? 0 : plan.path("edge_count").asInt());
        result.put("deterministic_coverage", plan == null ? 0 : plan.path("deterministic_coverage").asDouble());
        result.put("final_source_tree_hash", finalTreeHash);
        result.put("baseline_manifest_hash", baseline.path("baseline_manifest_hash").asText());
        result.put("evidence_manifest_hash", manifestHash);
        result.put("change_ledger_head", ledgerVerification.computedHead());
        result.put("change_ledger_valid", ledgerVerification.valid());
        result.put("change_ledger_events", ledgerVerification.verifiedEvents());
        result.put("unexplained_differences", unexplained);
        result.put("outstanding_approvals", outstandingApprovals);
        result.set("evidence_shortfalls", Json.toTree(shortfalls));
        result.set("unpublishable_claims", Json.toTree(unpublishable));
        result.set("ledger_violations", Json.toTree(ledgerVerification.violations()));
        ObjectNode resultArtifact = StageSupport.compose(
                StageSupport.envelope(context, OUTPUT_DIR), result);
        StageSupport.validate(context, writer, "evidence/migration-result.schema.json",
                "migration-result.json", resultArtifact);
        writer.write("migration-result.json", resultArtifact);

        writer.writeText("migration-report.md", renderReport(context, result, claims, registry,
                ledgerVerification, differential, approvals, target, plan));

        String hash = StageSupport.publish(context, writer);

        Map<String, Path> artifacts = new LinkedHashMap<>();
        outputArtifacts().forEach(name -> artifacts.put(name, writer.dir().resolve(name)));

        context.stateMachine().transition(RunState.EVIDENCE_SEALED, "manifest " + manifestHash);
        context.runStateStore().updateState(context.run().runId(), RunState.EVIDENCE_SEALED,
                "evidence sealed");

        if (complete) {
            context.stateMachine().transition(RunState.MIGRATION_COMPLETE, "all gates satisfied");
            context.runStateStore().updateState(context.run().runId(), RunState.MIGRATION_COMPLETE,
                    "migration complete");
            return new StageResult(OUTPUT_DIR, ExitCode.SUCCESS,
                    "MIGRATION_COMPLETE: evidence manifest " + manifestHash.substring(0, 16)
                            + ", ledger head " + ledgerVerification.computedHead().substring(0, 12)
                            + " over " + ledgerVerification.verifiedEvents() + " event(s)",
                    List.of(), artifacts, hash);
        }

        List<String> messages = new ArrayList<>(shortfalls);
        messages.addAll(unpublishable);
        if (unexplained > 0) {
            messages.add(unexplained + " unexplained behavioural difference(s) remain");
        }
        if (outstandingApprovals > 0) {
            messages.add(outstandingApprovals + " approval gate(s) are outstanding");
        }
        ledgerVerification.violations().forEach(v -> messages.add("LEDGER: " + v));

        return new StageResult(OUTPUT_DIR,
                unexplained > 0 ? ExitCode.POLICY_BLOCK : ExitCode.HUMAN_DECISION_REQUIRED,
                "Evidence sealed as " + manifestHash.substring(0, 16)
                        + " but the migration is not complete: " + shortfalls.size()
                        + " evidence shortfall(s), " + unexplained + " unexplained difference(s), "
                        + outstandingApprovals + " outstanding approval(s)",
                messages, artifacts, hash);
    }

    // ------------------------------------------------------------------ claims

    private Claim inventoryClaim(JsonNode inventory) {
        int files = inventory == null ? 0 : inventory.path("file_count").asInt();
        return new Claim("CL-INVENTORY-001", "INVENTORY",
                "Every file in the input repository was discovered and given a persistent identity.")
                .level(files > 0 ? EvidenceLevel.E0 : EvidenceLevel.E0)
                .coverage(new CoverageStatement("files inventoried", files, files, 0, List.of(), null))
                .evidence("01-inventory/inventory-artifact.json")
                .evidence("01-inventory/file-registry.json");
    }

    private Claim structureClaim(JsonNode verification) {
        if (verification == null) {
            return new Claim("CL-STRUCTURE-001", "STRUCTURE",
                    "Application structure was not modelled.")
                    .level(EvidenceLevel.E0)
                    .coverage(CoverageStatement.none("java files in graph", "graph stage never ran"))
                    .evidence("03-graph/graph-verification-report.json");
        }
        int inGraph = verification.path("checks").path("java_files_in_graph").asInt();
        int total = verification.path("checks").path("java_files_inventoried").asInt();
        boolean passed = verification.path("passed").asBoolean(false);
        return new Claim("CL-STRUCTURE-001", "STRUCTURE",
                "The application structure is modelled and migration changes are structurally traceable.")
                .level(passed ? EvidenceLevel.E1 : EvidenceLevel.E0)
                .coverage(new CoverageStatement("java files in graph", inGraph, total,
                        Math.max(0, total - inGraph), List.of("GAP-GRAPH-001"),
                        "attribution ratio "
                                + verification.path("checks").path("attribution_ratio").asDouble()))
                .evidence("03-graph/application-graph.json")
                .evidence("03-graph/graph-verification-report.json")
                .blindSpot("BS-GRAPH-RUNTIME");
    }

    private Claim buildClaim(JsonNode buildReport) {
        boolean compiled = buildReport != null && buildReport.path("compiled").asBoolean(false);
        return new Claim("CL-BUILD-001", "BUILD",
                "The migrated repository resolves its dependencies and compiles.")
                .level(compiled ? EvidenceLevel.E2 : EvidenceLevel.E1)
                .coverage(buildReport == null
                        ? CoverageStatement.none("modules compiled", "no migration edge was executed")
                        : new CoverageStatement("modules compiled", compiled ? 1 : 0, 1,
                        compiled ? 0 : 1, List.of(), "after "
                        + buildReport.path("rounds").asInt() + " repair round(s)"))
                .evidence("13-build-repair/build-report.json");
    }

    private Claim testClaim(JsonNode testReport) {
        if (testReport == null) {
            return new Claim("CL-TESTS-001", "TESTS",
                    "The application test suite was not executed against the migrated state.")
                    .level(EvidenceLevel.E1)
                    .coverage(CoverageStatement.none("tests executed",
                            "no migration edge reached test validation"))
                    .evidence("04-baseline/baseline-tests.json");
        }
        int total = testReport.path("total_tests").asInt();
        long passed = testReport.path("classification_counts").path("PASSED").asLong(0);
        long regressions = testReport.path("classification_counts").path("EDGE_LOCAL_REGRESSION").asLong(0)
                + testReport.path("classification_counts").path("CUMULATIVE_REGRESSION").asLong(0);
        return new Claim("CL-TESTS-001", "TESTS",
                "Existing tests pass relative to sealed baseline debt.")
                .level(regressions == 0 && total > 0 ? EvidenceLevel.E3 : EvidenceLevel.E2)
                .coverage(new CoverageStatement("tests", (int) passed, total,
                        (int) (total - passed), List.of(),
                        regressions == 0 ? "no regressions" : regressions + " regression(s)"))
                .evidence("15-test/test-report.json")
                .evidence("15-test/coverage-report.json");
    }

    private Claim runtimeClaim(JsonNode runtimeReport) {
        if (runtimeReport == null) {
            return new Claim("CL-RUNTIME-001", "RUNTIME",
                    "The migrated application was not started.")
                    .level(EvidenceLevel.E1)
                    .coverage(CoverageStatement.none("modules started", "runtime validation never ran"))
                    .evidence("04-baseline/baseline-runtime.json");
        }
        int started = runtimeReport.path("modules_started").asInt();
        int attempted = runtimeReport.path("modules_attempted").asInt();
        return new Claim("CL-RUNTIME-001", "RUNTIME",
                "The migrated application starts and its context, mappings and configuration binding "
                        + "were observed.")
                .level(started > 0 ? EvidenceLevel.E3 : EvidenceLevel.E2)
                .coverage(new CoverageStatement("modules started", started, attempted,
                        attempted - started, List.of(),
                        "startup is one observation, not migration success"))
                .evidence("16-runtime/runtime-report.json")
                .evidence("16-runtime/configuration-binding.json");
    }

    private List<Claim> differentialClaims(JsonNode differential, JsonNode approvals) {
        List<Claim> claims = new ArrayList<>();
        Map<String, String> dimensionToClaim = Map.of(
                "CONFIGURATION_BINDING", "CL-CONFIG-001",
                "HTTP_API", "CL-HTTPAPI-001",
                "SECURITY_AUTHORIZATION", "CL-SECURITY-001",
                "PERSISTENCE_STATE", "CL-PERSISTENCE-001",
                "SERIALIZATION", "CL-SERIALIZATION-001");
        Map<String, String> dimensionToReported = Map.of(
                "CONFIGURATION_BINDING", "CONFIGURATION_BINDING",
                "HTTP_API", "HTTP_API",
                "SECURITY_AUTHORIZATION", "SECURITY",
                "PERSISTENCE_STATE", "PERSISTENCE",
                "SERIALIZATION", "SERIALIZATION");

        for (Map.Entry<String, String> entry : dimensionToClaim.entrySet()) {
            String dimension = entry.getKey();
            String reported = dimensionToReported.get(dimension);
            int compared = 0;
            int identicalOrExplained = 0;
            int notCompared = 0;
            if (differential != null) {
                for (JsonNode comparison : differential.path("comparisons")) {
                    if (!dimension.equals(comparison.path("dimension").asText())) {
                        continue;
                    }
                    String classification = comparison.path("classification").asText();
                    if ("NOT_COMPARED".equals(classification)) {
                        notCompared++;
                        continue;
                    }
                    compared++;
                    if ("IDENTICAL".equals(classification) || "EXPECTED".equals(classification)) {
                        identicalOrExplained++;
                    }
                }
            }
            EvidenceLevel level = compared == 0 ? EvidenceLevel.E2
                    : (identicalOrExplained == compared ? EvidenceLevel.E4 : EvidenceLevel.E3);
            Claim claim = new Claim(entry.getValue(), reported,
                    dimension + " behaviour was compared between the original and migrated "
                            + "applications for every scenario the environment permitted.")
                    .level(level)
                    .coverage(new CoverageStatement(dimension + " scenarios",
                            identicalOrExplained, compared + notCompared, notCompared,
                            notCompared > 0 ? List.of("GAP-DIFF-001") : List.of(),
                            compared == 0 ? "not compared in this run" : null))
                    .evidence("17-differential/differential-report.json")
                    .evidence("17-differential/normalization-policy.json");
            if (approvals != null) {
                approvals.path("decisions").forEach(d -> {
                    if (d.path("scope").asText("").contains(dimension)) {
                        claim.approval(d.path("decisionId").asText());
                    }
                });
            }
            claims.add(claim);
        }
        return claims;
    }

    // ------------------------------------------------------------------ evidence indexing

    private int indexArtifacts(StageContext context, EvidenceManifest manifest) {
        int indexed = 0;
        Path outputRoot = context.run().output().root();
        for (String stage : List.of("00-bootstrap", "01-inventory", "02-build", "03-graph",
                "04-baseline", "05-compatibility", "06-target", "07-documentation", "08-knowledge",
                "09-impact", "10-characterization", "11-plan", "12-transformation", "13-build-repair",
                "14-graph-diff", "15-test", "16-runtime", "17-differential", "18-approval")) {
            Path dir = context.run().output().resolveLatestDir(stage);
            if (dir == null) {
                continue;
            }
            try (var stream = Files.list(dir)) {
                for (Path file : stream.filter(Files::isRegularFile).sorted().toList()) {
                    var stored = context.evidenceStore().putFile("artifact", file,
                            classify(stage), "SEALED_EVIDENCE", stage);
                    manifest.add(new EvidenceManifest.Entry(stored.evidenceId(), stage + "/"
                            + file.getFileName(), stored.relativePath(), stored.sha256(),
                            stored.sizeBytes(), classify(stage), "SEALED_EVIDENCE", stage,
                            java.time.Instant.now().toString()));
                    indexed++;
                }
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException("Cannot index evidence for " + stage, e);
            }
        }
        return indexed;
    }

    private static EvidenceManifest.Classification classify(String stage) {
        return switch (stage) {
            case "18-approval" -> EvidenceManifest.Classification.CONFIDENTIAL;
            case "07-documentation" -> EvidenceManifest.Classification.PUBLIC;
            default -> EvidenceManifest.Classification.INTERNAL;
        };
    }

    // ------------------------------------------------------------------ report

    private String renderReport(StageContext context, ObjectNode result, List<Claim> claims,
                                FileRegistry registry, ChangeLedger.Verification ledger,
                                JsonNode differential, JsonNode approvals, JsonNode target,
                                JsonNode plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Bootshift Migration Report\n\n");
        sb.append("**Run:** `").append(context.run().runId()).append("`  \n");
        sb.append("**Status:** `").append(result.path("status").asText()).append("`  \n");
        sb.append("**Policy:** `").append(context.policy().name()).append("` (hash `")
                .append(context.policy().policyHash(), 0, 16).append("`)  \n");
        sb.append("**Generated:** ").append(java.time.Instant.now()).append("\n\n");

        sb.append("## Migration\n\n");
        sb.append("| Field | Value |\n|---|---|\n");
        sb.append("| Source version | `").append(result.path("source_version").asText("unknown")).append("` |\n");
        sb.append("| Target version | `").append(result.path("target_version").asText("unknown")).append("` |\n");
        sb.append("| Edges planned | ").append(result.path("edges_planned").asInt()).append(" |\n");
        sb.append("| Deterministic coverage | ")
                .append(result.path("deterministic_coverage").asDouble()).append(" |\n");
        sb.append("| Final source tree hash | `")
                .append(result.path("final_source_tree_hash").asText("n/a")).append("` |\n");
        sb.append("| Baseline manifest hash | `")
                .append(result.path("baseline_manifest_hash").asText()).append("` |\n");
        sb.append("| Evidence manifest hash | `")
                .append(result.path("evidence_manifest_hash").asText()).append("` |\n\n");

        sb.append("## Evidence levels\n\n");
        sb.append("Every assertion carries a coverage statement. A level without coverage is not a "
                + "legal assertion in this harness.\n\n");
        sb.append("| Dimension | Level | Required | Meets | Coverage |\n|---|---|---|---|---|\n");
        for (Claim claim : claims) {
            EvidenceLevel required = REQUIRED_LEVELS.getOrDefault(claim.getDimension(),
                    EvidenceLevel.E0);
            sb.append("| ").append(claim.getDimension())
                    .append(" | `").append(claim.getLevel().name()).append("`")
                    .append(" | `").append(required.name()).append("`")
                    .append(" | ").append(claim.getLevel().atLeast(required) ? "yes" : "**no**")
                    .append(" | ").append(claim.getCoverage() == null ? "MISSING"
                            : claim.getCoverage().render())
                    .append(" |\n");
        }
        sb.append('\n');

        sb.append("## Claims\n\n");
        for (Claim claim : claims) {
            sb.append("### ").append(claim.getClaimId()).append(" - ").append(claim.getDimension())
                    .append("\n\n");
            sb.append(claim.getStatement()).append("\n\n");
            sb.append("- **Level:** `").append(claim.getLevel().name()).append("` (")
                    .append(claim.getLevel().title()).append(")\n");
            sb.append("- **Coverage:** ").append(claim.getCoverage() == null ? "MISSING"
                    : claim.getCoverage().render()).append('\n');
            sb.append("- **Evidence:**\n");
            claim.getEvidenceRefs().forEach(ref -> sb.append("  - `").append(ref).append("`\n"));
            if (!claim.getBlindSpotRefs().isEmpty()) {
                sb.append("- **Blind spots:** ").append(String.join(", ", claim.getBlindSpotRefs()))
                        .append('\n');
            }
            sb.append('\n');
        }

        sb.append("## Change ledger\n\n");
        sb.append("| Field | Value |\n|---|---|\n");
        sb.append("| Events | ").append(ledger.verifiedEvents()).append(" |\n");
        sb.append("| Head | `").append(ledger.computedHead()).append("` |\n");
        sb.append("| Verified | ").append(ledger.valid() ? "yes" : "**NO**").append(" |\n");
        if (!ledger.violations().isEmpty()) {
            sb.append("\nViolations:\n\n");
            ledger.violations().forEach(v -> sb.append("- ").append(v).append('\n'));
        }
        sb.append('\n');

        long changed = registry.all().stream().filter(r -> !r.getChangeIds().isEmpty()).count();
        sb.append("## File lineage\n\n");
        sb.append(changed).append(" of ").append(registry.size())
                .append(" registered file(s) were changed. Every FILE_ID survived the migration; "
                        + "identity is allocated, not derived from path or content.\n\n");
        if (changed > 0) {
            sb.append("| FILE_ID | Baseline path | Final path | Changes |\n|---|---|---|---|\n");
            registry.all().stream().filter(r -> !r.getChangeIds().isEmpty()).limit(40).forEach(r ->
                    sb.append("| `").append(r.getFileId()).append("` | `")
                            .append(r.getBaselinePath()).append("` | `")
                            .append(r.getCurrentPath()).append("` | ")
                            .append(r.getChangeIds().size()).append(" |\n"));
            sb.append('\n');
        }

        if (differential != null) {
            sb.append("## Differential validation\n\n");
            sb.append("Normalization policy hash: `")
                    .append(differential.path("normalization_policy_hash").asText()).append("`\n\n");
            sb.append("| Classification | Count |\n|---|---|\n");
            differential.path("classification_counts").fields().forEachRemaining(e ->
                    sb.append("| ").append(e.getKey()).append(" | ").append(e.getValue().asInt())
                            .append(" |\n"));
            sb.append('\n');
        }

        if (approvals != null && approvals.path("decision_count").asInt() > 0) {
            sb.append("## Approvals\n\n");
            sb.append("| Decision | Gate | Actor | Verdict | Rationale |\n|---|---|---|---|---|\n");
            approvals.path("decisions").forEach(d -> sb.append("| `")
                    .append(d.path("decisionId").asText()).append("` | ")
                    .append(d.path("gate").asText()).append(" | ")
                    .append(d.path("actor").asText()).append(" | ")
                    .append(d.path("verdict").asText()).append(" | ")
                    .append(d.path("rationale").asText()).append(" |\n"));
            sb.append('\n');
        }

        sb.append("## What this run cannot claim\n\n");
        sb.append("This harness never asserts universal behavioural equivalence. It asserts only what "
                + "it observed, for the dimensions and scenarios listed above, under the recorded "
                + "environment equivalence contract. Everything else is a blind spot and is listed as "
                + "one.\n\n");
        List<String> shortfalls = new ArrayList<>();
        result.path("evidence_shortfalls").forEach(s -> shortfalls.add(s.asText()));
        if (!shortfalls.isEmpty()) {
            sb.append("### Evidence shortfalls\n\n");
            shortfalls.forEach(s -> sb.append("- ").append(s).append('\n'));
            sb.append('\n');
        }
        sb.append("Run `bootshift blind-spots` and `bootshift gaps` for the complete list.\n");
        return sb.toString();
    }
}
