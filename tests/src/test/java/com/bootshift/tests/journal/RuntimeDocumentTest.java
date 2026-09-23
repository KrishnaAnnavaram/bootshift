package com.bootshift.tests.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.journal.EdgeDocumentRenderer;
import com.bootshift.core.journal.EdgeExecutionAggregate;
import com.bootshift.core.journal.ExecutionStatus;
import com.bootshift.core.journal.Markdown;
import com.bootshift.core.journal.RunJournal;
import com.bootshift.core.journal.StageDocumentRenderer;
import com.bootshift.core.journal.StageExecutionRecord;
import com.bootshift.core.journal.DecisionRecord;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Json;
import com.bootshift.stages.Stage;
import com.bootshift.stages.StageContext;
import com.bootshift.stages.StageExecutor;
import com.bootshift.stages.StageSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The three runtime documents, and the properties that make them worth generating.
 *
 * <p>Two claims are load-bearing and are tested here rather than assumed. The Markdown is a
 * rendering of the JSON and holds no facts of its own, so it can be regenerated from a record on
 * disk. And a document must never let an absence read as a pass: a stage that never ran, a step that
 * never executed and an edge that stopped half way all have to be visibly different from success.
 */
class RuntimeDocumentTest {

    private static final class SimpleStage implements Stage {
        private final String id;
        private final String edgeId;
        private final java.util.function.Function<StageContext, StageResult> body;

        SimpleStage(String id, String edgeId,
                    java.util.function.Function<StageContext, StageResult> body) {
            this.id = id;
            this.edgeId = edgeId;
            this.body = body;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String outputDirectory() {
            return id;
        }

        @Override
        public String purpose() {
            return "Stage under document test";
        }

        @Override
        public String edgeId() {
            return edgeId;
        }

        @Override
        public List<RunState> preconditions() {
            return List.of();
        }

        @Override
        public RunState postcondition() {
            return RunState.CREATED;
        }

        @Override
        public List<String> outputArtifacts() {
            return List.of();
        }

        @Override
        public StageResult execute(StageContext context) {
            return body.apply(context);
        }
    }

    private static StageResult ok(StageContext context, String stageId, String summary) {
        OutputLayout.StageWriter writer = context.run().output().open(stageId);
        writer.write("payload.json", Map.of("ok", true));
        String hash = StageSupport.publish(context, writer);
        Map<String, Path> artifacts = new LinkedHashMap<>();
        artifacts.put("payload.json", writer.dir().resolve("payload.json"));
        return new StageResult(stageId, ExitCode.SUCCESS, summary, List.of(), artifacts, hash);
    }

    @Nested
    @DisplayName("STAGE_DOCUMENT.md")
    class StageDocument {

        @Test
        @DisplayName("renders every declared section and carries the record's content")
        void rendersSections() {
            StageExecutionRecord record = new StageExecutionRecord("RUN-D1", "ATT-1", "07-documentation",
                    "DocumentationStage", null, "Retrieve authoritative sources",
                    StageExecutionRecord.Trigger.PIPELINE, null);
            record.stateBefore("TARGET_FROZEN").stateAfter("DOCUMENTATION_RETRIEVED");
            record.declareStep("DOC-001", "Retrieve", "Fetch the guide").begin().succeed("21 pinned");
            record.declareStep("DOC-002", "Verify", "Check the hash");
            record.decision(DecisionRecord.deterministic("DEC-1", "DOCUMENT_ACCEPTED",
                            "spring-boot-3.0-migration-guide")
                    .decided("ACCEPTED").because("Host is on the allowlist and the version applies")
                    .document("DOC-77").confidence("HIGH").build());
            record.warning("One source redirected");
            record.error("RETRIEVAL", "example.invalid returned 503", "Retry when the host is up");
            record.blindSpot("BS-DOC-1", "DOCUMENTATION", "No guide exists for one component",
                    "Facts for it stay CANDIDATE");
            record.output("document-registry.json", "/out/document-registry.json", "abc123");
            record.nextAction("Run: bootshift knowledge");
            record.stopReason("One retrieval failed");
            record.finish(ExecutionStatus.DEGRADED);

            String markdown = StageDocumentRenderer.render(record, "recordhash");

            for (String heading : List.of("## 1. Purpose", "## 2. Why this stage ran",
                    "## 3. State transition", "## 4. Preconditions", "## 5. Input artifacts",
                    "## 6. Planned execution steps", "## 7. Actual execution steps",
                    "## 8. Tools and commands executed", "## 9. Decisions made",
                    "## 10. Evidence used", "## 11. Migration documents used",
                    "## 12. Migration facts used or produced", "## 13. Impact analysis involved",
                    "## 14. Source mutations", "## 15. Validation performed",
                    "## 16. Retries and fallback paths", "## 17. Warnings",
                    "## 18. Errors and blockers", "## 19. Blind spots and unknowns",
                    "## 20. Output artifacts", "## 21. Result", "## 22. Next action",
                    "## 23. Integrity and provenance")) {
                assertThat(markdown).as(heading).contains(heading);
            }

            assertThat(markdown).contains("DEC-1", "DOCUMENT_ACCEPTED", "One source redirected",
                    "example.invalid returned 503", "BS-DOC-1", "document-registry.json",
                    "Run: bootshift knowledge", "recordhash", "DOC-002");
            // The unexecuted step must be called out, not merely listed among the others.
            assertThat(markdown).contains("never executed");
        }

