package com.bootshift.tests.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.HarnessException;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.journal.RunJournal;
import com.bootshift.core.journal.StepDeclaration;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The execution journal's central guarantee: every stage attempt leaves a record.
 *
 * <p>The interesting cases here are the unhappy ones. A stage that succeeds is trivially documented
 * by anything; a stage that refuses at the door, fails half way through, or throws something nobody
 * expected is the stage whose documentation an operator actually needs, and is exactly the stage a
 * success-path recorder would produce nothing for.
 */
class StageJournalLifecycleTest {

    /** A stage whose behaviour each test dictates. */
    private static final class ScriptedStage implements Stage {

        private final String id;
        private final String edgeId;
        private final java.util.function.Function<StageContext, StageResult> body;
        private final List<StepDeclaration> steps;
        private List<String> inputs = List.of();

        ScriptedStage(String id, String edgeId, List<StepDeclaration> steps,
                      java.util.function.Function<StageContext, StageResult> body) {
            this.id = id;
            this.edgeId = edgeId;
            this.steps = steps;
            this.body = body;
        }

        ScriptedStage requiring(String... artifacts) {
            this.inputs = List.of(artifacts);
            return this;
        }

        @Override
        public List<String> inputArtifacts() {
            return inputs;
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
            return "Scripted stage for journal tests";
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
        public List<StepDeclaration> declaredSteps() {
            return steps;
        }

        @Override
        public StageResult execute(StageContext context) {
            return body.apply(context);
        }
    }

    private static StageResult publishing(StageContext context, String stageId) {
        OutputLayout.StageWriter writer = context.run().output().open(stageId);
        writer.write("payload.json", Map.of("ok", true));
        String hash = StageSupport.publish(context, writer);
        Map<String, Path> artifacts = new LinkedHashMap<>();
        artifacts.put("payload.json", writer.dir().resolve("payload.json"));
        return new StageResult(stageId, ExitCode.SUCCESS, "did the thing", List.of(), artifacts, hash);
    }

    /** Reads the single execution record a stage wrote, wherever the journal put it. */
    private static JsonNode record(StageContext context, String stageId) throws IOException {
        List<Path> found = new ArrayList<>();
        Path stageRoot = context.run().output().stageRoot(stageId);
        try (var walk = Files.walk(stageRoot)) {
            walk.filter(p -> p.getFileName().toString().equals(RunJournal.STAGE_EXECUTION_FILE))
                    .forEach(found::add);
        }
        assertThat(found).as("execution records under " + stageRoot).hasSize(1);
        return Json.read(found.get(0));
    }

    @Nested
    @DisplayName("every terminal outcome is recorded")
    class Outcomes {

        @Test
        @DisplayName("a successful stage writes its record beside its own artifacts")
        void success(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J1");
            StageExecutor.run(new ScriptedStage("90-ok", null, List.of(),
                    c -> publishing(c, "90-ok")), context);

            JsonNode record = record(context, "90-ok");
            assertThat(record.path("status").asText()).isEqualTo("SUCCESS");
            assertThat(record.path("summary").asText()).isEqualTo("did the thing");
            assertThat(record.path("outputs")).hasSize(1);

            // Beside the artifacts, not in a parallel tree: a reader who opens the attempt
            // directory finds the artifacts and the account of how they were produced together.
            Path attempt = context.run().output().resolveLatestDir("90-ok");
            assertThat(attempt.resolve(RunJournal.STAGE_EXECUTION_FILE)).exists();
            assertThat(attempt.resolve(RunJournal.STAGE_DOCUMENT_FILE)).exists();
            assertThat(attempt.resolve("payload.json")).exists();
        }

        @Test
        @DisplayName("a failing stage is documented, and the pointer does not advance")
        void failure(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J2");
            StageResult result = StageExecutor.run(new ScriptedStage("90-fail", null, List.of(),
                    c -> StageResult.failure("90-fail", ExitCode.STAGE_FAILURE,
                            "the tool exited 1", List.of("see the build log"))), context);

            assertThat(result.succeeded()).isFalse();
            JsonNode record = record(context, "90-fail");
            assertThat(record.path("status").asText()).isEqualTo("FAILED");
            assertThat(record.path("stop_reason").asText()).contains("the tool exited 1");
            assertThat(context.run().output().resolveLatestDir("90-fail")).isNull();
        }

        @Test
        @DisplayName("a stage refused on a precondition is documented with its remediation")
        void refusal(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J3");
            Stage needsPlan = new ScriptedStage("90-refused", null, List.of(),
                    c -> publishing(c, "90-refused")).requiring("11-plan/edge-plan.json");
            StageResult result = StageExecutor.run(needsPlan, context);

            assertThat(result.exitCode()).isEqualTo(ExitCode.STRUCTURED_REFUSAL);
            JsonNode record = record(context, "90-refused");
            assertThat(record.path("status").asText()).isEqualTo("REFUSED");
            assertThat(record.path("next_action").asText()).isNotBlank();
            assertThat(record.path("precondition_results")).isNotEmpty();
            assertThat(record.path("errors").toString()).contains("11-plan/edge-plan.json");
        }

