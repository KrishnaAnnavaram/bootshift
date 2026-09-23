package com.bootshift.tests.journal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.journal.DocumentationIndex;
import com.bootshift.core.journal.RunJournal;
import com.bootshift.core.state.RunState;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The documentation index has to be able to report its own omissions.
 *
 * <p>A catalogue that lists only what was written cannot say anything about what is missing, and a
 * missing entry in an audit trail is exactly what a reviewer needs told rather than left to notice.
 * The index is therefore built by checking the run timeline against the filesystem, not by
 * remembering what the journal believes it wrote.
 */
class DocumentationIndexTest {

    private static final class Simple implements Stage {
        private final String id;
        private final String edgeId;

        Simple(String id, String edgeId) {
            this.id = id;
            this.edgeId = edgeId;
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
            return "Stage under documentation index test";
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
            OutputLayout.StageWriter writer = context.run().output().open(id);
            writer.write("payload.json", Map.of("ok", true));
            String hash = StageSupport.publish(context, writer);
            return new StageResult(id, ExitCode.SUCCESS, "ok", List.of(), Map.of(), hash);
        }
    }

    @Test
    @DisplayName("a complete run indexes every stage, edge and run document with no gaps")
    void indexesEverything(@TempDir Path root) throws IOException {
        StageContext context = JournalHarness.context(root, "RUN-DI1");
        StageExecutor.run(new Simple("01-inventory", null), context);
        StageExecutor.run(new Simple("12-transformation", "EDGE-1"), context);

        ObjectNode index = DocumentationIndex.build(context.run().output(), "RUN-DI1",
                context.journal().timeline(), context.journal().journalFailures());

        assertThat(index.path("stage_document_count").asInt()).isEqualTo(2);
        assertThat(index.path("edge_document_count").asInt()).isEqualTo(1);
        assertThat(index.path("run_document").path("present").asBoolean()).isTrue();
        assertThat(index.path("documentation_gap_count").asInt()).isZero();
        assertThat(DocumentationIndex.complete(index)).isTrue();

        // Paths, not contents: the index points at the authoritative records rather than copying
        // them, so it stays small on a run with hundreds of attempts.
        assertThat(index.path("stage_documents").get(0).path("execution_record").asText())
                .endsWith(RunJournal.STAGE_EXECUTION_FILE);
    }

    @Test
    @DisplayName("a document deleted after the fact is reported as a gap, not silently omitted")
    void reportsMissingDocument(@TempDir Path root) throws IOException {
        StageContext context = JournalHarness.context(root, "RUN-DI2");
        StageExecutor.run(new Simple("01-inventory", null), context);

        Path attempt = context.run().output().resolveLatestDir("01-inventory");
        Files.delete(attempt.resolve(RunJournal.STAGE_DOCUMENT_FILE));

        ObjectNode index = DocumentationIndex.build(context.run().output(), "RUN-DI2",
                context.journal().timeline(), context.journal().journalFailures());

        assertThat(DocumentationIndex.complete(index)).isFalse();
        assertThat(index.path("documentation_gaps").toString())
                .contains("STAGE_DOCUMENT.md is absent");
        // The structured record is still there, and the index says so - the two are tracked apart
        // because losing the rendering and losing the evidence are different severities.
        assertThat(index.path("stage_documents").get(0).path("execution_record_present").asBoolean())
                .isTrue();
        assertThat(index.path("stage_documents").get(0).path("stage_document_present").asBoolean())
                .isFalse();
    }

    @Test
    @DisplayName("journal failures are carried into the documentation gaps")
    void carriesJournalFailures(@TempDir Path root) throws IOException {
        StageContext context = JournalHarness.context(root, "RUN-DI3");
        StageExecutor.run(new Simple("01-inventory", null), context);
        context.journal().noteFailure("EDGE_DOCUMENT.md rendering failed for EDGE-9");

        ObjectNode index = DocumentationIndex.build(context.run().output(), "RUN-DI3",
                context.journal().timeline(), context.journal().journalFailures());

        assertThat(index.path("documentation_gaps").toString()).contains("EDGE-9");
        assertThat(DocumentationIndex.complete(index)).isFalse();
    }
}
