package com.bootshift.stages.stage07;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bootshift.adapters.docs.HttpDocumentationAdapter;
import com.bootshift.core.domain.Envelope;
import com.bootshift.core.domain.ExitCode;
import com.bootshift.core.domain.OutputLayout;
import com.bootshift.core.domain.StageResult;
import com.bootshift.core.evidence.EvidenceManifest;
import com.bootshift.core.state.RunState;
import com.bootshift.core.util.Json;
import com.bootshift.ports.docs.DocumentationPort;
import com.bootshift.stages.Stage;
import com.bootshift.stages.StageContext;
import com.bootshift.stages.StageSupport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Agent 07 - Documentation Registry (spec section 18).
 *
 * <p>Runs after the path is frozen, because only then is it known which edge-specific documents
 * matter. Every retrieved document is pinned by content hash; the stored snapshot is the authority
 * and no summary may replace it.
 *
 * <p>Documents are recorded with an explicit trust level. A community source is admitted only as
 * ADVISORY and can never become the sole basis for a verified migration fact.
 */
public final class DocumentationStage implements Stage {

    public static final String OUTPUT_DIR = "07-documentation";

    /** A document the harness knows how to locate for a given edge. */
    private record Source(String urlTemplate, String publisher, String component,
                          DocumentationPort.TrustLevel trustLevel, String description) {
    }

    private static final List<Source> EDGE_SOURCES = List.of(
            new Source("https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-%s-Release-Notes",
                    "Spring", "spring-boot", DocumentationPort.TrustLevel.OFFICIAL_RELEASE_NOTES,
                    "Official release notes for the target line"),
            new Source("https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-%s-Migration-Guide",
                    "Spring", "spring-boot", DocumentationPort.TrustLevel.OFFICIAL_MIGRATION_GUIDE,
                    "Official migration guide for the target line"));

