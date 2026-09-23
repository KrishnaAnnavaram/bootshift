package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders {@code EDGE_DOCUMENT.md} from an edge execution aggregate.
 *
 * <p>The edge is the unit a reviewer signs off on, so this document answers the questions a sign-off
 * needs in one place: why the edge exists, what it changed, what was rejected, what was validated,
 * and - the one most easily lost across six stage directories - what was never validated at all.
 */
public final class EdgeDocumentRenderer {

    /** Display names for the edge stages, in execution order. */
    private static final List<String[]> STAGE_TITLES = List.of(
            new String[]{"12-transformation", "Stage 12 — Transformation"},
            new String[]{"13-build-repair", "Stage 13 — Build and repair"},
            new String[]{"14-graph-diff", "Stage 14 — Graph rebuild and scope verification"},
            new String[]{"15-test", "Stage 15 — Test validation"},
            new String[]{"16-runtime", "Stage 16 — Runtime validation"},
            new String[]{"17-differential", "Stage 17 — Differential validation"});

    private EdgeDocumentRenderer() {
    }

    public static String render(JsonNode edge) {
        StringBuilder sb = new StringBuilder();
        header(sb, edge);
        whyThisEdgeExists(sb, edge);
        planLists(sb, edge);
        stageSections(sb, edge);
        changes(sb, edge);
        validationResults(sb, edge);
        evidence(sb, edge);
        blockers(sb, edge);
        result(sb, edge);
        return sb.toString();
    }

    private static void header(StringBuilder sb, JsonNode edge) {
        sb.append("# Edge ").append(Markdown.text(edge.path("edge_id").asText("unknown")))
                .append("\n\n");
        JsonNode plan = edge.path("plan");
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Source state", Markdown.code(plan.path("source_state").asText(null))));
        rows.add(List.of("Target state", Markdown.code(plan.path("target_state").asText(null))));
        rows.add(List.of("Java", Markdown.code(plan.path("java_version").asText(null))));
        rows.add(List.of("Spring Cloud", Markdown.code(plan.path("spring_cloud_version").asText(null))));
        rows.add(List.of("Class", Markdown.cell(plan.path("edge_class").asText(null))));
        rows.add(List.of("Validation depth", Markdown.cell(plan.path("validation_depth").asText(null))));
        rows.add(List.of("Risk", Markdown.cell(plan.path("risk").asText(null))));
        rows.add(List.of("Mandatory checkpoint",
                plan.path("mandatory_checkpoint").asBoolean() ? "yes" : "no"));
        rows.add(List.of("Deterministic coverage",
                Markdown.cell(plan.path("deterministic_coverage").asText(null))));
        rows.add(List.of("Expected residual",
                Markdown.cell(plan.path("expected_residual").asText(null))));
        rows.add(List.of("Result", "**" + Markdown.cell(edge.path("result").asText()) + "**"));
        Markdown.table(sb, List.of("Field", "Value"), rows, "");
        sb.append("---\n\n");
    }

    private static void whyThisEdgeExists(StringBuilder sb, JsonNode edge) {
        sb.append("## Why this edge exists\n\n");
        String rationale = edge.path("plan").path("rationale").asText(null);
        sb.append(rationale == null || rationale.isBlank()
                ? "The frozen plan recorded no rationale for this edge."
                : Markdown.text(rationale)).append("\n\n");
    }

