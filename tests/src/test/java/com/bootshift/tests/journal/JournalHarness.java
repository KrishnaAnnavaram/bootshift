package com.bootshift.tests.journal;

import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.RunContext;
import com.bootshift.core.policy.HarnessPolicy;
import com.bootshift.core.state.StateMachine;
import com.bootshift.core.util.SchemaValidator;
import com.bootshift.stages.StageContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds the smallest {@link StageContext} the execution journal needs.
 *
 * <p>Deliberately minimal. The journal reaches only the run context, the state machine and the
 * artifact layout, so the ports it never touches are left null: a test harness that wires a
 * telemetry adapter and an HTTP fetcher to test a document renderer is a harness that will break
 * whenever either of those changes, for no benefit.
 */
final class JournalHarness {

    private JournalHarness() {
    }

    static StageContext context(Path root, String runId) throws IOException {
        Path output = root.resolve("output");
        Path workspace = root.resolve("workspaces");
        Path source = root.resolve("src");
        Files.createDirectories(output);
        Files.createDirectories(workspace);
        Files.createDirectories(source);

        RunContext run = new RunContext(runId, source, workspace, new OutputLayout(output),
                HarnessPolicy.development(), false, "local");
        Files.createDirectories(run.runWorkspace());

        return new StageContext(run, new StateMachine(), null, null, null, null,
                new SchemaValidator(schemaRoot()), null, null, null, harnessRoot(), null);
    }

    /** The repository root, located from the working directory the surefire fork runs in. */
    static Path harnessRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.isDirectory(candidate.resolve("schemas"))) {
            candidate = candidate.getParent();
        }
        return candidate == null ? Path.of("").toAbsolutePath() : candidate;
    }

    static Path schemaRoot() {
        return harnessRoot().resolve("schemas");
    }
}
