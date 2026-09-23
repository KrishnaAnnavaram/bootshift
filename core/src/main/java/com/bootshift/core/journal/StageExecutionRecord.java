package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.util.Json;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What one stage attempt actually did.
 *
 * <p>{@code StageResult} is the stage's answer to its caller: an exit code, a summary and the
 * artifacts it published. It is deliberately small, it is returned by value through the whole CLI,
 * and widening it into an audit object would push the cost of auditing onto every code path that
 * only wanted to know whether the stage worked. This record is the audit object, written beside the
 * stage's own artifacts, and nothing downstream is required to carry it.
 *
 * <p>The record is written even when the stage refuses, fails or throws. That is the requirement it
 * exists for: a run that stops at stage 13 never reaches the evidence stage, and if runtime
 * documentation were produced only at finalization, the runs most in need of explanation would be
 * exactly the ones with none.
 *
 * <p>Mutable during an attempt and read from more than one thread - command journalling happens on
 * whichever thread launched the process - so every mutator is synchronized and every collection
 * handed out is a copy.
 */
public final class StageExecutionRecord {

    public static final String SCHEMA_VERSION = "1.0.0";

    /** Why a stage attempt began. */
    public enum Trigger {
        /** The orchestrator's sequence reached it. */
        PIPELINE,
        /** An operator invoked the stage directly from the CLI. */
        CLI,
        /** The edge loop reached it for a specific edge. */
        EDGE_LOOP,
        /** A retry of a previous attempt. */
        RETRY,
        /** A resumed run re-entered it. */
        RESUME
    }

    private final String runId;
    private final String attemptId;
    private final String stageId;
    private final String stageName;
    private final String edgeId;
    private final String purpose;
    private final Trigger trigger;
    private final String triggerDetail;

    private final Instant startedAt;
    private Instant finishedAt;

    private String stateBefore;
    private String stateAfter;
    private ExecutionStatus status = ExecutionStatus.INCOMPLETE;

    private final List<String> declaredPreconditions = new ArrayList<>();
    private final List<ObjectNode> preconditionResults = new ArrayList<>();
    private final List<String> declaredInputs = new ArrayList<>();
    private final List<ObjectNode> resolvedInputs = new ArrayList<>();
    private final List<StageStepRecord> steps = new ArrayList<>();
    private final List<CommandExecutionRecord> commands = new ArrayList<>();
    private final List<ObjectNode> toolInvocations = new ArrayList<>();
    private final List<DecisionRecord> decisions = new ArrayList<>();
    private final List<String> evidenceReferences = new ArrayList<>();
    private final List<String> documentReferences = new ArrayList<>();
    private final List<String> migrationFactReferences = new ArrayList<>();
    private final List<String> impactReferences = new ArrayList<>();
    private final Map<String, Object> mutationSummary = new LinkedHashMap<>();
    private final Map<String, Object> validationSummary = new LinkedHashMap<>();
    private final List<ObjectNode> retries = new ArrayList<>();
    private final List<ObjectNode> fallbacks = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<ObjectNode> errors = new ArrayList<>();
    private final List<ObjectNode> blindSpots = new ArrayList<>();
    private final List<ObjectNode> outputs = new ArrayList<>();
    private final Map<String, Object> metrics = new LinkedHashMap<>();

    private String primaryArtifactHash;
    private String nextAction;
    private String stopReason;
    private String summary;
    private String attemptDirectory;
    private String policyHash;
    private String toolVersion;
    private String renderingError;

    public StageExecutionRecord(String runId, String attemptId, String stageId, String stageName,
                                String edgeId, String purpose, Trigger trigger, String triggerDetail) {
        this.runId = runId;
        this.attemptId = attemptId;
        this.stageId = stageId;
        this.stageName = stageName;
        this.edgeId = edgeId;
        this.purpose = purpose;
        this.trigger = trigger == null ? Trigger.PIPELINE : trigger;
        this.triggerDetail = triggerDetail;
        this.startedAt = Instant.now();
    }

    public String runId() {
        return runId;
    }

    public String attemptId() {
        return attemptId;
    }

    public String stageId() {
        return stageId;
    }

