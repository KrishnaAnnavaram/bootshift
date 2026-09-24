package com.bootshift.ports.transformation;

import com.bootshift.core.ledger.ChangeEvent;

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

    /**
     * The capability that actually implements this recipe.
     *
     * <p>The planner used to attach "the first AVAILABLE capability whose declared fact types
     * overlap anything in the run" to every scheduled recipe. That produced plan entries pairing a
     * Maven POM recipe with a JUnit capability - a claim that a reviewer checking the plan would
     * find is simply untrue, and a coverage number computed from it means nothing.
     *
     * <p>Providers that expose one capability per recipe return it here. The default returns empty,
     * which the planner records as an explicit "no capability claims this recipe" rather than
     * silently substituting one.
     */
    default java.util.Optional<Capability> capabilityFor(String recipeId, String sourceVersion,
                                                         String targetVersion) {
        if (!handles(recipeId)) {
            return java.util.Optional.empty();
        }
        List<Capability> available = capabilities(sourceVersion, targetVersion).stream()
                .filter(c -> "AVAILABLE".equals(c.status()))
                .toList();
        // Exactly one capability means there is no ambiguity to resolve. More than one, and the
        // provider has to say which, because guessing is what this method exists to stop.
        return available.size() == 1 ? java.util.Optional.of(available.get(0)) : java.util.Optional.empty();
    }

    TransformationOutcome apply(String recipeId, TransformationRequest request);

    /**
     * How a change this provider produced must be attributed in the change ledger.
     *
     * <p>Agent 12 used to stamp every ledger event with one hardcoded
     * {@code BOOTSHIFT_DETERMINISTIC / bootshift-transformers / 1.0.0} descriptor, whichever
     * provider had actually produced the change. The transformation report and the proposal record
     * still named the real engine, but the ledger did not - and the ledger is what the evidence
     * document and the provenance graph read. A migration performed by OpenRewrite 8.x was
     * therefore reported, in the sealed evidence, as having been performed by Bootshift's own
     * transformers at version 1.0.0. That is the one class of error this harness exists to prevent.
     *
     * <p>The default is the descriptor the harness's own deterministic transformers have always
     * carried; the recipe id on each event already distinguishes them from one another. A provider
     * that delegates to an external engine overrides this so the engine and its version reach the
     * ledger.
     */
    default ChangeEvent.Provider ledgerIdentity() {
        return new ChangeEvent.Provider("BOOTSHIFT_DETERMINISTIC", "bootshift-transformers", "1.0.0");
    }
}
