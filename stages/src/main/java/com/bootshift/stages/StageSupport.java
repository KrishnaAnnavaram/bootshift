package com.bootshift.stages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.adapters.exec.ProcessRunner;
import com.bootshift.core.domain.Envelope;
import com.bootshift.core.domain.HarnessException;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.evidence.EvidenceManifest;
import com.bootshift.core.journal.DecisionRecord;
import com.bootshift.core.journal.StageStepRecord;
import com.bootshift.core.util.Json;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Shared plumbing for stages: envelope construction, schema-validated publishing and upstream
 * artifact resolution.
 *
 * <p>Publishing goes through {@link #publish} so no stage can accidentally advance
 * {@code latest.json} past a schema violation.
 */
public final class StageSupport {

    public static final String SCHEMA_VERSION = "1.0.0";

    private StageSupport() {
    }

    /**
     * A process runner that reports what it runs to the execution journal.
     *
     * <p>Every adapter that shells out already accepts a runner, so attribution is added by handing
     * them this one rather than by teaching each adapter about runs and stages. A runner built any
     * other way still works and still enforces every control; it simply records nothing, which is the
     * right behaviour for a unit test.
     */
    public static ProcessRunner runner(StageContext context) {
        return new ProcessRunner().observedBy(context.journal().commandObserver());
    }

    /**
     * Settles a step the stage declared.
     *
     * <p>Returns a detached record when there is no attempt in flight, so instrumentation never has
     * to be guarded by a null check and a stage remains runnable outside a journalled run.
     */
    public static StageStepRecord step(StageContext context, String stepId) {
        return StageExecutionRecorder.step(context, stepId);
    }

    /** Records a decision against the attempt in flight. */
    public static void decision(StageContext context, DecisionRecord decision) {
        context.journal().decision(decision);
    }

    /** Starts a deterministic decision with the next run-scoped decision id. */
    public static DecisionRecord.Builder decide(StageContext context, String type, String subject) {
        return DecisionRecord.deterministic(context.journal().nextDecisionId(), type, subject);
    }

    /** Records a blind spot on the attempt in flight, when there is one. */
    public static void blindSpot(StageContext context, String id, String dimension,
                                 String description, String impact) {
        var record = context.journal().current();
        if (record != null) {
            record.blindSpot(id, dimension, description, impact);
        }
    }

    /** Records a fallback on the attempt in flight, when there is one. */
    public static void fallback(StageContext context, String from, String to, String reason,
                                String consequence) {
        var record = context.journal().current();
        if (record != null) {
            record.fallback(from, to, reason, consequence);
        }
    }

    /** Records a measure in the attempt's mutation summary. */
    public static void mutationMeasure(StageContext context, String key, Object value) {
        var record = context.journal().current();
        if (record != null) {
            record.mutation(key, value);
        }
    }

    /** Records a measure in the attempt's validation summary. */
    public static void validationMeasure(StageContext context, String key, Object value) {
        var record = context.journal().current();
        if (record != null) {
            record.validation(key, value);
        }
    }

    /** Records what the operator should do next, used most by stages that stop. */
    public static void nextAction(StageContext context, String action) {
        var record = context.journal().current();
        if (record != null) {
            record.nextAction(action);
        }
    }

    public static Envelope envelope(StageContext context, String stageId) {
        return new Envelope(stageId, SCHEMA_VERSION, context.run().runId())
                .stat("policy", context.policy().name())
                .stat("policy_hash", context.policy().policyHash())
                .stat("ai_enabled", context.run().aiEnabled())
                .stat("environment_mode", context.run().environmentMode());
    }

    /** Merges stage payload into the envelope node, refusing to shadow reserved keys. */
    public static ObjectNode compose(Envelope envelope, ObjectNode payload) {
        ObjectNode node = envelope.toNode();
        payload.fields().forEachRemaining(entry -> {
            if (Envelope.RESERVED_KEYS.contains(entry.getKey())) {
                throw HarnessException.refusal(
                        "Stage payload attempted to shadow reserved envelope key: " + entry.getKey());
            }
            node.set(entry.getKey(), entry.getValue());
        });
        return node;
    }

    /**
     * Validates an artifact against its schema and records any violation on the writer so
     * {@link OutputLayout.StageWriter#publish} refuses to advance the pointer.
     */
    public static void validate(StageContext context, OutputLayout.StageWriter writer,
                                String schemaPath, String artifactName, JsonNode payload) {
        if (!context.schemas().available()) {
            return;
        }
        List<String> errors = context.schemas().validate(schemaPath, payload);
        boolean missing = errors.size() == 1 && errors.get(0).startsWith("Schema not found");
        if (missing) {
            return;
        }
        errors.forEach(e -> writer.validationError(artifactName + ": " + e));
    }

    /** Reads an artifact published by an upstream stage, refusing when the upstream never ran. */
    public static JsonNode requireUpstream(StageContext context, String stageDirectory,
                                           String artifactName, String remediation) {
        JsonNode node = context.run().output().readLatest(stageDirectory, artifactName);
        if (node == null) {
            throw HarnessException.refusal("Missing required upstream artifact "
                    + stageDirectory + "/" + artifactName + ". " + remediation);
        }
        return node;
    }

    public static JsonNode optionalUpstream(StageContext context, String stageDirectory,
                                            String artifactName) {
        return context.run().output().readLatest(stageDirectory, artifactName);
    }

    public static Path upstreamPath(StageContext context, String stageDirectory, String artifactName) {
        return context.run().output().latestArtifactPath(stageDirectory, artifactName);
    }

    /** Stores an artifact copy in the content-addressed evidence store and returns its id. */
    public static String toEvidence(StageContext context, String kind, ObjectNode payload,
                                    EvidenceManifest.Classification classification,
                                    String retentionClass, String producedBy) {
        byte[] bytes = Json.canonical(payload).getBytes(StandardCharsets.UTF_8);
        return context.evidenceStore()
                .put(kind, kind + ".json", bytes, classification, retentionClass, producedBy)
                .evidenceId();
    }

    public static String toEvidence(StageContext context, String kind, String text,
                                    EvidenceManifest.Classification classification,
                                    String retentionClass, String producedBy) {
        return context.evidenceStore()
                .put(kind, kind + ".txt", text.getBytes(StandardCharsets.UTF_8), classification,
                        retentionClass, producedBy)
                .evidenceId();
    }

    /** Publishes the writer and returns the manifest hash for the stage result. */
    public static String publish(StageContext context, OutputLayout.StageWriter writer) {
        writer.publish(context.run().runId());
        return writer.hashOf("manifest.json") == null
                ? String.valueOf(writer.hashes().hashCode()) : writer.hashOf("manifest.json");
    }

    /**
     * Publishes an edge-scoped stage and records where it published into the edge index.
     *
     * <p>Publishing advances one {@code latest.json} pointer per stage. In a per-edge loop that
     * pointer names whichever edge ran last, so a later reader that only follows the pointer sees
     * one edge and reports it as the migration. Recording the directory here is what lets final
     * evidence aggregate all of them.
     */
    public static String publishForEdge(StageContext context, OutputLayout.StageWriter writer,
                                        String edgeId, String stageDirectory,
                                        EdgeIndex.Phase phase, String outcome) {
        String hash = publish(context, writer);
        EdgeIndex.open(context)
                .recordStage(edgeId, stageDirectory, writer.dir(), phase, outcome)
                .persist();
        return hash;
    }
}
