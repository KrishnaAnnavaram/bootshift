package com.bootshift.tests.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Json;
import com.bootshift.stages.EdgeIndex;
import com.bootshift.stages.RunFactory;
import com.bootshift.stages.StageContext;
import com.bootshift.stages.StageExecutor;
import com.bootshift.stages.bootstrap.RunBootstrap;
import com.bootshift.stages.stage01.InventoryStage;
import com.bootshift.stages.stage18.ApprovalStage;
import com.bootshift.stages.stage19.EvidenceStage;
import com.bootshift.stages.stage20.ProvenanceStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The finalization tail: approval, evidence sealing and provenance.
 *
 * <p>These three stages decide whether a run may call itself {@code MIGRATION_COMPLETE}, and until
 * this test they were the least proven code in the repository. Every recorded run in {@code output/},
 * {@code output-run-1/} and {@code output-fresh/} stopped at stage 17 - correctly, on an unexplained
 * behavioural difference - so stages 18, 19 and 20 had never executed, and no test referenced them.
 * A defect in them would therefore have surfaced for the first time on the first migration that
 * actually succeeded, which is the worst possible moment.
 *
 * <p>Two paths are covered, because they are the two that matter:
 *
 * <ul>
 *   <li><b>Clean.</b> Nothing outstanding, so the gates evaluate, the evidence manifest is sealed and
 *       the provenance graph is built.</li>
 *   <li><b>Blocked.</b> One unexplained behavioural difference, so approval refuses to complete and
 *       evidence sealing is not even legal to attempt. Crucially the harness does not decide the gate
 *       for itself.</li>
 * </ul>
 *
 * <p>The upstream analysis and edge stages are represented by the artifacts these three actually
 * read. Almost every read is optional by design, so the seeded set is deliberately minimal: what is
 * under test is the tail, not the pipeline that feeds it.
 */
class FinalizationStagesTest {

    private static final String EDGE_ID = "EDGE-1-PATCH";

