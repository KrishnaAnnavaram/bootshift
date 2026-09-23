package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.util.Hashing;
import com.bootshift.core.util.Ids;
import com.bootshift.core.util.Json;
import com.bootshift.core.util.SchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The execution journal for one run.
 *
 * <p>Owns three things: the record of the attempt currently in flight, the run-wide timeline, and
 * the point at which both become files on disk. Everything else in the journal package is a value
 * object; this is the only part with a lifecycle.
 *
 * <h2>Why the current attempt is a single reference</h2>
 *
 * <p>Stages run one at a time - the orchestrator is sequential and the CLI runs one stage per
 * invocation - so at most one attempt is ever in flight. Within an attempt, work does fan out across
 * threads: {@code ProcessRunner} drains stdout and stderr on separate threads, and the managed
 * environment provider starts containers concurrently. All of that belongs to the same attempt, so a
 * single reference is both correct and the thing that makes command attribution work without
 * threading run context through every adapter. A {@code ThreadLocal} would have been wrong here for
 * exactly that reason: the thread that finishes a command is frequently not the thread that started
 * the stage.
 *
 * <h2>Failure policy</h2>
 *
 * <p>Capturing execution truth and rendering it for humans are separated deliberately.
 * {@code stage-execution.json} is mandatory: if it cannot be written the journal says so loudly,
 * because a missing execution record means the run has no audit trail for that attempt. Markdown
 * rendering is best-effort: a formatting defect in a document generator must never turn a stage that
 * migrated an application correctly into a failed stage. A rendering failure is recorded on the
 * record itself, surfaced in the timeline, and reported by the evidence stage as a documentation
 * gap - visible, attributable, and not fatal.
 */
public final class RunJournal {

    private static final Logger LOG = LoggerFactory.getLogger(RunJournal.class);

    public static final String STAGE_EXECUTION_FILE = "stage-execution.json";
    public static final String STAGE_DOCUMENT_FILE = "STAGE_DOCUMENT.md";
    public static final String RUN_DOCUMENT_FILE = "RUN_DOCUMENT.md";
    public static final String TIMELINE_FILE = "run-timeline.json";
    public static final String EDGE_DOCUMENT_FILE = "EDGE_DOCUMENT.md";
    public static final String EDGE_EXECUTION_FILE = "edge-execution.json";
    public static final String EDGES_DIRECTORY = "edges";
    public static final String JOURNAL_DIRECTORY = "journal";

    public static final String SCHEMA_VERSION = "1.0.0";

    private final String runId;
    private final OutputLayout output;
    private final AtomicReference<StageExecutionRecord> current = new AtomicReference<>();
    private final AtomicLong commandSequence = new AtomicLong();
    private final AtomicLong decisionSequence = new AtomicLong();
    private final List<ObjectNode> timeline = new ArrayList<>();
    private final List<String> journalFailures = new ArrayList<>();
    private String policyHash;
    private String toolVersion = "1.0.0";
    private SchemaValidator schemaValidator;
    private volatile boolean renderRunDocument = true;

    public RunJournal(String runId, OutputLayout output) {
        this.runId = runId;
        this.output = output;
        loadExistingTimeline();
    }

    /** A journal that records nothing, for callers with no run - unit tests and probes. */
    public static RunJournal disabled() {
        return new RunJournal(null, null);
    }

    public boolean enabled() {
        return runId != null && output != null;
    }

    public String runId() {
        return runId;
    }

    public OutputLayout output() {
        return output;
    }

    public RunJournal policyHash(String hash) {
        this.policyHash = hash;
        return this;
    }

    public RunJournal toolVersion(String version) {
        this.toolVersion = version;
        return this;
    }

    /**
     * Validates journal artifacts against their schemas before they are written.
     *
     * <p>A violation is recorded as a journal failure rather than thrown. The execution record is
     * the only durable account of what a stage did, and refusing to write a malformed one would
     * destroy the evidence instead of reporting the defect - the opposite of what an audit trail is
     * for. Publishing of stage artifacts keeps its existing, stricter rule.
     */
    public RunJournal schemaValidator(SchemaValidator validator) {
        this.schemaValidator = validator;
        return this;
    }

