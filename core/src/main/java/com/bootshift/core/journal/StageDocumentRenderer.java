package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders {@code STAGE_DOCUMENT.md} from a stage execution record.
 *
 * <p>The renderer reads the serialized record rather than the live object, so the Markdown is
 * provably a view of the JSON: there is one source of truth and no second code path that can drift
 * away from it. It also means a stage document can be regenerated from a {@code stage-execution.json}
 * on disk months later, without the run that produced it.
 *
 * <p>Sections that do not apply to a stage say so explicitly. Silently dropping them would leave a
 * reader unable to distinguish "this stage performs no mutations" from "the mutation section was
 * lost", and in an audit document those are very different statements.
 */
public final class StageDocumentRenderer {

    private StageDocumentRenderer() {
    }

    public static String render(StageExecutionRecord record, String recordHash) {
        return render(record.toNode(), recordHash);
    }

    public static String render(JsonNode r, String recordHash) {
        StringBuilder sb = new StringBuilder();
        header(sb, r);
        purpose(sb, r);
        whyItRan(sb, r);
        stateTransition(sb, r);
        preconditions(sb, r);
        inputs(sb, r);
        plannedSteps(sb, r);
        actualSteps(sb, r);
        commands(sb, r);
        decisions(sb, r);
        references(sb, r, "10. Evidence used", "evidence_references",
                "This stage referenced no evidence objects.");
        references(sb, r, "11. Migration documents used", "document_references",
                "This stage consumed no migration documentation.");
        references(sb, r, "12. Migration facts used or produced", "migration_fact_references",
                "This stage neither consumed nor produced migration facts.");
        references(sb, r, "13. Impact analysis involved", "impact_references",
                "No impact findings were involved in this stage.");
        mutations(sb, r);
        validation(sb, r);
        retriesAndFallbacks(sb, r);
        warnings(sb, r);
        errors(sb, r);
        blindSpots(sb, r);
        outputs(sb, r);
        result(sb, r);
        nextAction(sb, r);
        integrity(sb, r, recordHash);
        return sb.toString();
    }

    private static void header(StringBuilder sb, JsonNode r) {
        String stageId = r.path("stage_id").asText("unknown");
        sb.append("# Stage ").append(Markdown.text(stageId));
        String name = r.path("stage_name").asText(null);
        if (name != null && !name.isBlank()) {
            sb.append(" — ").append(Markdown.text(name));
        }
        sb.append("\n\n");

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Run", Markdown.code(r.path("run_id").asText(null))));
        rows.add(List.of("Attempt", Markdown.code(r.path("attempt_id").asText(null))));
        rows.add(List.of("Edge", r.path("edge_id").isNull() || r.path("edge_id").asText("").isBlank()
                ? "Not an edge-scoped stage" : Markdown.code(r.path("edge_id").asText())));
        rows.add(List.of("Status", "**" + Markdown.cell(r.path("status").asText()) + "**"));
        rows.add(List.of("Start", Markdown.cell(r.path("started_at").asText(null))));
        rows.add(List.of("End", Markdown.cell(r.path("finished_at").asText(null))));
        rows.add(List.of("Duration", Markdown.cell(Markdown.duration(r.path("duration_ms").asLong()))));
        Markdown.table(sb, List.of("Field", "Value"), rows, "No attempt metadata recorded.");

        String summary = r.path("summary").asText(null);
        if (summary != null && !summary.isBlank()) {
            sb.append("> ").append(Markdown.text(summary)).append("\n\n");
        }
        sb.append("---\n\n");
    }

    private static void purpose(StringBuilder sb, JsonNode r) {
        sb.append("## 1. Purpose\n\n");
        sb.append(Markdown.text(r.path("purpose").asText(null))).append("\n\n");
    }

