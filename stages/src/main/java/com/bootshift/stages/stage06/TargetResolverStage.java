package com.bootshift.stages.stage06;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.domain.Envelope;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.HarnessException;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.evidence.EvidenceManifest;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Json;
import com.bootshift.stages.Stage;
import com.bootshift.stages.StageContext;
import com.bootshift.stages.StageSupport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 06 - Target Resolver (spec section 17).
 *
 * <p>Decides <em>where</em> the application should go, never how to get there. The distinction that
 * matters is transit checkpoint versus landing target: 3.0 may be a legal step even though it is an
 * unacceptable place to stop.
 *
 * <p>{@code --target auto} means the highest <em>safe supported</em> stable GA state (R25). Ranking
 * is by supportability and compatibility evidence, not by version number, and every eliminated
 * candidate keeps its elimination reason so the decision is auditable.
 */
public final class TargetResolverStage implements Stage {

    public static final String OUTPUT_DIR = "06-target";

    /**
     * One evaluated candidate landing state.
     *
     * <p>{@code eliminations} disqualify a candidate outright. {@code cautions} do not: they are
     * things the harness believes but cannot prove to the standard required to reject a target, and
     * each one becomes an approval gate instead of a silent rejection.
     */
    public record Candidate(String line, String version, boolean stableGa, boolean eol,
                            long supportHorizonMonths, boolean artifactAvailable,
                            String springFrameworkLine, String springCloudTrain,
                            List<Integer> supportedJavaMajors, String lifecycleEvidenceQuality,
                            List<String> eliminations, List<String> cautions, double score) {

        public boolean viable() {
            return eliminations.isEmpty();
        }
    }

    private final String requestedTarget;

    public TargetResolverStage() {
        this("auto");
    }

    public TargetResolverStage(String requestedTarget) {
        this.requestedTarget = requestedTarget == null ? "auto" : requestedTarget;
    }

    @Override
    public String id() {
        return OUTPUT_DIR;
    }

    @Override
    public String outputDirectory() {
        return OUTPUT_DIR;
    }

    @Override
    public String purpose() {
        return "Choose the landing target and the transit checkpoints that reach it";
    }

    @Override
    public List<RunState> preconditions() {
        return List.of(RunState.COMPATIBILITY_REGISTRY_READY);
    }

    @Override
    public RunState postcondition() {
        return RunState.TARGET_FROZEN;
    }

    @Override
    public List<String> inputArtifacts() {
        return List.of("05-compatibility/lifecycle-registry.json",
                "05-compatibility/compatibility-registry.json",
                "05-compatibility/artifact-availability.json",
                "05-compatibility/internal-components.json");
    }

    @Override
    public List<String> outputArtifacts() {
        return List.of("target-state.json", "migration-path.json", "target-resolution-report.json",
                "manifest.json");
    }