    private void validate(String schemaPath, String artifact, com.fasterxml.jackson.databind.JsonNode payload) {
        SchemaValidator validator = this.schemaValidator;
        if (validator == null || !validator.available()) {
            return;
        }
        try {
            List<String> errors = validator.validate(schemaPath, payload);
            boolean missing = errors.size() == 1 && errors.get(0).startsWith("Schema not found");
            if (!missing) {
                errors.forEach(e -> noteFailure(artifact + ": " + e));
            }
        } catch (RuntimeException e) {
            noteFailure("Cannot validate " + artifact + ": " + e.getMessage());
        }
    }

    /**
     * Suppresses run-document refresh.
     *
     * <p>Used by the finalization stages, which write the run document themselves once from a
     * complete picture rather than being refreshed from underneath while they read it.
     */
    public RunJournal renderRunDocument(boolean enabled) {
        this.renderRunDocument = enabled;
        return this;
    }

    /** The attempt currently in flight, or {@code null} outside a stage. */
    public StageExecutionRecord current() {
        return current.get();
    }

    /** Problems the journal itself hit. Non-empty means the audit trail has a hole in it. */
    public synchronized List<String> journalFailures() {
        return List.copyOf(journalFailures);
    }

    public synchronized List<ObjectNode> timeline() {
        return List.copyOf(timeline);
    }

    /**
     * Opens a new attempt.
     *
     * <p>The attempt id is minted here and is independent of the output directory's timestamp.
     * Timestamps collide - two attempts of a fast-failing stage land in the same millisecond - and a
     * colliding identifier makes retries indistinguishable from each other in exactly the situation
     * where telling them apart matters.
     */
    public StageExecutionRecord begin(String stageId, String stageName, String edgeId, String purpose,
                                      StageExecutionRecord.Trigger trigger, String triggerDetail) {
        StageExecutionRecord record = new StageExecutionRecord(runId, Ids.ulid(), stageId, stageName,
                edgeId, purpose, trigger, triggerDetail);
        record.policyHash(policyHash).toolVersion(toolVersion);
        current.set(record);
        return record;
    }

    /**
     * Settles an attempt and writes it out.
     *
     * <p>Called from a {@code finally} block, so it must tolerate being handed a record that a crash
     * left half-populated.
     */
    public void end(StageExecutionRecord record, ExecutionStatus status, Path attemptDirectory) {
        if (record == null) {
            return;
        }
        record.finish(status);
        if (attemptDirectory != null) {
            record.attemptDirectory(attemptDirectory.getFileName().toString());
        }
        try {
            persistAttempt(record, attemptDirectory);
        } catch (RuntimeException e) {
            noteFailure("Cannot persist execution record for " + record.stageId() + ": "
                    + e.getMessage());
        } finally {
            current.compareAndSet(record, null);
        }
        appendToTimeline(record);
    }

    /**
     * Writes {@code stage-execution.json} and then {@code STAGE_DOCUMENT.md}.
     *
     * <p>Order matters. The JSON is the authoritative record and is written first, so a renderer that
     * throws cannot cost the run its execution truth.
     */
    private void persistAttempt(StageExecutionRecord record, Path attemptDirectory) {
        if (!enabled()) {
            return;
        }
        Path directory = attemptDirectory != null ? attemptDirectory
                : fallbackAttemptDirectory(record);
        if (directory == null) {
            noteFailure("No directory available for the execution record of " + record.stageId());
            return;
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            noteFailure("Cannot create journal directory " + directory + ": " + e.getMessage());
            return;
        }

        Path json = directory.resolve(STAGE_EXECUTION_FILE);
        com.fasterxml.jackson.databind.node.ObjectNode payload = record.toNode();
        validate("journal/stage-execution.schema.json", STAGE_EXECUTION_FILE, payload);
        Json.write(json, payload);

        // Rendering is separated from truth capture: see the class comment.
        try {
            String markdown = StageDocumentRenderer.render(record, executionRecordHash(json));
            Files.writeString(directory.resolve(STAGE_DOCUMENT_FILE), markdown,
                    StandardCharsets.UTF_8);
        } catch (RuntimeException | IOException e) {
            String message = "STAGE_DOCUMENT.md rendering failed for " + record.stageId() + ": "
                    + e.getClass().getSimpleName() + " " + e.getMessage();
            LOG.warn(message);
            record.renderingError(message);
            noteFailure(message);
            // Re-write the JSON so the recorded rendering failure is itself durable.
            try {
                Json.write(json, record.toNode());
            } catch (RuntimeException ignored) {
                // The original record is already on disk; nothing further to salvage.
            }
        }
    }