    private static void whyItRan(StringBuilder sb, JsonNode r) {
        sb.append("## 2. Why this stage ran\n\n");
        String trigger = r.path("trigger").asText("PIPELINE");
        String detail = r.path("trigger_detail").asText(null);
        sb.append(switch (trigger) {
            case "CLI" -> "An operator invoked this stage directly from the command line.";
            case "EDGE_LOOP" -> "The migration edge loop reached this stage for edge `"
                    + Markdown.cell(r.path("edge_id").asText("?")) + "`.";
            case "RETRY" -> "This is a retry of a previous attempt at the same stage.";
            case "RESUME" -> "A resumed run re-entered this stage.";
            default -> "The pipeline orchestrator reached this stage in sequence.";
        });
        sb.append("\n\n");
        if (detail != null && !detail.isBlank()) {
            sb.append(Markdown.text(detail)).append("\n\n");
        }
    }

    private static void stateTransition(StringBuilder sb, JsonNode r) {
        sb.append("## 3. State transition\n\n");
        String before = r.path("state_before").asText(null);
        String after = r.path("state_after").asText(null);
        sb.append("| Before | After |\n|  --- |  --- |\n| ")
                .append(Markdown.code(before)).append(" | ").append(Markdown.code(after))
                .append(" |\n\n");
        if (after == null || after.isBlank() || after.equals(before)) {
            sb.append("The run state did not advance in this attempt.\n\n");
        }
    }

    private static void preconditions(StringBuilder sb, JsonNode r) {
        sb.append("## 4. Preconditions\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode p : r.path("precondition_results")) {
            rows.add(List.of(
                    Markdown.code(p.path("precondition").asText()),
                    p.path("satisfied").asBoolean() ? "satisfied" : "**NOT satisfied**",
                    Markdown.cell(p.path("detail").asText(null)),
                    Markdown.cell(p.path("remediation").asText(null))));
        }
        Markdown.table(sb, List.of("Precondition", "Result", "Detail", "Remediation"), rows,
                "This stage declared no preconditions.");
    }

    private static void inputs(StringBuilder sb, JsonNode r) {
        sb.append("## 5. Input artifacts\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode i : r.path("resolved_inputs")) {
            rows.add(List.of(
                    Markdown.code(i.path("artifact").asText()),
                    i.path("present").asBoolean() ? "resolved" : "**missing**",
                    Markdown.code(Markdown.shortHash(i.path("hash").asText(null)))));
        }
        Markdown.table(sb, List.of("Artifact", "Status", "Hash"), rows,
                "This stage consumes no upstream artifacts.");
    }

    private static void plannedSteps(StringBuilder sb, JsonNode r) {
        sb.append("## 6. Planned execution steps\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode s : r.path("executed_steps")) {
            rows.add(List.of(
                    Markdown.code(s.path("step_id").asText()),
                    Markdown.cell(s.path("name").asText()),
                    Markdown.cell(s.path("purpose").asText(null))));
        }
        Markdown.table(sb, List.of("Step", "Name", "Purpose"), rows,
                "This stage declares no step plan.");
    }

    private static void actualSteps(StringBuilder sb, JsonNode r) {
        sb.append("## 7. Actual execution steps\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode s : r.path("executed_steps")) {
            String status = s.path("status").asText("PENDING");
            rows.add(List.of(
                    Markdown.code(s.path("step_id").asText()),
                    "PENDING".equals(status) ? "**PENDING (never ran)**" : Markdown.cell(status),
                    Markdown.cell(Markdown.duration(s.path("duration_ms").asLong())),
                    Markdown.cell(s.path("status_reason").asText(null))));
        }
        Markdown.table(sb, List.of("Step", "Status", "Duration", "Note"), rows,
                "No steps were instrumented for this stage.");

        JsonNode unexecuted = r.path("unexecuted_steps");
        if (unexecuted.isArray() && !unexecuted.isEmpty()) {
            sb.append("**").append(unexecuted.size())
                    .append(" declared step(s) never executed.** A step declared and not run is a "
                            + "capability this stage claims but did not exercise on this attempt:\n\n");
            List<String> ids = new ArrayList<>();
            unexecuted.forEach(n -> ids.add(n.asText()));
            Markdown.bullets(sb, ids, "");
        }
    }

