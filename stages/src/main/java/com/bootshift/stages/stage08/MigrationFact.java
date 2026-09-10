package com.bootshift.stages.stage08;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.core.util.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * A migration fact (spec section 19).
 *
 * <p>Only {@link Status#VERIFIED} facts may authorize automatic transformation. A fact extracted
 * from documentation alone stays CANDIDATE until an artifact-channel observation corroborates it, and
 * a fact the two channels disagree about becomes CONFLICTING rather than being quietly resolved.
 */
public final class MigrationFact {

    public enum Type {
        API_REMOVED,
        API_RENAMED,
        API_SIGNATURE_CHANGED,
        PROPERTY_REMOVED,
        PROPERTY_RENAMED,
        PROPERTY_SILENTLY_IGNORED,
        DEFAULT_CHANGED,
        ARTIFACT_REMOVED,
        ARTIFACT_RELOCATED,
        MANAGED_VERSION_CHANGED,
        BEHAVIOR_CHANGED_NO_API_CHANGE,
        COMPATIBILITY_REQUIREMENT,
        BASELINE_REQUIREMENT
    }

    public enum Status {
        CANDIDATE, VERIFIED, CONFLICTING, REJECTED
    }

    public enum Channel {
        DOCUMENTATION, ARTIFACT, BOTH
    }

    private final String knowledgeId;
    private final Type type;
    private final String subject;
    private String from;
    private String to;
    private String sourceVersion;
    private String targetVersion;
    private String summary;
    private Status status = Status.CANDIDATE;
    private Channel channel = Channel.DOCUMENTATION;
    private double confidence;
    private final List<String> documentRefs = new ArrayList<>();
    private final List<String> artifactEvidence = new ArrayList<>();
    private final List<String> conflicts = new ArrayList<>();
    private String detectionRule;
    private boolean aiAssisted;

    public MigrationFact(String knowledgeId, Type type, String subject) {
        this.knowledgeId = knowledgeId;
        this.type = type;
        this.subject = subject;
    }

    public String knowledgeId() {
        return knowledgeId;
    }

    public Type type() {
        return type;
    }

    public String subject() {
        return subject;
    }

    public String from() {
        return from;
    }

    public MigrationFact from(String value) {
        this.from = value;
        return this;
    }

    public String to() {
        return to;
    }

    public MigrationFact to(String value) {
        this.to = value;
        return this;
    }

    public MigrationFact versions(String sourceVersion, String targetVersion) {
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
        return this;
    }

    public String sourceVersion() {
        return sourceVersion;
    }

    public String targetVersion() {
        return targetVersion;
    }

    public String summary() {
        return summary;
    }

    public MigrationFact summary(String value) {
        this.summary = value;
        return this;
    }

    public Status status() {
        return status;
    }

    public MigrationFact status(Status value) {
        this.status = value;
        return this;
    }

    public Channel channel() {
        return channel;
    }

    public MigrationFact channel(Channel value) {
        this.channel = value;
        return this;
    }

    public double confidence() {
        return confidence;
    }

    public MigrationFact confidence(double value) {
        this.confidence = value;
        return this;
    }

    public List<String> documentRefs() {
        return documentRefs;
    }

    public MigrationFact document(String ref) {
        documentRefs.add(ref);
        return this;
    }

    public List<String> artifactEvidence() {
        return artifactEvidence;
    }

    public MigrationFact evidence(String value) {
        artifactEvidence.add(value);
        return this;
    }

    public List<String> conflicts() {
        return conflicts;
    }

    public MigrationFact conflict(String value) {
        conflicts.add(value);
        this.status = Status.CONFLICTING;
        return this;
    }

    public String detectionRule() {
        return detectionRule;
    }

    public MigrationFact detectionRule(String value) {
        this.detectionRule = value;
        return this;
    }

    public boolean aiAssisted() {
        return aiAssisted;
    }

    public MigrationFact aiAssisted(boolean value) {
        this.aiAssisted = value;
        return this;
    }

    /**
     * Promotes a fact to VERIFIED. Requires artifact-channel evidence: documentation alone describes
     * intent, artifacts describe reality, and only reality can authorize a code change (R10).
     */
    public MigrationFact verifyWithArtifactEvidence(String evidence) {
        artifactEvidence.add(evidence);
        if (conflicts.isEmpty()) {
            this.status = Status.VERIFIED;
            this.channel = documentRefs.isEmpty() ? Channel.ARTIFACT : Channel.BOTH;
            this.confidence = Math.max(confidence, documentRefs.isEmpty() ? 0.85 : 0.97);
        }
        return this;
    }

    public boolean authorizesTransformation() {
        return status == Status.VERIFIED;
    }

    public ObjectNode toNode() {
        ObjectNode node = Json.obj();
        node.put("knowledge_id", knowledgeId);
        node.put("type", type.name());
        node.put("subject", subject);
        node.put("from", from);
        node.put("to", to);
        node.put("source_version", sourceVersion);
        node.put("target_version", targetVersion);
        node.put("summary", summary);
        node.put("status", status.name());
        node.put("channel", channel.name());
        node.put("confidence", confidence);
        node.put("detection_rule", detectionRule);
        node.put("ai_assisted", aiAssisted);
        node.put("authorizes_transformation", authorizesTransformation());
        node.set("document_refs", Json.toTree(documentRefs));
        node.set("artifact_evidence", Json.toTree(artifactEvidence));
        node.set("conflicts", Json.toTree(conflicts));
        return node;
    }
}
