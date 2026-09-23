package com.bootshift.tests.journal;

import com.bootshift.core.journal.RunJournal;
import com.bootshift.core.journal.StepDeclaration;
import com.bootshift.stages.PipelineOrchestrator;
import com.bootshift.stages.Stage;
import com.bootshift.stages.bootstrap.RunBootstrap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the documentation architecture honest about itself.
 *
 * <p>This repository has already shipped documentation describing commands that did not exist and
 * code that was implemented and never invoked. Both are invisible failures: nothing throws, nothing
 * fails, and the artifact looks right. The defence is to derive the contract from the code and
 * assert the documents against it, rather than to maintain a second list that drifts at its own
 * pace.
 */
class DocumentationContractTest {

    private static final Path ROOT = Path.of("").toAbsolutePath().getParent();

    private static List<Stage> catalog() {
        return PipelineOrchestrator.catalog();
    }

    @Test
    @DisplayName("every stage in the catalog declares a step plan")
    void everyStageDeclaresSteps() {
        List<String> withoutPlan = new ArrayList<>();
        for (Stage stage : catalog()) {
            if (stage.declaredSteps().isEmpty()) {
                withoutPlan.add(stage.id());
            }
        }
        assertThat(withoutPlan)
                .as("a stage with no declared steps cannot report a step it failed to run")
                .isEmpty();
    }

    @Test
    @DisplayName("step ids are unique within a stage and carry the stage's own prefix shape")
    void stepIdsAreWellFormed() {
        List<String> problems = new ArrayList<>();
        for (Stage stage : catalog()) {
            Set<String> seen = new LinkedHashSet<>();
            for (StepDeclaration step : stage.declaredSteps()) {
                if (!seen.add(step.stepId())) {
                    problems.add(stage.id() + " declares " + step.stepId() + " twice");
                }
                if (!step.stepId().matches("[A-Z]{3,4}-\\d{3}")) {
                    problems.add(stage.id() + " step id " + step.stepId()
                            + " is not of the form ABC-001");
                }
                if (step.name() == null || step.name().isBlank()) {
                    problems.add(stage.id() + " step " + step.stepId() + " has no name");
                }
                if (step.purpose() == null || step.purpose().isBlank()) {
                    problems.add(stage.id() + " step " + step.stepId() + " has no purpose");
                }
            }
        }
        assertThat(problems).isEmpty();
    }

    @Test
    @DisplayName("bootstrap declares a step plan even though it is not a Stage")
    void bootstrapDeclaresSteps() {
        // Bootstrap is explicitly not an agent, but it is the first thing that can fail, and a run
        // whose bootstrap failed needs a document more than most.
        assertThat(RunBootstrap.STEPS).isNotEmpty();
        assertThat(RunBootstrap.PURPOSE).isNotBlank();
        RunBootstrap.STEPS.forEach(step ->
                assertThat(step.stepId()).matches("BOOT-\\d{3}"));
    }

    @Test
    @DisplayName("stage ids are unique and every stage names an output directory")
    void stageIdentityIsSound() {
        Set<String> ids = new LinkedHashSet<>();
        List<String> problems = new ArrayList<>();
        for (Stage stage : catalog()) {
            if (!ids.add(stage.id())) {
                problems.add("duplicate stage id " + stage.id());
            }
            if (stage.outputDirectory() == null || stage.outputDirectory().isBlank()) {
                problems.add(stage.id() + " has no output directory");
            }
            if (stage.purpose() == null || stage.purpose().isBlank()) {
                problems.add(stage.id() + " has no purpose");
            }
        }
        assertThat(problems).isEmpty();
        assertThat(ids).hasSize(20);
    }

    @Test
    @DisplayName("the six edge stages report an edge id, and the analysis half does not")
    void edgeAttributionIsDeclared() {
        List<String> edgeStages = new ArrayList<>();
        List<String> analysisStagesWithEdge = new ArrayList<>();
        for (Stage stage : PipelineOrchestrator.catalog()) {
            if (stage.edgeId() != null) {
                edgeStages.add(stage.id());
            }
        }
        // catalog() builds edge stages with a placeholder, which is what makes them identifiable
        // here without standing up a run.
        assertThat(edgeStages).containsExactly("12-transformation", "13-build-repair",
                "14-graph-diff", "15-test", "16-runtime", "17-differential");
        assertThat(analysisStagesWithEdge).isEmpty();
    }

    @Test
    @DisplayName("every stage in the catalog appears in the README's stage reference")
    void readmeCoversEveryStage() throws IOException {
        String readme = Files.readString(ROOT.resolve("README.md"), StandardCharsets.UTF_8);
        List<String> missing = new ArrayList<>();
        for (Stage stage : catalog()) {
            if (!readme.contains(stage.id())) {
                missing.add(stage.id());
            }
        }
        assertThat(missing).as("stages absent from README.md").isEmpty();
    }

    @Test
    @DisplayName("the README documents the four documentation levels by their real filenames")
    void readmeDocumentsTheDocumentationLevels() throws IOException {
        String readme = Files.readString(ROOT.resolve("README.md"), StandardCharsets.UTF_8);
        // The filenames are constants in the journal; documenting a different name would send a
        // reader looking for a file that is not there.
        assertThat(readme).contains(RunJournal.STAGE_DOCUMENT_FILE);
        assertThat(readme).contains(RunJournal.EDGE_DOCUMENT_FILE);
        assertThat(readme).contains(RunJournal.RUN_DOCUMENT_FILE);
        assertThat(readme).contains("MIGRATION_DOCUMENT.md");
        assertThat(readme).contains(RunJournal.STAGE_EXECUTION_FILE);
        assertThat(readme).contains(RunJournal.TIMELINE_FILE);
    }

    @Test
    @DisplayName("the journal artifacts each have a schema")
    void journalArtifactsHaveSchemas() {
        Path schemas = ROOT.resolve("schemas/journal");
        assertThat(schemas.resolve("stage-execution.schema.json")).exists();
        assertThat(schemas.resolve("run-timeline.schema.json")).exists();
        assertThat(schemas.resolve("edge-execution.schema.json")).exists();
    }

    @Test
    @DisplayName("stage contracts exist in a vendor-neutral location for every stage")
    void stageContractsAreVendorNeutral() throws IOException {
        Path contracts = ROOT.resolve("docs/pipeline/contracts");
        assertThat(contracts).as("canonical stage contracts directory").exists();
        List<String> missing = new ArrayList<>();
        for (Stage stage : catalog()) {
            Path contract = contracts.resolve(stage.id() + ".md");
            if (!Files.isRegularFile(contract)) {
                missing.add(stage.id() + ".md");
            }
        }
        assertThat(missing).as("stage contracts absent from docs/pipeline/contracts").isEmpty();
    }
}
