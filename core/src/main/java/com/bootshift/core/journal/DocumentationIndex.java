package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.util.Json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The catalogue of documents a run produced, and of the ones it should have produced and did not.
 *
 * <p>The second half is the reason this exists. A documentation system that only lists what it wrote
 * cannot report its own omissions, and an omission in an audit trail is precisely the thing a
 * reviewer needs told rather than left to notice. Each timeline entry names an attempt directory; if
 * the document that belongs there is absent, that is a gap with a stage id attached to it.
 */
public final class DocumentationIndex {

    private DocumentationIndex() {
    }

    /** Builds the index from the run timeline and what is actually on disk. */
    public static ObjectNode build(OutputLayout output, String runId, List<ObjectNode> timeline,
                                   List<String> journalFailures) {
        ObjectNode node = Json.obj();
        node.put("schema_version", RunJournal.SCHEMA_VERSION);
        node.put("run_id", runId);
        node.put("generated_at", Instant.now().toString());
        node.put("purpose", "Every document this run generated, and every document it should have "
                + "generated and did not. The second list is what makes the first trustworthy.");

        List<ObjectNode> stageDocuments = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Set<String> edgeIds = new LinkedHashSet<>();

        for (ObjectNode entry : timeline) {
            String stageId = entry.path("stage_id").asText(null);
            String directory = entry.path("attempt_directory").asText(null);
            String edgeId = entry.path("edge_id").asText(null);
            if (edgeId != null && !edgeId.isBlank()) {
                edgeIds.add(edgeId);
            }
            if (stageId == null || directory == null || directory.isBlank()) {
                missing.add((stageId == null ? "unknown stage" : stageId)
                        + ": the attempt recorded no output directory, so it has no stage document");
                continue;
            }
            Path attemptDir = resolveAttemptDirectory(output, stageId, directory);
            ObjectNode document = Json.obj();
            document.put("stage_id", stageId);
            document.put("attempt_id", entry.path("attempt_id").asText(null));
            document.put("edge_id", edgeId);
            document.put("status", entry.path("status").asText(null));
            String relative = stageId + "/" + directory + "/";
            document.put("stage_document", relative + RunJournal.STAGE_DOCUMENT_FILE);
            document.put("execution_record", relative + RunJournal.STAGE_EXECUTION_FILE);
            boolean documentPresent = attemptDir != null
                    && Files.isRegularFile(attemptDir.resolve(RunJournal.STAGE_DOCUMENT_FILE));
            boolean recordPresent = attemptDir != null
                    && Files.isRegularFile(attemptDir.resolve(RunJournal.STAGE_EXECUTION_FILE));
            document.put("stage_document_present", documentPresent);
            document.put("execution_record_present", recordPresent);
            stageDocuments.add(document);
            if (!recordPresent) {
                missing.add(stageId + " attempt " + directory + ": stage-execution.json is absent");
            }
            if (!documentPresent) {
                missing.add(stageId + " attempt " + directory + ": STAGE_DOCUMENT.md is absent");
            }
        }
        node.set("stage_documents", Json.toTree(stageDocuments));

        List<ObjectNode> edgeDocuments = new ArrayList<>();
        for (String edgeId : edgeIds) {
            Path directory = output.root().resolve(RunJournal.EDGES_DIRECTORY).resolve(edgeId);
            ObjectNode document = Json.obj();
            document.put("edge_id", edgeId);
            document.put("edge_document", RunJournal.EDGES_DIRECTORY + "/" + edgeId + "/"
                    + RunJournal.EDGE_DOCUMENT_FILE);
            document.put("edge_execution", RunJournal.EDGES_DIRECTORY + "/" + edgeId + "/"
                    + RunJournal.EDGE_EXECUTION_FILE);
            boolean present = Files.isRegularFile(directory.resolve(RunJournal.EDGE_DOCUMENT_FILE));
            document.put("present", present);
            edgeDocuments.add(document);
            if (!present) {
                missing.add("edge " + edgeId + ": EDGE_DOCUMENT.md is absent");
            }
        }
        node.set("edge_documents", Json.toTree(edgeDocuments));

        ObjectNode runDocument = Json.obj();
        runDocument.put("run_document", RunJournal.RUN_DOCUMENT_FILE);
        runDocument.put("timeline", RunJournal.TIMELINE_FILE);
        boolean runPresent = Files.isRegularFile(output.root().resolve(RunJournal.RUN_DOCUMENT_FILE));
        runDocument.put("present", runPresent);
        node.set("run_document", runDocument);
        if (!runPresent) {
            missing.add("RUN_DOCUMENT.md is absent from the output root");
        }

        journalFailures.forEach(f -> missing.add("journal: " + f));

        node.put("stage_document_count", stageDocuments.size());
        node.put("edge_document_count", edgeDocuments.size());
        node.put("documentation_gap_count", missing.size());
        node.set("documentation_gaps", Json.toTree(missing));
        node.put("complete", missing.isEmpty());
        return node;
    }

    /**
     * Locates an attempt directory.
     *
     * <p>A published attempt writes into the stage's timestamped directory; an attempt that refused
     * before opening a writer is journalled under {@code journal/<attemptId>} instead. Both are real
     * attempts and both are checked here.
     */
    private static Path resolveAttemptDirectory(OutputLayout output, String stageId, String directory) {
        Path published = output.stageRoot(stageId).resolve(directory);
        if (Files.isDirectory(published)) {
            return published;
        }
        Path journalled = output.stageRoot(stageId).resolve(RunJournal.JOURNAL_DIRECTORY)
                .resolve(directory);
        return Files.isDirectory(journalled) ? journalled : null;
    }

    /** True when every attempt in the timeline has both of its documents on disk. */
    public static boolean complete(JsonNode index) {
        return index != null && index.path("complete").asBoolean();
    }
}
