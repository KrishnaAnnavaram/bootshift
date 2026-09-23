package com.bootshift.core.journal;

/**
 * A step a stage promises to perform, declared before the stage runs.
 *
 * <p>Declaration is what makes an omission visible. A step list assembled while a stage executes can
 * only contain steps that executed, so the failure mode this project has already had - code that was
 * implemented and never invoked - leaves no trace in it. A declared list still holds the step, marked
 * {@link StepStatus#PENDING}, and the stage document reports it.
 *
 * @param stepId  stable identifier, unique within the stage, e.g. {@code IMP-003}
 * @param name    what the step does, in a few words
 * @param purpose why the step exists, for a reader who does not already know
 */
public record StepDeclaration(String stepId, String name, String purpose) {

    public static StepDeclaration of(String stepId, String name, String purpose) {
        return new StepDeclaration(stepId, name, purpose);
    }
}