    private static void commands(StringBuilder sb, JsonNode r) {
        sb.append("## 8. Tools and commands executed\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode c : r.path("commands")) {
            rows.add(List.of(
                    Markdown.code(c.path("command_id").asText()),
                    Markdown.code(c.path("sanitized_command").asText()),
                    Markdown.cell(c.path("exit_code").asInt()),
                    c.path("timed_out").asBoolean() ? "**timed out**" : "no",
                    Markdown.cell(Markdown.duration(c.path("duration_ms").asLong())),
                    Markdown.cell(c.path("result").asText())));
        }
        Markdown.table(sb, List.of("Id", "Command", "Exit", "Timed out", "Duration", "Result"), rows,
                "This stage launched no external processes.");

        List<List<String>> tools = new ArrayList<>();
        for (JsonNode t : r.path("tool_invocations")) {
            tools.add(List.of(
                    Markdown.cell(t.path("tool").asText()),
                    Markdown.cell(t.path("operation").asText()),
                    Markdown.cell(t.path("outcome").asText()),
                    Markdown.cell(t.path("detail").asText(null))));
        }
        if (!tools.isEmpty()) {
            sb.append("### In-process tool invocations\n\n");
            Markdown.table(sb, List.of("Tool", "Operation", "Outcome", "Detail"), tools, "");
        }
        sb.append("> Command arguments are redacted before they are written. Process output is not "
                + "copied into this document; where a log was captured it is referenced above.\n\n");
    }

    private static void decisions(StringBuilder sb, JsonNode r) {
        sb.append("## 9. Decisions made\n\n");
        JsonNode decisions = r.path("decisions");
        if (!decisions.isArray() || decisions.isEmpty()) {
            sb.append("This stage recorded no decisions.\n\n");
            return;
        }
        for (JsonNode d : decisions) {
            sb.append("### ").append(Markdown.text(d.path("decision_id").asText()))
                    .append(" — ").append(Markdown.text(d.path("type").asText())).append("\n\n");
            List<List<String>> rows = new ArrayList<>();
            rows.add(List.of("Subject", Markdown.code(d.path("subject").asText(null))));
            rows.add(List.of("Decision", Markdown.cell(d.path("decision").asText(null))));
            rows.add(List.of("Reason", Markdown.cell(d.path("reason").asText(null))));
            rows.add(List.of("Confidence", Markdown.cell(d.path("confidence").asText(null))));
            rows.add(List.of("Made by", Markdown.cell(d.path("made_by").asText(null))));
            rows.add(List.of("Authorized by", Markdown.cell(d.path("authorized_by").asText(null))));
            Markdown.table(sb, List.of("Field", "Value"), rows, "");
            appendRefList(sb, d, "alternatives_considered", "Alternatives considered");
            appendRefList(sb, d, "evidence_refs", "Evidence");
            appendRefList(sb, d, "document_refs", "Documents");
            appendRefList(sb, d, "policy_refs", "Policies");
        }
    }

    private static void appendRefList(StringBuilder sb, JsonNode parent, String field, String label) {
        JsonNode list = parent.path(field);
        if (!list.isArray() || list.isEmpty()) {
            return;
        }
        sb.append("*").append(label).append(":* ");
        List<String> items = new ArrayList<>();
        list.forEach(n -> items.add(Markdown.code(n.asText())));
        sb.append(String.join(", ", items)).append("\n\n");
    }

    private static void references(StringBuilder sb, JsonNode r, String heading, String field,
                                   String emptyState) {
        sb.append("## ").append(heading).append("\n\n");
        JsonNode list = r.path(field);
        if (!list.isArray() || list.isEmpty()) {
            sb.append(emptyState).append("\n\n");
            return;
        }
        // Long reference lists are summarised rather than dumped: the artifact is authoritative and
        // a document that reprints a thousand ids is not more auditable, only longer.
        int limit = 40;
        List<String> items = new ArrayList<>();
        int index = 0;
        for (JsonNode n : list) {
            if (index++ >= limit) {
                break;
            }
            items.add(Markdown.code(n.asText()));
        }
        sb.append(list.size()).append(" reference(s)");
        if (list.size() > limit) {
            sb.append(", first ").append(limit).append(" shown");
        }
        sb.append(": ").append(String.join(", ", items)).append("\n\n");
    }