    @Override
    public StageResult execute(StageContext context) {
        JsonNode lifecycle = StageSupport.requireUpstream(context, "05-compatibility",
                "lifecycle-registry.json", "Run: harness compatibility");
        JsonNode compatibility = StageSupport.requireUpstream(context, "05-compatibility",
                "compatibility-registry.json", "Run: harness compatibility");
        JsonNode availability = StageSupport.requireUpstream(context, "05-compatibility",
                "artifact-availability.json", "Run: harness compatibility");
        JsonNode internal = StageSupport.requireUpstream(context, "05-compatibility",
                "internal-components.json", "Run: harness compatibility");

        String currentVersion = compatibility.path("current_spring_boot").asText(null);
        String currentJava = compatibility.path("current_java").asText("17");
        int runningJdk = compatibility.path("running_jdk_major").asInt(Runtime.version().feature());
        if (currentVersion == null || currentVersion.isBlank()) {
            throw HarnessException.refusal(
                    "The current Spring Boot version could not be determined from the build model; "
                            + "target resolution cannot proceed without a source state.");
        }

        Map<String, Boolean> artifactAvailable = new LinkedHashMap<>();
        availability.path("candidates").forEach(node ->
                artifactAvailable.put(node.path("line").asText(), node.path("exists").asBoolean(false)));

        int unknownInternal = internal.path("unknown_count").asInt();
        String internalAction = internal.path("policy_action").asText("BLOCK");

        // Judge Java constraints against the JDKs actually installed, not only the one running the
        // harness: the harness JVM and the application toolchain are separate concerns.
        List<Integer> availableJdks = new ArrayList<>();
        compatibility.path("available_jdk_majors").forEach(n -> availableJdks.add(n.asInt()));
        if (availableJdks.isEmpty()) {
            availableJdks.add(runningJdk);
        }

        boolean usesSpringCloud = compatibility.path("application_uses_spring_cloud").asBoolean(false);

        List<Candidate> candidates = new ArrayList<>();
        for (JsonNode release : lifecycle.path("releases")) {
            candidates.add(evaluate(context, release, currentVersion, currentJava, availableJdks,
                    artifactAvailable, unknownInternal, internalAction, usesSpringCloud));
        }

        Candidate landing;
        String selectionMode;
        if ("auto".equalsIgnoreCase(requestedTarget)) {
            selectionMode = "AUTO_HIGHEST_SAFE_SUPPORTED";
            landing = candidates.stream()
                    .filter(Candidate::viable)
                    .max(java.util.Comparator.comparingDouble(Candidate::score))
                    .orElse(null);
        } else {
            selectionMode = "EXPLICIT";
            String requestedLine = lineOf(requestedTarget);
            landing = candidates.stream()
                    .filter(c -> c.line().equals(requestedLine) || c.version().equals(requestedTarget))
                    .findFirst()
                    .orElse(null);
            if (landing == null) {
                throw HarnessException.refusal("Requested target " + requestedTarget
                        + " is not a known Spring Boot release line. Known lines: "
                        + candidates.stream().map(Candidate::line).toList());
            }
            if (!landing.viable()) {
                throw HarnessException.block("Requested target " + requestedTarget
                        + " is not a safe landing state: " + landing.eliminations());
            }
        }

        OutputLayout.StageWriter writer = context.run().output().open(OUTPUT_DIR);
        Envelope envelope = StageSupport.envelope(context, OUTPUT_DIR)
                .stat("candidates_evaluated", candidates.size())
                .stat("selection_mode", selectionMode);

        if (landing == null) {
            ObjectNode report = renderReport(currentVersion, currentJava, candidates, null,
                    selectionMode, List.of());
            writer.write("target-resolution-report.json",
                    StageSupport.compose(envelope, report));
            StageSupport.publish(context, writer);
            throw HarnessException.block(buildNoViableTargetExplanation(context, candidates));
        }

        List<Checkpoint> path = buildPath(currentVersion, currentJava, runningJdk, landing, candidates);

        ObjectNode targetState = Json.obj();
        targetState.put("source_version", currentVersion);
        targetState.put("source_java", currentJava);
        targetState.put("landing_version", landing.version());
        targetState.put("landing_line", landing.line());
        targetState.put("landing_spring_framework_line", landing.springFrameworkLine());
        targetState.put("landing_spring_cloud_train", landing.springCloudTrain());
        targetState.put("landing_java", String.valueOf(recommendedJava(landing, runningJdk)));
        targetState.put("selection_mode", selectionMode);
        targetState.put("support_horizon_months", landing.supportHorizonMonths());
        targetState.put("lifecycle_evidence_quality", landing.lifecycleEvidenceQuality());
        targetState.set("cautions", Json.toTree(landing.cautions()));
        targetState.put("frozen", true);
        targetState.put("freeze_note", "Changing a frozen target requires a new run or an explicit "
                + "controlled replan.");
        ObjectNode targetArtifact = StageSupport.compose(envelope, targetState);
        StageSupport.validate(context, writer, "target/target-state.schema.json",
                "target-state.json", targetArtifact);
        writer.write("target-state.json", targetArtifact);

        ObjectNode migrationPath = Json.obj();
        migrationPath.put("edge_count", path.size());
        migrationPath.put("mandatory_checkpoints",
                path.stream().filter(Checkpoint::mandatory).count());
        migrationPath.set("edges", Json.toTree(path));
        writer.write("migration-path.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), migrationPath));

        ObjectNode report = renderReport(currentVersion, currentJava, candidates, landing,
                selectionMode, path);
        writer.write("target-resolution-report.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), report));

        StageSupport.toEvidence(context, "target-state", targetArtifact,
                EvidenceManifest.Classification.INTERNAL, "SEALED_EVIDENCE", OUTPUT_DIR);

        if (!writer.validationErrors().isEmpty()) {
            return StageResult.failure(OUTPUT_DIR, ExitCode.STAGE_FAILURE,
                    "Target artifacts failed schema validation", writer.validationErrors());
        }

        String hash = StageSupport.publish(context, writer);
        context.stateMachine().transition(RunState.TARGET_RESOLVED, "landing " + landing.version());
        context.stateMachine().transition(RunState.TARGET_FROZEN, "target frozen");
        context.runStateStore().updateState(context.run().runId(), RunState.TARGET_FROZEN,
                "target frozen at " + landing.version());
        context.runStateStore().putAttribute(context.run().runId(), "target_version", landing.version());

        Map<String, Path> artifacts = new LinkedHashMap<>();
        outputArtifacts().forEach(name -> artifacts.put(name, writer.dir().resolve(name)));

        List<String> messages = new ArrayList<>();
        candidates.stream().filter(c -> !c.viable())
                .forEach(c -> messages.add("eliminated " + c.line() + ": "
                        + String.join("; ", c.eliminations())));
        landing.cautions().forEach(caution ->
                messages.add("CAUTION on the selected target: " + caution
                        + " (raised as an approval gate rather than a silent elimination)"));

        return new StageResult(OUTPUT_DIR, ExitCode.SUCCESS,
                "Landing target Spring Boot " + landing.version() + " (Java "
                        + recommendedJava(landing, runningJdk) + ", Spring Cloud "
                        + landing.springCloudTrain() + "), " + path.size() + " migration edge(s), "
                        + landing.supportHorizonMonths() + " month support horizon",
                messages, artifacts, hash);
    }

    /**
     * Explains a no-viable-target outcome in terms an operator can act on.
     *
     * <p>The common real-world shape is a squeeze: the newest lines have no ecosystem support yet and
     * the lines that do have it have just gone end of life. Saying that plainly, and naming the
     * closest candidate plus the exact policy flag that would admit it, is more useful than a list of
     * rejections.
     */
    private String buildNoViableTargetExplanation(StageContext context, List<Candidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("No Spring Boot line satisfies the active policy as a landing target.\n");

        Candidate closest = candidates.stream()
                .filter(c -> c.artifactAvailable() && c.stableGa())
                .filter(c -> c.eliminations().stream()
                        .noneMatch(e -> e.contains("no GA release train")))
                .max(java.util.Comparator.comparingInt(c -> numericLine(c.line())))
                .orElse(null);

        for (Candidate candidate : candidates) {
            sb.append("  ").append(candidate.line()).append(" -> ")
                    .append(String.join("; ", candidate.eliminations())).append('\n');
        }
        if (closest != null) {
            sb.append("\nClosest supportable candidate: ").append(closest.version())
                    .append(" with Spring Cloud ").append(closest.springCloudTrain())
                    .append(".\n");
            sb.append("It was rejected because: ")
                    .append(String.join("; ", closest.eliminations())).append(".\n");
            sb.append("If that is an accepted business risk, re-run with a policy that sets ")
                    .append("allow_eol_landing_target=true, which records the exception explicitly ")
                    .append("rather than hiding it.\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ evaluation

    private Candidate evaluate(StageContext context, JsonNode release, String currentVersion,
                               String currentJava, List<Integer> availableJdks,
                               Map<String, Boolean> artifactAvailable, int unknownInternal,
                               String internalAction, boolean usesSpringCloud) {
        String line = release.path("line").asText();
        String version = release.path("latest_patch").asText(null);
        boolean stableGa = release.path("stable_ga").asBoolean(false);
        boolean eol = release.path("eol").asBoolean(false);
        boolean horizonKnown = !release.path("support_horizon_months").isNull()
                && release.path("support_horizon_months").isNumber();
        long horizon = horizonKnown ? release.path("support_horizon_months").asLong() : 0;
        String quality = release.path("lifecycle_evidence_quality").asText("UNKNOWN");
        boolean lifecycleVerified = "VERIFIED".equals(quality);
        boolean available = artifactAvailable.getOrDefault(line, false);
        List<Integer> javaMajors = new ArrayList<>();
        release.path("supported_java_majors").forEach(n -> javaMajors.add(n.asInt()));
        String cloudTrain = release.path("spring_cloud_train").asText(null);

        List<String> eliminations = new ArrayList<>();
        List<String> cautions = new ArrayList<>();

        // 1. only stable GA candidates may land
        if (!stableGa && !context.policy().allowMilestoneTargets()) {
            eliminations.add("not a stable GA release");
        }
        // 2. never move backwards
        if (version == null || compare(version, currentVersion) <= 0) {
            eliminations.add("not newer than the current state " + currentVersion);
        }
        // 3. artifacts must actually exist
        if (!available) {
            eliminations.add("required artifacts are not available in the configured repositories");
        }
        // 4. the Spring Cloud train must exist when the application uses Spring Cloud. A missing
        // train is a hard constraint for an application that depends on it: the required artifacts
        // simply do not exist for that Boot line yet (R25 step 4).
        if (cloudTrain == null || cloudTrain.isBlank()) {
            if (usesSpringCloud) {
                eliminations.add("the application uses Spring Cloud but no GA release train targets "
                        + "this Boot line, so the required artifacts do not exist");
            } else {
                cautions.add("no published Spring Cloud release train targets this Boot line");
            }
        }
        // 5. Java and toolchain constraints, judged against the JDKs actually installed
        int sourceJava = parseJava(currentJava);
        if (!javaMajors.isEmpty()) {
            boolean buildable = availableJdks.stream()
                    .anyMatch(jdk -> javaMajors.contains(jdk) && jdk >= sourceJava);
            if (!buildable) {
                if (availableJdks.stream().anyMatch(javaMajors::contains)) {
                    eliminations.add("no installed JDK satisfies both the target range " + javaMajors
                            + " and the project level " + currentJava);
                } else {
                    eliminations.add("no installed JDK is in the supported range " + javaMajors
                            + " (installed: " + availableJdks + ")");
                }
            }
        }
        // 6. lifecycle constraints, weighted by how well the lifecycle evidence is established
        if (lifecycleVerified) {
            if (eol && !context.policy().allowEolLandingTarget()) {
                eliminations.add("open-source support has ended");
            } else if (horizonKnown && horizon < context.policy().minimumSupportHorizonMonths()) {
                eliminations.add("support horizon of " + horizon
                        + " month(s) is below the policy minimum "
                        + context.policy().minimumSupportHorizonMonths());
            }
        } else {
            if (eol) {
                cautions.add("lifecycle evidence is " + quality + " and suggests support has ended; "
                        + "this cannot eliminate the target on its own and requires approval");
            } else if (horizonKnown && horizon < context.policy().minimumSupportHorizonMonths()) {
                cautions.add("lifecycle evidence is " + quality + " and suggests only " + horizon
                        + " month(s) of support remain");
            } else if (!horizonKnown) {
                cautions.add("no lifecycle evidence exists for this line; its support window is UNKNOWN");
            }
        }
        // 7. internal components with unknown compatibility (R28)
        if (unknownInternal > 0) {
            if ("BLOCK".equals(internalAction)) {
                eliminations.add(unknownInternal + " internal component(s) have UNKNOWN compatibility "
                        + "and policy action is BLOCK");
            } else {
                cautions.add(unknownInternal + " internal component(s) have UNKNOWN compatibility");
            }
        }

        // Ranking: supportability first, then compatibility quality. Version number is a tiebreak,
        // never the primary criterion (R25).
        double score = (horizonKnown ? horizon : -12) * 10.0
                + (stableGa ? 50 : 0)
                + (available ? 25 : 0)
                + (lifecycleVerified ? 30 : 0)
                + (cloudTrain != null ? 20 : 0)
                + availableJdks.stream().filter(javaMajors::contains).count() * 2
                - cautions.size() * 5
                + numericLine(line) * 0.1;

        return new Candidate(line, version, stableGa, eol, horizon, available,
                release.path("spring_framework_line").asText(null), cloudTrain, javaMajors,
                quality, eliminations, cautions, score);
    }

    /** One migration edge on the path from source to landing. */
    public record Checkpoint(String edgeId, String fromVersion, String toVersion, String edgeClass,
                             boolean landing, boolean mandatory, String rationale,
                             List<String> requiredPreparations) {
    }

    /**
     * Builds the checkpoint sequence.
     *
     * <p>The major boundary is never collapsed: 2.x to 3.x is modelled as its own edge because it
     * carries the Jakarta namespace change, the Spring Security 6 rewrite and the Java 17 baseline.
     * Test-infrastructure work is modelled as a PREPARATORY edge that runs first (spec section 22).
     */
    private List<Checkpoint> buildPath(String currentVersion, String currentJava, int runningJdk,
                                       Candidate landing, List<Candidate> candidates) {
        List<Checkpoint> path = new ArrayList<>();
        int index = 0;
        String from = currentVersion;
        String currentLine = lineOf(currentVersion);

        path.add(new Checkpoint("EDGE-" + (++index) + "-PREP-TEST", from, from, "PREPARATORY", false,
                true, "Migrate test infrastructure before framework changes so pass/fail/skip semantics "
                + "are proven to survive independently.",
                List.of("JUnit 4 to Jupiter where present", "assert pass/fail/skip parity")));

        // Land on the newest patch of the current line first: patch edges are cheap and remove
        // known defects before the boundary.
        Candidate sameLine = candidates.stream().filter(c -> c.line().equals(currentLine))
                .findFirst().orElse(null);
        if (sameLine != null && compare(sameLine.version(), from) > 0) {
            path.add(new Checkpoint("EDGE-" + (++index) + "-PATCH", from, sameLine.version(), "PATCH",
                    false, false, "Move to the latest patch of the current line before the boundary.",
                    List.of()));
            from = sameLine.version();
        }

        int fromMajor = majorOf(from);
        int toMajor = majorOf(landing.version());
        if (fromMajor < toMajor) {
            String boundaryTarget = candidates.stream()
                    .filter(c -> majorOf(c.version()) == toMajor)
                    .min(java.util.Comparator.comparingInt(c -> numericLine(c.line())))
                    .map(Candidate::version)
                    .orElse(landing.version());
            path.add(new Checkpoint("EDGE-" + (++index) + "-MAJOR", from, boundaryTarget,
                    "MAJOR_BOUNDARY", boundaryTarget.equals(landing.version()), true,
                    "Major boundary carrying the Jakarta EE namespace relocation, the Spring Security "
                            + "configuration model change and the Java 17 baseline. This checkpoint is "
                            + "mandatory and may not be collapsed silently (R26).",
                    List.of("javax to jakarta namespace", "Java " + Math.min(17, runningJdk) + " baseline",
                            "Spring Security 6 configuration model", "spring.factories to "
                            + "AutoConfiguration.imports")));
            from = boundaryTarget;
        }

        while (!from.equals(landing.version())) {
            String nextLine = nextLineAfter(lineOf(from), candidates, landing.line());
            if (nextLine == null) {
                break;
            }
            Candidate next = candidates.stream().filter(c -> c.line().equals(nextLine))
                    .findFirst().orElse(null);
            if (next == null || compare(next.version(), from) <= 0) {
                break;
            }
            boolean isLanding = next.line().equals(landing.line());
            path.add(new Checkpoint("EDGE-" + (++index) + "-MINOR", from, next.version(), "MINOR",
                    isLanding, false,
                    "Minor upgrade to " + next.line() + " carrying deprecations and default changes.",
                    List.of()));
            from = next.version();
            if (isLanding) {
                break;
            }
        }

        if (path.stream().noneMatch(Checkpoint::landing)) {
            path.add(new Checkpoint("EDGE-" + (++index) + "-LANDING", from, landing.version(),
                    "MINOR", true, false, "Final hop to the frozen landing target.", List.of()));
        }
        return path;
    }

    private ObjectNode renderReport(String currentVersion, String currentJava, List<Candidate> candidates,
                                    Candidate landing, String selectionMode, List<Checkpoint> path) {
        ObjectNode report = Json.obj();
        report.put("source_version", currentVersion);
        report.put("source_java", currentJava);
        report.put("selection_mode", selectionMode);
        report.put("rule", "auto means the highest safe supported stable GA target, not the "
                + "numerically newest (R25)");
        report.put("selected", landing == null ? null : landing.version());
        ArrayNode evaluated = Json.arr();
        candidates.stream()
                .sorted(java.util.Comparator.comparingDouble(Candidate::score).reversed())
                .forEach(c -> {
                    ObjectNode node = Json.obj();
                    node.put("line", c.line());
                    node.put("version", c.version());
                    node.put("stable_ga", c.stableGa());
                    node.put("eol", c.eol());
                    node.put("support_horizon_months", c.supportHorizonMonths());
                    node.put("artifact_available", c.artifactAvailable());
                    node.put("score", Math.round(c.score() * 100.0) / 100.0);
                    node.put("viable", c.viable());
                    node.put("lifecycle_evidence_quality", c.lifecycleEvidenceQuality());
                    node.put("spring_cloud_train", c.springCloudTrain());
                    node.set("eliminations", Json.toTree(c.eliminations()));
                    node.set("cautions", Json.toTree(c.cautions()));
                    node.put("selected", landing != null && landing.line().equals(c.line()));
                    evaluated.add(node);
                });
        report.set("candidates", evaluated);
        report.set("path", Json.toTree(path));
        return report;
    }

    // ------------------------------------------------------------------ version helpers

    private static int recommendedJava(Candidate landing, int runningJdk) {
        return landing.supportedJavaMajors().stream()
                .filter(major -> major <= runningJdk)
                .max(Integer::compareTo)
                .orElse(17);
    }

    private static String nextLineAfter(String currentLine, List<Candidate> candidates, String stopLine) {
        int current = numericLine(currentLine);
        int stop = numericLine(stopLine);
        return candidates.stream()
                .map(Candidate::line)
                .filter(line -> numericLine(line) > current && numericLine(line) <= stop)
                .min(java.util.Comparator.comparingInt(TargetResolverStage::numericLine))
                .orElse(null);
    }

    /** Encodes a "major.minor" line as a sortable integer, e.g. 3.4 becomes 304. */
    static int numericLine(String line) {
        if (line == null) {
            return -1;
        }
        String[] parts = line.split("\\.");
        int major = safeInt(parts[0]);
        int minor = parts.length > 1 ? safeInt(parts[1]) : 0;
        return major * 100 + minor;
    }

    static String lineOf(String version) {
        String[] parts = version.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : version;
    }

    static int majorOf(String version) {
        return safeInt(version.split("\\.")[0]);
    }

    static int parseJava(String value) {
        if (value == null) {
            return 17;
        }
        String cleaned = value.startsWith("1.") ? value.substring(2) : value;
        return safeInt(cleaned.split("\\.")[0]);
    }

    private static int safeInt(String token) {
        try {
            return Integer.parseInt(token.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static int compare(String left, String right) {
        return com.bootshift.stages.stage05.CompatibilityStage.compareVersions(left, right);
    }
}
