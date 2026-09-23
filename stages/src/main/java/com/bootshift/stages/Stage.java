package com.bootshift.stages;

import com.bootshift.core.domain.StageResult;
import com.bootshift.core.journal.StepDeclaration;
import com.bootshift.core.state.RunState;

import java.util.List;

/**
 * A pipeline stage (spec section 9).
 *
 * <p>"Agent" here means a controlled stage with declared inputs, outputs, preconditions,
 * postconditions and authority boundaries - not an autonomous LLM. Most stages are fully
 * deterministic; the few that may consult AI keep it bounded and non-authorizing.
 *
 * <p>R24: every stage is independently runnable. The orchestrator sequences them and holds no
 * migration semantics of its own, so deleting it would not delete any stage logic.
 */
public interface Stage {

    /** Stable stage identifier, e.g. {@code 01-inventory}. */
    String id();

    /** Directory under {@code output/} that this stage publishes into. */
    String outputDirectory();

    /** Human-readable purpose, surfaced by {@code harness stages}. */
    String purpose();

    /** True when the stage may write to application source. Only Agent 12 and 13 return true. */
    default boolean mutating() {
        return false;
    }

    /** True when the stage may consult the optional AI provider. */
    default boolean aiAssisted() {
        return false;
    }

    /**
     * The migration edge this stage instance acts on, or {@code null} for the analysis half.
     *
     * <p>Edge-scoped stages already hold this; exposing it lets cross-cutting infrastructure attribute
     * an attempt to an edge without knowing which concrete stage it is looking at. Without it, a
     * per-edge loop's six stages are indistinguishable from six unrelated attempts in any record kept
     * outside the stages themselves.
     */
    default String edgeId() {
        return null;
    }

    /** States that must already have been reached for this stage to be legal. */
    List<RunState> preconditions();

    /** State the run reaches when this stage succeeds. */
    RunState postcondition();

    /** Artifacts this stage consumes, as {@code stageDirectory/artifactName} pairs. */
    default List<String> inputArtifacts() {
        return List.of();
    }

    /** Artifacts this stage publishes. */
    List<String> outputArtifacts();

    /**
     * The steps this stage intends to perform, in order.
     *
     * <p>Declared here rather than accumulated during execution, so that a step which never runs is
     * still present in the record. The execution journal marks each one as the stage settles it, and
     * anything left {@link com.bootshift.core.journal.StepStatus#PENDING} at the end is reported as a
     * declared capability that was not exercised.
     *
     * <p>Empty means the stage has not been instrumented, which is itself visible in its document.
     */
    default List<StepDeclaration> declaredSteps() {
        return List.of();
    }

    StageResult execute(StageContext context);
}