    /**
     * Where a journal goes when the stage published nothing.
     *
     * <p>A refused stage never opens a writer, so there is no attempt directory to write beside. It
     * still gets one - under {@code journal/} within the stage's output root - because the whole
     * point of the journal is that the attempts which produced no artifacts are the ones a reader
     * most needs explained. {@code latest.json} is untouched, so a refused attempt remains invisible
     * to every consumer that follows the pointer.
     */
    private Path fallbackAttemptDirectory(StageExecutionRecord record) {
        if (record.stageId() == null) {
            return null;
        }
        return output.stageRoot(record.stageId())
                .resolve(JOURNAL_DIRECTORY)
                .resolve(record.attemptId());
    }

    private String executionRecordHash(Path json) {
        try {
            return Hashing.sha256File(json);
        } catch (IOException e) {
            return null;
        }
    }

    private synchronized void appendToTimeline(StageExecutionRecord record) {
        ObjectNode entry = Json.obj();
        entry.put("stage_id", record.stageId());
        entry.put("stage_name", record.stageName());
        entry.put("attempt_id", record.attemptId());
        entry.put("edge_id", record.edgeId());
        entry.put("trigger", record.trigger().name());
        entry.put("start", record.startedAt() == null ? null : record.startedAt().toString());
        entry.put("end", record.finishedAt() == null ? null : record.finishedAt().toString());
        entry.put("duration_ms", record.durationMs());
        entry.put("status", record.status().name());
        entry.put("summary", record.summary());
        entry.put("state_before", record.stateBefore());
        entry.put("state_after", record.stateAfter());
        entry.put("attempt_directory", record.attemptDirectory());
        entry.put("stop_reason", record.stopReason());
        entry.put("next_action", record.nextAction());
        entry.put("command_count", record.commands().size());
        entry.put("decision_count", record.decisions().size());
        entry.put("warning_count", record.warnings().size());
        entry.put("error_count", record.errors().size());
        entry.put("unexecuted_step_count", record.unexecutedSteps().size());
        entry.put("rendering_error", record.renderingError());
        timeline.add(entry);
        persistTimeline();
    }

    private synchronized void persistTimeline() {
        if (!enabled()) {
            return;
        }
        try {
            ObjectNode node = Json.obj();
            node.put("schema_version", SCHEMA_VERSION);
            node.put("run_id", runId);
            node.put("generated_at", Instant.now().toString());
            node.put("attempt_count", timeline.size());
            node.put("purpose", "Ordered record of every stage attempt in this run, including "
                    + "attempts that refused, failed or crashed. Written after each attempt so it "
                    + "survives a run that never reaches finalization.");
            node.set("entries", Json.toTree(timeline));
            node.set("journal_failures", Json.toTree(journalFailures));
            validate("journal/run-timeline.schema.json", TIMELINE_FILE, node);
            Json.writeAtomic(output.root().resolve(TIMELINE_FILE), node);
        } catch (RuntimeException e) {
            LOG.warn("Cannot persist run timeline: {}", e.getMessage());
        }
    }