    private static void mutations(StringBuilder sb, JsonNode r) {
        sb.append("## 14. Source mutations\n\n");
        JsonNode summary = r.path("mutation_summary");
        if (!summary.isObject() || summary.isEmpty()) {
            sb.append("This stage does not write to application source.\n\n");
            return;
        }
        keyValueTable(sb, summary);
        sb.append("> Every mutation counted here passed through `FileMutationGateway`. "
                + "The authoritative record is the change ledger.\n\n");
    }

    private static void validation(StringBuilder sb, JsonNode r) {
        sb.append("## 15. Validation performed\n\n");
        JsonNode summary = r.path("validation_summary");
        if (!summary.isObject() || summary.isEmpty()) {
            sb.append("This stage performs no validation of its own.\n\n");
            return;
        }
        keyValueTable(sb, summary);
    }

    private static void keyValueTable(StringBuilder sb, JsonNode object) {
        List<List<String>> rows = new ArrayList<>();
        object.fields().forEachRemaining(e -> rows.add(List.of(
                Markdown.code(e.getKey()),
                Markdown.cell(e.getValue().isValueNode() ? e.getValue().asText()
                        : e.getValue().toString()))));
        Markdown.table(sb, List.of("Measure", "Value"), rows, "Nothing recorded.");
    }

    private static void retriesAndFallbacks(StringBuilder sb, JsonNode r) {
        sb.append("## 16. Retries and fallback paths\n\n");
        List<List<String>> retries = new ArrayList<>();
        for (JsonNode n : r.path("retries")) {
            retries.add(List.of(
                    Markdown.cell(n.path("attempt").asInt()),
                    Markdown.cell(n.path("reason").asText(null)),
                    Markdown.cell(n.path("outcome").asText(null))));
        }
        sb.append("### Retries\n\n");
        Markdown.table(sb, List.of("Attempt", "Reason", "Outcome"), retries,
                "No retries occurred.");

        List<List<String>> fallbacks = new ArrayList<>();
        for (JsonNode n : r.path("fallbacks")) {
            fallbacks.add(List.of(
                    Markdown.cell(n.path("from").asText(null)),
                    Markdown.cell(n.path("to").asText(null)),
                    Markdown.cell(n.path("reason").asText(null)),
                    Markdown.cell(n.path("consequence").asText(null))));
        }
        sb.append("### Fallbacks\n\n");
        Markdown.table(sb, List.of("Intended", "Used", "Reason", "Consequence for confidence"),
                fallbacks, "The stage completed by its primary method; no fallback was used.");
    }

    private static void warnings(StringBuilder sb, JsonNode r) {
        sb.append("## 17. Warnings\n\n");
        List<String> items = new ArrayList<>();
        r.path("warnings").forEach(n -> items.add(n.asText()));
        Markdown.bullets(sb, items, "No warnings.");
    }

    private static void errors(StringBuilder sb, JsonNode r) {
        sb.append("## 18. Errors and blockers\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode n : r.path("errors")) {
            rows.add(List.of(
                    Markdown.cell(n.path("kind").asText(null)),
                    Markdown.cell(n.path("message").asText(null)),
                    Markdown.cell(n.path("remediation").asText(null))));
        }
        Markdown.table(sb, List.of("Kind", "Message", "Remediation"), rows, "No errors.");

        String stop = r.path("stop_reason").asText(null);
        if (stop != null && !stop.isBlank()) {
            sb.append("**What stopped the pipeline here:** ").append(Markdown.text(stop))
                    .append("\n\n");
        }
    }

    private static void blindSpots(StringBuilder sb, JsonNode r) {
        sb.append("## 19. Blind spots and unknowns\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode n : r.path("blind_spots")) {
            rows.add(List.of(
                    Markdown.code(n.path("id").asText(null)),
                    Markdown.cell(n.path("dimension").asText(null)),
                    Markdown.cell(n.path("description").asText(null)),
                    Markdown.cell(n.path("impact").asText(null))));
        }
        Markdown.table(sb, List.of("Id", "Dimension", "Description", "Consequence"), rows,
                "This stage recorded no blind spots. That is a statement about this stage only.");
    }

