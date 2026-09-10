package com.bootshift.stages;

import com.fasterxml.jackson.databind.JsonNode;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.StageResult;
import com.bootshift.stages.bootstrap.RunBootstrap;
import com.bootshift.stages.stage01.InventoryStage;
import com.bootshift.stages.stage02.BuildResolverStage;
import com.bootshift.stages.stage03.ApplicationGraphStage;
import com.bootshift.stages.stage04.BaselineStage;
import com.bootshift.stages.stage05.CompatibilityStage;
import com.bootshift.stages.stage06.TargetResolverStage;
import com.bootshift.stages.stage07.DocumentationStage;
import com.bootshift.stages.stage08.KnowledgeStage;
import com.bootshift.stages.stage09.ImpactStage;
import com.bootshift.stages.stage10.CharacterizationStage;
import com.bootshift.stages.stage11.PlannerStage;
import com.bootshift.stages.stage12.TransformationStage;
import com.bootshift.stages.stage13.BuildRepairStage;
import com.bootshift.stages.stage14.GraphDiffStage;
import com.bootshift.stages.stage15.TestValidationStage;
import com.bootshift.stages.stage16.RuntimeValidationStage;
import com.bootshift.stages.stage17.DifferentialStage;
import com.bootshift.stages.stage18.ApprovalStage;
import com.bootshift.stages.stage19.EvidenceStage;
import com.bootshift.stages.stage20.ProvenanceStage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Thin orchestrator (R24).
 *
 * <p>It sequences stages and enforces the edge loop. It contains no migration semantics of its own:
 * deleting this class would remove the convenience of {@code harness run}, and nothing else. Every
 * stage remains independently invocable from the CLI.
 */
public final class PipelineOrchestrator {

    /** What the orchestrator did, so the CLI can report it without re-deriving anything. */
    public record RunOutcome(List<StageResult> results, ExitCode exitCode, String summary) {
    }

    private final StageContext context;
    private final Consumer<StageResult> reporter;

    public PipelineOrchestrator(StageContext context, Consumer<StageResult> reporter) {
        this.context = context;
        this.reporter = reporter;
    }

    /** The non-mutating analysis half: bootstrap through frozen plan (milestones M1 to M10). */
    public List<Stage> analysisStages(String requestedTarget) {
        List<Stage> stages = new ArrayList<>();
        stages.add(new InventoryStage());
        stages.add(new BuildResolverStage());
        stages.add(new ApplicationGraphStage());
        stages.add(new BaselineStage());
        stages.add(new CompatibilityStage());
        stages.add(new TargetResolverStage(requestedTarget));
        stages.add(new DocumentationStage());
        stages.add(new KnowledgeStage());
        stages.add(new ImpactStage());
        stages.add(new CharacterizationStage());
        stages.add(new PlannerStage());
        return stages;
    }

    /** The per-edge mutating loop (spec section 34). */
    public List<Stage> edgeStages(String edgeId) {
        return List.of(
                new TransformationStage(edgeId),
                new BuildRepairStage(edgeId),
                new GraphDiffStage(edgeId),
                new TestValidationStage(edgeId),
                new RuntimeValidationStage(edgeId),
                new DifferentialStage(edgeId));
    }

    public List<Stage> finalizationStages() {
        return List.of(new ApprovalStage(), new EvidenceStage(), new ProvenanceStage());
    }

    /** Runs the analysis half. Stops at the first non-success. */
    public RunOutcome runAnalysis(String requestedTarget) {
        List<StageResult> results = new ArrayList<>();
        if (context.run().output().resolveLatestDir(RunBootstrap.OUTPUT_DIR) == null) {
            StageResult bootstrap = new RunBootstrap(context).execute();
            results.add(bootstrap);
            reporter.accept(bootstrap);
            if (!bootstrap.succeeded()) {
                return new RunOutcome(results, bootstrap.exitCode(), "bootstrap failed");
            }
        }
        for (Stage stage : analysisStages(requestedTarget)) {
            StageResult result = stage.execute(context);
            results.add(result);
            reporter.accept(result);
            if (!result.succeeded()) {
                return new RunOutcome(results, result.exitCode(),
                        "analysis stopped at " + stage.id());
            }
        }
        return new RunOutcome(results, ExitCode.SUCCESS, "analysis complete; plan frozen");
    }

