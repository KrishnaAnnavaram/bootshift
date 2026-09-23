package com.bootshift.core.journal;

/**
 * Outcome of one declared step inside a stage attempt.
 *
 * <p>The distinctions here exist because "no exception was thrown" is not a synonym for "the work
 * happened". A step that found nothing to do, a step that fell back to a weaker method and a step
 * that was never reached are three different facts about a run, and collapsing them into SUCCESS is
 * how a report ends up claiming coverage the run never had.
 */
public enum StepStatus {

    /** Declared, not yet started. A step still PENDING when the attempt finalizes was never run. */
    PENDING(false, false),

    /** Started and not finished. Only observed when a stage crashed mid-step. */
    RUNNING(false, false),

    /** Did what it declared, with the method it declared. */
    SUCCESS(true, true),

    /** Deliberately not run because its precondition did not hold. Nothing is claimed from it. */
    SKIPPED(true, false),

    /** Produced a result by a weaker route than declared. The result stands; the confidence drops. */
    DEGRADED(true, true),

    /** Refused on policy grounds. A refusal is a decision, not an error. */
    REFUSED(true, false),

    /** Attempted and failed. */
    FAILED(false, false),

    /** Cannot proceed until something outside the harness changes, usually a human decision. */
    BLOCKED(false, false),

    /** Does not apply to this stage in this configuration; carries no expectation of execution. */
    NOT_APPLICABLE(true, false);

    private final boolean terminal;
    private final boolean produced;

    StepStatus(boolean terminal, boolean produced) {
        this.terminal = terminal;
        this.produced = produced;
    }

    /** True when the step reached a settled outcome rather than being cut short. */
    public boolean terminal() {
        return terminal;
    }

    /** True when downstream work may rely on this step having produced something. */
    public boolean producedResult() {
        return produced;
    }

    /** True when the step was declared but never actually entered. */
    public boolean unexecuted() {
        return this == PENDING;
    }
}