    private static final List<Source> GENERAL_SOURCES = List.of(
            new Source("https://raw.githubusercontent.com/spring-projects/spring-boot/main/README.adoc",
                    "Spring", "spring-boot", DocumentationPort.TrustLevel.OFFICIAL_GENERAL_DOC,
                    "Project overview and system requirements"));

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
        return "Fetch and content-address the authoritative documents for the frozen migration path";
    }

    @Override
    public List<RunState> preconditions() {
        return List.of(RunState.TARGET_FROZEN);
    }

    @Override
    public RunState postcondition() {
        return RunState.DOCUMENTATION_RETRIEVED;
    }

    @Override
    public List<String> inputArtifacts() {
        return List.of("06-target/target-state.json", "06-target/migration-path.json");
    }

    @Override
    public List<String> outputArtifacts() {
        return List.of("document-registry.json", "document-coverage.json", "manifest.json");
    }

    @Override
    public StageResult execute(StageContext context) {
        JsonNode target = StageSupport.requireUpstream(context, "06-target", "target-state.json",
                "Run: harness resolve-target --target auto");
        JsonNode path = StageSupport.requireUpstream(context, "06-target", "migration-path.json",
                "Run: harness resolve-target --target auto");

        HttpDocumentationAdapter documentation = new HttpDocumentationAdapter(context.http(),
                context.run().runWorkspace().resolve("documents"));

        OutputLayout.StageWriter writer = context.run().output().open(OUTPUT_DIR);
        Envelope envelope = StageSupport.envelope(context, OUTPUT_DIR);

        List<DocumentationPort.DocumentRef> retrieved = new ArrayList<>();
        List<ObjectNode> attempts = new ArrayList<>();

        for (JsonNode edge : path.path("edges")) {
            String from = edge.path("fromVersion").asText();
            String to = edge.path("toVersion").asText();
            if (from.equals(to)) {
                continue;
            }
            String line = lineOf(to);
            for (Source source : EDGE_SOURCES) {
                String url = String.format(source.urlTemplate(), line);
                Optional<DocumentationPort.DocumentRef> ref = documentation.retrieve(url,
                        source.publisher(), source.component(), from, to, source.trustLevel());
                attempts.add(renderAttempt(url, source, from, to, ref));
                ref.ifPresent(retrieved::add);
            }
        }
        for (Source source : GENERAL_SOURCES) {
            Optional<DocumentationPort.DocumentRef> ref = documentation.retrieve(source.urlTemplate(),
                    source.publisher(), source.component(),
                    target.path("source_version").asText(), target.path("landing_version").asText(),
                    source.trustLevel());
            attempts.add(renderAttempt(source.urlTemplate(), source,
                    target.path("source_version").asText(), target.path("landing_version").asText(), ref));
            ref.ifPresent(retrieved::add);
        }

        ObjectNode registry = Json.obj();
        registry.put("online", documentation.online());
        registry.put("document_count", retrieved.size());
        registry.put("precedence", "1 exact migration guide, 2 release notes, 3 BOM and POM metadata, "
                + "4 API docs, 5 general docs, 6 upstream project docs, 7 community (advisory only)");
        ArrayNode documents = Json.arr();
        for (DocumentationPort.DocumentRef ref : retrieved) {
            ObjectNode node = Json.obj();
            node.put("document_id", ref.documentId());
            node.put("publisher", ref.publisher());
            node.put("component", ref.component());
            node.put("source_version", ref.sourceVersion());
            node.put("target_version", ref.targetVersion());
            node.put("url", ref.url());
            node.put("retrieved_at", ref.retrievedAt());
            node.put("content_hash", ref.contentHash());
            node.put("trust_level", ref.trustLevel().name());
            node.put("etag", ref.etag());
            node.put("last_modified", ref.lastModified());
            node.put("from_cache", ref.fromCache());
            node.put("size_bytes", ref.sizeBytes());
            node.put("extracted_text_length", ref.extractedTextLength());
            node.put("extraction_usable", ref.extractedTextLength() > 500);
            node.put("local_path", ref.localPath());
            documents.add(node);
            documentation.body(ref.documentId()).ifPresent(body ->
                    StageSupport.toEvidence(context, "document", body,
                            EvidenceManifest.Classification.PUBLIC, "SEALED_EVIDENCE", OUTPUT_DIR));
        }
        registry.set("documents", documents);
        writer.write("document-registry.json", StageSupport.compose(envelope, registry));

        ObjectNode coverage = Json.obj();
        long edgesNeedingDocs = 0;
        for (JsonNode edge : path.path("edges")) {
            if (!edge.path("fromVersion").asText().equals(edge.path("toVersion").asText())) {
                edgesNeedingDocs++;
            }
        }
        long edgesWithGuide = retrieved.stream()
                .filter(r -> r.trustLevel() == DocumentationPort.TrustLevel.OFFICIAL_MIGRATION_GUIDE)
                .map(DocumentationPort.DocumentRef::targetVersion)
                .distinct().count();
        long unusable = retrieved.stream().filter(r -> r.extractedTextLength() <= 500).count();
        coverage.put("documents_with_unusable_extraction", unusable);
        coverage.put("edges_requiring_documentation", edgesNeedingDocs);
        coverage.put("edges_with_official_migration_guide", edgesWithGuide);
        coverage.put("attempt_count", attempts.size());
        coverage.set("attempts", Json.toTree(attempts));
        writer.write("document-coverage.json",
                StageSupport.compose(StageSupport.envelope(context, OUTPUT_DIR), coverage));

        if (edgesWithGuide < edgesNeedingDocs) {
            envelope.gap(new Envelope.Gap("GAP-DOC-001", "DOCUMENTATION",
                    "Only " + edgesWithGuide + " of " + edgesNeedingDocs
                            + " edge(s) have an official migration guide pinned",
                    "Migration facts for the uncovered edges must come from the artifact channel alone"));
        }
        if (unusable > 0) {
            envelope.gap(new Envelope.Gap("GAP-DOC-002", "DOCUMENTATION",
                    unusable + " pinned document(s) yielded no usable readable text",
                    "The documentation channel cannot extract candidate facts from those documents; "
                            + "only the artifact channel covers the edges they describe"));
        }
        if (!documentation.online()) {
            envelope.blindSpot(new Envelope.BlindSpot("BS-DOC-001", "DOCUMENTATION",
                    "Offline mode: no new documentation could be retrieved",
                    "Only previously pinned documents are available to the knowledge engine"));
        }

        String hash = StageSupport.publish(context, writer);
        context.stateMachine().transition(RunState.DOCUMENTATION_RETRIEVED,
                retrieved.size() + " document(s) pinned");
        context.runStateStore().updateState(context.run().runId(), RunState.DOCUMENTATION_RETRIEVED,
                "documentation pinned");

        Map<String, Path> artifacts = new LinkedHashMap<>();
        outputArtifacts().forEach(name -> artifacts.put(name, writer.dir().resolve(name)));

        List<String> messages = new ArrayList<>();
        attempts.stream().filter(a -> !a.path("retrieved").asBoolean())
                .forEach(a -> messages.add("not retrieved: " + a.path("url").asText()));

        return new StageResult(OUTPUT_DIR, ExitCode.SUCCESS,
                retrieved.size() + " document(s) pinned for " + edgesNeedingDocs + " edge(s); "
                        + edgesWithGuide + " official migration guide(s)",
                messages, artifacts, hash);
    }

    private ObjectNode renderAttempt(String url, Source source, String from, String to,
                                     Optional<DocumentationPort.DocumentRef> ref) {
        ObjectNode node = Json.obj();
        node.put("url", url);
        node.put("publisher", source.publisher());
        node.put("component", source.component());
        node.put("trust_level", source.trustLevel().name());
        node.put("source_version", from);
        node.put("target_version", to);
        node.put("retrieved", ref.isPresent());
        node.put("document_id", ref.map(DocumentationPort.DocumentRef::documentId).orElse(null));
        node.put("description", source.description());
        return node;
    }

    private static String lineOf(String version) {
        String[] parts = version.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : version;
    }
}