    /**
     * Runs the migration edges in plan order.
     *
     * <p>The edge loop reads the frozen validation depth from the plan; it never decides for itself
     * how much validation an edge deserves (R16).
     */
    public RunOutcome runEdges() {
        List<StageResult> results = new ArrayList<>();
        JsonNode edgePlan = StageSupport.requireUpstream(context, "11-plan", "edge-plan.json",
                "Run: harness plan");
        List<String> edgeIds = EdgeSupport.edgeIds(edgePlan);
        for (String edgeId : edgeIds) {
            for (Stage stage : edgeStages(edgeId)) {
                StageResult result = stage.execute(context);
                results.add(result);
                reporter.accept(result);
                if (!result.succeeded()) {
                    return new RunOutcome(results, result.exitCode(),
                            "edge " + edgeId + " stopped at " + stage.id());
                }
            }
            context.stateMachine().transition(com.bootshift.core.state.RunState.EDGE_COMPLETE,
                    "edge " + edgeId + " complete");
            EdgeSupport.checkpoint(context, edgeId, "complete", "Edge " + edgeId + " complete");
        }
        return new RunOutcome(results, ExitCode.SUCCESS, edgeIds.size() + " edge(s) complete");
    }

    /** Runs approval, evidence sealing and provenance. */
    public RunOutcome runFinalization() {
        List<StageResult> results = new ArrayList<>();
        for (Stage stage : finalizationStages()) {
            StageResult result = stage.execute(context);
            results.add(result);
            reporter.accept(result);
            if (!result.succeeded()) {
                return new RunOutcome(results, result.exitCode(), "stopped at " + stage.id());
            }
        }
        return new RunOutcome(results, ExitCode.SUCCESS, "migration complete");
    }

    /** Everything, in order. */
    public RunOutcome runAll(String requestedTarget, boolean includeMutation) {
        List<StageResult> results = new ArrayList<>();
        RunOutcome analysis = runAnalysis(requestedTarget);
        results.addAll(analysis.results());
        if (analysis.exitCode() != ExitCode.SUCCESS) {
            return new RunOutcome(results, analysis.exitCode(), analysis.summary());
        }
        if (!includeMutation) {
            return new RunOutcome(results, ExitCode.SUCCESS,
                    "analysis complete; mutation not requested");
        }
        RunOutcome edges = runEdges();
        results.addAll(edges.results());
        if (edges.exitCode() != ExitCode.SUCCESS) {
            return new RunOutcome(results, edges.exitCode(), edges.summary());
        }
        RunOutcome finalization = runFinalization();
        results.addAll(finalization.results());
        return new RunOutcome(results, finalization.exitCode(), finalization.summary());
    }

    /** The complete stage catalog, used by {@code harness stages}. */
    public static List<Stage> catalog() {
        List<Stage> stages = new ArrayList<>();
        stages.add(new InventoryStage());
        stages.add(new BuildResolverStage());
        stages.add(new ApplicationGraphStage());
        stages.add(new BaselineStage());
        stages.add(new CompatibilityStage());
        stages.add(new TargetResolverStage());
        stages.add(new DocumentationStage());
        stages.add(new KnowledgeStage());
        stages.add(new ImpactStage());
        stages.add(new CharacterizationStage());
        stages.add(new PlannerStage());
        stages.add(new TransformationStage("<edge>"));
        stages.add(new BuildRepairStage("<edge>"));
        stages.add(new GraphDiffStage("<edge>"));
        stages.add(new TestValidationStage("<edge>"));
        stages.add(new RuntimeValidationStage("<edge>"));
        stages.add(new DifferentialStage("<edge>"));
        stages.add(new ApprovalStage());
        stages.add(new EvidenceStage());
        stages.add(new ProvenanceStage());
        return stages;
    }
}
