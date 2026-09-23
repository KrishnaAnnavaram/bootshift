package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.OutputLayout;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders {@code RUN_DOCUMENT.md}: everything that has happened in this run so far.
 *
 * <p>Distinct from the final migration document, and the distinction is the reason this exists. The
 * migration document is a consolidated account of a migration, and it is written by the evidence
 * stage - which a run that fails at stage 13 never reaches. This document is rewritten after every
 * stage attempt, so the runs that most need explaining are the ones that have it.
 *
 * <p>Everything here is derived from the timeline and the published artifact plane. Nothing is read
 * from console output, and nothing is inferred: a section with no artifact behind it says so rather
 * than guessing.
 */
public final class RunDocumentRenderer {

    private RunDocumentRenderer() {
    }

    public static String render(RunJournal journal) {
        OutputLayout output = journal.output();
        List<ObjectNode> timeline = journal.timeline();
        StringBuilder sb = new StringBuilder();

        JsonNode bootstrap = read(output, "00-bootstrap", "bootstrap.json");
        JsonNode inventory = read(output, "01-inventory", "inventory-artifact.json");
        JsonNode build = read(output, "02-build", "build-model.json");
        JsonNode target = read(output, "06-target", "target-state.json");
        JsonNode path = read(output, "06-target", "migration-path.json");
        JsonNode plan = read(output, "11-plan", "edge-plan.json");

        header(sb, journal, timeline, bootstrap, target);
        executiveSummary(sb, timeline, plan);
        pipelineState(sb, journal, timeline);
        sourceSummary(sb, inventory, build);
        targetDecision(sb, target);
        migrationPath(sb, path, plan);
        stageTimeline(sb, timeline);
        stageSummaries(sb, timeline);
        edgeTimeline(sb, timeline, plan);
        changes(sb, timeline);
        validation(sb, timeline);
        blockers(sb, timeline);
        humanDecisions(sb, timeline);
        warnings(sb, timeline);
        gaps(sb, journal, timeline);
        blindSpots(sb, timeline);
        evidence(sb, output, timeline);
        nextAction(sb, timeline);
        integrity(sb, journal, timeline);
        return sb.toString();
    }