        @Test
        @DisplayName("a section that does not apply says so instead of vanishing")
        void inapplicableSectionsAreExplicit() {
            StageExecutionRecord record = new StageExecutionRecord("RUN-D2", "ATT-2", "01-inventory",
                    "InventoryStage", null, "Discover the repository",
                    StageExecutionRecord.Trigger.CLI, null);
            record.finish(ExecutionStatus.SUCCESS);

            String markdown = StageDocumentRenderer.render(record, null);
            assertThat(markdown).contains("This stage does not write to application source.");
            assertThat(markdown).contains("This stage performs no validation of its own.");
            assertThat(markdown).contains("This stage launched no external processes.");
        }

        @Test
        @DisplayName("is a pure view: it regenerates from the JSON with no live object")
        void regeneratesFromJsonAlone() {
            StageExecutionRecord record = new StageExecutionRecord("RUN-D3", "ATT-3", "09-impact",
                    "ImpactStage", null, "Bind facts to files",
                    StageExecutionRecord.Trigger.PIPELINE, null);
            record.summary("42 findings").finish(ExecutionStatus.SUCCESS);

            String fromObject = StageDocumentRenderer.render(record, "h");
            JsonNode reparsed = Json.parse(Json.canonical(record.toNode()));
            String fromJson = StageDocumentRenderer.render(reparsed, "h");

            assertThat(fromJson).isEqualTo(fromObject);
        }
    }

    @Nested
    @DisplayName("RUN_DOCUMENT.md")
    class RunDocument {

        @Test
        @DisplayName("exists after the first attempt and is refreshed after every one")
        void existsAndRefreshes(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-D4");
            Path runDocument = context.run().output().root().resolve(RunJournal.RUN_DOCUMENT_FILE);

            StageExecutor.run(new SimpleStage("01-inventory", null,
                    c -> ok(c, "01-inventory", "1,204 files")), context);
            assertThat(runDocument).exists();
            String afterFirst = Files.readString(runDocument);
            assertThat(afterFirst).contains("01-inventory", "1,204 files", "RUNNING");
            assertThat(afterFirst).doesNotContain("02-build");

            StageExecutor.run(new SimpleStage("02-build", null,
                    c -> ok(c, "02-build", "6 modules")), context);
            assertThat(Files.readString(runDocument)).contains("02-build", "6 modules");
        }

        @Test
        @DisplayName("survives a failed run and names the blocker")
        void survivesFailure(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-D5");
            StageExecutor.run(new SimpleStage("01-inventory", null,
                    c -> ok(c, "01-inventory", "ok")), context);
            StageExecutor.run(new SimpleStage("13-build-repair", "EDGE-2", c -> {
                StageSupport.nextAction(c, "Fix the compile errors and re-run");
                return StageResult.failure("13-build-repair", ExitCode.STAGE_FAILURE,
                        "compilation failed after 3 repair rounds", List.of());
            }), context);

            String document = Files.readString(
                    context.run().output().root().resolve(RunJournal.RUN_DOCUMENT_FILE));
            assertThat(document).contains("FAILED");
            assertThat(document).contains("compilation failed after 3 repair rounds");
            assertThat(document).contains("## 11. Current blockers");
            // A run that stopped at 13 must not read as a run that reached the end.
            assertThat(document).doesNotContain("19-evidence");
        }

        @Test
        @DisplayName("names the human decision a blocked run is waiting on")
        void reportsHumanDecision(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-D6");
            StageExecutor.run(new SimpleStage("17-differential", "EDGE-2", c -> {
                StageSupport.nextAction(c, "File a decision: bootshift approve --decision-file <f>");
                return StageResult.failure("17-differential", ExitCode.HUMAN_DECISION_REQUIRED,
                        "1 unexplained behavioural difference", List.of());
            }), context);

            String document = Files.readString(
                    context.run().output().root().resolve(RunJournal.RUN_DOCUMENT_FILE));
            assertThat(document).contains("## 12. Human decisions required");
            assertThat(document).contains("1 unexplained behavioural difference");
            assertThat(document).contains("bootshift approve");
            assertThat(document).contains("BLOCKED");
        }
    }

