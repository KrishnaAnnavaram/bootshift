package com.bootshift.stages;

import com.fasterxml.jackson.databind.JsonNode;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.HarnessException;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.journal.EdgeDocumentRenderer;
import com.bootshift.core.journal.EdgeExecutionAggregate;
import com.bootshift.core.journal.ExecutionStatus;
import com.bootshift.core.journal.RunJournal;
import com.bootshift.core.journal.StageExecutionRecord;
import com.bootshift.core.journal.StageStepRecord;
import com.bootshift.core.journal.StepDeclaration;
import com.bootshift.core.util.Hashing;
import com.bootshift.core.util.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Wraps a stage attempt in an execution record.
 *
 * <p>This is the single cross-cutting place where runtime documentation is produced. It sits at
 * {@link StageExecutor}, which is the one door every stage is entered through, so no stage carries
 * journalling code and no future stage has to remember to add any.
 *
 * <p>The lifecycle is deliberately built around {@code finally}. A stage that throws before it
 * publishes anything is precisely the stage a reader most needs explained, and an execution record
 * created on the success path would not exist for it. Every exit - success, refusal, failure,
 * unhandled throwable - passes through the same finalization.
 */
public final class StageExecutionRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(StageExecutionRecorder.class);

    private StageExecutionRecorder() {
    }

    /**
     * Runs a stage, recording everything it did.
     *
     * <p>Preconditions are verified <em>inside</em> the record, not before it, so a refusal is
     * documented with the same detail as a run: which precondition failed, what was expected, and
     * what an operator should do about it.
     */
    public static StageResult record(Stage stage, StageContext context,
                                     StageExecutionRecord.Trigger trigger, String triggerDetail) {
        RunJournal journal = context.journal();
        String edgeId = stage.edgeId();
        StageExecutionRecord record = journal.begin(stage.id(), stage.getClass().getSimpleName(),
                edgeId, stage.purpose(), trigger, triggerDetail);
        record.stateBefore(safeState(context));
        stage.preconditions().forEach(p -> record.declarePrecondition(p.name()));
        stage.inputArtifacts().forEach(record::declareInput);
        declareSteps(record, stage.declaredSteps());

        AtomicReference<Path> attemptDirectory = new AtomicReference<>();
        OutputLayout output = context.run().output();
        output.openListener((stageDir, directory) -> {
            if (stage.outputDirectory().equals(stageDir)) {
                attemptDirectory.compareAndSet(null, directory);
            }
        });

        ExecutionStatus status = ExecutionStatus.INCOMPLETE;
        StageResult result;
        try {
            StageExecutor.Precondition check = StageExecutor.verify(stage, context);
            recordPreconditionResults(record, stage, context, check);
            if (!check.satisfied()) {
                result = StageResult.failure(stage.id(), ExitCode.STRUCTURED_REFUSAL,
                        "Stage " + stage.id() + " cannot run yet: " + check.violations().size()
                                + " precondition(s) unsatisfied",
                        concat(check.violations(), check.remediation()));
                status = ExecutionStatus.REFUSED;
                record.stopReason("Refused before executing: " + String.join("; ", check.violations()));
                record.nextAction(check.remediation().isEmpty() ? "Satisfy the failed preconditions"
                        : String.join("; ", check.remediation().stream().distinct().toList()));
                check.violations().forEach(v -> record.error("PRECONDITION", v, null));
                return finish(record, result);
            }

            result = stage.execute(context);
            status = classify(result);
            applyResult(record, result);
            correctNextAction(record, result, status);
            return finish(record, result);
        } catch (HarnessException e) {
            // A structured refusal is the harness working; it is recorded as such rather than as a
            // crash, and it keeps the exit code the stage chose.
            status = e.exitCode() == ExitCode.HUMAN_DECISION_REQUIRED
                    || e.exitCode() == ExitCode.POLICY_BLOCK
                    ? ExecutionStatus.BLOCKED
                    : e.exitCode() == ExitCode.STRUCTURED_REFUSAL
                    ? ExecutionStatus.REFUSED : ExecutionStatus.FAILED;
            record.error(e.exitCode().name(), e.getMessage(), null);
            record.stopReason(e.getMessage());
            record.summary(e.getMessage());
            result = StageResult.failure(stage.id(), e.exitCode(), String.valueOf(e.getMessage()),
                    List.of());
            // Stages that stop by throwing have usually already recorded the next pipeline step on
            // their way past the publish call - the target resolver publishes its report and then
            // throws a policy block - so the optimistic action has to be corrected here too, not
            // only on the returned-result path.
            correctNextAction(record, result, status);
            return result;
        } catch (RuntimeException | Error e) {
            status = ExecutionStatus.CRASHED;
            record.error("UNHANDLED", e.getClass().getName() + ": " + e.getMessage(),
                    "This is a harness defect. The stage produced no trustworthy result.");
            record.stopReason("Unhandled " + e.getClass().getSimpleName() + " in " + stage.id());
            record.summary("Stage crashed: " + e.getClass().getSimpleName());
            throw e;
        } finally {
            output.openListener(null);
            record.stateAfter(safeState(context));
            settleRunningSteps(record, status);
            journal.end(record, status, attemptDirectory.get());
            emitTelemetry(context, record, status);
            journal.refreshRunDocument();
            if (edgeId != null && !edgeId.isBlank() && !edgeId.startsWith("<")) {
                refreshEdgeDocument(context, edgeId);
            }
        }
    }

    /**
     * Mirrors the attempt's headline numbers into telemetry.
     *
     * <p>Telemetry, never evidence (R22). It is allowed to be sampled, dropped or switched off, so
     * nothing may be concluded from it - the execution record on disk is what a claim rests on. What
     * this buys is the operational view the artifact plane is bad at: how long stages take across
     * many runs, which ones retry, which ones shell out most.
     */
    private static void emitTelemetry(StageContext context, StageExecutionRecord record,
                                      ExecutionStatus status) {
        try {
            if (context.telemetry() == null) {
                return;
            }
            Map<String, Object> tags = new java.util.LinkedHashMap<>();
            tags.put("stage_id", record.stageId());
            tags.put("status", status.name());
            if (record.edgeId() != null) {
                tags.put("edge_id", record.edgeId());
            }
            context.telemetry().gauge("stage_duration_ms", record.durationMs(), tags);
            context.telemetry().counter("commands_executed", record.commands().size(), tags);
            context.telemetry().counter("warnings", record.warnings().size(), tags);
            context.telemetry().counter("errors", record.errors().size(), tags);
            context.telemetry().counter("artifacts_generated", record.outputs().size(), tags);
            context.telemetry().counter("blind_spots", record.blindSpots().size(), tags);
            context.telemetry().counter("decisions", record.decisions().size(), tags);
            context.telemetry().counter("unexecuted_steps", record.unexecutedSteps().size(), tags);
            Object appliedMutations = record.mutationSummary().get("applied");
            if (appliedMutations instanceof Number applied) {
                context.telemetry().counter("mutations_applied", applied.longValue(), tags);
            }
        } catch (RuntimeException e) {
            // Telemetry is diagnostic. It does not get to affect a stage outcome.
            LOG.debug("Cannot emit stage telemetry: {}", e.getMessage());
        }
    }

    /**
     * Runs a non-stage step of the pipeline - bootstrap - under the same journal.
     *
     * <p>Bootstrap is explicitly not an agent and does not implement {@link Stage}, but a run whose
     * bootstrap failed still needs a document explaining why, and it is the first thing that can
     * fail.
     */
    public static StageResult record(String stageId, String stageName, String purpose,
                                     String outputDirectory, List<StepDeclaration> steps,
                                     StageContext context, StageExecutionRecord.Trigger trigger,
                                     Supplier<StageResult> body) {
        RunJournal journal = context.journal();
        StageExecutionRecord record = journal.begin(stageId, stageName, null, purpose, trigger, null);
        record.stateBefore(safeState(context));
        declareSteps(record, steps);

        AtomicReference<Path> attemptDirectory = new AtomicReference<>();
        OutputLayout output = context.run().output();
        output.openListener((stageDir, directory) -> {
            if (outputDirectory.equals(stageDir)) {
                attemptDirectory.compareAndSet(null, directory);
            }
        });

        ExecutionStatus status = ExecutionStatus.INCOMPLETE;
        try {
            StageResult result = body.get();
            status = classify(result);
            applyResult(record, result);
            correctNextAction(record, result, status);
            return finish(record, result);
        } catch (HarnessException e) {
            status = e.exitCode() == ExitCode.STRUCTURED_REFUSAL
                    ? ExecutionStatus.REFUSED : ExecutionStatus.FAILED;
            record.error(e.exitCode().name(), e.getMessage(), null);
            record.stopReason(e.getMessage());
            correctNextAction(record, StageResult.failure(stageId, e.exitCode(),
                    String.valueOf(e.getMessage()), List.of()), status);
            throw e;
        } catch (RuntimeException | Error e) {
            status = ExecutionStatus.CRASHED;
            record.error("UNHANDLED", e.getClass().getName() + ": " + e.getMessage(), null);
            record.stopReason("Unhandled " + e.getClass().getSimpleName() + " in " + stageId);
            throw e;
        } finally {
            output.openListener(null);
            record.stateAfter(safeState(context));
            settleRunningSteps(record, status);
            journal.end(record, status, attemptDirectory.get());
            journal.refreshRunDocument();
        }
    }

    private static StageResult finish(StageExecutionRecord record, StageResult result) {
        return result;
    }

    private static void declareSteps(StageExecutionRecord record, List<StepDeclaration> steps) {
        if (steps == null) {
            return;
        }
        steps.forEach(s -> record.declareStep(s.stepId(), s.name(), s.purpose()));
    }

    /**
     * Settles any step still RUNNING when the attempt ends, according to how the attempt ended.
     *
     * <p>Left alone such a step serializes as RUNNING forever, which reads as a live step in a
     * finished record. Settling it here rather than inside the stage is what keeps the record honest:
     * a stage cannot know its own outcome while it is still executing, so a step that marked itself
     * successful before the stage returned would claim success on a stage that went on to block. That
     * is not hypothetical - the target resolver publishes its artifacts and <em>then</em> reports a
     * policy block, and its "select the landing target" step duly reported SUCCESS on a run where no
     * target was selected at all.
     */
    private static void settleRunningSteps(StageExecutionRecord record, ExecutionStatus status) {
        String reason = record.stopReason();
        record.steps().stream()
                .filter(step -> step.status() == com.bootshift.core.journal.StepStatus.RUNNING)
                .forEach(step -> {
                    switch (status) {
                        case SUCCESS, DEGRADED -> step.finish(
                                status == ExecutionStatus.DEGRADED
                                        ? com.bootshift.core.journal.StepStatus.DEGRADED
                                        : com.bootshift.core.journal.StepStatus.SUCCESS,
                                "Completed; the stage reported " + status.name());
                        case BLOCKED -> step.block(reason == null
                                ? "The stage stopped on a finding it may not decide alone" : reason);
                        case REFUSED -> step.finish(com.bootshift.core.journal.StepStatus.REFUSED,
                                reason == null ? "The stage refused" : reason);
                        case FAILED -> step.fail(reason == null ? "The stage failed" : reason, null);
                        case CRASHED -> step.fail("The attempt ended while this step was still "
                                + "running", null);
                        default -> step.fail("The attempt ended with no terminal outcome", null);
                    }
                });
    }

    /**
     * Replaces an optimistic next action when the stage did not actually succeed.
     *
     * <p>Stages record the next pipeline step as they publish, which is correct on the success path
     * and actively misleading otherwise: a blocked target resolver was telling operators to run the
     * documentation stage, which cannot run and would not help if it could. The remediation for a
     * stage that stopped is about the stop, not about what comes next.
     */
    private static void correctNextAction(StageExecutionRecord record, StageResult result,
                                          ExecutionStatus status) {
        if (status.succeeded() || result == null) {
            return;
        }
        String remediation = switch (result.exitCode()) {
            case POLICY_BLOCK -> "This stage is blocked by the active policy. Read section 18, then "
                    + "either change the policy deliberately or accept that the migration cannot "
                    + "proceed as specified.";
            case HUMAN_DECISION_REQUIRED -> "An authorized human decision is required before the run "
                    + "can continue. File one with: bootshift approve --decision-file <file>";
            case STRUCTURED_REFUSAL -> "Satisfy the preconditions listed in section 4, then re-run "
                    + "this stage.";
            case STAGE_FAILURE -> "Read the errors in section 18 and the commands in section 8, then "
                    + "re-run this stage once the cause is addressed.";
            case SUCCESS -> null;
        };
        if (remediation != null) {
            record.nextAction(remediation);
        }
    }

    private static void recordPreconditionResults(StageExecutionRecord record, Stage stage,
                                                  StageContext context,
                                                  StageExecutor.Precondition check) {
        // Declared states first, each answered individually so a document can show which one failed
        // rather than only that something did.
        for (var state : stage.preconditions()) {
            String proof = StageExecutor.proofArtifactFor(state);
            boolean satisfied = check.violations().stream()
                    .noneMatch(v -> v.contains(state.name()));
            record.preconditionResult("state:" + state.name(), satisfied,
                    proof == null ? "No artifact proof declared for this state"
                            : "Proven by " + proof,
                    satisfied ? null : remediationFor(check, state.name()));
        }
        for (String artifact : stage.inputArtifacts()) {
            Path path = artifactPath(context, artifact);
            boolean present = path != null && Files.isRegularFile(path);
            record.preconditionResult("artifact:" + artifact, present,
                    present ? "Published and readable" : "Not published",
                    present ? null : "Run the stage that publishes " + artifact);
            record.resolvedInput(artifact, path == null ? null : path.toString(), present,
                    present ? hashOf(path) : null);
        }
    }

    private static String remediationFor(StageExecutor.Precondition check, String stateName) {
        return check.remediation().stream().findFirst().orElse("Run the stage that reaches "
                + stateName);
    }

    private static Path artifactPath(StageContext context, String artifact) {
        int slash = artifact.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        try {
            return context.run().output().latestArtifactPath(artifact.substring(0, slash),
                    artifact.substring(slash + 1));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String hashOf(Path path) {
        try {
            return Hashing.sha256File(path);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static void applyResult(StageExecutionRecord record, StageResult result) {
        if (result == null) {
            return;
        }
        record.summary(result.summary());
        record.primaryArtifactHash(result.primaryArtifactHash());
        result.messages().forEach(record::warning);
        result.artifacts().forEach((name, path) ->
                record.output(name, path == null ? null : path.toString(),
                        path != null && Files.isRegularFile(path) ? hashOf(path) : null));
        if (!result.succeeded() && record.stopReason() == null) {
            record.stopReason(result.summary());
        }
    }

    private static ExecutionStatus classify(StageResult result) {
        if (result == null) {
            return ExecutionStatus.INCOMPLETE;
        }
        return switch (result.exitCode()) {
            case SUCCESS -> ExecutionStatus.SUCCESS;
            case STRUCTURED_REFUSAL -> ExecutionStatus.REFUSED;
            case POLICY_BLOCK, HUMAN_DECISION_REQUIRED -> ExecutionStatus.BLOCKED;
            case STAGE_FAILURE -> ExecutionStatus.FAILED;
        };
    }

    private static String safeState(StageContext context) {
        try {
            return context.stateMachine().current().name();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> all = new java.util.ArrayList<>(first);
        second.stream().distinct().forEach(r -> all.add("remediation: " + r));
        return all;
    }

    /**
     * Rewrites the edge document after any edge-scoped attempt.
     *
     * <p>Best-effort, like every other rendering path: an edge document that cannot be produced is a
     * documentation gap to report, never a reason to fail a migration stage that succeeded.
     */
    static void refreshEdgeDocument(StageContext context, String edgeId) {
        RunJournal journal = context.journal();
        try {
            JsonNode planEdge = null;
            JsonNode plan = context.run().output().readLatest("11-plan", "edge-plan.json");
            if (plan != null) {
                for (JsonNode edge : plan.path("edges")) {
                    if (edgeId.equals(edge.path("edge_id").asText(null))) {
                        planEdge = edge;
                        break;
                    }
                }
            }
            ObjectNode aggregate = EdgeExecutionAggregate.build(context.run().output(),
                    context.run().runId(), edgeId, journal.timeline(), planEdge);
            Path directory = journal.edgeDirectory(edgeId);
            Files.createDirectories(directory);
            Json.write(directory.resolve(RunJournal.EDGE_EXECUTION_FILE), aggregate);
            Files.writeString(directory.resolve(RunJournal.EDGE_DOCUMENT_FILE),
                    EdgeDocumentRenderer.render(aggregate), StandardCharsets.UTF_8);
        } catch (RuntimeException | IOException e) {
            String message = "EDGE_DOCUMENT.md rendering failed for " + edgeId + ": "
                    + e.getClass().getSimpleName() + " " + e.getMessage();
            LOG.warn(message);
            journal.noteFailure(message);
        }
    }

    /** Looks up a declared step so a stage can settle it, or a detached one when unjournalled. */
    public static StageStepRecord step(StageContext context, String stepId) {
        StageExecutionRecord record = context.journal().current();
        if (record == null) {
            // A stage invoked outside a journalled attempt - a unit test, usually - still runs.
            return new StageStepRecord(stepId, 0, stepId, null);
        }
        StageStepRecord step = record.step(stepId);
        return step != null ? step : record.declareStep(stepId, stepId,
                "Undeclared step, recorded on first use");
    }
}