    private static void outputs(StringBuilder sb, JsonNode r) {
        sb.append("## 20. Output artifacts\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode n : r.path("outputs")) {
            rows.add(List.of(
                    Markdown.code(n.path("name").asText()),
                    Markdown.code(Markdown.shortHash(n.path("hash").asText(null)))));
        }
        Markdown.table(sb, List.of("Artifact", "SHA-256"), rows,
                "This attempt published no artifacts.");
        String dir = r.path("attempt_directory").asText(null);
        if (dir != null && !dir.isBlank()) {
            sb.append("Attempt directory: ").append(Markdown.code(dir)).append("\n\n");
        }
    }

    private static void result(StringBuilder sb, JsonNode r) {
        sb.append("## 21. Result\n\n");
        String status = r.path("status").asText("INCOMPLETE");
        sb.append("**").append(Markdown.cell(status)).append("**");
        String summary = r.path("summary").asText(null);
        if (summary != null && !summary.isBlank()) {
            sb.append(" — ").append(Markdown.text(summary));
        }
        sb.append("\n\n");
        sb.append(switch (status) {
            case "SUCCESS" -> "The stage did what it declared it would do.";
            case "DEGRADED" -> "The stage produced a result by a weaker route than declared. "
                    + "See section 16 for what that costs downstream.";
            case "REFUSED" -> "The stage refused to run because a declared precondition did not "
                    + "hold. Nothing was attempted and nothing was changed.";
            case "BLOCKED" -> "The stage ran and stopped on a finding it is not permitted to "
                    + "decide on its own. This is the harness working as designed.";
            case "FAILED" -> "The stage ran and failed.";
            case "CRASHED" -> "The stage threw an exception it did not handle. No conclusion from "
                    + "this attempt should be relied on.";
            default -> "The stage started and no terminal outcome was recorded.";
        });
        sb.append("\n\n");
    }

    private static void nextAction(StringBuilder sb, JsonNode r) {
        sb.append("## 22. Next action\n\n");
        String next = r.path("next_action").asText(null);
        sb.append(next == null || next.isBlank()
                ? "No follow-up action was recorded for this attempt."
                : Markdown.text(next)).append("\n\n");
    }

    private static void integrity(StringBuilder sb, JsonNode r, String recordHash) {
        sb.append("## 23. Integrity and provenance\n\n");
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Run id", Markdown.code(r.path("run_id").asText(null))));
        rows.add(List.of("Attempt id", Markdown.code(r.path("attempt_id").asText(null))));
        rows.add(List.of("Stage id", Markdown.code(r.path("stage_id").asText(null))));
        rows.add(List.of("Edge id", Markdown.code(r.path("edge_id").asText(null))));
        rows.add(List.of("Generated from", Markdown.code(RunJournal.STAGE_EXECUTION_FILE)));
        rows.add(List.of("Execution record SHA-256", Markdown.code(recordHash)));
        rows.add(List.of("Primary artifact hash",
                Markdown.code(Markdown.shortHash(r.path("primary_artifact_hash").asText(null)))));
        rows.add(List.of("Policy hash",
                Markdown.code(Markdown.shortHash(r.path("policy_hash").asText(null)))));
        rows.add(List.of("Tool version", Markdown.code(r.path("tool_version").asText(null))));
        rows.add(List.of("Schema version", Markdown.code(r.path("schema_version").asText(null))));
        Markdown.table(sb, List.of("Field", "Value"), rows, "");

        String renderingError = r.path("rendering_error").asText(null);
        if (renderingError != null && !renderingError.isBlank()) {
            sb.append("> **Documentation defect recorded on this attempt:** ")
                    .append(Markdown.text(renderingError)).append("\n\n");
        }
        sb.append("This document is generated from the execution record named above. The JSON is "
                + "authoritative; this file is a rendering of it and holds no facts of its own.\n");
    }
}