    private static JsonNode read(OutputLayout output, String stage, String artifact) {
        try {
            return output.readLatest(stage, artifact);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ObjectNode last(List<ObjectNode> timeline) {
        return timeline.isEmpty() ? null : timeline.get(timeline.size() - 1);
    }

    private static void header(StringBuilder sb, RunJournal journal, List<ObjectNode> timeline,
                               JsonNode bootstrap, JsonNode target) {
        sb.append("# Bootshift Migration Run Document\n\n");
        ObjectNode latest = last(timeline);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Run id", Markdown.code(journal.runId())));
        rows.add(List.of("Source", Markdown.code(sourceRoot(bootstrap))));
        rows.add(List.of("Started", Markdown.cell(timeline.isEmpty() ? null
                : timeline.get(0).path("start").asText(null))));
        rows.add(List.of("Current status", "**" + Markdown.cell(runStatus(timeline)) + "**"));
        rows.add(List.of("Current stage", latest == null ? Markdown.ABSENT
                : Markdown.code(latest.path("stage_id").asText())));
        rows.add(List.of("Current edge", latest == null || latest.path("edge_id").isNull()
                ? "Not in the edge loop" : Markdown.code(latest.path("edge_id").asText())));
        rows.add(List.of("Migrating from", Markdown.code(text(target, "source_version"))));
        rows.add(List.of("Landing target", Markdown.code(resolvedTarget(target))));
        rows.add(List.of("Document generated", Markdown.cell(Instant.now().toString())));
        Markdown.table(sb, List.of("Field", "Value"), rows, "");
        sb.append("> This document is rewritten after every stage attempt. It describes what has "
                + "happened so far, not what is planned. If the run is still going, this is a "
                + "snapshot.\n\n---\n\n");
    }

    /**
     * The run's status, derived from attempts rather than from a stored cursor.
     *
     * <p>A cursor says where a run got to; the attempts say what actually happened, and the two
     * disagree exactly when something went wrong.
     */
    private static String runStatus(List<ObjectNode> timeline) {
        ObjectNode latest = last(timeline);
        if (latest == null) {
            return "NOT STARTED";
        }
        String status = latest.path("status").asText("INCOMPLETE");
        return switch (status) {
            case "SUCCESS", "DEGRADED" -> "RUNNING";
            case "BLOCKED" -> "BLOCKED — human decision required";
            case "REFUSED" -> "REFUSED — precondition not met";
            case "FAILED" -> "FAILED";
            case "CRASHED" -> "FAILED — unhandled exception";
            default -> "INCOMPLETE";
        };
    }

    private static void executiveSummary(StringBuilder sb, List<ObjectNode> timeline, JsonNode plan) {
        sb.append("## 1. Executive summary\n\n");
        long success = timeline.stream().filter(e -> e.path("status").asText().equals("SUCCESS")).count();
        long failed = timeline.stream().filter(e -> {
            String s = e.path("status").asText();
            return s.equals("FAILED") || s.equals("CRASHED");
        }).count();
        long blocked = timeline.stream().filter(e -> e.path("status").asText().equals("BLOCKED")).count();
        long refused = timeline.stream().filter(e -> e.path("status").asText().equals("REFUSED")).count();
        long commands = timeline.stream().mapToLong(e -> e.path("command_count").asLong()).sum();

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Stage attempts", String.valueOf(timeline.size())));
        rows.add(List.of("Succeeded", String.valueOf(success)));
        rows.add(List.of("Degraded", String.valueOf(timeline.stream()
                .filter(e -> e.path("status").asText().equals("DEGRADED")).count())));
        rows.add(List.of("Refused", String.valueOf(refused)));
        rows.add(List.of("Blocked", String.valueOf(blocked)));
        rows.add(List.of("Failed or crashed", String.valueOf(failed)));
        rows.add(List.of("External commands run", String.valueOf(commands)));
        rows.add(List.of("Edges planned", plan == null ? Markdown.ABSENT
                : String.valueOf(plan.path("edges").size())));
        Markdown.table(sb, List.of("Measure", "Value"), rows, "Nothing has run yet.");
    }

    private static void pipelineState(StringBuilder sb, RunJournal journal, List<ObjectNode> timeline) {
        sb.append("## 2. Current pipeline state\n\n");
        ObjectNode latest = last(timeline);
        if (latest == null) {
            sb.append("No stage has been attempted in this run.\n\n");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Last stage attempted", Markdown.code(latest.path("stage_id").asText())));
        rows.add(List.of("Attempt id", Markdown.code(latest.path("attempt_id").asText())));
        rows.add(List.of("Outcome", "**" + Markdown.cell(latest.path("status").asText()) + "**"));
        rows.add(List.of("Run state after", Markdown.code(latest.path("state_after").asText(null))));
        rows.add(List.of("Stop reason", Markdown.cell(latest.path("stop_reason").asText(null))));
        Markdown.table(sb, List.of("Field", "Value"), rows, "");
    }

    private static void sourceSummary(StringBuilder sb, JsonNode inventory, JsonNode build) {
        sb.append("## 3. Source application summary\n\n");
        if (inventory == null) {
            sb.append("Inventory has not run, so the source application has not been described yet."
                    + "\n\n");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Files", Markdown.cell(inventory.path("file_count").asInt())));
        rows.add(List.of("Modules", Markdown.cell(inventory.path("modules").size())));
        if (build != null) {
            JsonNode frameworks = build.path("frameworks");
            rows.add(List.of("Build system", Markdown.cell(build.path("kind").asText(null))));
            rows.add(List.of("Build model authoritative",
                    build.path("authoritative").asBoolean() ? "yes"
                            : "**no** - " + Markdown.cell(build.path("degraded_reason").asText(null))));
            rows.add(List.of("Spring Boot", Markdown.code(frameworks.path("spring-boot").asText(null))));
            rows.add(List.of("Spring Cloud", Markdown.code(frameworks.path("spring-cloud").asText(null))));
            rows.add(List.of("Spring Framework",
                    Markdown.code(frameworks.path("spring-framework").asText(null))));
            rows.add(List.of("Java", Markdown.code(frameworks.path("java").asText(null))));
        }
        Markdown.table(sb, List.of("Property", "Value"), rows, "");
    }

    private static void targetDecision(StringBuilder sb, JsonNode target) {
        sb.append("## 4. Target decision\n\n");
        if (target == null) {
            sb.append("The target has not been resolved yet.\n\n");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("From", Markdown.code(text(target, "source_version"))
                + " on Java " + Markdown.code(text(target, "source_java"))));
        rows.add(List.of("Landing target", Markdown.code(resolvedTarget(target))));
        rows.add(List.of("Landing Java", Markdown.code(text(target, "landing_java_version"))));
        rows.add(List.of("Spring Cloud train",
                Markdown.code(text(target, "landing_spring_cloud_train"))));
        rows.add(List.of("Selection mode", Markdown.cell(text(target, "selection_mode"))));
        rows.add(List.of("Support horizon",
                Markdown.cell(text(target, "support_horizon_months")) + " month(s)"));
        rows.add(List.of("Lifecycle evidence",
                Markdown.cell(text(target, "lifecycle_evidence_quality"))));
        rows.add(List.of("Frozen", target.path("frozen").asBoolean() ? "yes" : "no"));
        Markdown.table(sb, List.of("Field", "Value"), rows, "");
        sb.append("Why this target and not the others: section 9 of the `06-target` stage document "
                + "records one decision per candidate, each with the rule that rejected it.\n\n");
    }

    private static void migrationPath(StringBuilder sb, JsonNode path, JsonNode plan) {
        sb.append("## 5. Migration path\n\n");
        JsonNode edges = plan != null ? plan.path("edges")
                : path != null ? path.path("edges") : null;
        if (edges == null || !edges.isArray() || edges.isEmpty()) {
            sb.append("No migration path has been frozen yet.\n\n");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode e : edges) {
            rows.add(List.of(
                    Markdown.code(e.path("edge_id").asText()),
                    Markdown.cell(e.path("edge_class").asText(null)),
                    Markdown.code(e.path("source_state").asText(null)),
                    Markdown.code(e.path("target_state").asText(null)),
                    Markdown.code(e.path("edge_java").asText(null)),
                    Markdown.cell(e.path("risk").asText(null)),
                    Markdown.cell(e.path("frozen_validation_depth").asText(null))));
        }
        Markdown.table(sb, List.of("Edge", "Class", "From", "To", "Java", "Risk",
                "Validation depth"), rows, "");
        sb.append("Each edge's full record - facts in force, impacts, recipes and what each stage "
                + "did - is in `").append(RunJournal.EDGES_DIRECTORY).append("/<edge-id>/")
                .append(RunJournal.EDGE_DOCUMENT_FILE)
                .append("` once the edge has been attempted.\n\n");
    }

    private static void stageTimeline(StringBuilder sb, List<ObjectNode> timeline) {
        sb.append("## 6. Stage timeline\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (ObjectNode e : timeline) {
            rows.add(List.of(
                    Markdown.code(e.path("stage_id").asText()),
                    e.path("edge_id").isNull() ? Markdown.ABSENT
                            : Markdown.code(e.path("edge_id").asText()),
                    statusCell(e.path("status").asText()),
                    Markdown.cell(Markdown.duration(e.path("duration_ms").asLong())),
                    Markdown.cell(e.path("start").asText(null))));
        }
        Markdown.table(sb, List.of("Stage", "Edge", "Status", "Duration", "Started"), rows,
                "No stage attempts recorded.");
        sb.append("Machine-readable form: ").append(Markdown.code(RunJournal.TIMELINE_FILE))
                .append("\n\n");
    }

    private static String statusCell(String status) {
        return switch (status) {
            case "SUCCESS" -> "SUCCESS";
            case "FAILED", "CRASHED", "BLOCKED", "REFUSED" -> "**" + status + "**";
            default -> Markdown.cell(status);
        };
    }

    private static void stageSummaries(StringBuilder sb, List<ObjectNode> timeline) {
        sb.append("## 7. Stage execution summaries\n\n");
        if (timeline.isEmpty()) {
            sb.append("No stage attempts recorded.\n\n");
            return;
        }
        for (ObjectNode e : timeline) {
            sb.append("### ").append(Markdown.text(e.path("stage_id").asText()));
            if (!e.path("edge_id").isNull() && !e.path("edge_id").asText("").isBlank()) {
                sb.append(" · ").append(Markdown.text(e.path("edge_id").asText()));
            }
            sb.append(" — ").append(e.path("status").asText()).append("\n\n");
            String summary = e.path("summary").asText(null);
            if (summary != null && !summary.isBlank()) {
                sb.append(Markdown.text(summary)).append("\n\n");
            }
            List<String> facts = new ArrayList<>();
            facts.add("Duration " + Markdown.duration(e.path("duration_ms").asLong()));
            facts.add(e.path("command_count").asInt() + " command(s)");
            facts.add(e.path("decision_count").asInt() + " decision(s)");
            if (e.path("warning_count").asInt() > 0) {
                facts.add(e.path("warning_count").asInt() + " warning(s)");
            }
            if (e.path("error_count").asInt() > 0) {
                facts.add("**" + e.path("error_count").asInt() + " error(s)**");
            }
            if (e.path("unexecuted_step_count").asInt() > 0) {
                facts.add("**" + e.path("unexecuted_step_count").asInt()
                        + " declared step(s) never ran**");
            }
            sb.append(String.join(" · ", facts)).append("\n\n");
            String dir = e.path("attempt_directory").asText(null);
            if (dir != null && !dir.isBlank()) {
                sb.append("Detail: ").append(Markdown.code(e.path("stage_id").asText() + "/" + dir
                        + "/" + RunJournal.STAGE_DOCUMENT_FILE)).append("\n\n");
            }
        }
    }

    private static void edgeTimeline(StringBuilder sb, List<ObjectNode> timeline, JsonNode plan) {
        sb.append("## 8. Edge timeline\n\n");
        Map<String, List<ObjectNode>> byEdge = new LinkedHashMap<>();
        for (ObjectNode e : timeline) {
            if (e.path("edge_id").isNull() || e.path("edge_id").asText("").isBlank()) {
                continue;
            }
            byEdge.computeIfAbsent(e.path("edge_id").asText(), k -> new ArrayList<>()).add(e);
        }
        if (byEdge.isEmpty()) {
            sb.append("The migration edge loop has not started.");
            if (plan != null && plan.path("edges").isArray() && !plan.path("edges").isEmpty()) {
                sb.append(" ").append(plan.path("edges").size())
                        .append(" edge(s) are planned and none has been attempted.");
            }
            sb.append("\n\n");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        byEdge.forEach((edgeId, attempts) -> {
            ObjectNode lastAttempt = attempts.get(attempts.size() - 1);
            rows.add(List.of(
                    Markdown.code(edgeId),
                    String.valueOf(attempts.size()),
                    Markdown.code(lastAttempt.path("stage_id").asText()),
                    statusCell(lastAttempt.path("status").asText()),
                    Markdown.code(RunJournal.EDGES_DIRECTORY + "/" + edgeId + "/"
                            + RunJournal.EDGE_DOCUMENT_FILE)));
        });
        Markdown.table(sb, List.of("Edge", "Stage attempts", "Last stage", "Last outcome",
                "Edge document"), rows, "");
    }

    private static void changes(StringBuilder sb, List<ObjectNode> timeline) {
        sb.append("## 9. Changes performed so far\n\n");
        boolean anyMutating = timeline.stream()
                .anyMatch(e -> e.path("stage_id").asText().startsWith("12-")
                        || e.path("stage_id").asText().startsWith("13-"));
        if (!anyMutating) {
            sb.append("No mutating stage has run. Application source has not been modified by this "
                    + "run.\n\n");
            return;
        }
        sb.append("Mutating stages have run. Per-attempt mutation counts are in each stage document's "
                + "section 14; the authoritative record of every change is the append-only change "
                + "ledger.\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (ObjectNode e : timeline) {
            String stage = e.path("stage_id").asText();
            if (stage.startsWith("12-") || stage.startsWith("13-")) {
                rows.add(List.of(
                        Markdown.code(stage),
                        e.path("edge_id").isNull() ? Markdown.ABSENT
                                : Markdown.code(e.path("edge_id").asText()),
                        statusCell(e.path("status").asText()),
                        Markdown.cell(e.path("summary").asText(null))));
            }
        }
        Markdown.table(sb, List.of("Stage", "Edge", "Status", "Summary"), rows, "");
    }

    private static void validation(StringBuilder sb, List<ObjectNode> timeline) {
        sb.append("## 10. Validation performed so far\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (ObjectNode e : timeline) {
            String stage = e.path("stage_id").asText();
            if (stage.startsWith("14-") || stage.startsWith("15-") || stage.startsWith("16-")
                    || stage.startsWith("17-")) {
                rows.add(List.of(
                        Markdown.code(stage),
                        e.path("edge_id").isNull() ? Markdown.ABSENT
                                : Markdown.code(e.path("edge_id").asText()),
                        statusCell(e.path("status").asText()),
                        Markdown.cell(e.path("summary").asText(null))));
            }
        }
        Markdown.table(sb, List.of("Stage", "Edge", "Status", "Summary"), rows,
                "No validation stage has run yet.");
    }

    private static void blockers(StringBuilder sb, List<ObjectNode> timeline) {
        sb.append("## 11. Current blockers\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (ObjectNode e : timeline) {
            String status = e.path("status").asText();
            if (status.equals("BLOCKED") || status.equals("FAILED") || status.equals("CRASHED")
                    || status.equals("REFUSED")) {
                rows.add(List.of(
                        Markdown.code(e.path("stage_id").asText()),
                        Markdown.cell(status),
                        Markdown.cell(e.path("stop_reason").asText(
                                e.path("summary").asText(null)))));
            }
        }
        Markdown.table(sb, List.of("Stage", "Status", "Reason"), rows,
                "No stage attempt is currently blocking the run.");
    }

    private static void humanDecisions(StringBuilder sb, List<ObjectNode> timeline) {
        sb.append("## 12. Human decisions required\n\n");
        List<ObjectNode> blocked = timeline.stream()
                .filter(e -> e.path("status").asText().equals("BLOCKED")).toList();
        if (blocked.isEmpty()) {
            sb.append("No stage attempt is currently waiting on a human decision.\n\n");
            return;
        }
        for (ObjectNode e : blocked) {
            sb.append("- ").append(Markdown.code(e.path("stage_id").asText())).append(": ")
                    .append(Markdown.text(e.path("stop_reason").asText(
                            e.path("summary").asText("blocked"))));
            String next = e.path("next_action").asText(null);
            if (next != null && !next.isBlank()) {
                sb.append(" — **").append(Markdown.cell(next)).append("**");
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private static void warnings(StringBuilder sb, List<ObjectNode> timeline) {
        sb.append("## 13. Warnings\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (ObjectNode e : timeline) {
            int count = e.path("warning_count").asInt();
            if (count > 0) {
                rows.add(List.of(Markdown.code(e.path("stage_id").asText()), String.valueOf(count)));
            }
        }
        Markdown.table(sb, List.of("Stage", "Warnings"), rows, "No warnings recorded.");
        if (!rows.isEmpty()) {
            sb.append("Warning text is in each stage document's section 17.\n\n");
        }
    }

    private static void gaps(StringBuilder sb, RunJournal journal, List<ObjectNode> timeline) {
        sb.append("## 14. Gaps\n\n");
        List<String> items = new ArrayList<>();
        for (ObjectNode e : timeline) {
            int unexecuted = e.path("unexecuted_step_count").asInt();
            if (unexecuted > 0) {
                items.add(e.path("stage_id").asText() + ": " + unexecuted
                        + " declared step(s) were never executed");
            }
            String renderingError = e.path("rendering_error").asText(null);
            if (renderingError != null && !renderingError.isBlank()) {
                items.add(e.path("stage_id").asText() + ": " + renderingError);
            }
        }
        journal.journalFailures().forEach(f -> items.add("Journal: " + f));
        Markdown.bullets(sb, items, "No documentation or execution gaps detected in this run.");
    }

    private static void blindSpots(StringBuilder sb, List<ObjectNode> timeline) {
        sb.append("## 15. Blind spots\n\n");
        sb.append("Blind spots are recorded per stage attempt, in section 19 of each stage document. "
                + "The evidence stage consolidates them for the run.\n\n");
    }

    private static void evidence(StringBuilder sb, OutputLayout output, List<ObjectNode> timeline) {
        sb.append("## 16. Evidence produced so far\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (ObjectNode e : timeline) {
            String dir = e.path("attempt_directory").asText(null);
            if (dir == null || dir.isBlank()) {
                continue;
            }
            rows.add(List.of(
                    Markdown.code(e.path("stage_id").asText()),
                    Markdown.code(dir),
                    statusCell(e.path("status").asText())));
        }
        Markdown.table(sb, List.of("Stage", "Attempt directory", "Status"), rows,
                "No attempt directories recorded yet.");
    }

    private static void nextAction(StringBuilder sb, List<ObjectNode> timeline) {
        sb.append("## 17. Next action\n\n");
        ObjectNode latest = last(timeline);
        if (latest == null) {
            sb.append("Start the run.\n\n");
            return;
        }
        String next = latest.path("next_action").asText(null);
        sb.append(next == null || next.isBlank()
                ? "No explicit next action was recorded by the last stage attempt."
                : Markdown.text(next)).append("\n\n");
    }

    private static void integrity(StringBuilder sb, RunJournal journal, List<ObjectNode> timeline) {
        sb.append("## 18. Integrity information\n\n");
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Run id", Markdown.code(journal.runId())));
        rows.add(List.of("Attempts recorded", String.valueOf(timeline.size())));
        rows.add(List.of("Timeline artifact", Markdown.code(RunJournal.TIMELINE_FILE)));
        rows.add(List.of("Journal failures", String.valueOf(journal.journalFailures().size())));
        rows.add(List.of("Schema version", Markdown.code(RunJournal.SCHEMA_VERSION)));
        Markdown.table(sb, List.of("Field", "Value"), rows, "");
        sb.append("Every row in the stage timeline is backed by a `")
                .append(RunJournal.STAGE_EXECUTION_FILE)
                .append("` in the named attempt directory. This document holds no facts that are not "
                        + "in those records.\n");
    }

    private static String text(JsonNode node, String field) {
        return node == null ? null : node.path(field).asText(null);
    }

    /**
     * The repository the run was pointed at.
     *
     * <p>Read from {@code repo_state}, which is where bootstrap actually records it. Field names
     * guessed from memory are how a document ends up rendering an em dash for something the run
     * knows perfectly well, so the two shapes this has had are both handled.
     */
    private static String sourceRoot(JsonNode bootstrap) {
        if (bootstrap == null) {
            return null;
        }
        String fromRepoState = bootstrap.path("repo_state").path("rootPath").asText(null);
        if (fromRepoState != null && !fromRepoState.isBlank()) {
            return fromRepoState;
        }
        return bootstrap.path("source_provenance").path("rootPath").asText(null);
    }

    /** The resolved landing target, as the target stage actually records it. */
    private static String resolvedTarget(JsonNode target) {
        if (target == null) {
            return null;
        }
        return target.path("landing_version").asText(null);
    }
}
