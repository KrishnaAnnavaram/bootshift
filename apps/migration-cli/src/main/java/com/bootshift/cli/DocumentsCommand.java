package com.bootshift.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.journal.RunJournal;
import com.bootshift.stages.StageContext;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Locates the documents a run produced.
 *
 * <p>The documents are useless if nobody can find them. They live in timestamped attempt
 * directories - which is right, because attempts are immutable and a run has many - and that makes
 * them exactly the kind of thing a person will not go looking for by hand. This command answers
 * "where is it" and nothing else; it deliberately does not reprint the content, because the
 * documents are already files and a terminal is a poor place to read a hundred-line table.
 */
@CommandLine.Command(name = "documents", description =
        "Locate the runtime documents for a run: run, stage, edge and final migration document")
final class DocumentsCommand implements Callable<Integer> {

    @CommandLine.Mixin
    CommonOptions options = new CommonOptions();

    @CommandLine.Option(names = "--stage", description =
            "Show only documents for this stage id, e.g. 13-build-repair")
    String stage;

    @CommandLine.Option(names = "--edge", description =
            "Show only the document for this edge id, e.g. EDGE-2-PATCH")
    String edge;

    @Override
    public Integer call() {
        StageContext context = options.context();
        RunJournal journal = context.journal();
        Path root = context.run().output().root();

        System.out.println();
        System.out.println("  run: " + context.run().runId());
        System.out.println();

        if (edge == null && stage == null) {
            printIfPresent("run document      ", root.resolve(RunJournal.RUN_DOCUMENT_FILE));
            printIfPresent("run timeline      ", root.resolve(RunJournal.TIMELINE_FILE));
            printIfPresent("migration document",
                    context.run().output().latestArtifactPath("19-evidence",
                            "MIGRATION_DOCUMENT.md"));
            System.out.println();
        }

        if (edge == null) {
            printStageDocuments(context, journal);
        }
        if (stage == null) {
            printEdgeDocuments(root, journal);
        }
        System.out.println();
        return 0;
    }

    /**
     * Every attempt, not only the published one.
     *
     * <p>A stage that failed twice before succeeding has three documents, and the two failures are
     * usually the interesting ones. Showing only what {@code latest.json} names would hide exactly
     * the attempts a person runs this command to find.
     */
    private void printStageDocuments(StageContext context, RunJournal journal) {
        List<ObjectNode> timeline = journal.timeline();
        boolean any = false;
        for (ObjectNode entry : timeline) {
            String stageId = entry.path("stage_id").asText(null);
            if (stageId == null || (stage != null && !stage.equals(stageId))) {
                continue;
            }
            String directory = entry.path("attempt_directory").asText(null);
            if (directory == null || directory.isBlank()) {
                continue;
            }
            Path document = resolve(context, stageId, directory);
            if (document == null) {
                continue;
            }
            if (!any) {
                System.out.println("  stage documents:");
                any = true;
            }
            String edgeId = entry.path("edge_id").asText(null);
            System.out.printf("    %-20s %-10s %s%s%n", stageId,
                    entry.path("status").asText("?"),
                    edgeId == null || edgeId.isBlank() ? "" : edgeId + "  ",
                    document);
        }
        if (!any) {
            System.out.println("  no stage documents found"
                    + (stage == null ? "" : " for " + stage));
        }
    }

    private Path resolve(StageContext context, String stageId, String directory) {
        Path published = context.run().output().stageRoot(stageId).resolve(directory)
                .resolve(RunJournal.STAGE_DOCUMENT_FILE);
        if (Files.isRegularFile(published)) {
            return published;
        }
        // An attempt that refused before opening a writer is journalled under journal/<attemptId>.
        Path journalled = context.run().output().stageRoot(stageId)
                .resolve(RunJournal.JOURNAL_DIRECTORY).resolve(directory)
                .resolve(RunJournal.STAGE_DOCUMENT_FILE);
        return Files.isRegularFile(journalled) ? journalled : null;
    }

    private void printEdgeDocuments(Path root, RunJournal journal) {
        Path edges = root.resolve(RunJournal.EDGES_DIRECTORY);
        if (!Files.isDirectory(edges)) {
            return;
        }
        System.out.println();
        System.out.println("  edge documents:");
        try (var stream = Files.list(edges)) {
            stream.sorted().forEach(directory -> {
                String edgeId = directory.getFileName().toString();
                if (edge != null && !edge.equals(edgeId)) {
                    return;
                }
                Path document = directory.resolve(RunJournal.EDGE_DOCUMENT_FILE);
                if (Files.isRegularFile(document)) {
                    System.out.printf("    %-20s %s%n", edgeId, document);
                }
            });
        } catch (java.io.IOException e) {
            System.out.println("    cannot list " + edges + ": " + e.getMessage());
        }
    }

    private void printIfPresent(String label, Path path) {
        if (path != null && Files.isRegularFile(path)) {
            System.out.println("  " + label + "  " + path);
        } else {
            System.out.println("  " + label + "  (not produced yet)");
        }
    }
}