    private static void planLists(StringBuilder sb, JsonNode edge) {
        JsonNode plan = edge.path("plan");
        listSection(sb, "Facts in force", plan.path("facts"),
                "The plan bound no migration facts to this edge.");
        listSection(sb, "Impacts", plan.path("impacts"),
                "The plan bound no impact findings to this edge.");

        sb.append("## Recipes planned\n\n");
        JsonNode recipes = plan.path("recipes");
        if (!recipes.isArray() || recipes.isEmpty()) {
            sb.append("No recipes were scheduled for this edge.\n\n");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode recipe : recipes) {
            if (recipe.isTextual()) {
                rows.add(List.of(Markdown.code(recipe.asText()), Markdown.ABSENT, Markdown.ABSENT,
                        Markdown.ABSENT));
            } else {
                rows.add(List.of(
                        Markdown.code(recipe.path("recipe_id").asText(null)),
                        Markdown.cell(recipe.path("capability_provider")
                                .asText(recipe.path("preferred_transformer").asText(null))),
                        Markdown.cell(recipe.path("capability_status").asText(null)),
                        Markdown.cell(recipe.path("why").asText(null))));
            }
        }
        Markdown.table(sb, List.of("Recipe", "Provider", "Capability", "Why"), rows, "");

        // A recipe the plan scheduled with no provider behind it is residual by definition, and has
        // to be visible here rather than only in the planner's own artifact.
        JsonNode uncovered = plan.path("recipes_without_capability");
        if (uncovered.isArray() && !uncovered.isEmpty()) {
            sb.append("**").append(uncovered.size())
                    .append(" scheduled recipe(s) have no available deterministic capability.** "
                            + "Those become residual and raise this edge's validation depth:\n\n");
            List<String> items = new ArrayList<>();
            uncovered.forEach(n -> items.add(Markdown.code(
                    n.isTextual() ? n.asText() : n.path("recipe_id").asText(n.toString()))));
            Markdown.bullets(sb, items, "");
        }
    }

    private static void listSection(StringBuilder sb, String heading, JsonNode array,
                                    String emptyState) {
        sb.append("## ").append(heading).append("\n\n");
        if (!array.isArray() || array.isEmpty()) {
            sb.append(emptyState).append("\n\n");
            return;
        }
        int limit = 50;
        List<String> items = new ArrayList<>();
        int index = 0;
        for (JsonNode n : array) {
            if (index++ >= limit) {
                break;
            }
            items.add(Markdown.code(n.isTextual() ? n.asText() : n.path("id").asText(n.toString())));
        }
        sb.append(array.size()).append(" total");
        if (array.size() > limit) {
            sb.append(", first ").append(limit).append(" shown");
        }
        sb.append(":\n\n");
        Markdown.bullets(sb, items, "");
    }

    private static void stageSections(StringBuilder sb, JsonNode edge) {
        for (String[] title : STAGE_TITLES) {
            JsonNode stage = findStage(edge, title[0]);
            sb.append("## ").append(title[1]).append("\n\n");
            if (stage == null || !stage.path("executed").asBoolean()) {
                sb.append("**Not executed for this edge.** No result from this stage may be assumed; "
                        + "the absence of a failure here is not evidence of a pass.\n\n");
                continue;
            }
            List<List<String>> rows = new ArrayList<>();
            rows.add(List.of("Status", "**" + Markdown.cell(stage.path("status").asText()) + "**"));
            rows.add(List.of("Attempt", Markdown.code(stage.path("attempt_id").asText(null))));
            rows.add(List.of("Duration",
                    Markdown.cell(Markdown.duration(stage.path("duration_ms").asLong()))));
            rows.add(List.of("Detail", Markdown.code(title[0] + "/"
                    + stage.path("attempt_directory").asText("?") + "/"
                    + RunJournal.STAGE_DOCUMENT_FILE)));
            Markdown.table(sb, List.of("Field", "Value"), rows, "");
            String summary = stage.path("summary").asText(null);
            if (summary != null && !summary.isBlank()) {
                sb.append(Markdown.text(summary)).append("\n\n");
            }
            appendMeasures(sb, stage.path("mutation_summary"), "Mutations");
            appendMeasures(sb, stage.path("validation_summary"), "Validation");
        }
    }

    private static void appendMeasures(StringBuilder sb, JsonNode object, String label) {
        if (!object.isObject() || object.isEmpty()) {
            return;
        }
        sb.append("*").append(label).append(":* ");
        List<String> parts = new ArrayList<>();
        object.fields().forEachRemaining(e -> parts.add(
                Markdown.cell(e.getKey()) + " " + Markdown.cell(
                        e.getValue().isValueNode() ? e.getValue().asText() : e.getValue().toString())));
        sb.append(String.join(" · ", parts)).append("\n\n");
    }

    private static JsonNode findStage(JsonNode edge, String stageId) {
        for (JsonNode stage : edge.path("stages")) {
            if (stageId.equals(stage.path("stage_id").asText(null))) {
                return stage;
            }
        }
        return null;
    }

