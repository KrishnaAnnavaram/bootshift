package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.util.Json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gathers everything one migration edge did into a single machine-readable record.
 *
 * <p>An edge is the unit a reviewer actually reasons about - "what did the jump to 3.0 do to this
 * application" - but its evidence is scattered across six stage directories, each of which publishes
 * a pointer that names whichever edge ran last. This aggregate resolves the edge's own attempts from
 * the run timeline, which records the attempt directory for every attempt rather than only the most
 * recent one.
 *
 * <p>The aggregate is derived, never authoritative: everything in it is a copy of, or a reference
 * to, a stage execution record that already exists on disk.
 */
public final class EdgeExecutionAggregate {

    /** The stages that make up an edge, in execution order. */
    public static final List<String> EDGE_STAGES = List.of(
            "12-transformation", "13-build-repair", "14-graph-diff", "15-test", "16-runtime",
            "17-differential");

    private EdgeExecutionAggregate() {
    }

    /**
     * Builds the edge record.
     *
     * @param timeline the run timeline entries, in order
     * @param planEdge the frozen plan's description of this edge, or {@code null} when unavailable
     */
    public static ObjectNode build(OutputLayout output, String runId, String edgeId,
                                   List<ObjectNode> timeline, JsonNode planEdge) {
        ObjectNode node = Json.obj();
        node.put("schema_version", RunJournal.SCHEMA_VERSION);
        node.put("run_id", runId);
        node.put("edge_id", edgeId);
        node.put("generated_at", Instant.now().toString());
        node.put("purpose", "Everything this migration edge did, resolved from the run timeline so "
                + "every attempt is included rather than only the one the stage pointer names.");

        if (planEdge != null) {
            // Field names taken from the frozen plan as the planner actually writes it. Guessing
            // them is not a harmless mistake: a document that renders an em dash for a validation
            // depth the plan states perfectly clearly reads as "the harness does not know", which is
            // the opposite of true and the opposite of useful.
            ObjectNode plan = Json.obj();
            plan.put("edge_class", planEdge.path("edge_class").asText(null));
            plan.put("source_state", planEdge.path("source_state").asText(null));
            plan.put("target_state", planEdge.path("target_state").asText(null));
            plan.put("java_version", planEdge.path("edge_java_version")
                    .asText(planEdge.path("edge_java").asText(null)));
            plan.put("spring_cloud_version", planEdge.path("spring_cloud_train").asText(null));
            plan.put("validation_depth", planEdge.path("frozen_validation_depth").asText(null));
            plan.put("risk", planEdge.path("risk").asText(null));
            plan.put("rationale", planEdge.path("rationale").asText(
                    planEdge.path("exists_because").asText(null)));
            plan.put("deterministic_coverage", planEdge.path("deterministic_coverage").asDouble());
            plan.put("expected_residual", planEdge.path("expected_residual").asDouble());
            plan.put("mandatory_checkpoint", planEdge.path("mandatory_checkpoint").asBoolean());
            plan.put("approval_required", planEdge.path("approval_required").asBoolean());
            plan.set("recipes", planEdge.path("ordered_transformations").isMissingNode()
                    ? Json.arr() : planEdge.path("ordered_transformations"));
            plan.set("recipes_without_capability",
                    planEdge.path("recipes_without_available_capability").isMissingNode()
                            ? Json.arr() : planEdge.path("recipes_without_available_capability"));
            plan.set("facts", planEdge.path("knowledge_refs").isMissingNode()
                    ? Json.arr() : planEdge.path("knowledge_refs"));
            plan.set("impacts", planEdge.path("impact_refs").isMissingNode()
                    ? Json.arr() : planEdge.path("impact_refs"));
            plan.set("required_validation_dimensions",
                    planEdge.path("required_validation_dimensions").isMissingNode()
                            ? Json.arr() : planEdge.path("required_validation_dimensions"));
            node.set("plan", plan);
        } else {
            node.putNull("plan");
        }

        List<ObjectNode> attempts = new ArrayList<>();
        Map<String, ObjectNode> latestByStage = new LinkedHashMap<>();
        for (ObjectNode entry : timeline) {
            if (!edgeId.equals(entry.path("edge_id").asText(null))) {
                continue;
            }
            ObjectNode attempt = entry.deepCopy();
            JsonNode execution = readExecutionRecord(output, entry);
            if (execution != null) {
                // Referenced, not inlined: the full record is large and already durable.
                attempt.put("execution_record",
                        entry.path("stage_id").asText() + "/" + entry.path("attempt_directory").asText()
                                + "/" + RunJournal.STAGE_EXECUTION_FILE);
                attempt.set("mutation_summary", execution.path("mutation_summary"));
                attempt.set("validation_summary", execution.path("validation_summary"));
                attempt.set("blind_spots", execution.path("blind_spots"));
                attempt.set("errors", execution.path("errors"));
                attempt.set("warnings", execution.path("warnings"));
                attempt.set("decisions", execution.path("decisions"));
                attempt.set("fallbacks", execution.path("fallbacks"));
                attempt.set("retries", execution.path("retries"));
                attempt.set("commands", execution.path("commands"));
            }
            attempts.add(attempt);
            latestByStage.put(entry.path("stage_id").asText(), attempt);
        }
        node.set("attempts", Json.toTree(attempts));

        List<ObjectNode> stageSummaries = new ArrayList<>();
        for (String stage : EDGE_STAGES) {
            ObjectNode summary = Json.obj();
            summary.put("stage_id", stage);
            ObjectNode attempt = latestByStage.get(stage);
            if (attempt == null) {
                // Explicitly not executed, rather than absent. An edge that stopped at 13 must not
                // read as an edge whose runtime validation passed silently.
                summary.put("executed", false);
                summary.put("status", "NOT_EXECUTED");
                summary.put("note", "This stage was never attempted for this edge.");
            } else {
                summary.put("executed", true);
                summary.put("status", attempt.path("status").asText());
                summary.put("attempt_id", attempt.path("attempt_id").asText(null));
                summary.put("attempt_directory", attempt.path("attempt_directory").asText(null));
                summary.put("summary", attempt.path("summary").asText(null));
                summary.put("duration_ms", attempt.path("duration_ms").asLong());
                summary.set("mutation_summary", attempt.path("mutation_summary"));
                summary.set("validation_summary", attempt.path("validation_summary"));
            }
            stageSummaries.add(summary);
        }
        node.set("stages", Json.toTree(stageSummaries));

        node.put("attempt_count", attempts.size());
        node.put("result", classify(stageSummaries, attempts));
        node.put("blocking_reason", blockingReason(attempts));
        return node;
    }

