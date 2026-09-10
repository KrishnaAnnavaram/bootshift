package com.bootshift.adapters.transform;

import com.bootshift.ports.transformation.TransformationPort;

import java.util.List;

/**
 * Dynamic capability discovery for OpenRewrite core (R31, R9).
 *
 * <p>OpenRewrite core is Apache-2.0 and is a legitimate tool for this harness, but it is
 * <em>discovered</em>, not assumed: the adapter probes the classpath at planning time and reports
 * AVAILABLE only when the core recipe API is actually loadable. The source-available Spring recipe
 * estate ({@code rewrite-spring} and relatives) is never loaded, and the license gate blocks those
 * coordinates independently.
 *
 * <p>When OpenRewrite is absent the capability registry records the gap, and the missing
 * deterministic coverage flows into the residual calculation, which raises validation depth. That is
 * the designed behaviour: absent tooling becomes measured residual, not a silent hole.
 */
public final class OpenRewriteCoreProbe implements TransformationPort {

    public static final String PROVIDER = "OPENREWRITE_CORE";

    private static final String CORE_MARKER = "org.openrewrite.Recipe";
    private static final String JAVA_MARKER = "org.openrewrite.java.JavaVisitor";
    private static final String MAVEN_MARKER = "org.openrewrite.maven.MavenVisitor";

    /** Coordinates that must never be loaded even if someone puts them on the classpath. */
    public static final List<String> FORBIDDEN_RECIPE_ESTATES = List.of(
            "org.openrewrite.java.spring",
            "io.moderne.recipe",
            "org.openrewrite.recipe.spring");

    private final ClassLoader classLoader;

    public OpenRewriteCoreProbe() {
        this(OpenRewriteCoreProbe.class.getClassLoader());
    }

    public OpenRewriteCoreProbe(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public String providerName() {
        return PROVIDER;
    }

    public boolean coreAvailable() {
        return classPresent(CORE_MARKER);
    }

    public boolean javaModuleAvailable() {
        return classPresent(JAVA_MARKER);
    }

    public boolean mavenModuleAvailable() {
        return classPresent(MAVEN_MARKER);
    }

    /** True when a forbidden source-available recipe estate is on the classpath. */
    public boolean forbiddenEstatePresent() {
        return FORBIDDEN_RECIPE_ESTATES.stream()
                .anyMatch(pkg -> classPresent(pkg + ".NoOpRecipe") || classPresent(pkg + ".UpgradeSpringBoot_3_0"));
    }

    private boolean classPresent(String className) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public List<Capability> capabilities(String sourceVersion, String targetVersion) {
        if (forbiddenEstatePresent()) {
            return List.of(new Capability(
                    "CAP-OPENREWRITE-CORE", PROVIDER, "openrewrite-core", "unknown",
                    "Source-Available", "classpath-probe", sourceVersion, targetVersion,
                    List.of(), List.of(), true, true, "UNKNOWN", 0.0,
                    "BLOCKED_BY_LICENSE_POLICY",
                    "A source-available OpenRewrite Spring recipe estate is present on the classpath. "
                            + "Strict-OSS policy forbids loading it (R8, R9)."));
        }
        if (!coreAvailable()) {
            return List.of(new Capability(
                    "CAP-OPENREWRITE-CORE", PROVIDER, "openrewrite-core", null,
                    "Apache-2.0", "not-loaded", sourceVersion, targetVersion,
                    List.of(), List.of(), true, true, "UNKNOWN", 0.0, "UNAVAILABLE",
                    "OpenRewrite core is not on the harness classpath. Deterministic coverage for the "
                            + "recipe types it would provide is counted as residual, which raises the "
                            + "planned validation depth for affected edges."));
        }
        List<String> factTypes = new java.util.ArrayList<>();
        factTypes.add("API_RENAMED");
        if (javaModuleAvailable()) {
            factTypes.add("API_SIGNATURE_CHANGED");
        }
        if (mavenModuleAvailable()) {
            factTypes.add("MANAGED_VERSION_CHANGED");
            factTypes.add("ARTIFACT_RELOCATED");
        }
        return List.of(new Capability(
                "CAP-OPENREWRITE-CORE", PROVIDER, "openrewrite-core", detectVersion(),
                "Apache-2.0", "classpath-probe", sourceVersion, targetVersion,
                factTypes, List.of(), true, true, "SINGLE_EDGE", 0.9, "AVAILABLE",
                "OpenRewrite core detected. Only Apache-licensed core, java and maven modules are used; "
                        + "no source-available Spring recipe estate is loaded."));
    }

    private String detectVersion() {
        try {
            Class<?> recipe = Class.forName(CORE_MARKER, false, classLoader);
            Package pkg = recipe.getPackage();
            return pkg == null || pkg.getImplementationVersion() == null
                    ? "unknown" : pkg.getImplementationVersion();
        } catch (ClassNotFoundException | LinkageError e) {
            return "unknown";
        }
    }

    @Override
    public boolean handles(String recipeId) {
        return coreAvailable() && recipeId != null && recipeId.startsWith("openrewrite.");
    }

    @Override
    public TransformationOutcome apply(String recipeId, TransformationRequest request) {
        return new TransformationOutcome(List.of(),
                List.of("OpenRewrite core is not loaded in this deployment; recipe " + recipeId
                        + " produced no changes and is recorded as residual."),
                List.of(recipeId), false);
    }
}