    /**
     * Rewrites {@code RUN_DOCUMENT.md} from the timeline and the published artifact plane.
     *
     * <p>Called after every attempt, so the document describes everything that has happened so far
     * whether or not the run will ever reach the evidence stage.
     */
    public void refreshRunDocument() {
        if (!enabled() || !renderRunDocument) {
            return;
        }
        try {
            String markdown = RunDocumentRenderer.render(this);
            Path target = output.root().resolve(RUN_DOCUMENT_FILE);
            Files.createDirectories(target.getParent());
            Files.writeString(target, markdown, StandardCharsets.UTF_8);
        } catch (RuntimeException | IOException e) {
            String message = "RUN_DOCUMENT.md rendering failed: " + e.getClass().getSimpleName()
                    + " " + e.getMessage();
            LOG.warn(message);
            noteFailure(message);
        }
    }

    /** Where the edge journal for an edge lives. */
    public Path edgeDirectory(String edgeId) {
        return output.root().resolve(EDGES_DIRECTORY).resolve(edgeId);
    }

    public synchronized void noteFailure(String message) {
        if (message != null && !journalFailures.contains(message)) {
            journalFailures.add(message);
        }
    }

    /** Mints the next command id for this run. */
    public String nextCommandId() {
        return String.format("CMD-%06d", commandSequence.incrementAndGet());
    }

    /** Mints the next decision id for this run. */
    public String nextDecisionId() {
        return Ids.decisionId(decisionSequence.incrementAndGet());
    }

    /**
     * Records a decision against the attempt in flight.
     *
     * <p>Silently ignored when no attempt is open, so a helper shared between a stage and a unit test
     * does not need to know which it is running in.
     */
    public void decision(DecisionRecord record) {
        StageExecutionRecord attempt = current.get();
        if (attempt != null && record != null) {
            attempt.decision(record);
        }
    }

    /**
     * The command sink handed to {@code ProcessRunner}.
     *
     * <p>Attribution is filled in from the attempt in flight. A command executed outside any attempt
     * is dropped rather than recorded against whichever stage happened to run last.
     */
    public CommandObserver commandObserver() {
        return observation -> {
            try {
                StageExecutionRecord attempt = current.get();
                if (attempt == null || observation == null) {
                    return;
                }
                List<String> command = observation.command();
                String executable = command == null || command.isEmpty() ? null
                        : Path.of(command.get(0)).getFileName().toString();
                Set<String> environmentKeys = observation.environmentKeys() == null
                        ? Set.of() : new LinkedHashSet<>(observation.environmentKeys());
                attempt.command(new CommandExecutionRecord(
                        nextCommandId(),
                        attempt.stageId(),
                        attempt.edgeId(),
                        attempt.attemptId(),
                        null,
                        executable,
                        CommandExecutionRecord.sanitize(command),
                        observation.workingDirectory() == null ? null
                                : observation.workingDirectory().toString(),
                        observation.startedAt(),
                        observation.finishedAt(),
                        observation.startedAt() == null || observation.finishedAt() == null ? 0L
                                : java.time.Duration.between(observation.startedAt(),
                                        observation.finishedAt()).toMillis(),
                        observation.exitCode(),
                        observation.timedOut(),
                        observation.logSink() == null ? null : observation.logSink().toString(),
                        null,
                        observation.stdoutLines(),
                        observation.stderrLines(),
                        new ArrayList<>(environmentKeys),
                        CommandExecutionRecord.classify(observation.exitCode(), observation.timedOut())));
            } catch (RuntimeException e) {
                // A journal must never be the reason a build result is lost.
                LOG.warn("Cannot journal command: {}", e.getMessage());
            }
        };
    }

    /**
     * Reloads the timeline of a run that is being resumed.
     *
     * <p>Without this a resumed run would write a run document describing only the attempts made
     * since resumption, which is the opposite of what the document is for.
     */
    private void loadExistingTimeline() {
        if (!enabled()) {
            return;
        }
        Path file = output.root().resolve(TIMELINE_FILE);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonNode node = Json.read(file);
            for (JsonNode entry : node.path("entries")) {
                if (entry instanceof ObjectNode object) {
                    timeline.add(object);
                }
            }
            for (JsonNode failure : node.path("journal_failures")) {
                journalFailures.add(failure.asText());
            }
        } catch (RuntimeException e) {
            LOG.warn("Cannot read existing run timeline, starting a fresh one: {}", e.getMessage());
        }
    }
}
