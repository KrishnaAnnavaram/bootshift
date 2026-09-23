package com.bootshift.stages;

import com.bootshift.core.journal.StepDeclaration;
import com.bootshift.stages.bootstrap.RunBootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the canonical stage contracts from the stage catalog.
 *
 * <p>Design-time documentation - what a stage is <em>supposed</em> to do - as opposed to the runtime
 * documentation the execution journal produces, which is what a stage <em>actually did</em> on a
 * given run. Both are needed and neither substitutes for the other.
 *
 * <p>Generated rather than hand-written, and generated from the {@link Stage} interface itself. A
 * hand-maintained contract file is a second description of the pipeline that drifts at its own pace;
 * this repository has already shipped documentation for a CLI command that was never implemented,
 * which is the same failure one level up. Deriving the contract from the code means it cannot
 * describe a stage that does not exist, cannot omit one that does, and cannot disagree with the
 * declared preconditions, artifacts or steps.
 */
public final class StageContractRenderer {

    /** Where the vendor-neutral contracts live. */
    public static final String CONTRACTS_DIRECTORY = "docs/pipeline/contracts";

    private StageContractRenderer() {
    }

    /** Writes one contract per stage into {@code directory}, returning the files written. */
    public static List<Path> writeAll(Path directory) throws IOException {
        Files.createDirectories(directory);
        List<Path> written = new ArrayList<>();

        // Bootstrap first. It is explicitly not an agent and does not implement Stage, but it is the
        // first thing that runs and the first that can fail, so a reader looking for "what happens
        // at 00" has to find it here rather than conclude nothing happens.
        Path bootstrap = directory.resolve(RunBootstrap.OUTPUT_DIR + ".md");
        Files.writeString(bootstrap, renderBootstrap(), StandardCharsets.UTF_8);
        written.add(bootstrap);

        for (Stage stage : PipelineOrchestrator.catalog()) {
            Path file = directory.resolve(stage.id() + ".md");
            Files.writeString(file, render(stage), StandardCharsets.UTF_8);
            written.add(file);
        }
        return written;
    }

    static String renderBootstrap() {
        return body(RunBootstrap.OUTPUT_DIR, "RunBootstrap", RunBootstrap.PURPOSE,
                RunBootstrap.OUTPUT_DIR, false, false, null, List.of(),
                List.of("bootstrap.json", "oss-license-gate.json", "manifest.json"),
                List.of(), "WORKSPACE_READY, OSS_POLICY_VERIFIED", RunBootstrap.STEPS,
                "Infrastructure preflight, explicitly NOT an agent. It may not perform migration "
                        + "analysis or make any migration decision. Inventory remains the first "
                        + "analysis stage.");
    }

    static String render(Stage stage) {
        return body(stage.id(), stage.getClass().getSimpleName(), stage.purpose(),
                stage.outputDirectory(), stage.mutating(), stage.aiAssisted(), stage.edgeId(),
                stage.inputArtifacts(), stage.outputArtifacts(),
                stage.preconditions().stream().map(Enum::name).toList(),
                stage.postcondition().name(), stage.declaredSteps(), null);
    }

    private static String body(String id, String className, String purpose, String outputDirectory,
                               boolean mutating, boolean aiAssisted, String edgeId,
                               List<String> inputs, List<String> outputs, List<String> preconditions,
                               String postcondition, List<StepDeclaration> steps, String note) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Stage ").append(id).append("\n\n");
        sb.append("> Generated from the stage contract in code by `bootshift stages "
                + "--write-contracts`.\n> Do not edit by hand; edit the stage and regenerate.\n\n");

        sb.append("| | |\n|  --- |  --- |\n");
        sb.append("| Implementation | `").append(className).append("` |\n");
        sb.append("| Output directory | `").append(outputDirectory).append("` |\n");
        sb.append("| Edge-scoped | ").append(edgeId != null ? "yes" : "no").append(" |\n");
        sb.append("| May write to application source | ").append(mutating ? "**yes**" : "no")
                .append(" |\n");
        sb.append("| May consult the optional AI provider | ")
                .append(aiAssisted ? "yes, and never as an authority" : "no").append(" |\n");
        sb.append("| Postcondition | `").append(postcondition).append("` |\n\n");

        sb.append("## Purpose\n\n").append(purpose).append("\n\n");
        if (note != null) {
            sb.append("> ").append(note).append("\n\n");
        }

        sb.append("## Preconditions\n\n");
        if (preconditions.isEmpty()) {
            sb.append("None declared.\n\n");
        } else {
            preconditions.forEach(p -> sb.append("- `").append(p).append("`\n"));
            sb.append("\n");
        }

        sb.append("## Input artifacts\n\n");
        if (inputs.isEmpty()) {
            sb.append("This stage consumes no published artifacts.\n\n");
        } else {
            inputs.forEach(a -> sb.append("- `").append(a).append("`\n"));
            sb.append("\n");
        }

        sb.append("## Output artifacts\n\n");
        outputs.forEach(a -> sb.append("- `").append(a).append("`\n"));
        sb.append("\n");

        sb.append("## Declared steps\n\n");
        if (steps.isEmpty()) {
            sb.append("This stage declares no step plan.\n\n");
        } else {
            sb.append("Declared before the stage runs, so a step that never executes is reported "
                    + "rather than absent.\n\n");
            sb.append("| Step | Name | Purpose |\n|  --- |  --- |  --- |\n");
            steps.forEach(s -> sb.append("| `").append(s.stepId()).append("` | ")
                    .append(s.name()).append(" | ").append(s.purpose()).append(" |\n"));
            sb.append("\n");
        }

        sb.append("## Runtime documentation\n\n");
        sb.append("Every attempt at this stage writes `stage-execution.json` and "
                        + "`STAGE_DOCUMENT.md` into its own attempt directory under `output/")
                .append(outputDirectory)
                .append("/`, including attempts that refuse, fail or crash.\n");
        return sb.toString();
    }
}