        @Test
        @DisplayName("an unhandled exception still produces a record, then propagates")
        void crash(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J4");
            assertThatThrownBy(() -> StageExecutor.run(
                    new ScriptedStage("90-crash", null, List.of(), c -> {
                        throw new IllegalStateException("nobody expected this");
                    }), context))
                    .isInstanceOf(IllegalStateException.class);

            JsonNode record = record(context, "90-crash");
            assertThat(record.path("status").asText()).isEqualTo("CRASHED");
            assertThat(record.path("errors").toString()).contains("nobody expected this");
        }

        @Test
        @DisplayName("a stage that blocks by throwing loses its optimistic next action")
        void blockingByThrowCorrectsNextAction(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J5b");
            // The real shape of the target resolver: record the next pipeline step on the way past
            // the publish call, then throw a policy block. Telling an operator to run the next stage
            // is wrong there - it cannot run, and would not help if it could.
            StageExecutor.run(new ScriptedStage("90-block", null, List.of(), c -> {
                StageSupport.nextAction(c, "Run: bootshift documentation");
                throw HarnessException.block("No target satisfies the active policy");
            }), context);

            JsonNode record = record(context, "90-block");
            assertThat(record.path("status").asText()).isEqualTo("BLOCKED");
            assertThat(record.path("next_action").asText())
                    .doesNotContain("bootshift documentation");
            assertThat(record.path("next_action").asText()).contains("policy");
        }

        @Test
        @DisplayName("a structured refusal thrown mid-stage is a refusal, not a crash")
        void structuredRefusalIsNotACrash(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J5");
            StageExecutor.run(new ScriptedStage("90-throw", null, List.of(), c -> {
                throw HarnessException.refusal("upstream artifact missing");
            }), context);

            JsonNode record = record(context, "90-throw");
            assertThat(record.path("status").asText()).isEqualTo("REFUSED");
        }
    }

    @Nested
    @DisplayName("attempts are immutable and distinguishable")
    class Attempts {

        @Test
        @DisplayName("a retry gets a new attempt id and does not overwrite the first")
        void retriesAreSeparateAttempts(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J6");
            Stage stage = new ScriptedStage("90-retry", null, List.of(),
                    c -> publishing(c, "90-retry"));

            StageExecutor.run(stage, context);
            StageExecutor.run(stage, context);

            List<Path> records = new ArrayList<>();
            try (var walk = Files.walk(context.run().output().stageRoot("90-retry"))) {
                walk.filter(p -> p.getFileName().toString()
                        .equals(RunJournal.STAGE_EXECUTION_FILE)).forEach(records::add);
            }
            assertThat(records).as("one record per attempt").hasSize(2);

            String first = Json.read(records.get(0)).path("attempt_id").asText();
            String second = Json.read(records.get(1)).path("attempt_id").asText();
            assertThat(first).isNotEqualTo(second);

            // Two attempts of a fast stage can land in the same millisecond, so identity must not
            // come from the timestamp the directory is named after.
            assertThat(context.journal().timeline()).hasSize(2);
        }

        @Test
        @DisplayName("the timeline is written after every attempt, not at the end of the run")
        void timelineIsIncremental(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J7");
            Path timeline = context.run().output().root().resolve(RunJournal.TIMELINE_FILE);

            StageExecutor.run(new ScriptedStage("90-a", null, List.of(),
                    c -> publishing(c, "90-a")), context);
            assertThat(timeline).exists();
            assertThat(Json.read(timeline).path("entries")).hasSize(1);

            StageExecutor.run(new ScriptedStage("90-b", null, List.of(),
                    c -> StageResult.failure("90-b", ExitCode.STAGE_FAILURE, "stopped", List.of())),
                    context);
            JsonNode after = Json.read(timeline);
            assertThat(after.path("entries")).hasSize(2);
            assertThat(after.path("entries").get(1).path("status").asText()).isEqualTo("FAILED");
        }
    }

    @Nested
    @DisplayName("documentation failure policy")
    class DocumentationFailure {

