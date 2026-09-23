package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.security.SensitiveValues;
import com.bootshift.core.util.Json;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One external process the harness launched, attributed to the stage attempt that launched it.
 *
 * <p>Builds are where a migration actually meets reality, and until now the only durable trace of a
 * build was its own log file. A log answers "what did Maven print"; it does not answer "which stage,
 * on which edge, on which attempt, ran this, and how long did the run wait for it". This record is
 * the second half of that.
 *
 * <p>Nothing here holds command output. Output is large, is frequently the thing that contains a
 * credential, and already has a redacted home in the evidence store - so the record carries an
 * evidence reference and a line count, and a reader who needs the text follows the reference.
 */
public record CommandExecutionRecord(String commandId,
                                     String stageId,
                                     String edgeId,
                                     String attemptId,
                                     String stepId,
                                     String executable,
                                     String sanitizedCommand,
                                     String workingDirectory,
                                     Instant startedAt,
                                     Instant finishedAt,
                                     long durationMs,
                                     int exitCode,
                                     boolean timedOut,
                                     String stdoutEvidenceRef,
                                     String stderrEvidenceRef,
                                     int stdoutLines,
                                     int stderrLines,
                                     List<String> environmentKeys,
                                     String result) {

    /** Terminal classifications for a command. */
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String TIMED_OUT = "TIMED_OUT";
    public static final String NOT_STARTED = "NOT_STARTED";

    public CommandExecutionRecord {
        environmentKeys = environmentKeys == null ? List.of() : List.copyOf(environmentKeys);
    }

    /**
     * Classifies a finished command.
     *
     * <p>A timeout is kept distinct from a non-zero exit because they mean opposite things about the
     * evidence: a failing build told the harness something, whereas a killed one told it nothing and
     * every conclusion drawn from it is unsupported.
     */
    public static String classify(int exitCode, boolean timedOut) {
        if (timedOut) {
            return TIMED_OUT;
        }
        if (exitCode == 0) {
            return SUCCESS;
        }
        return exitCode < 0 ? NOT_STARTED : FAILED;
    }

    /**
     * Redacts a command vector for storage.
     *
     * <p>Arguments carry credentials often enough to matter: {@code -Dsonar.login=}, a repository URL
     * with an embedded password, a {@code --token} flag. The same line redaction that guards logs is
     * applied here, and additionally any {@code key=value} argument whose key reads as sensitive has
     * its value replaced rather than merely being pattern-matched.
     */
    public static String sanitize(List<String> command) {
        if (command == null || command.isEmpty()) {
            return "";
        }
        List<String> safe = new ArrayList<>(command.size());
        for (String argument : command) {
            safe.add(sanitizeArgument(argument));
        }
        return String.join(" ", safe);
    }

    /**
     * Build-tool argument keys that are credentials but do not read as one to the shared property
     * rules.
     *
     * <p>{@code sonar.login} is the archetype: the shared rules match {@code token} and
     * {@code password} because those are what configuration properties are called, whereas build
     * tools have their own vocabulary for the same secret. Scoped to command arguments deliberately -
     * widening the shared rule would start redacting {@code user.timezone} in configuration
     * artifacts, which is not a secret and is worth reading.
     */
    private static final List<String> COMMAND_SECRET_KEYS = List.of("login", "auth", "passphrase");

    static String sanitizeArgument(String argument) {
        if (argument == null) {
            return "";
        }
        int equals = argument.indexOf('=');
        if (equals > 0) {
            String key = argument.substring(0, equals);
            // -Dspring.datasource.password=... : the flag prefix is not part of the property name.
            String bare = key.startsWith("-D") ? key.substring(2)
                    : key.startsWith("--") ? key.substring(2) : key;
            String lower = bare.toLowerCase(java.util.Locale.ROOT);
            if (SensitiveValues.isSensitiveKey(bare)
                    || COMMAND_SECRET_KEYS.stream().anyMatch(lower::contains)) {
                return key + "=REDACTED";
            }
        }
        String redacted = SensitiveValues.redactLine(null, argument);
        if (!redacted.equals(argument)) {
            // Redaction already did its job. Re-testing the result would discard the whole argument,
            // because a redacted URI still has the shape of a credentialed one - REDACTED:REDACTED@ -
            // and the host is exactly the part a reviewer needs to keep.
            return redacted;
        }
        return SensitiveValues.looksSensitive(redacted) ? "REDACTED" : redacted;
    }

    public ObjectNode toNode() {
        ObjectNode node = Json.obj();
        node.put("command_id", commandId);
        node.put("stage_id", stageId);
        node.put("edge_id", edgeId);
        node.put("attempt_id", attemptId);
        node.put("step_id", stepId);
        node.put("executable", executable);
        node.put("sanitized_command", sanitizedCommand);
        node.put("working_directory", workingDirectory);
        node.put("started_at", startedAt == null ? null : startedAt.toString());
        node.put("finished_at", finishedAt == null ? null : finishedAt.toString());
        node.put("duration_ms", durationMs);
        node.put("exit_code", exitCode);
        node.put("timed_out", timedOut);
        node.put("stdout_evidence_ref", stdoutEvidenceRef);
        node.put("stderr_evidence_ref", stderrEvidenceRef);
        node.put("stdout_lines", stdoutLines);
        node.put("stderr_lines", stderrLines);
        // Names only. The values are exactly what must not be written, and the names alone answer
        // the question the record exists for: what was this process allowed to see.
        node.set("environment_summary", Json.toTree(Map.of(
                "variable_count", environmentKeys.size(),
                "variable_names", environmentKeys,
                "policy", "names only; values are never journalled")));
        node.put("result", result);
        return node;
    }
}
