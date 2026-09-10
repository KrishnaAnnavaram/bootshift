package com.bootshift.ports.transformation;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Deterministic source transformation (Agent 12).
 *
 * <p>R13: a transformer computes a proposed patch. It never writes to the repository. The returned
 * {@link ProposedChange} list is handed to the FileMutationGateway, which is the only writer.
 *
 * <p>R31: capabilities are discovered at planning time from the tools actually present, not assumed.
 */
public interface TransformationPort {

    /** What a transformer can actually do for a given edge, with license evidence. */
    /**
     * A transformation capability discovered at planning time.
     *
     * <p>{@code handledFactTypes} alone overstates coverage. A transformer that rewrites JUnit 4
     * constructs handles some {@code API_REMOVED} facts and no others, and declaring the type made
     * every removed API in the run count as covered - including annotations nothing can rewrite.
     *
     * <p>{@code handledSubjectPrefixes} narrows the claim: a capability covers a fact when it
     * declares the fact's type <em>and</em> either declares no prefixes at all (a genuinely
     * type-wide capability, such as configuration-property migration) or declares one the fact's
     * subject starts with.
     */
    record Capability(String capabilityId, String provider, String toolName, String toolVersion,
                      String licenseSpdx, String licenseEvidenceRef, String sourceVersionRange,
                      String targetVersionRange, List<String> handledFactTypes,
                      List<String> handledSubjectPrefixes, boolean deterministic,
                      boolean idempotent, String checkpointSpan, double confidence, String status,
                      String notes) {

        /** True when this capability's claim actually reaches the given fact. */
        public boolean covers(String factType, String subject) {
            if (!handledFactTypes.contains(factType)) {
                return false;
            }
            if (handledSubjectPrefixes == null || handledSubjectPrefixes.isEmpty()) {
                return true;
            }
            if (subject == null) {
                return false;
            }
            return handledSubjectPrefixes.stream().anyMatch(subject::startsWith);
        }
    }

    /** A patch the transformer proposes but has not applied. */
    record ProposedChange(String path, String newPath, String operation, String newContent,
                          String rationale, String recipeId, List<String> knowledgeRefs,
                          List<String> impactRefs, Map<String, String> attributes) {
    }

    record TransformationRequest(Path workspaceRoot, String edgeId, String sourceVersion,
                                 String targetVersion, List<String> targetPaths,
                                 Map<String, String> parameters) {
    }

    record TransformationOutcome(List<ProposedChange> changes, List<String> messages,
                                 List<String> unhandledFactTypes, boolean success) {
    }

    String providerName();

    /** Reports what this provider can do right now, given the tools on the classpath. */
    List<Capability> capabilities(String sourceVersion, String targetVersion);

    boolean handles(String recipeId);

    TransformationOutcome apply(String recipeId, TransformationRequest request);
}