    /**
     * The edge's outcome.
     *
     * <p>Completion requires every edge stage to have executed successfully. An edge whose later
     * stages simply never ran is INCOMPLETE, not complete - the distinction the final report depends
     * on.
     */
    private static String classify(List<ObjectNode> stageSummaries, List<ObjectNode> attempts) {
        if (attempts.isEmpty()) {
            return "NOT_STARTED";
        }
        for (ObjectNode attempt : attempts) {
            String status = attempt.path("status").asText();
            if (status.equals("BLOCKED")) {
                return "BLOCKED";
            }
            if (status.equals("FAILED") || status.equals("CRASHED")) {
                return "FAILED";
            }
            if (status.equals("REFUSED")) {
                return "REFUSED";
            }
        }
        boolean allExecuted = stageSummaries.stream().allMatch(s -> s.path("executed").asBoolean());
        return allExecuted ? "COMPLETE" : "INCOMPLETE";
    }

    private static String blockingReason(List<ObjectNode> attempts) {
        for (ObjectNode attempt : attempts) {
            String status = attempt.path("status").asText();
            if (status.equals("BLOCKED") || status.equals("FAILED") || status.equals("CRASHED")
                    || status.equals("REFUSED")) {
                String stop = attempt.path("stop_reason").asText(null);
                return stop != null && !stop.isBlank() ? stop
                        : attempt.path("summary").asText(null);
            }
        }
        return null;
    }

    private static JsonNode readExecutionRecord(OutputLayout output, ObjectNode entry) {
        String stage = entry.path("stage_id").asText(null);
        String directory = entry.path("attempt_directory").asText(null);
        if (stage == null || directory == null || directory.isBlank()) {
            return null;
        }
        Path file = output.stageRoot(stage).resolve(directory).resolve(RunJournal.STAGE_EXECUTION_FILE);
        if (!Files.isRegularFile(file)) {
            // A refused attempt writes its journal under journal/<attemptId> instead.
            file = output.stageRoot(stage).resolve(RunJournal.JOURNAL_DIRECTORY).resolve(directory)
                    .resolve(RunJournal.STAGE_EXECUTION_FILE);
        }
        try {
            return Files.isRegularFile(file) ? Json.read(file) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
