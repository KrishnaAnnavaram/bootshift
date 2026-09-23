package com.bootshift.core.journal;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * How process execution reports itself to the run journal.
 *
 * <p>Deliberately an outbound interface owned by the journal rather than something the executor
 * imports from a stage. {@code ProcessRunner} knows what it ran and what happened; it does not know
 * which stage, which edge or which attempt it was running for, and teaching it would mean threading
 * run context through every adapter that shells out. The journal already holds that context, so the
 * runner reports facts and the journal supplies attribution.
 *
 * <p>Implementations must not throw. A stage that migrated an application correctly does not become
 * a failed stage because its journal could not record a command.
 */
@FunctionalInterface
public interface CommandObserver {

    /** Raw facts about one finished process, before any attribution. */
    record CommandObservation(List<String> command,
                              Path workingDirectory,
                              Instant startedAt,
                              Instant finishedAt,
                              int exitCode,
                              boolean timedOut,
                              int stdoutLines,
                              int stderrLines,
                              Path logSink,
                              Set<String> environmentKeys) {
    }

    void observe(CommandObservation observation);

    /** No-op sink, used wherever a runner executes outside a journalled run - tests, probes. */
    CommandObserver NONE = observation -> {
    };
}
