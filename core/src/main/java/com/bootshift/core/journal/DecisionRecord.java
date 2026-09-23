package com.bootshift.core.journal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.util.Json;

import java.time.Instant;
import java.util.List;

/**
 * A judgement the run made, with what it was made from.
 *
 * <p>A migration report that says "target 3.5.16 was selected" is not auditable. One that says which
 * candidates existed, which were rejected, on what rule, against which lifecycle document, is. The
 * difference costs one record per decision and is the whole distinction between a tool that
 * migrated an application and a tool that can show why.
 *
 * <p>{@link Authority} is deliberately narrow. AI may appear as {@link Authority#AI_PROPOSAL}, and
 * only ever as a proposal: the field that says who authorized the action is a separate one, and no
 * code path sets it to AI. That is an invariant of the architecture, and
 * {@code AiBoundaryTest} enforces it.
 */
public record DecisionRecord(String decisionId,
                             String type,
                             String subject,
                             String decision,
                             String reason,
                             List<String> alternativesConsidered,
                             List<String> evidenceRefs,
                             List<String> documentRefs,
                             List<String> policyRefs,
                             String confidence,
                             Authority madeBy,
                             String authorizedBy,
                             Instant decidedAt) {

    /** Who produced a decision. */
    public enum Authority {
        /** A rule in the harness, evaluated on artifacts. The default and the overwhelming majority. */
        DETERMINISTIC_RULE,
        /** A frozen policy file. */
        POLICY,
        /** A signed human decision from outside the run. */
        HUMAN_DECISION,
        /** An AI suggestion. Never authorizing on its own; always verified before use. */
        AI_PROPOSAL
    }

    /** The authorizing authority when AI proposed something. Never the AI itself. */
    public static final String DETERMINISTIC_VERIFICATION = "DETERMINISTIC_VERIFICATION";

    public DecisionRecord {
        alternativesConsidered = alternativesConsidered == null ? List.of()
                : List.copyOf(alternativesConsidered);
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        documentRefs = documentRefs == null ? List.of() : List.copyOf(documentRefs);
        policyRefs = policyRefs == null ? List.of() : List.copyOf(policyRefs);
        if (madeBy == Authority.AI_PROPOSAL
                && (authorizedBy == null || authorizedBy.isBlank())) {
            // An AI proposal with nothing recorded as having verified it would read, downstream, as
            // an AI-authorized change. Refusing to construct one is cheaper than auditing for it.
            throw new IllegalArgumentException(
                    "An AI-proposed decision must record what deterministically authorized it");
        }
    }

    /** Convenience for the common case: a deterministic rule reaching a conclusion. */
    public static Builder deterministic(String decisionId, String type, String subject) {
        return new Builder(decisionId, type, subject).madeBy(Authority.DETERMINISTIC_RULE);
    }

    public static Builder builder(String decisionId, String type, String subject) {
        return new Builder(decisionId, type, subject);
    }

    /** Mutable assembly for a decision; produces an immutable record. */
    public static final class Builder {
        private final String decisionId;
        private final String type;
        private final String subject;
        private String decision;
        private String reason;
        private final List<String> alternatives = new java.util.ArrayList<>();
        private final List<String> evidence = new java.util.ArrayList<>();
        private final List<String> documents = new java.util.ArrayList<>();
        private final List<String> policies = new java.util.ArrayList<>();
        private String confidence = "UNSPECIFIED";
        private Authority madeBy = Authority.DETERMINISTIC_RULE;
        private String authorizedBy;

        private Builder(String decisionId, String type, String subject) {
            this.decisionId = decisionId;
            this.type = type;
            this.subject = subject;
        }

        public Builder decided(String value) {
            this.decision = value;
            return this;
        }

        public Builder because(String value) {
            this.reason = value;
            return this;
        }

        public Builder alternative(String value) {
            if (value != null) {
                alternatives.add(value);
            }
            return this;
        }

        public Builder evidence(String value) {
            if (value != null) {
                evidence.add(value);
            }
            return this;
        }

        public Builder document(String value) {
            if (value != null) {
                documents.add(value);
            }
            return this;
        }

        public Builder policy(String value) {
            if (value != null) {
                policies.add(value);
            }
            return this;
        }

        public Builder confidence(String value) {
            this.confidence = value;
            return this;
        }

        public Builder madeBy(Authority value) {
            this.madeBy = value;
            return this;
        }

        /** Records what verified an AI proposal before it was allowed to have any effect. */
        public Builder authorizedBy(String value) {
            this.authorizedBy = value;
            return this;
        }

        public DecisionRecord build() {
            return new DecisionRecord(decisionId, type, subject, decision, reason, alternatives,
                    evidence, documents, policies, confidence, madeBy, authorizedBy, Instant.now());
        }
    }

    public ObjectNode toNode() {
        ObjectNode node = Json.obj();
        node.put("decision_id", decisionId);
        node.put("type", type);
        node.put("subject", subject);
        node.put("decision", decision);
        node.put("reason", reason);
        node.set("alternatives_considered", Json.toTree(alternativesConsidered));
        node.set("evidence_refs", Json.toTree(evidenceRefs));
        node.set("document_refs", Json.toTree(documentRefs));
        node.set("policy_refs", Json.toTree(policyRefs));
        node.put("confidence", confidence);
        node.put("made_by", madeBy.name());
        node.put("authorized_by", authorizedBy == null && madeBy != Authority.AI_PROPOSAL
                ? madeBy.name() : authorizedBy);
        node.put("decided_at", decidedAt == null ? null : decidedAt.toString());
        return node;
    }
}
