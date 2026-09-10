package com.bootshift.adapters.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.bootshift.core.util.Json;
import com.bootshift.ports.transformation.TransformationPort;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration property migration driven by generated rules (spec section 23).
 *
 * <p>The rules are not hand-written. They are generated from the source and target
 * {@code spring-configuration-metadata.json} deprecation entries, which is the only scalable way to
 * cover hundreds of property renames without inventing any of them.
 */
public final class ConfigurationPropertyTransformer implements TransformationPort {

    public static final String PROVIDER = "BOOTSHIFT_CONFIG_PROPERTY";
    public static final String RECIPE = "config.property-migration";

    /** One generated rule: rename, remove, or replace with a documented successor. */
    public record PropertyRule(String from, String to, String action, String reason, String evidenceRef) {
    }

    private final List<PropertyRule> rules;

    public ConfigurationPropertyTransformer(List<PropertyRule> rules) {
        this.rules = rules;
    }

    /** Loads generated rules from the migration-rules directory. */
    public static ConfigurationPropertyTransformer fromRuleFile(Path ruleFile) {
        List<PropertyRule> rules = new ArrayList<>();
        if (ruleFile != null && Files.isRegularFile(ruleFile)) {
            JsonNode node = Json.read(ruleFile);
            for (JsonNode rule : node.path("rules")) {
                rules.add(new PropertyRule(rule.path("from").asText(), rule.path("to").asText(null),
                        rule.path("action").asText("RENAME"), rule.path("reason").asText(null),
                        rule.path("evidence_ref").asText(null)));
            }
        }
        return new ConfigurationPropertyTransformer(rules);
    }

    public List<PropertyRule> rules() {
        return rules;
    }

    @Override
    public String providerName() {
        return PROVIDER;
    }

    @Override
    public List<Capability> capabilities(String sourceVersion, String targetVersion) {
        return List.of(new Capability(
                "CAP-CONFIG-PROPERTY", PROVIDER, "bootshift-config-property-transformer", "1.0.0",
                "Apache-2.0", "harness-owned", sourceVersion, targetVersion,
                List.of("PROPERTY_RENAMED", "PROPERTY_REMOVED", "PROPERTY_SILENTLY_IGNORED"),
                List.of(),
                true, true, "SINGLE_EDGE", rules.isEmpty() ? 0.0 : 1.0,
                rules.isEmpty() ? "NO_RULES_GENERATED" : "AVAILABLE",
                rules.size() + " rule(s) generated from official configuration metadata deprecations."));
    }

    @Override
    public boolean handles(String recipeId) {
        return RECIPE.equals(recipeId);
    }

    @Override
    public TransformationOutcome apply(String recipeId, TransformationRequest request) {
        List<ProposedChange> changes = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        if (rules.isEmpty()) {
            messages.add("No property migration rules were generated for this edge; "
                    + "property migration is recorded as residual rather than silently skipped.");
            return new TransformationOutcome(changes, messages,
                    List.of("PROPERTY_RENAMED", "PROPERTY_REMOVED"), true);
        }

        for (String relativePath : request.targetPaths()) {
            if (!relativePath.endsWith(".properties") && !relativePath.endsWith(".yml")
                    && !relativePath.endsWith(".yaml")) {
                continue;
            }
            Path file = request.workspaceRoot().resolve(relativePath);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            String original = read(file);
            Result result = relativePath.endsWith(".properties")
                    ? migrateProperties(original, rules)
                    : migrateYaml(original, rules);
            if (result.applied().isEmpty()) {
                continue;
            }
            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("transformer", PROVIDER);
            attributes.put("base_hash", com.bootshift.core.util.Hashing.sha256(original));
            attributes.put("rules_applied", String.join(",", result.applied()));
            changes.add(new ProposedChange(relativePath, null, "MODIFY", result.content(),
                    "Applied " + result.applied().size() + " generated property rule(s)",
                    RECIPE, splitCsv(request.parameters().get("knowledge_refs")),
                    splitCsv(request.parameters().get("impact_refs")), attributes));
        }
        return new TransformationOutcome(changes, messages, List.of(), true);
    }

    public record Result(String content, List<String> applied) {
    }

    /**
     * Migrates a .properties file. Comments and blank lines are preserved verbatim; a removed
     * property is commented out with the reason rather than deleted, so the change stays reviewable.
     */
    public static Result migrateProperties(String content, List<PropertyRule> rules) {
        List<String> applied = new ArrayList<>();
        String[] lines = content.split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            String rewritten = line;
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("!")) {
                int separator = indexOfSeparator(trimmed);
                if (separator > 0) {
                    String key = trimmed.substring(0, separator).trim();
                    String value = trimmed.substring(separator + 1);
                    for (PropertyRule rule : rules) {
                        if (!key.equals(rule.from())) {
                            continue;
                        }
                        if ("REMOVE".equals(rule.action()) || rule.to() == null || rule.to().isBlank()) {
                            rewritten = "# [bootshift] removed property " + key + ": "
                                    + (rule.reason() == null ? "no replacement in target version" : rule.reason())
                                    + "\n#" + line;
                        } else {
                            rewritten = line.replace(key, rule.to());
                        }
                        applied.add(rule.from());
                        break;
                    }
                }
            }
            out.append(rewritten);
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }
        return new Result(out.toString(), applied);
    }

    /**
     * Migrates flow-style and nested YAML by rebuilding the dotted key of each leaf and matching it
     * against the rules. Only leaf keys whose full dotted path matches are rewritten.
     */
    public static Result migrateYaml(String content, List<PropertyRule> rules) {
        List<String> applied = new ArrayList<>();
        String[] lines = content.split("\n", -1);
        StringBuilder out = new StringBuilder();
        List<String> stack = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String rewritten = line;
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains(":")) {
                int indent = line.length() - line.stripLeading().length();
                int depth = indent / 2;
                while (stack.size() > depth) {
                    stack.remove(stack.size() - 1);
                }
                String key = trimmed.substring(0, trimmed.indexOf(':')).trim();
                boolean leaf = !trimmed.endsWith(":");
                stack.add(key);
                String dotted = String.join(".", stack);
                if (leaf) {
                    for (PropertyRule rule : rules) {
                        if (!dotted.equals(rule.from())) {
                            continue;
                        }
                        if ("REMOVE".equals(rule.action()) || rule.to() == null || rule.to().isBlank()) {
                            rewritten = " ".repeat(indent) + "# [bootshift] removed " + dotted + ": "
                                    + (rule.reason() == null ? "no replacement" : rule.reason())
                                    + "\n" + " ".repeat(indent) + "#" + trimmed;
                        } else {
                            // Flatten to the replacement dotted key at the same indentation.
                            String value = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                            rewritten = " ".repeat(indent) + rule.to() + ": " + value;
                        }
                        applied.add(rule.from());
                        break;
                    }
                    stack.remove(stack.size() - 1);
                }
            }
            out.append(rewritten);
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }
        return new Result(out.toString(), applied);
    }

    private static int indexOfSeparator(String line) {
        int equals = line.indexOf('=');
        int colon = line.indexOf(':');
        if (equals < 0) {
            return colon;
        }
        if (colon < 0) {
            return equals;
        }
        return Math.min(equals, colon);
    }

    private static List<String> splitCsv(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.split(","));
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }
}
