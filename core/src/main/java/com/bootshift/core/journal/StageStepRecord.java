package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.util.Json;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One declared unit of work inside a stage, and what became of it.
 *
 * <p>Steps are <em>declared before the stage runs</em> and then resolved as it proceeds. That order
 * is the point: a step list built from what happened can only ever describe what happened, whereas a
 * declared list still holds the steps that did not. This project has already shipped code that was
 * implemented and never invoked, and nothing in the artifact plane showed it, because the artifact
 * recorded results and results of work that never ran are simply absent.
 *
 * <p>A step left {@link StepStatus#PENDING} at finalization is therefore a finding, not a gap in the
 * record.
 */
public final class StageStepRecord {

    private final String stepId;
    private final int sequence;
    private final String name;
    private final String purpose;

    private StepStatus status = StepStatus.PENDING;
    private Instant startedAt;
    private Instant finishedAt;
    private final List<String> inputs = new ArrayList<>();
    private final List<String> outputs = new ArrayList<>();
    private final List<String> evidenceRefs = new ArrayList<>();
    private final Map<String, Object> details = new LinkedHashMap<>();
    private String error;
    private String statusReason;

    public StageStepRecord(String stepId, int sequence, String name, String purpose) {
        this.stepId = stepId;
        this.sequence = sequence;
        this.name = name;
        this.purpose = purpose;
    }

    public String stepId() {
        return stepId;
    }

    public int sequence() {
        return sequence;
    }

    public String name() {
        return name;
    }

    public String purpose() {
        return purpose;
    }

    public StepStatus status() {
        return status;
    }

    public String statusReason() {
        return statusReason;
    }

    public String error() {
        return error;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public List<String> outputs() {
        return List.copyOf(outputs);
    }

    public Map<String, Object> details() {
        return Map.copyOf(details);
    }

    public synchronized StageStepRecord begin() {
        this.startedAt = Instant.now();
        this.status = StepStatus.RUNNING;
        return this;
    }

    /** Settles the step. Idempotent in the sense that the first terminal outcome wins. */
    public synchronized StageStepRecord finish(StepStatus outcome, String reason) {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        this.finishedAt = Instant.now();
        this.status = outcome;
        this.statusReason = reason;
        return this;
    }

    public StageStepRecord succeed(String reason) {
        return finish(StepStatus.SUCCESS, reason);
    }

    public StageStepRecord degrade(String reason) {
        return finish(StepStatus.DEGRADED, reason);
    }

    public StageStepRecord skip(String reason) {
        return finish(StepStatus.SKIPPED, reason);
    }

    public StageStepRecord notApplicable(String reason) {
        return finish(StepStatus.NOT_APPLICABLE, reason);
    }

    public synchronized StageStepRecord fail(String reason, Throwable cause) {
        this.error = cause == null ? reason
                : cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage());
        return finish(StepStatus.FAILED, reason);
    }

    public StageStepRecord block(String reason) {
        return finish(StepStatus.BLOCKED, reason);
    }

    public synchronized StageStepRecord input(String reference) {
        if (reference != null) {
            inputs.add(reference);
        }
        return this;
    }

    public synchronized StageStepRecord output(String reference) {
        if (reference != null) {
            outputs.add(reference);
        }
        return this;
    }

    public synchronized StageStepRecord evidence(String evidenceId) {
        if (evidenceId != null) {
            evidenceRefs.add(evidenceId);
        }
        return this;
    }

    /**
     * Records a measurement about the step.
     *
     * <p>Intended for counts and short scalars. Large collections belong in the stage's own artifact
     * with a reference from here; the journal is an index of a run, not a second copy of it.
     */
    public synchronized StageStepRecord detail(String key, Object value) {
        if (key != null) {
            details.put(key, value);
        }
        return this;
    }

    public long durationMs() {
        if (startedAt == null || finishedAt == null) {
            return 0L;
        }
        return Duration.between(startedAt, finishedAt).toMillis();
    }

    public synchronized ObjectNode toNode() {
        ObjectNode node = Json.obj();
        node.put("step_id", stepId);
        node.put("sequence", sequence);
        node.put("name", name);
        node.put("purpose", purpose);
        node.put("status", status.name());
        node.put("status_reason", statusReason);
        node.put("started_at", startedAt == null ? null : startedAt.toString());
        node.put("finished_at", finishedAt == null ? null : finishedAt.toString());
        node.put("duration_ms", durationMs());
        node.set("inputs", Json.toTree(inputs));
        node.set("outputs", Json.toTree(outputs));
        node.set("evidence_refs", Json.toTree(evidenceRefs));
        node.set("details", Json.toTree(details));
        node.put("error", error);
        return node;
    }
}