    private static void changes(StringBuilder sb, JsonNode edge) {
        JsonNode transformation = findStage(edge, "12-transformation");
        JsonNode repair = findStage(edge, "13-build-repair");

        sb.append("## Applied changes\n\n");
        if (transformation == null || !transformation.path("executed").asBoolean()) {
            sb.append("Transformation did not run for this edge, so nothing was applied.\n\n");
        } else {
            appendMeasures(sb, transformation.path("mutation_summary"), "Transformation");
            if (repair != null && repair.path("executed").asBoolean()) {
                appendMeasures(sb, repair.path("mutation_summary"), "Repair");
            }
            sb.append("The authoritative record of every applied change is the append-only change "
                    + "ledger; the counts above are the journal's view of it.\n\n");
        }

        sb.append("## Rejected changes\n\n");
        sb.append("Mutations the gateway refused are counted in the mutation summaries above and "
                + "recorded individually in each stage's own artifact. A rejection is a control "
                + "working, not a failure.\n\n");

        sb.append("## Residuals\n\n");
        sb.append("Facts with no deterministic transformer are recorded as residual by the planner "
                + "and raise the validation depth for this edge. See the plan artifact and stage 11 "
                + "for the residual calculation.\n\n");
    }

    private static void validationResults(StringBuilder sb, JsonNode edge) {
        sb.append("## Validation results\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (String[] title : STAGE_TITLES) {
            if (!title[0].startsWith("14-") && !title[0].startsWith("15-")
                    && !title[0].startsWith("16-") && !title[0].startsWith("17-")) {
                continue;
            }
            JsonNode stage = findStage(edge, title[0]);
            boolean executed = stage != null && stage.path("executed").asBoolean();
            rows.add(List.of(
                    Markdown.code(title[0]),
                    executed ? Markdown.cell(stage.path("status").asText())
                            : "**NOT EXECUTED**",
                    executed ? Markdown.cell(stage.path("summary").asText(null))
                            : "No evidence from this dimension."));
        }
        Markdown.table(sb, List.of("Stage", "Status", "Result"), rows, "");
    }

    private static void evidence(StringBuilder sb, JsonNode edge) {
        sb.append("## Evidence\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode attempt : edge.path("attempts")) {
            rows.add(List.of(
                    Markdown.code(attempt.path("stage_id").asText()),
                    Markdown.code(attempt.path("attempt_id").asText(null)),
                    Markdown.code(attempt.path("execution_record").asText(null))));
        }
        Markdown.table(sb, List.of("Stage", "Attempt", "Execution record"), rows,
                "No attempts recorded for this edge.");
    }

    private static void blockers(StringBuilder sb, JsonNode edge) {
        sb.append("## Blockers\n\n");
        String reason = edge.path("blocking_reason").asText(null);
        sb.append(reason == null || reason.isBlank()
                ? "Nothing is blocking this edge."
                : "**" + Markdown.cell(reason) + "**").append("\n\n");
    }

    private static void result(StringBuilder sb, JsonNode edge) {
        sb.append("## Edge result\n\n");
        String result = edge.path("result").asText("UNKNOWN");
        sb.append("**").append(Markdown.cell(result)).append("** — ");
        sb.append(switch (result) {
            case "COMPLETE" -> "every stage in the edge executed and succeeded.";
            case "INCOMPLETE" -> "the edge started and did not reach the end of its stage sequence. "
                    + "Stages marked NOT EXECUTED above produced no evidence at all.";
            case "BLOCKED" -> "the edge stopped on a finding that requires a human decision.";
            case "FAILED" -> "a stage in this edge failed.";
            case "REFUSED" -> "a stage in this edge refused to run on a precondition.";
            case "NOT_STARTED" -> "this edge was planned and never attempted.";
            default -> "the outcome could not be classified.";
        });
        sb.append("\n\n");
        sb.append("Generated from ").append(Markdown.code(RunJournal.EDGE_EXECUTION_FILE))
                .append(", which is itself derived from the per-attempt execution records.\n");
    }
}
