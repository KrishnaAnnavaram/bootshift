package com.bootshift.tests.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.bootshift.adapters.exec.ProcessRunner;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.journal.CommandExecutionRecord;
import com.bootshift.core.journal.RunJournal;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Json;
import com.bootshift.stages.Stage;
import com.bootshift.stages.StageContext;
import com.bootshift.stages.StageExecutor;
import com.bootshift.stages.StageSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Commands are attributable to the stage that ran them, and never carry a credential.
 *
 * <p>Both halves matter. A build log answers what Maven printed; it cannot answer which stage, on
 * which edge, on which attempt, waited four minutes for it. And a command line is one of the more
 * reliable places to find a secret in a real repository - {@code -Dsonar.login=}, a repository URL
 * with a password in it - so the record is redacted before it is written, not after.
 */
class CommandTracingTest {

    private static final class CommandStage implements Stage {
        private final java.util.function.Consumer<StageContext> body;

        CommandStage(java.util.function.Consumer<StageContext> body) {
            this.body = body;
        }

        @Override
        public String id() {
            return "90-commands";
        }

        @Override
        public String outputDirectory() {
            return "90-commands";
        }

        @Override
        public String purpose() {
            return "Runs a real process so the journal has something to record";
        }

        @Override
        public String edgeId() {
            return "EDGE-1";
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
            body.accept(context);
            OutputLayout.StageWriter writer = context.run().output().open(outputDirectory());
            writer.write("payload.json", Map.of("ok", true));
            String hash = StageSupport.publish(context, writer);
            return new StageResult(id(), ExitCode.SUCCESS, "ran commands", List.of(), Map.of(), hash);
        }
    }

    private static JsonNode record(StageContext context) throws IOException {
        List<Path> found = new ArrayList<>();
        try (var walk = Files.walk(context.run().output().stageRoot("90-commands"))) {
            walk.filter(p -> p.getFileName().toString().equals(RunJournal.STAGE_EXECUTION_FILE))
                    .forEach(found::add);
        }
        assertThat(found).hasSize(1);
        return Json.read(found.get(0));
    }

    @Test
    @DisplayName("a real process is recorded with exit code, duration, directory and attribution")
    void recordsRealProcess(@TempDir Path root) throws IOException {
        StageContext context = JournalHarness.context(root, "RUN-C1");
        Path workingDirectory = root.resolve("wd");
        Files.createDirectories(workingDirectory);

        StageExecutor.run(new CommandStage(c -> {
            ProcessRunner runner = StageSupport.runner(c);
            // java -version is on the allowlist, exists wherever these tests run, and is cheap.
            runner.run(List.of(ProcessRunner.jdkTool("java"), "-version"), workingDirectory,
                    Duration.ofSeconds(60), Map.of());
        }), context);

        JsonNode commands = record(context).path("commands");
        assertThat(commands).hasSize(1);
        JsonNode command = commands.get(0);

        assertThat(command.path("stage_id").asText()).isEqualTo("90-commands");
        assertThat(command.path("edge_id").asText()).isEqualTo("EDGE-1");
        assertThat(command.path("attempt_id").asText()).isNotBlank();
        assertThat(command.path("command_id").asText()).startsWith("CMD-");
        assertThat(command.path("exit_code").asInt()).isZero();
        assertThat(command.path("timed_out").asBoolean()).isFalse();
        assertThat(command.path("result").asText()).isEqualTo("SUCCESS");
        assertThat(command.path("duration_ms").asLong()).isGreaterThanOrEqualTo(0L);
        assertThat(command.path("working_directory").asText()).contains("wd");
        assertThat(command.path("sanitized_command").asText()).contains("-version");
    }

