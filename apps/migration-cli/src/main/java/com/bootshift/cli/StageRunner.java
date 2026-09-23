package com.bootshift.cli;

import com.bootshift.core.domain.StageResult;
import com.bootshift.core.journal.RunJournal;
import com.bootshift.core.journal.StageExecutionRecord;
import com.bootshift.stages.Stage;
import com.bootshift.stages.StageContext;
import com.bootshift.stages.StageExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Renders a stage result for humans while the artifacts stay rich JSON (spec section 54).
 */
final class StageRunner {

    private StageRunner() {
    }

    /**
     * Runs one stage from the command line.
     *
     * <p>Goes through {@link StageExecutor}, which {@code StageRunner} previously bypassed by calling
     * {@code stage.execute(context)} directly. Two things were lost by that: a stage invoked out of
     * order failed somewhere in its middle instead of refusing at the door, and - once the execution
     * journal existed - every single-stage CLI invocation would have produced no record at all, which
     * is exactly the invocation an operator debugging a run reaches for.
     */
    static int run(Stage stage, StageContext context) {
        StageResult result = StageExecutor.run(stage, context, StageExecutionRecord.Trigger.CLI,
                "Invoked directly as `bootshift " + stage.id() + "`");
        print(result, context);
        printDocuments(result, context, stage);
        return result.exitCode().code();
    }

    /**
     * Points the operator at the documents this attempt produced.
     *
     * <p>The console stays a summary - detail belongs in artifacts - but a path the operator can open
     * is the difference between artifacts that exist and artifacts that get read.
     */
    private static void printDocuments(StageResult result, StageContext context, Stage stage) {
        Path stageDocument = latestStageDocument(context, stage);
        Path runDocument = context.run().output().root().resolve(RunJournal.RUN_DOCUMENT_FILE);
        if (stageDocument != null || Files.isRegularFile(runDocument)) {
            System.out.println("  documentation:");
            if (stageDocument != null) {
                System.out.println("    stage: " + stageDocument);
            }
            if (Files.isRegularFile(runDocument)) {
                System.out.println("    run:   " + runDocument);
            }
            System.out.println();
        }
    }

    private static Path latestStageDocument(StageContext context, Stage stage) {
        String directory = context.journal().timeline().stream()
                .filter(e -> stage.id().equals(e.path("stage_id").asText(null)))
                .reduce((first, second) -> second)
                .map(e -> e.path("attempt_directory").asText(null))
                .orElse(null);
        if (directory == null || directory.isBlank()) {
            return null;
        }
        Path candidate = context.run().output().stageRoot(stage.outputDirectory())
                .resolve(directory).resolve(RunJournal.STAGE_DOCUMENT_FILE);
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        Path journalled = context.run().output().stageRoot(stage.outputDirectory())
                .resolve(RunJournal.JOURNAL_DIRECTORY).resolve(directory)
                .resolve(RunJournal.STAGE_DOCUMENT_FILE);
        return Files.isRegularFile(journalled) ? journalled : null;
    }

    static void print(StageResult result, StageContext context) {
        System.out.println();
        System.out.println("  " + result.stageId() + "  [" + result.exitCode().name() + "]");
        System.out.println("  run: " + context.run().runId());
        System.out.println("  " + result.summary());
        if (!result.messages().isEmpty()) {
            System.out.println();
            result.messages().forEach(m -> System.out.println("  ! " + m));
        }
        if (!result.artifacts().isEmpty()) {
            System.out.println();
            System.out.println("  artifacts:");
            for (Map.Entry<String, Path> entry : result.artifacts().entrySet()) {
                System.out.println("    " + entry.getValue());
            }
        }
        System.out.println();
    }
}
