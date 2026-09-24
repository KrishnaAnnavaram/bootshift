package com.bootshift.tests.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.adapters.transform.OpenRewriteCoreProvider;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.ledger.ChangeLedger;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Json;
import com.bootshift.stages.EdgeSupport;
import com.bootshift.stages.RunFactory;
import com.bootshift.stages.StageContext;
import com.bootshift.stages.StageExecutor;
import com.bootshift.stages.bootstrap.RunBootstrap;
import com.bootshift.stages.stage01.InventoryStage;
import com.bootshift.stages.stage11.PlannerStage;
import com.bootshift.stages.stage12.TransformationStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Proves the OpenRewrite integration through the pipeline, not just through the provider.
 *
 * <p>{@link TransformerTest} already proves that {@link OpenRewriteCoreProvider} runs a real recipe
 * when called directly. That is a necessary proof and not a sufficient one: a provider that works in
 * isolation is still dead code if the planner never schedules its recipes, if the transformation
 * stage's provider lookup never resolves to it, or if the mutation gateway rejects what it proposes.
 * Every one of those is a silent failure - the stage records NO_PROVIDER or zero applied changes and
 * still reports SUCCESS.
 *
 * <p>This test therefore drives the production chain end to end on a fixture module:
 *
 * <pre>
 *   PlannerStage.scheduleFor(MAJOR_BOUNDARY)   -> schedules openrewrite.java.change-package
 *        -> frozen edge plan on disk
 *        -> StageExecutor.run(TransformationStage)
 *             -> provider lookup resolves OPENREWRITE_CORE
 *             -> OpenRewriteCoreProvider.apply -> Recipe.run over a parsed LST
 *             -> FileMutationGateway writes the file
 *             -> ChangeLedger records the change with OpenRewrite provenance
 * </pre>
 *
 * <p>The assertions below are about that chain. The semantics of the rewrite itself (comments and
 * string literals untouched, {@code javax.sql} preserved) are asserted here as well, because a chain
 * that runs the wrong transformation correctly is not worth more than a broken one.
 */
class OpenRewritePipelineIntegrationTest {