        @Test
        @DisplayName("a stage that succeeded stays succeeded when its Markdown cannot be written")
        void renderingFailureDoesNotFailTheStage(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-JD1");

            // The stage occupies STAGE_DOCUMENT.md with a directory, so writing the rendered
            // document is guaranteed to fail while the structured record is written first and
            // succeeds. This is the policy under test: execution truth is mandatory, the
            // human-readable rendering is best-effort, and a documentation defect must never turn a
            // migration stage that worked into one that failed.
            StageResult result = StageExecutor.run(new ScriptedStage("90-render", null, List.of(),
                    c -> {
                        OutputLayout.StageWriter writer = c.run().output().open("90-render");
                        writer.write("payload.json", Map.of("ok", true));
                        try {
                            Files.createDirectory(writer.dir().resolve(RunJournal.STAGE_DOCUMENT_FILE));
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                        String hash = StageSupport.publish(c, writer);
                        return new StageResult("90-render", ExitCode.SUCCESS, "did the thing",
                                List.of(), Map.of(), hash);
                    }), context);

            assertThat(result.succeeded())
                    .as("a rendering defect is not a stage failure")
                    .isTrue();

            // Execution truth survives, and says so.
            Path attempt = context.run().output().resolveLatestDir("90-render");
            assertThat(attempt.resolve(RunJournal.STAGE_EXECUTION_FILE)).exists();
            JsonNode record = Json.read(attempt.resolve(RunJournal.STAGE_EXECUTION_FILE));
            assertThat(record.path("status").asText()).isEqualTo("SUCCESS");
            assertThat(record.path("rendering_error").asText()).isNotBlank();

            // And the defect is surfaced rather than swallowed, so evidence can report it as a gap.
            assertThat(context.journal().journalFailures())
                    .anyMatch(f -> f.contains("STAGE_DOCUMENT.md rendering failed"));
            assertThat(context.journal().timeline().get(0).path("rendering_error").asText())
                    .isNotBlank();
        }

        @Test
        @DisplayName("the run document keeps being written after one stage fails to render")
        void runDocumentSurvivesRenderingFailure(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-JD2");
            StageExecutor.run(new ScriptedStage("90-render2", null, List.of(), c -> {
                OutputLayout.StageWriter writer = c.run().output().open("90-render2");
                writer.write("payload.json", Map.of("ok", true));
                try {
                    Files.createDirectory(writer.dir().resolve(RunJournal.STAGE_DOCUMENT_FILE));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                String hash = StageSupport.publish(c, writer);
                return new StageResult("90-render2", ExitCode.SUCCESS, "ok", List.of(), Map.of(),
                        hash);
            }), context);

            Path runDocument = context.run().output().root().resolve(RunJournal.RUN_DOCUMENT_FILE);
            assertThat(runDocument).exists();
            // The gap is reported in the run document, not left for a reader to notice.
            assertThat(Files.readString(runDocument)).contains("rendering failed");
        }
    }

    @Nested
    @DisplayName("planned versus executed")
    class PlannedVersusExecuted {

        @Test
        @DisplayName("a declared step that never runs is reported, not silently absent")
        void unexecutedStepIsVisible(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J8");
            List<StepDeclaration> steps = List.of(
                    StepDeclaration.of("SCR-001", "Runs", "This one is exercised"),
                    StepDeclaration.of("SCR-002", "Never runs", "This one is not"));

            StageExecutor.run(new ScriptedStage("90-steps", null, steps, c -> {
                StageSupport.step(c, "SCR-001").begin();
                StageSupport.step(c, "SCR-001").succeed("done");
                return publishing(c, "90-steps");
            }), context);

            JsonNode record = record(context, "90-steps");
            assertThat(record.path("declared_steps")).hasSize(2);
            assertThat(record.path("unexecuted_steps")).hasSize(1);
            assertThat(record.path("unexecuted_steps").get(0).asText()).isEqualTo("SCR-002");

            // The failure mode this exists for: code implemented and never invoked used to leave no
            // trace, because an artifact records results and unrun work produces none.
            Path attempt = context.run().output().resolveLatestDir("90-steps");
            String document = Files.readString(attempt.resolve(RunJournal.STAGE_DOCUMENT_FILE));
            assertThat(document).contains("SCR-002");
            assertThat(document).contains("never executed");
        }

        @Test
        @DisplayName("a step still running when a stage crashes is closed as failed, not left RUNNING")
        void runningStepIsClosedOnCrash(@TempDir Path root) throws IOException {
            StageContext context = JournalHarness.context(root, "RUN-J9");
            List<StepDeclaration> steps = List.of(
                    StepDeclaration.of("SCR-010", "Interrupted", "Begins and never finishes"));

            assertThatThrownBy(() -> StageExecutor.run(
                    new ScriptedStage("90-abandon", null, steps, c -> {
                        StageSupport.step(c, "SCR-010").begin();
                        throw new IllegalStateException("died mid-step");
                    }), context)).isInstanceOf(IllegalStateException.class);

            JsonNode record = record(context, "90-abandon");
            JsonNode step = record.path("executed_steps").get(0);
            assertThat(step.path("status").asText()).isEqualTo("FAILED");
            assertThat(step.path("status_reason").asText()).contains("still running");
        }
    }
}