    @Test
    @DisplayName("only environment variable names are journalled, never their values")
    void environmentValuesAreNeverWritten(@TempDir Path root) throws IOException {
        StageContext context = JournalHarness.context(root, "RUN-C2");
        Path workingDirectory = root.resolve("wd");
        Files.createDirectories(workingDirectory);

        StageExecutor.run(new CommandStage(c -> StageSupport.runner(c).run(
                List.of(ProcessRunner.jdkTool("java"), "-version"), workingDirectory,
                Duration.ofSeconds(60),
                Map.of("BOOTSHIFT_TEST_TOKEN", "s3cr3t-value-must-not-appear"))), context);

        JsonNode command = record(context).path("commands").get(0);
        JsonNode environment = command.path("environment_summary");
        assertThat(environment.path("variable_names").toString()).contains("BOOTSHIFT_TEST_TOKEN");
        assertThat(environment.toString()).doesNotContain("s3cr3t-value-must-not-appear");

        // And nowhere else in the record either.
        Path attempt = context.run().output().resolveLatestDir("90-commands");
        assertThat(Files.readString(attempt.resolve(RunJournal.STAGE_EXECUTION_FILE)))
                .doesNotContain("s3cr3t-value-must-not-appear");
        assertThat(Files.readString(attempt.resolve(RunJournal.STAGE_DOCUMENT_FILE)))
                .doesNotContain("s3cr3t-value-must-not-appear");
    }

    @Test
    @DisplayName("an argument whose key reads as sensitive has its value redacted")
    void redactsSensitiveArguments() {
        String sanitized = CommandExecutionRecord.sanitize(List.of(
                "mvn", "-Dsonar.login=abcd1234", "-Dspring.datasource.password=hunter2",
                "-Dmaven.test.skip=true"));
        assertThat(sanitized).doesNotContain("abcd1234", "hunter2");
        assertThat(sanitized).contains("-Dsonar.login=REDACTED");
        assertThat(sanitized).contains("-Dspring.datasource.password=REDACTED");
        // A harmless flag keeps its value; over-redaction makes the record useless.
        assertThat(sanitized).contains("-Dmaven.test.skip=true");
    }

    @Test
    @DisplayName("a credentialed URI in an argument is redacted on both halves")
    void redactsCredentialedUri() {
        String sanitized = CommandExecutionRecord.sanitize(List.of(
                "git", "clone", "https://alice:tokenvalue@git.example.com/repo.git"));
        assertThat(sanitized).doesNotContain("tokenvalue");
        assertThat(sanitized).doesNotContain("alice");
        // Scheme and host survive, because that is what a reviewer needs from the string.
        assertThat(sanitized).contains("git.example.com");
    }

    @Test
    @DisplayName("a command that could not start is recorded rather than lost")
    void recordsUnstartableCommand(@TempDir Path root) throws IOException {
        StageContext context = JournalHarness.context(root, "RUN-C3");
        Path workingDirectory = root.resolve("wd");
        Files.createDirectories(workingDirectory);

        StageExecutor.run(new CommandStage(c -> StageSupport.runner(c).run(
                List.of(workingDirectory.resolve("git").toString(), "status"), workingDirectory,
                Duration.ofSeconds(10), Map.of())), context);

        JsonNode command = record(context).path("commands").get(0);
        assertThat(command.path("result").asText()).isEqualTo("NOT_STARTED");
        assertThat(command.path("exit_code").asInt()).isNegative();
    }

    @Test
    @DisplayName("a timeout is classified apart from a non-zero exit")
    void timeoutIsItsOwnClassification() {
        assertThat(CommandExecutionRecord.classify(0, true))
                .isEqualTo(CommandExecutionRecord.TIMED_OUT);
        assertThat(CommandExecutionRecord.classify(1, false))
                .isEqualTo(CommandExecutionRecord.FAILED);
        assertThat(CommandExecutionRecord.classify(0, false))
                .isEqualTo(CommandExecutionRecord.SUCCESS);
        // A killed build told the harness nothing; a failing build told it something. Collapsing
        // the two would let a conclusion be drawn from evidence that was never produced.
        assertThat(CommandExecutionRecord.TIMED_OUT).isNotEqualTo(CommandExecutionRecord.FAILED);
    }

    @Test
    @DisplayName("a command run outside any attempt is dropped, not misattributed")
    void unattributedCommandIsDropped(@TempDir Path root) throws IOException {
        StageContext context = JournalHarness.context(root, "RUN-C4");
        Path workingDirectory = root.resolve("wd");
        Files.createDirectories(workingDirectory);

        // No stage is in flight here.
        StageSupport.runner(context).run(List.of(ProcessRunner.jdkTool("java"), "-version"),
                workingDirectory, Duration.ofSeconds(60), Map.of());

        assertThat(context.journal().current()).isNull();
        assertThat(context.journal().timeline()).isEmpty();
    }
}