    /** The repository root, located from the working directory the surefire fork runs in. */
    private static Path harnessRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.isDirectory(candidate.resolve("schemas"))) {
            candidate = candidate.getParent();
        }
        return candidate == null ? Path.of("").toAbsolutePath() : candidate;
    }

    @Test
    @DisplayName("the planner schedules OpenRewrite and the transformation stage actually runs it")
    void openRewriteRunsThroughTheTransformationStage(@TempDir Path root) throws IOException {
        OpenRewriteCoreProvider provider = new OpenRewriteCoreProvider();
        assumeTrue(provider.coreAvailable() && provider.javaModuleAvailable(),
                "OpenRewrite java module is not on the classpath");

        Path repository = root.resolve("app");
        writeFixture(repository);

        StageContext context = RunFactory.create(new RunFactory.Options(
                repository, root.resolve("workspaces"), root.resolve("output"), harnessRoot(),
                // Null run id: the harness allocates a ULID. The artifact schemas require that
                // shape, so a readable literal here fails stage 01 rather than this test's subject.
                "production", null, false, "managed", false, null, Map.of()));

        // Bootstrap and inventory are run for real: they create the migration workspace and allocate
        // the permanent file identities the mutation gateway authorizes changes against.
        StageResult bootstrap = new RunBootstrap(context).execute();
        assertThat(bootstrap.succeeded()).as("bootstrap: %s", bootstrap.summary()).isTrue();
        StageResult inventory = StageExecutor.run(new InventoryStage(), context);
        assertThat(inventory.succeeded()).as("inventory: %s", inventory.summary()).isTrue();

        // The analysis stages between inventory and the plan need Maven Central and a full build, so
        // they are replaced here by the three artifacts the transformation stage declares as inputs.
        // The edge plan is not hand-written: it is built from PlannerStage.scheduleFor, which is the
        // production scheduling decision under test.
        String edgeId = "EDGE-OR-MAJOR";
        List<PlannerStage.ScheduledRecipe> schedule =
                PlannerStage.scheduleFor("MAJOR_BOUNDARY", Json.obj(), true);
        assertThat(schedule)
                .as("the planner must schedule OpenRewrite for a major boundary when it is available")
                .anyMatch(r -> OpenRewriteCoreProvider.RECIPE_CHANGE_PACKAGE.equals(r.recipeId()));

        seedGraphRegistry(context);
        seedTarget(context);
        seedEdgePlan(context, edgeId, schedule);

        // The run is restored to the state a completed analysis would have left it in: plan frozen,
        // baseline sealed. Without the seal the stage refuses to mutate at all (R7), which is the
        // behaviour MutationBoundaryTest covers.
        context.stateMachine().restore(RunState.PLAN_FROZEN, true, "0".repeat(64));

        StageResult result = StageExecutor.run(new TransformationStage(edgeId), context);

        assertThat(result.succeeded())
                .as("transformation stage failed: %s", result.summary())
                .isTrue();

        // ---- the stage resolved the OpenRewrite provider and applied its changes ----------------
        JsonNode report = context.run().output().readLatest("12-transformation",
                "transformation-report.json");
        assertThat(report).isNotNull();

        List<JsonNode> openRewriteRecipes = new ArrayList<>();
        for (JsonNode recipe : report.path("recipes")) {
            if (recipe.path("recipe_id").asText().startsWith("openrewrite.")) {
                openRewriteRecipes.add(recipe);
            }
        }
        assertThat(openRewriteRecipes)
                .as("the frozen plan scheduled OpenRewrite recipes but the stage recorded none")
                .isNotEmpty();
        assertThat(openRewriteRecipes)
                .as("a scheduled recipe with no provider is silently never applied")
                .allSatisfy(recipe -> assertThat(recipe.path("provider").asText())
                        .isEqualTo(OpenRewriteCoreProvider.PROVIDER));

        int appliedByOpenRewrite = openRewriteRecipes.stream()
                .mapToInt(recipe -> recipe.path("applied").asInt()).sum();
        assertThat(appliedByOpenRewrite)
                .as("OpenRewrite proposed changes that the gateway never wrote")
                .isGreaterThan(0);

        // ---- the workspace file was really rewritten, and rewritten semantically ----------------
        Path migrated = context.run().migrationWorkspace()
                .resolve("src/main/java/com/example/Order.java");
        String rewritten = Files.readString(migrated);

        assertThat(rewritten).contains("import jakarta.persistence.Entity;");
        assertThat(rewritten).doesNotContain("import javax.persistence.Entity;");
        // javax.sql never relocated. A textual rewrite moves it and the module stops compiling for a
        // reason that has nothing to do with the migration.
        assertThat(rewritten).contains("import javax.sql.DataSource;");
        assertThat(rewritten).contains("comment mentions javax.persistence");
        assertThat(rewritten).contains("\"javax.persistence stays inside this literal\"");

        // The original repository is never touched; only the migration workspace is.
        assertThat(Files.readString(repository.resolve("src/main/java/com/example/Order.java")))
                .contains("import javax.persistence.Entity;");

        // ---- the change is in the tamper-evident ledger with OpenRewrite provenance -------------
        ChangeLedger.Verification verification = EdgeSupport.verifyLedger(context);
        assertThat(verification.valid()).isTrue();

        List<ChangeLedger.Entry> openRewriteEntries = EdgeSupport.openLedger(context).entries().stream()
                .filter(e -> e.event().getProvider() != null
                        && OpenRewriteCoreProvider.PROVIDER.equals(e.event().getProvider().name()))
                .toList();
        assertThat(openRewriteEntries)
                .as("a change made by OpenRewrite must be attributable to OpenRewrite in the ledger")
                .isNotEmpty();
        assertThat(openRewriteEntries)
                .allSatisfy(entry -> {
                    assertThat(entry.event().getProvider().version()).isNotBlank();
                    assertThat(entry.event().getRecipeId())
                            .isEqualTo(OpenRewriteCoreProvider.RECIPE_CHANGE_PACKAGE);
                    assertThat(entry.event().getBeforeSha256()).isNotBlank();
                    assertThat(entry.event().getAfterSha256()).isNotBlank();
                    assertThat(entry.event().getBeforeSha256())
                            .isNotEqualTo(entry.event().getAfterSha256());
                });

        // ---- the proposal record names the engine, so the diff is explainable ------------------
        JsonNode proposals = context.run().output().readLatest("12-transformation",
                "proposed-changes.json");
        assertThat(proposals).isNotNull();
        boolean engineRecorded = false;
        for (JsonNode proposal : proposals.path("proposals")) {
            if (proposal.path("recipe_id").asText().startsWith("openrewrite.")) {
                engineRecorded = true;
                assertThat(proposal.path("attributes").path("engine").asText())
                        .isEqualTo("openrewrite");
                assertThat(proposal.path("attributes").path("engine_version").asText())
                        .isNotBlank().isNotEqualTo("unknown");
            }
        }
        assertThat(engineRecorded)
                .as("the published proposal record must name the engine that produced the change")
                .isTrue();
    }

    // ------------------------------------------------------------------ fixture and seeded inputs

    /** A one-module Maven application on the pre-Jakarta namespace. */
    private static void writeFixture(Path repository) throws IOException {
        Path source = repository.resolve("src/main/java/com/example/Order.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package com.example;

                import javax.persistence.Entity;
                import javax.persistence.Id;
                import javax.sql.DataSource;

                // This comment mentions javax.persistence and must survive the rewrite.
                @Entity
                public class Order {

                    @Id
                    Long id;

                    String note = "javax.persistence stays inside this literal";

                    DataSource dataSource;
                }
                """);
        Files.writeString(repository.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.7.18</version>
                    <relativePath/>
                  </parent>
                  <groupId>com.example</groupId>
                  <artifactId>order-service</artifactId>
                  <version>1.0.0</version>
                  <properties>
                    <java.version>11</java.version>
                  </properties>
                </project>
                """);
    }

    /**
     * The file registry under the name the transformation stage declares as its input.
     *
     * <p>Agent 03 republishes Agent 01's registry after attaching symbols. The symbol attachment is
     * not what this test is about, so the inventory registry is republished verbatim.
     */
    private static void seedGraphRegistry(StageContext context) {
        JsonNode registry = context.run().output().readLatest("01-inventory", "file-registry.json");
        assertThat(registry).as("inventory published no file registry").isNotNull();
        publish(context, "03-graph", "file-registry.json", registry);
    }

    /** The frozen target state, in the shape Agent 06 publishes it. */
    private static void seedTarget(StageContext context) {
        ObjectNode target = Json.obj();
        target.put("pipeline_stage", "06-target");
        target.put("schema_version", "1.0.0");
        target.put("landing_target", "3.0.13");
        target.put("source_state", "2.7.18");
        publish(context, "06-target", "target-state.json", target);
    }

    /** Writes an artifact and advances the stage pointer, as a real stage would. */
    private static void publish(StageContext context, String stageDir, String name,
                                JsonNode payload) {
        OutputLayout.StageWriter writer = context.run().output().open(stageDir);
        writer.write(name, payload);
        writer.publish(context.run().runId());
    }

    /**
     * The frozen edge plan, built from the production scheduling decision.
     *
     * <p>Only the Java recipes are scheduled here. The Maven recipes in the same schedule need the
     * Spring Cloud train and effective build model that the skipped analysis stages resolve, and
     * their absence is what this test is not about.
     */
    private static void seedEdgePlan(StageContext context, String edgeId,
                                     List<PlannerStage.ScheduledRecipe> schedule) {
        ArrayNode transformations = Json.arr();
        for (PlannerStage.ScheduledRecipe scheduled : schedule) {
            if (!scheduled.recipeId().startsWith("openrewrite.")) {
                continue;
            }
            ObjectNode node = Json.obj();
            node.put("recipe_id", scheduled.recipeId());
            node.put("why", scheduled.why());
            ObjectNode parameters = Json.obj();
            scheduled.parameters().forEach(parameters::put);
            node.set("parameters", parameters);
            transformations.add(node);
        }

        ObjectNode edge = Json.obj();
        edge.put("edge_id", edgeId);
        edge.put("edge_class", "MAJOR_BOUNDARY");
        edge.put("source_state", "2.7.18");
        edge.put("target_state", "3.0.13");
        edge.set("ordered_transformations", transformations);
        edge.set("knowledge_refs", Json.toTree(List.of("MF-JAKARTA-RELOCATION")));
        edge.set("impact_refs", Json.toTree(List.of("IMP-ORDER-JAVAX")));

        // Authorize every active file. The gateway's own scoping rules are covered by
        // MutationHardeningTest; this test is about whether OpenRewrite's output reaches disk.
        ArrayNode authorized = Json.arr();
        EdgeSupport.loadRegistry(context).active()
                .forEach(record -> authorized.add(record.getFileId()));
        edge.set("affected_file_ids", authorized);

        ObjectNode plan = Json.obj();
        plan.put("pipeline_stage", "11-plan");
        plan.put("schema_version", "1.0.0");
        ArrayNode edges = Json.arr();
        edges.add(edge);
        plan.set("edges", edges);
        publish(context, "11-plan", "edge-plan.json", plan);
    }
}