    /** The repository root, located from the working directory the surefire fork runs in. */
    private static Path harnessRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.isDirectory(candidate.resolve("schemas"))) {
            candidate = candidate.getParent();
        }
        return candidate == null ? Path.of("").toAbsolutePath() : candidate;
    }

    @Test
    @DisplayName("with nothing outstanding the tail seals evidence and builds the provenance graph")
    void cleanRunReachesEvidenceAndProvenance(@TempDir Path root) throws IOException {
        StageContext context = completedEdgeRun(root, differentialReport(0));

        // ---- 18: approval ------------------------------------------------------------------------
        StageResult approval = StageExecutor.run(new ApprovalStage(), context);
        assertThat(approval.succeeded())
                .as("approval stage failed: %s", approval.summary())
                .isTrue();

        JsonNode approvalReport = context.run().output()
                .readLatest("18-approval", "approval-report.json");
        assertThat(approvalReport).isNotNull();
        assertThat(approvalReport.path("decision_count").asInt())
                .as("the harness must not have recorded a decision nobody filed")
                .isZero();
        assertThat(context.stateMachine().current()).isEqualTo(RunState.FINAL_APPROVAL);

        // ---- 19: evidence ------------------------------------------------------------------------
        StageResult evidence = StageExecutor.run(new EvidenceStage(), context);

        JsonNode manifest = context.run().output()
                .readLatest("19-evidence", "evidence-manifest.json");
        assertThat(manifest)
                .as("evidence stage published no manifest: %s", evidence.summary())
                .isNotNull();
        // Sealed regardless of whether the run qualified as complete: the manifest records what the
        // run can and cannot prove, and a run that proved little still has to say so.
        assertThat(context.stateMachine().current())
                .isIn(RunState.EVIDENCE_SEALED, RunState.MIGRATION_COMPLETE);

        Path stageDir = context.run().output().resolveLatestDir("19-evidence");
        assertThat(stageDir).isNotNull();
        assertThat(stageDir.resolve("MIGRATION_DOCUMENT.md")).exists();
        assertThat(stageDir.resolve("migration-report.md")).exists();
        assertThat(stageDir.resolve("claims.json")).exists();

        // ---- 20: provenance ----------------------------------------------------------------------
        StageResult provenance = StageExecutor.run(new ProvenanceStage(), context);
        assertThat(provenance.succeeded())
                .as("provenance stage failed: %s", provenance.summary())
                .isTrue();

        JsonNode graph = context.run().output()
                .readLatest("20-provenance", "provenance-graph.json");
        assertThat(graph).isNotNull();
        assertThat(context.run().output().readLatest("20-provenance", "question-catalog.json"))
                .as("the provenance stage exists to answer the question catalog")
                .isNotNull();
        assertThat(context.run().output().resolveLatestDir("20-provenance")
                .resolve("blind-spots.json")).exists();
    }

    @Test
    @DisplayName("an unexplained difference blocks the tail, and the harness never approves itself")
    void unexplainedDifferenceBlocksCompletion(@TempDir Path root) throws IOException {
        StageContext context = completedEdgeRun(root, differentialReport(1));

        StageResult approval = StageExecutor.run(new ApprovalStage(), context);

        assertThat(approval.succeeded())
                .as("an unexplained behavioural difference must not pass the approval gate")
                .isFalse();
        assertThat(approval.exitCode()).isEqualTo(ExitCode.HUMAN_DECISION_REQUIRED);
        assertThat(context.stateMachine().current()).isEqualTo(RunState.NEEDS_HUMAN);

        JsonNode report = context.run().output().readLatest("18-approval", "approval-report.json");
        assertThat(report).isNotNull();
        assertThat(report.path("outstanding_requests").asInt()).isPositive();
        assertThat(report.path("decision_count").asInt())
                .as("the harness must never decide its own gate")
                .isZero();
        assertThat(report.path("rule").asText())
                .isEqualTo("An empty rationale is not a decision. The harness never approves itself.");

        // Sealing may still be *attempted* from here: StageExecutor answers a mutating-state
        // precondition by ordinal, and NEEDS_HUMAN is declared after FINAL_APPROVAL, so it compares
        // as "past" it. See the audit's F-13. What must hold regardless is that sealing does not
        // launder the outstanding gate into a completed migration, so that is what is asserted.
        StageResult evidence = StageExecutor.run(new EvidenceStage(), context);

        assertThat(evidence.succeeded())
                .as("evidence must not report success while an approval gate is outstanding")
                .isFalse();
        assertThat(context.stateMachine().current())
                .as("MIGRATION_COMPLETE must be unreachable with an outstanding gate")
                .isNotEqualTo(RunState.MIGRATION_COMPLETE);

        JsonNode result = context.run().output()
                .readLatest("19-evidence", "migration-result.json");
        assertThat(result).isNotNull();
        assertThat(result.path("status").asText()).isNotEqualTo("MIGRATION_COMPLETE");
        assertThat(result.path("outstanding_approvals").asInt()).isPositive();
    }

    @Test
    @DisplayName("landing past end of support raises a gate even under the policy that permits it")
    void endOfLifeLandingRaisesItsGate(@TempDir Path root) throws IOException {
        // production-eol-exception.json permits an end-of-life landing target, and its own rationale
        // says Agent 18 raises a SHORT_HORIZON_TARGET gate for it. It did not: the gate was
        // expressed relative to minimum_support_horizon_months, and that policy lowers the minimum
        // to -24 precisely in order to allow the landing - putting the trigger at -48 months and out
        // of reach of any real target. The one configuration that allows landing somewhere
        // unsupported was the one configuration where nobody was asked to approve it.
        Path policyFile = harnessRoot().resolve("policies/default/production-eol-exception.json");
        assertThat(policyFile).exists();

        StageContext context = completedEdgeRun(root, differentialReport(0), -2L, null, policyFile);

        StageResult approval = StageExecutor.run(new ApprovalStage(), context);

        JsonNode requests = context.run().output()
                .readLatest("18-approval", "approval-requests.json");
        assertThat(requests).isNotNull();

        boolean shortHorizonRaised = false;
        for (JsonNode request : requests.path("requests")) {
            if ("SHORT_HORIZON_TARGET".equals(request.path("gate").asText())) {
                shortHorizonRaised = true;
                assertThat(request.path("satisfied").asBoolean())
                        .as("the harness must not satisfy its own gate")
                        .isFalse();
                assertThat(request.path("summary").asText())
                        .contains("allow_eol_landing_target");
            }
        }
        assertThat(shortHorizonRaised)
                .as("a target 2 months past end of support must require a human decision")
                .isTrue();

        assertThat(approval.succeeded()).isFalse();
        assertThat(approval.exitCode()).isEqualTo(ExitCode.HUMAN_DECISION_REQUIRED);
    }

    // ------------------------------------------------------------------ fixture

    /**
     * A run that has genuinely bootstrapped and inventoried a small application, then had one edge
     * driven to completion, with the upstream artifacts the finalization stages read.
     */
    private static StageContext completedEdgeRun(Path root, ObjectNode differential)
            throws IOException {
        return completedEdgeRun(root, differential, null, "production", null);
    }

    private static StageContext completedEdgeRun(Path root, ObjectNode differential,
                                                 Long supportHorizonMonths, String policyName,
                                                 Path policyFile) throws IOException {
        Path repository = root.resolve("app");
        Files.createDirectories(repository.resolve("src/main/java/com/example"));
        Files.writeString(repository.resolve("src/main/java/com/example/App.java"), """
                package com.example;

                public class App {
                    public static void main(String[] args) {
                    }
                }
                """);
        Files.writeString(repository.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.7.18</version>
                    <relativePath/>
                  </parent>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                </project>
                """);

        StageContext context = RunFactory.create(new RunFactory.Options(
                repository, root.resolve("workspaces"), root.resolve("output"), harnessRoot(),
                policyName, policyFile, false, "managed", false, null, Map.of()));

        assertThat(new RunBootstrap(context).execute().succeeded()).isTrue();
        assertThat(StageExecutor.run(new InventoryStage(), context).succeeded()).isTrue();

        ObjectNode edgePlan = edgePlan();
        publish(context, "04-baseline", "baseline-manifest.json", baselineManifest());
        publish(context, "06-target", "target-state.json", targetState(supportHorizonMonths));
        publish(context, "11-plan", "edge-plan.json", edgePlan);
        publish(context, "11-plan", "migration-plan.json", migrationPlan());
        publish(context, "17-differential", "differential-report.json", differential);

        // The edge index is how EDGE_COMPLETE is proven; the orchestrator writes it the same way.
        EdgeIndex.open(context).seedFromPlan(edgePlan).markComplete(EDGE_ID).persist();

        context.stateMachine().restore(RunState.EDGE_COMPLETE, true, "0".repeat(64));
        return context;
    }

    /** Writes an artifact and advances the stage pointer, as a real stage would. */
    private static void publish(StageContext context, String stageDir, String name,
                                JsonNode payload) {
        OutputLayout.StageWriter writer = context.run().output().open(stageDir);
        writer.write(name, payload);
        writer.publish(context.run().runId());
    }

    private static ObjectNode baselineManifest() {
        ObjectNode node = base("04-baseline");
        node.put("sealed", true);
        node.put("baseline_manifest_hash", "0".repeat(64));
        return node;
    }

    private static ObjectNode targetState(Long supportHorizonMonths) {
        ObjectNode node = base("06-target");
        node.put("landing_version", "2.7.18");
        node.put("source_state", "2.7.18");
        // Absent by default: the stage defaults the horizon high, so no SHORT_HORIZON_TARGET gate
        // is raised and the clean path stays clean.
        if (supportHorizonMonths != null) {
            node.put("support_horizon_months", supportHorizonMonths);
        }
        return node;
    }

    private static ObjectNode edgePlan() {
        ObjectNode edge = Json.obj();
        edge.put("edge_id", EDGE_ID);
        edge.put("edge_class", "PATCH");
        edge.put("source_state", "2.7.12");
        edge.put("target_state", "2.7.18");
        edge.put("frozen_validation_depth", "BUILD_AND_TESTS");
        edge.set("ordered_transformations", Json.arr());
        edge.set("affected_file_ids", Json.arr());

        ObjectNode plan = base("11-plan");
        ArrayNode edges = Json.arr();
        edges.add(edge);
        plan.set("edges", edges);
        return plan;
    }

    private static ObjectNode migrationPlan() {
        ObjectNode plan = base("11-plan");
        plan.put("edge_count", 1);
        return plan;
    }

    /** A differential report with {@code unexplained} comparisons that could not be explained. */
    private static ObjectNode differentialReport(int unexplained) {
        ObjectNode report = base("17-differential");
        report.put("edge_id", EDGE_ID);
        ArrayNode comparisons = Json.arr();

        ObjectNode identical = Json.obj();
        identical.put("scenario_id", "SCN-00001");
        identical.put("dimension", "HTTP_API");
        identical.put("module", "app");
        identical.put("classification", "IDENTICAL");
        identical.put("detail", "no difference");
        comparisons.add(identical);

        for (int i = 0; i < unexplained; i++) {
            ObjectNode node = Json.obj();
            node.put("scenario_id", "SCN-9000" + i);
            node.put("dimension", "CONFIGURATION_BINDING");
            node.put("module", "app");
            node.put("classification", "UNEXPLAINED");
            node.put("detail", "a configuration property changed and no verified fact explains it");
            comparisons.add(node);
        }

        report.set("comparisons", comparisons);
        ObjectNode counts = Json.obj();
        counts.put("IDENTICAL", 1);
        counts.put("UNEXPLAINED", unexplained);
        report.set("classification_counts", counts);
        report.put("unexplained", unexplained);
        return report;
    }

    private static ObjectNode base(String stage) {
        ObjectNode node = Json.obj();
        node.put("pipeline_stage", stage);
        node.put("schema_version", "1.0.0");
        node.set("gaps", Json.arr());
        node.set("blind_spots", Json.arr());
        return node;
    }
}