    public String stageName() {
        return stageName;
    }

    public String edgeId() {
        return edgeId;
    }

    public String purpose() {
        return purpose;
    }

    public Trigger trigger() {
        return trigger;
    }

    public String triggerDetail() {
        return triggerDetail;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public synchronized ExecutionStatus status() {
        return status;
    }

    public synchronized String summary() {
        return summary;
    }

    public synchronized String stateBefore() {
        return stateBefore;
    }

    public synchronized String stateAfter() {
        return stateAfter;
    }

    public synchronized String stopReason() {
        return stopReason;
    }

    public synchronized String nextAction() {
        return nextAction;
    }

    public synchronized String primaryArtifactHash() {
        return primaryArtifactHash;
    }

    public synchronized String attemptDirectory() {
        return attemptDirectory;
    }

    public synchronized String renderingError() {
        return renderingError;
    }

    public synchronized List<StageStepRecord> steps() {
        return List.copyOf(steps);
    }

    public synchronized List<CommandExecutionRecord> commands() {
        return List.copyOf(commands);
    }

    public synchronized List<DecisionRecord> decisions() {
        return List.copyOf(decisions);
    }

    public synchronized List<String> warnings() {
        return List.copyOf(warnings);
    }

    public synchronized List<ObjectNode> errors() {
        return List.copyOf(errors);
    }

    public synchronized List<ObjectNode> blindSpots() {
        return List.copyOf(blindSpots);
    }

    public synchronized List<ObjectNode> outputs() {
        return List.copyOf(outputs);
    }

    public synchronized List<String> declaredInputs() {
        return List.copyOf(declaredInputs);
    }

    public synchronized Map<String, Object> mutationSummary() {
        return Map.copyOf(mutationSummary);
    }

    public synchronized Map<String, Object> validationSummary() {
        return Map.copyOf(validationSummary);
    }

    public long durationMs() {
        Instant end = finishedAt == null ? Instant.now() : finishedAt;
        return Duration.between(startedAt, end).toMillis();
    }

    public synchronized StageExecutionRecord stateBefore(String state) {
        this.stateBefore = state;
        return this;
    }

    public synchronized StageExecutionRecord stateAfter(String state) {
        this.stateAfter = state;
        return this;
    }

    public synchronized StageExecutionRecord attemptDirectory(String directory) {
        this.attemptDirectory = directory;
        return this;
    }

    public synchronized StageExecutionRecord policyHash(String hash) {
        this.policyHash = hash;
        return this;
    }

    public synchronized StageExecutionRecord toolVersion(String version) {
        this.toolVersion = version;
        return this;
    }

    public synchronized StageExecutionRecord renderingError(String message) {
        this.renderingError = message;
        return this;
    }

    public synchronized StageExecutionRecord declarePrecondition(String precondition) {
        declaredPreconditions.add(precondition);
        return this;
    }

    public synchronized StageExecutionRecord preconditionResult(String precondition, boolean satisfied,
                                                                String detail, String remediation) {
        ObjectNode node = Json.obj();
        node.put("precondition", precondition);
        node.put("satisfied", satisfied);
        node.put("detail", detail);
        node.put("remediation", remediation);
        preconditionResults.add(node);
        return this;
    }

    public synchronized StageExecutionRecord declareInput(String artifact) {
        declaredInputs.add(artifact);
        return this;
    }

    public synchronized StageExecutionRecord resolvedInput(String artifact, String path,
                                                           boolean present, String hash) {
        ObjectNode node = Json.obj();
        node.put("artifact", artifact);
        node.put("path", path);
        node.put("present", present);
        node.put("hash", hash);
        resolvedInputs.add(node);
        return this;
    }

    /**
     * Declares a step before the stage runs.
     *
     * <p>Returns the record so the stage can settle it later. Declaring the same id twice returns the
     * existing step rather than shadowing it, so a stage that declares its plan and then re-enters a
     * loop cannot silently accumulate duplicates.
     */
    public synchronized StageStepRecord declareStep(String stepId, String name, String purpose) {
        for (StageStepRecord existing : steps) {
            if (existing.stepId().equals(stepId)) {
                return existing;
            }
        }
        StageStepRecord step = new StageStepRecord(stepId, steps.size() + 1, name, purpose);
        steps.add(step);
        return step;
    }

    public synchronized StageStepRecord step(String stepId) {
        for (StageStepRecord existing : steps) {
            if (existing.stepId().equals(stepId)) {
                return existing;
            }
        }
        return null;
    }

    public synchronized StageExecutionRecord command(CommandExecutionRecord record) {
        commands.add(record);
        return this;
    }

    public synchronized StageExecutionRecord toolInvocation(String tool, String operation,
                                                            String outcome, String detail) {
        ObjectNode node = Json.obj();
        node.put("tool", tool);
        node.put("operation", operation);
        node.put("outcome", outcome);
        node.put("detail", detail);
        node.put("at", Instant.now().toString());
        toolInvocations.add(node);
        return this;
    }

    public synchronized StageExecutionRecord decision(DecisionRecord record) {
        decisions.add(record);
        return this;
    }

    public synchronized StageExecutionRecord evidenceReference(String evidenceId) {
        if (evidenceId != null && !evidenceReferences.contains(evidenceId)) {
            evidenceReferences.add(evidenceId);
        }
        return this;
    }

    public synchronized StageExecutionRecord documentReference(String documentId) {
        if (documentId != null && !documentReferences.contains(documentId)) {
            documentReferences.add(documentId);
        }
        return this;
    }

    public synchronized StageExecutionRecord migrationFactReference(String factId) {
        if (factId != null && !migrationFactReferences.contains(factId)) {
            migrationFactReferences.add(factId);
        }
        return this;
    }

    public synchronized StageExecutionRecord impactReference(String impactId) {
        if (impactId != null && !impactReferences.contains(impactId)) {
            impactReferences.add(impactId);
        }
        return this;
    }

    public synchronized StageExecutionRecord mutation(String key, Object value) {
        mutationSummary.put(key, value);
        return this;
    }

    public synchronized StageExecutionRecord validation(String key, Object value) {
        validationSummary.put(key, value);
        return this;
    }

    public synchronized StageExecutionRecord metric(String key, Object value) {
        metrics.put(key, value);
        return this;
    }

    public synchronized StageExecutionRecord retry(int attempt, String reason, String outcome) {
        ObjectNode node = Json.obj();
        node.put("attempt", attempt);
        node.put("reason", reason);
        node.put("outcome", outcome);
        node.put("at", Instant.now().toString());
        retries.add(node);
        return this;
    }

    /**
     * Records that the stage reached its result by a route other than its primary one.
     *
     * <p>A fallback is not a warning. It is a statement about how much the result is worth, and it is
     * what lets a reader understand why two runs of the same stage on the same input produced
     * different confidence.
     */
    public synchronized StageExecutionRecord fallback(String from, String to, String reason,
                                                      String consequence) {
        ObjectNode node = Json.obj();
        node.put("from", from);
        node.put("to", to);
        node.put("reason", reason);
        node.put("consequence", consequence);
        node.put("at", Instant.now().toString());
        fallbacks.add(node);
        return this;
    }

    public synchronized StageExecutionRecord warning(String message) {
        if (message != null) {
            warnings.add(message);
        }
        return this;
    }

    public synchronized StageExecutionRecord error(String kind, String message, String remediation) {
        ObjectNode node = Json.obj();
        node.put("kind", kind);
        node.put("message", message);
        node.put("remediation", remediation);
        node.put("at", Instant.now().toString());
        errors.add(node);
        return this;
    }

    public synchronized StageExecutionRecord blindSpot(String id, String dimension, String description,
                                                       String impact) {
        ObjectNode node = Json.obj();
        node.put("id", id);
        node.put("dimension", dimension);
        node.put("description", description);
        node.put("impact", impact);
        blindSpots.add(node);
        return this;
    }

    public synchronized StageExecutionRecord output(String name, String path, String hash) {
        ObjectNode node = Json.obj();
        node.put("name", name);
        node.put("path", path);
        node.put("hash", hash);
        outputs.add(node);
        return this;
    }

    public synchronized StageExecutionRecord primaryArtifactHash(String hash) {
        this.primaryArtifactHash = hash;
        return this;
    }

    public synchronized StageExecutionRecord nextAction(String action) {
        this.nextAction = action;
        return this;
    }

    public synchronized StageExecutionRecord stopReason(String reason) {
        this.stopReason = reason;
        return this;
    }

    public synchronized StageExecutionRecord summary(String value) {
        this.summary = value;
        return this;
    }

    /** Settles the attempt. The first terminal status wins; a later call cannot rewrite history. */
    public synchronized StageExecutionRecord finish(ExecutionStatus outcome) {
        if (this.finishedAt == null) {
            this.finishedAt = Instant.now();
            this.status = outcome;
        }
        return this;
    }

    public synchronized boolean finished() {
        return finishedAt != null;
    }

    /** Steps declared but never entered. Empty is the expected state for a healthy stage. */
    public synchronized List<StageStepRecord> unexecutedSteps() {
        List<StageStepRecord> pending = new ArrayList<>();
        for (StageStepRecord step : steps) {
            if (step.status().unexecuted()) {
                pending.add(step);
            }
        }
        return Collections.unmodifiableList(pending);
    }

    public synchronized ObjectNode toNode() {
        ObjectNode node = Json.obj();
        node.put("schema_version", SCHEMA_VERSION);
        node.put("run_id", runId);
        node.put("attempt_id", attemptId);
        node.put("stage_id", stageId);
        node.put("stage_name", stageName);
        node.put("edge_id", edgeId);
        node.put("purpose", purpose);
        node.put("trigger", trigger.name());
        node.put("trigger_detail", triggerDetail);
        node.put("started_at", startedAt.toString());
        node.put("finished_at", finishedAt == null ? null : finishedAt.toString());
        node.put("duration_ms", durationMs());
        node.put("state_before", stateBefore);
        node.put("state_after", stateAfter);
        node.put("status", status.name());
        node.put("summary", summary);
        node.put("attempt_directory", attemptDirectory);

        node.set("declared_preconditions", Json.toTree(declaredPreconditions));
        node.set("precondition_results", Json.toTree(preconditionResults));
        node.set("declared_inputs", Json.toTree(declaredInputs));
        node.set("resolved_inputs", Json.toTree(resolvedInputs));

        List<String> declaredStepIds = new ArrayList<>();
        List<ObjectNode> executed = new ArrayList<>();
        for (StageStepRecord step : steps) {
            declaredStepIds.add(step.stepId());
            executed.add(step.toNode());
        }
        node.set("declared_steps", Json.toTree(declaredStepIds));
        node.set("executed_steps", Json.toTree(executed));
        node.set("unexecuted_steps", Json.toTree(unexecutedSteps().stream()
                .map(StageStepRecord::stepId).toList()));

        node.set("commands", Json.toTree(commands.stream().map(CommandExecutionRecord::toNode).toList()));
        node.set("tool_invocations", Json.toTree(toolInvocations));
        node.set("decisions", Json.toTree(decisions.stream().map(DecisionRecord::toNode).toList()));
        node.set("evidence_references", Json.toTree(evidenceReferences));
        node.set("document_references", Json.toTree(documentReferences));
        node.set("migration_fact_references", Json.toTree(migrationFactReferences));
        node.set("impact_references", Json.toTree(impactReferences));
        node.set("mutation_summary", Json.toTree(mutationSummary));
        node.set("validation_summary", Json.toTree(validationSummary));
        node.set("retries", Json.toTree(retries));
        node.set("fallbacks", Json.toTree(fallbacks));
        node.set("warnings", Json.toTree(warnings));
        node.set("errors", Json.toTree(errors));
        node.set("blind_spots", Json.toTree(blindSpots));
        node.set("outputs", Json.toTree(outputs));
        node.set("metrics", Json.toTree(metrics));
        node.put("primary_artifact_hash", primaryArtifactHash);
        node.put("next_action", nextAction);
        node.put("stop_reason", stopReason);
        node.put("policy_hash", policyHash);
        node.put("tool_version", toolVersion);
        node.put("rendering_error", renderingError);
        return node;
    }
}