    @Nested
    @DisplayName("EDGE_DOCUMENT.md")
    class EdgeDocument {

        @Test
        @DisplayName("is generated per edge and marks stages that never ran as not executed")
        void marksUnexecutedStages(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-D7");
            StageExecutor.run(new SimpleStage("12-transformation", "EDGE-2",
                    c -> ok(c, "12-transformation", "19 changes applied")), context);
            StageExecutor.run(new SimpleStage("13-build-repair", "EDGE-2",
                    c -> ok(c, "13-build-repair", "compiled clean")), context);

            Path edgeDir = context.run().output().root()
                    .resolve(RunJournal.EDGES_DIRECTORY).resolve("EDGE-2");
            assertThat(edgeDir.resolve(RunJournal.EDGE_DOCUMENT_FILE)).exists();
            assertThat(edgeDir.resolve(RunJournal.EDGE_EXECUTION_FILE)).exists();

            JsonNode aggregate = Json.read(edgeDir.resolve(RunJournal.EDGE_EXECUTION_FILE));
            assertThat(aggregate.path("result").asText()).isEqualTo("INCOMPLETE");

            String document = Files.readString(edgeDir.resolve(RunJournal.EDGE_DOCUMENT_FILE));
            assertThat(document).contains("Stage 12 — Transformation", "19 changes applied");
            assertThat(document).contains("Stage 15 — Test validation");
            assertThat(document).contains("**Not executed for this edge.**");
            // The sentence that stops an absence being read as a pass.
            assertThat(document).contains("the absence of a failure here is not evidence of a pass");
        }

        @Test
        @DisplayName("a blocking differential is reported as the edge's blocker")
        void reportsBlockingDifferential(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-D8");
            StageExecutor.run(new SimpleStage("17-differential", "EDGE-3",
                    c -> StageResult.failure("17-differential", ExitCode.HUMAN_DECISION_REQUIRED,
                            "1 UNEXPLAINED difference on discovery-service", List.of())), context);

            Path edgeDir = context.run().output().root()
                    .resolve(RunJournal.EDGES_DIRECTORY).resolve("EDGE-3");
            JsonNode aggregate = Json.read(edgeDir.resolve(RunJournal.EDGE_EXECUTION_FILE));
            assertThat(aggregate.path("result").asText()).isEqualTo("BLOCKED");

            String document = Files.readString(edgeDir.resolve(RunJournal.EDGE_DOCUMENT_FILE));
            assertThat(document).contains("## Blockers");
            assertThat(document).contains("1 UNEXPLAINED difference on discovery-service");
        }

        @Test
        @DisplayName("an edge that was planned and never attempted is NOT_STARTED, not complete")
        void unattemptedEdge(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-D9");
            ObjectNode aggregate = EdgeExecutionAggregate.build(context.run().output(), "RUN-D9",
                    "EDGE-7", List.of(), null);
            assertThat(aggregate.path("result").asText()).isEqualTo("NOT_STARTED");
            assertThat(EdgeDocumentRenderer.render(aggregate))
                    .contains("this edge was planned and never attempted");
        }
    }

    @Nested
    @DisplayName("Markdown safety")
    class MarkdownSafety {

        @Test
        @DisplayName("a pipe in repository-derived text cannot shift a table column")
        void escapesTableCells() {
            assertThat(Markdown.cell("a|b")).isEqualTo("a\\|b");
            assertThat(Markdown.cell("line\none")).isEqualTo("line one");
            assertThat(Markdown.cell(null)).isEqualTo(Markdown.ABSENT);
        }

        @Test
        @DisplayName("a value containing backticks is still fenced correctly")
        void escapesCode() {
            assertThat(Markdown.code("a`b")).isEqualTo("``a`b``");
            assertThat(Markdown.code("plain")).isEqualTo("`plain`");
        }

        @Test
        @DisplayName("a Windows path keeps its single backslashes inside a code span")
        void doesNotEscapeBackslashesInCode() {
            // Markdown does not process escapes inside a code span, so escaping a backslash there
            // renders literally. Every command line in a stage document is a path on Windows.
            assertThat(Markdown.code("C:\\Users\\annav\\mvnw.cmd"))
                    .isEqualTo("`C:\\Users\\annav\\mvnw.cmd`");
            // The pipe still has to go: a table row is split before its cells are parsed.
            assertThat(Markdown.code("a|b")).isEqualTo("`a\\|b`");
        }

        @Test
        @DisplayName("Mermaid labels lose the characters that break the diagram grammar")
        void escapesMermaid() {
            String label = Markdown.mermaid("edge [2] --> \"x\"; y");
            assertThat(label).doesNotContain("[", "]", "\"", ";", "-->");
        }
    }
}
