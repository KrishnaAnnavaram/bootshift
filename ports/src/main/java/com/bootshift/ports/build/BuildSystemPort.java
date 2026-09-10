package com.bootshift.ports.build;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Build tool interrogation and execution (Agent 02, Agent 13, Agent 15).
 *
 * <p>R5: the build tool itself is authoritative. Adapters must obtain the effective model by asking
 * Maven or Gradle, never by reconstructing it from XML or build-script parsing alone. Parsed
 * descriptors may only supply hints and are labelled as such.
 */
public interface BuildSystemPort {

    enum Kind {
        MAVEN, GRADLE, UNKNOWN
    }

    /** One resolved dependency with the provenance the spec requires. */
    record ResolvedDependency(String groupId, String artifactId, String version, String type,
                              String scope, String module, String checksum, String repository,
                              boolean direct, boolean managed, String bomSource, String resolutionStatus) {
        public String ga() {
            return groupId + ":" + artifactId;
        }

        public String gav() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }

    record ResolvedPlugin(String groupId, String artifactId, String version, String module, String phase) {
    }

    record ManagedVersion(String groupId, String artifactId, String version, String source) {
    }

    record RepositoryRef(String id, String url, boolean snapshots, boolean releases) {
    }

    /**
     * One module of the build. {@code classpath} holds the resolved compile-plus-test classpath as
     * reported by the build tool; it is what lets the code model resolve types coming from
     * dependencies instead of degrading to UNRESOLVED attribution.
     */
    record ModuleModel(String moduleId, String path, String groupId, String artifactId, String version,
                       String packaging, String parentGav, String javaVersion,
                       Map<String, String> properties, List<String> activeProfiles,
                       List<String> classpath) {

        public ModuleModel(String moduleId, String path, String groupId, String artifactId,
                           String version, String packaging, String parentGav, String javaVersion,
                           Map<String, String> properties, List<String> activeProfiles) {
            this(moduleId, path, groupId, artifactId, version, packaging, parentGav, javaVersion,
                    properties, activeProfiles, List.of());
        }
    }

    record ResolutionIssue(String severity, String module, String detail, String remediation) {
    }

    /** The complete authoritative build model for one repository. */
    record BuildModel(Kind kind, String toolVersion, String toolInvocation, boolean wrapperUsed,
                      List<ModuleModel> modules, List<ResolvedDependency> dependencies,
                      List<ResolvedPlugin> plugins, List<ManagedVersion> managedVersions,
                      List<RepositoryRef> repositories, List<ResolutionIssue> issues,
                      Map<String, String> toolchains, boolean authoritative, String degradedReason) {
    }

    /** Outcome of an actual build or test execution. */
    record ExecutionResult(boolean success, int exitCode, Duration duration, String command,
                           List<String> stdoutTail, List<String> stderrTail, Path workingDirectory) {
    }

    Kind detect(Path repositoryRoot);

    boolean supports(Path repositoryRoot);

    /** Interrogates the build tool for its effective model. Never mutates the repository. */
    BuildModel resolve(Path repositoryRoot, Path evidenceSink);

    ExecutionResult compile(Path repositoryRoot, Path modulePath, Map<String, String> options);

    ExecutionResult test(Path repositoryRoot, Path modulePath, Map<String, String> options);

    /** Runs an arbitrary allowlisted goal, e.g. coverage report generation. */
    ExecutionResult invoke(Path repositoryRoot, List<String> goals, Map<String, String> options);
}
