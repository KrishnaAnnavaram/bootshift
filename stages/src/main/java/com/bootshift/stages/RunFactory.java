package com.bootshift.stages;

import com.fasterxml.jackson.databind.JsonNode;
import com.bootshift.adapters.ai.LocalOssAIProvider;
import com.bootshift.adapters.environment.DelegatedEnvironmentProvider;
import com.bootshift.adapters.environment.ManagedEnvironmentProvider;
import com.bootshift.core.domain.HarnessException;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.RunContext;
import com.bootshift.core.policy.HarnessPolicy;
import com.bootshift.core.policy.LicensePolicy;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Ids;
import com.bootshift.core.util.Json;
import com.bootshift.ports.ai.AIProvider;
import com.bootshift.ports.environment.EnvironmentProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Builds a {@link RunContext} and {@link StageContext} from CLI options.
 *
 * <p>Also handles run continuation: when a run id already exists, the state machine is reconstructed
 * from validated artifacts rather than from a stored cursor (R23).
 */
public final class RunFactory {

    /** Everything the CLI can influence about a run. */
    public record Options(Path repository, Path workspaceRoot, Path outputRoot, Path harnessRoot,
                          String policyName, Path policyFile, boolean aiEnabled, String environmentMode,
                          boolean networkEnabled, String runId, Map<String, String> environmentAttributes) {
    }

    private RunFactory() {
    }

    public static StageContext create(Options options) {
        Path repository = options.repository().toAbsolutePath().normalize();
        if (!Files.isDirectory(repository)) {
            throw HarnessException.refusal("Repository path does not exist: " + repository);
        }

        HarnessPolicy policy = options.policyFile() != null
                ? HarnessPolicy.load(options.policyFile())
                : ("development".equalsIgnoreCase(options.policyName())
                ? HarnessPolicy.development() : HarnessPolicy.production());
        Path licenseFile = options.harnessRoot().resolve("policies/license/license-policy.json");
        if (Files.isRegularFile(licenseFile)) {
            policy.license(LicensePolicy.load(licenseFile));
        }

        Path workspaceRoot = options.workspaceRoot().toAbsolutePath().normalize();
        Path outputRoot = options.outputRoot().toAbsolutePath().normalize();
        String runId = options.runId() != null ? options.runId() : resolveRunId(outputRoot, workspaceRoot);

        RunContext run = new RunContext(runId, repository, workspaceRoot,
                new OutputLayout(outputRoot), policy, options.aiEnabled(), options.environmentMode());

        AIProvider ai = options.aiEnabled()
                ? new LocalOssAIProvider(true,
                System.getenv().getOrDefault("BOOTSHIFT_AI_ENDPOINT", "http://127.0.0.1:11434"),
                System.getenv().getOrDefault("BOOTSHIFT_AI_RUNTIME", "ollama"),
                System.getenv().getOrDefault("BOOTSHIFT_AI_MODEL", "qwen2.5-coder"),
                System.getenv().getOrDefault("BOOTSHIFT_AI_MODEL_LICENSE", "Apache-2.0"),
                policy.license())
                : LocalOssAIProvider.disabled();

        EnvironmentProvider environment = "delegated".equalsIgnoreCase(options.environmentMode())
                ? new DelegatedEnvironmentProvider(options.environmentAttributes(), Map.of())
                : new ManagedEnvironmentProvider();

        StageContext context = StageContext.localDefault(run, options.harnessRoot(), ai, environment,
                options.networkEnabled());
        restoreState(context);
        return context;
    }

    /**
     * Reconstructs run state from the artifact plane. If a stage published successfully its
     * postcondition is considered reached, regardless of what any state file claims.
     */
    public static void restoreState(StageContext context) {
        OutputLayout output = context.run().output();
        RunState reached = RunState.CREATED;
        if (output.resolveLatestDir("00-bootstrap") != null) {
            reached = RunState.OSS_POLICY_VERIFIED;
        }
        if (output.resolveLatestDir("01-inventory") != null) {
            reached = RunState.FILE_REGISTRY_SEALED;
        }
        if (output.resolveLatestDir("02-build") != null) {
            reached = RunState.BUILD_RESOLVED;
        }
        if (output.resolveLatestDir("03-graph") != null) {
            JsonNode verification = output.readLatest("03-graph", "graph-verification-report.json");
            reached = verification != null && verification.path("passed").asBoolean(false)
                    ? RunState.GRAPH_VERIFIED : RunState.APPLICATION_GRAPH_BUILT;
        }
        boolean sealed = false;
        String sealHash = null;
        JsonNode baseline = output.readLatest("04-baseline", "baseline-manifest.json");
        if (baseline != null) {
            sealed = baseline.path("sealed").asBoolean(false);
            sealHash = baseline.path("baseline_manifest_hash").asText(null);
            reached = sealed ? RunState.BASELINE_SEALED : RunState.BASELINE_CAPTURED;
        }
        if (output.resolveLatestDir("05-compatibility") != null) {
            reached = RunState.COMPATIBILITY_REGISTRY_READY;
        }
        if (output.resolveLatestDir("06-target") != null) {
            reached = RunState.TARGET_FROZEN;
        }
        if (output.resolveLatestDir("07-documentation") != null) {
            reached = RunState.DOCUMENTATION_RETRIEVED;
        }
        if (output.resolveLatestDir("08-knowledge") != null) {
            reached = RunState.KNOWLEDGE_VERIFIED;
        }
        if (output.resolveLatestDir("09-impact") != null) {
            reached = RunState.IMPACT_ANALYZED;
        }
        if (output.resolveLatestDir("10-characterization") != null) {
            reached = RunState.CHARACTERIZATION_COMPLETE;
        }
        if (output.resolveLatestDir("11-plan") != null) {
            reached = RunState.PLAN_FROZEN;
        }
        context.stateMachine().restore(reached, sealed, sealHash);
        if (sealed) {
            context.stateMachine().recordBaselineSeal(sealHash);
        }
    }

    /** Reuses the most recent run id in the output tree so stages can be invoked one at a time. */
    private static String resolveRunId(Path outputRoot, Path workspaceRoot) {
        Path marker = outputRoot.resolve("current-run.json");
        if (Files.isRegularFile(marker)) {
            JsonNode node = Json.read(marker);
            String existing = node.path("run_id").asText(null);
            if (existing != null && Files.isDirectory(workspaceRoot.resolve(existing))) {
                return existing;
            }
        }
        String runId = Ids.runId();
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("run_id", runId);
        payload.put("created_at", java.time.Instant.now().toString());
        try {
            Files.createDirectories(outputRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create output root " + outputRoot, e);
        }
        Json.write(marker, payload);
        return runId;
    }

    /** Starts a brand new run identity, which is what R30 requires for a new baseline. */
    public static String newRunId(Path outputRoot) {
        String runId = Ids.runId();
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("run_id", runId);
        payload.put("created_at", java.time.Instant.now().toString());
        Json.write(outputRoot.resolve("current-run.json"), payload);
        return runId;
    }

    public static Optional<String> currentRunId(Path outputRoot) {
        Path marker = outputRoot.resolve("current-run.json");
        if (!Files.isRegularFile(marker)) {
            return Optional.empty();
        }
        return Optional.ofNullable(Json.read(marker).path("run_id").asText(null));
    }
}
