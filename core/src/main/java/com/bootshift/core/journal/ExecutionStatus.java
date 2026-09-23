package com.bootshift.core.journal;

/**
 * Terminal classification of a stage attempt.
 *
 * <p>Separate from {@code ExitCode} on purpose. An exit code tells a shell what to do next; this
 * tells a reader what happened. The two differ in exactly the case that matters most: a structured
 * refusal and an unhandled crash can both end a run, and only one of them is the harness working
 * correctly.
 */
public enum ExecutionStatus {

    /** The stage did what it declared. */
    SUCCESS,

    /** The stage ran and produced a result, by a weaker route than declared. */
    DEGRADED,

    /** Refused before executing, because a declared precondition did not hold. */
    REFUSED,

    /** Ran, and stopped deliberately on a finding it is not permitted to decide alone. */
    BLOCKED,

    /** Ran and failed. */
    FAILED,

    /** Threw something the stage did not handle. Distinct from FAILED: nothing was concluded. */
    CRASHED,

    /** Started and never reached an end. Only written by recovery, never by the stage itself. */
    INCOMPLETE;

    public boolean succeeded() {
        return this == SUCCESS || this == DEGRADED;
    }

    /** True when the pipeline cannot legitimately continue past this attempt. */
    public boolean halting() {
        return this == BLOCKED || this == FAILED || this == CRASHED || this == REFUSED;
    }
}
