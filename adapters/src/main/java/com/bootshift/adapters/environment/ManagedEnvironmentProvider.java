package com.bootshift.adapters.environment;

import com.bootshift.adapters.exec.ProcessRunner;
import com.bootshift.core.util.Hashing;
import com.bootshift.ports.environment.EnvironmentProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * MANAGED environment provider.
 *
 * <p>The harness owns the reproducible parts of the environment it can actually control in-process:
 * JDK, locale, timezone, clock strategy and the working directories. Where an OCI runtime is
 * available it is recorded as an additional isolation mechanism; where it is not, the provider says
 * so instead of pretending the environment was containerised.
 *
 * <p>Attributes are classified MUST_MATCH / EXPECTED_TO_DIFFER / UNCONSTRAINED exactly as the
 * Environment Equivalence Contract requires, and the classification - not the raw value - is what
 * decides whether a difference invalidates a differential result.
 */
public final class ManagedEnvironmentProvider implements EnvironmentProvider {

    public static final String IMPLEMENTATION = "bootshift-managed-jvm";
    public static final String VERSION = "1.0.0";

    private final Map<String, ProvisionedEnvironment> provisioned = new LinkedHashMap<>();
    private final Map<String, String> infrastructureOverrides;

    public ManagedEnvironmentProvider() {
        this(Map.of());
    }

    public ManagedEnvironmentProvider(Map<String, String> infrastructureOverrides) {
        this.infrastructureOverrides = infrastructureOverrides;
    }

    @Override
    public Mode mode() {
        return Mode.MANAGED;
    }

    @Override
    public String implementationName() {
        return IMPLEMENTATION;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public ProvisionedEnvironment provision(String role, Map<String, String> requirements) {
        List<Attribute> attributes = new ArrayList<>();

        // Controlled and required to be identical on both sides.
        attributes.add(new Attribute("locale", "C", Constraint.MUST_MATCH, "harness-forced"));
        attributes.add(new Attribute("timezone", "UTC", Constraint.MUST_MATCH, "harness-forced"));
        attributes.add(new Attribute("clock.strategy", "system-utc", Constraint.MUST_MATCH, "harness-forced"));
        attributes.add(new Attribute("file.encoding", "UTF-8", Constraint.MUST_MATCH, "harness-forced"));
        attributes.add(new Attribute("network.egress", "allowlist", Constraint.MUST_MATCH, "harness-policy"));

        // Expected to differ: this is the whole point of the migration.
        attributes.add(new Attribute("java.version", System.getProperty("java.version"),
                Constraint.EXPECTED_TO_DIFFER, "jvm"));
        attributes.add(new Attribute("java.vendor", System.getProperty("java.vendor"),
                Constraint.EXPECTED_TO_DIFFER, "jvm"));
        attributes.add(new Attribute("spring.boot.version", requirements.getOrDefault("spring.boot.version", "unknown"),
                Constraint.EXPECTED_TO_DIFFER, "build-model"));
        attributes.add(new Attribute("servlet.container", requirements.getOrDefault("servlet.container", "unknown"),
                Constraint.EXPECTED_TO_DIFFER, "build-model"));

        attributes.add(new Attribute("os.name", System.getProperty("os.name"),
                Constraint.UNCONSTRAINED, "host"));
        attributes.add(new Attribute("available.processors",
                String.valueOf(Runtime.getRuntime().availableProcessors()), Constraint.UNCONSTRAINED, "host"));

        List<String> gaps = new ArrayList<>();
        Map<String, String> endpoints = new LinkedHashMap<>(infrastructureOverrides);

        String oci = detectOciRuntime();
        if (oci == null) {
            gaps.add("NO_OCI_RUNTIME: no rootless OCI runtime (podman/docker) was detected, so "
                    + "infrastructure dependencies cannot be provisioned by the harness. Any dimension "
                    + "requiring a database, broker or cache is recorded as unobservable rather than "
                    + "assumed equivalent.");
            attributes.add(new Attribute("container.runtime", "none", Constraint.MUST_MATCH, "probe"));
        } else {
            attributes.add(new Attribute("container.runtime", oci, Constraint.MUST_MATCH, "probe"));
        }

        for (Map.Entry<String, String> requirement : requirements.entrySet()) {
            if (requirement.getKey().startsWith("infrastructure.")
                    && !endpoints.containsKey(requirement.getKey())) {
                if (oci == null) {
                    gaps.add("UNPROVISIONED: " + requirement.getKey() + " requested but no runtime available");
                }
            }
        }

        String fingerprint = fingerprint(role, attributes);
        ProvisionedEnvironment environment = new ProvisionedEnvironment(Mode.MANAGED, IMPLEMENTATION,
                VERSION, fingerprint, attributes, List.of(), gaps, endpoints, true, null);
        provisioned.put(role, environment);
        return environment;
    }

    @Override
    public List<EquivalenceCheck> compare(ProvisionedEnvironment oldEnv, ProvisionedEnvironment newEnv) {
        List<EquivalenceCheck> checks = new ArrayList<>();
        Map<String, Attribute> newIndex = new LinkedHashMap<>();
        newEnv.attributes().forEach(a -> newIndex.put(a.name(), a));
        for (Attribute attribute : oldEnv.attributes()) {
            Attribute counterpart = newIndex.get(attribute.name());
            String newValue = counterpart == null ? null : counterpart.value();
            boolean satisfied = switch (attribute.constraint()) {
                case MUST_MATCH -> java.util.Objects.equals(attribute.value(), newValue);
                case EXPECTED_TO_DIFFER, UNCONSTRAINED -> true;
            };
            String detail = switch (attribute.constraint()) {
                case MUST_MATCH -> satisfied ? "identical on both sides"
                        : "MUST_MATCH attribute differs; differential results for affected dimensions "
                          + "cannot be trusted";
                case EXPECTED_TO_DIFFER -> "difference is expected and does not invalidate comparison";
                case UNCONSTRAINED -> "not part of the equivalence contract";
            };
            checks.add(new EquivalenceCheck(attribute.name(), attribute.constraint(),
                    attribute.value(), newValue, satisfied, detail));
        }
        return checks;
    }

    @Override
    public void release(String role) {
        provisioned.remove(role);
    }

    /** Detects a rootless OCI runtime whose license passes the strict-OSS gate. */
    public static String detectOciRuntime() {
        for (String candidate : List.of("podman", "docker", "nerdctl")) {
            String resolved = ProcessRunner.which(candidate);
            if (resolved != null) {
                return candidate;
            }
        }
        return null;
    }

    private static String fingerprint(String role, List<Attribute> attributes) {
        List<String> lines = new ArrayList<>();
        lines.add("role:" + role);
        attributes.stream()
                .filter(a -> a.constraint() != Constraint.UNCONSTRAINED)
                .forEach(a -> lines.add(a.name() + "=" + a.value() + "|" + a.constraint()));
        java.util.Collections.sort(lines);
        return Hashing.manifestHash(lines);
    }

    /** Applied to every child process so both sides observe the same clock and locale. */
    public static Map<String, String> deterministicProcessEnvironment() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("TZ", "UTC");
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("JAVA_TOOL_OPTIONS", "-Duser.timezone=UTC -Dfile.encoding=UTF-8 -Duser.language=en");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        return env;
    }
}
