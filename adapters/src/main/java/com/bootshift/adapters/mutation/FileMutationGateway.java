package com.bootshift.adapters.mutation;

import com.bootshift.core.domain.HarnessException;
import com.bootshift.core.identity.FileRecord;
import com.bootshift.core.identity.FileRegistry;
import com.bootshift.core.identity.FileRole;
import com.bootshift.core.identity.FileStatus;
import com.bootshift.core.ledger.ChangeEvent;
import com.bootshift.core.ledger.ChangeLedger;
import com.bootshift.core.util.Hashing;
import com.bootshift.core.util.Similarity;
import com.bootshift.ports.mutation.MutationPort;
import com.bootshift.ports.scm.ScmPort;
import com.bootshift.ports.transformation.TransformationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The single authorized writer of application source (R13, spec section 25).
 *
 * <p>Every proposal goes through the same ordered pipeline: verify the baseline seal, resolve
 * FILE_ID, verify authorization against the frozen plan and impact scope, capture the before state,
 * apply, detect the identity operation, capture the after state, update the registry, write the
 * patch, append the ledger entry, and checkpoint.
 *
 * <p>Two enforcement mechanisms back the "no bypass" rule. Statically, an ArchUnit rule forbids
 * mutation-capable stages from touching the filesystem write API. Dynamically,
 * {@link #detectBypass()} compares on-disk content against the hashes the registry believes are
 * current, so a write that skipped the gateway is detected even if it came from outside the JVM.
 */
public final class FileMutationGateway implements MutationPort {

    private static final Logger LOG = LoggerFactory.getLogger(FileMutationGateway.class);

    private final Path migrationWorkspace;
    private final Path gitDir;
    private final Path patchStore;
    private final FileRegistry registry;
    private final ChangeLedger ledger;
    private final ScmPort scm;
    private final String runId;
    private final BaselineSealVerifier sealVerifier;

    /** Supplies the baseline seal so the gateway can refuse to run before Agent 04 has sealed. */
    public interface BaselineSealVerifier {
        boolean sealed();

        String sealHash();
    }

    public FileMutationGateway(String runId, Path migrationWorkspace, Path gitDir, Path patchStore,
                               FileRegistry registry, ChangeLedger ledger, ScmPort scm,
                               BaselineSealVerifier sealVerifier) {
        this.runId = runId;
        this.migrationWorkspace = migrationWorkspace;
        this.gitDir = gitDir;
        this.patchStore = patchStore;
        this.registry = registry;
        this.ledger = ledger;
        this.scm = scm;
        this.sealVerifier = sealVerifier;
    }

    @Override
    public BatchOutcome apply(Authorization authorization,
                              List<TransformationPort.ProposedChange> proposals,
                              ChangeEvent.Provider provider) {
        // 1. verify baseline seal
        if (!sealVerifier.sealed()) {
            throw HarnessException.block(
                    "FileMutationGateway refuses to write before the baseline is sealed (R7). "
                            + "No source mutation is legal until Agent 04 has captured and sealed the baseline.");
        }

        List<MutationOutcome> outcomes = new ArrayList<>();
        int applied = 0;
        int rejected = 0;
        int failed = 0;

        if (proposals.size() > authorization.maxFiles() && authorization.maxFiles() > 0) {
            LOG.warn("Proposal batch of {} exceeds the authorized file budget {}",
                    proposals.size(), authorization.maxFiles());
        }

        for (TransformationPort.ProposedChange proposal : proposals) {
            try {
                outcomes.add(applyOne(authorization, proposal, provider));
            } catch (HarnessException e) {
                throw e;
            } catch (RuntimeException e) {
                LOG.error("Mutation failed for {}: {}", proposal.path(), e.toString());
                ChangeEvent event = baseEvent(authorization, proposal, provider)
                        .setStatus(ChangeEvent.Status.REJECTED)
                        .setRejectionReason("Apply failed: " + e.getMessage());
                ledger.append(event);
                outcomes.add(new MutationOutcome(event.getChangeId(), event.getFileId(),
                        ChangeEvent.Status.REJECTED, e.getMessage(), null, null, null));
            }
        }

        for (MutationOutcome outcome : outcomes) {
            if (outcome.status() == ChangeEvent.Status.APPLIED) {
                applied++;
            } else if (outcome.status() == ChangeEvent.Status.REJECTED) {
                rejected++;
            } else {
                failed++;
            }
        }

        // Checkpoint after the fact. If this fails the changes are already applied and already in the
        // ledger, so throwing would leave a mutated workspace with no record of why. The failure is
        // recorded as its own ledger event instead, because losing the ability to roll back is a
        // fact the operator must see rather than an exception that erases the work.
        String checkpoint = null;
        if (applied > 0 && gitDir != null) {
            try {
                ScmPort.Checkpoint created = scm.checkpoint(migrationWorkspace, gitDir,
                        "mig/" + runId + "/" + safe(authorization.edgeId()) + "/transformed",
                        "Edge " + authorization.edgeId() + " applied " + applied + " change(s) via "
                                + authorization.agent() + recipeSuffix(proposals));
                checkpoint = created.commitSha();
            } catch (RuntimeException e) {
                LOG.error("Checkpoint failed after applying {} change(s): {}", applied, e.toString());
                ledger.append(new ChangeEvent()
                        .setRunId(runId)
                        .setEdgeId(authorization.edgeId())
                        .setAgent(authorization.agent())
                        .setOperation(ChangeEvent.Operation.MODIFY)
                        .setProvider(new ChangeEvent.Provider("CHECKPOINT", "internal-git", "1.0"))
                        .setStatus(ChangeEvent.Status.FAILED_VALIDATION)
                        .setRejectionReason("Checkpoint creation failed after " + applied
                                + " applied change(s); deterministic rollback is unavailable for this "
                                + "batch: " + e.getMessage()));
                checkpoint = "CHECKPOINT_FAILED";
            }
        }
        return new BatchOutcome(outcomes, applied, rejected, failed, checkpoint);
    }

    /**
     * Names the recipe in the commit message when a batch is one recipe's work.
     *
     * <p>Recipes are applied one batch at a time - a transformer reads the file from disk, so
     * running them all against the pre-edge tree and writing at the end would let the last write for
     * a given file discard the others. One commit per recipe is the consequence, and a history of
     * identical messages would waste it.
     */
    private static String recipeSuffix(List<TransformationPort.ProposedChange> proposals) {
        List<String> recipes = proposals.stream()
                .map(TransformationPort.ProposedChange::recipeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return recipes.size() == 1 ? " [" + recipes.get(0) + "]" : "";
    }

    private MutationOutcome applyOne(Authorization authorization,
                                     TransformationPort.ProposedChange proposal,
                                     ChangeEvent.Provider provider) {
        String relativePath = FileRegistry.normalize(proposal.path());
        Path absolute = resolveInsideWorkspace(relativePath);

        // 2. resolve FILE_ID
        Optional<FileRecord> existing = registry.byPath(relativePath);
        String operation = proposal.operation() == null ? "MODIFY" : proposal.operation().toUpperCase(java.util.Locale.ROOT);

        // 3. verify authorization
        String denial = authorize(authorization, existing.orElse(null), relativePath, operation, proposal);
        if (denial != null) {
            ChangeEvent event = baseEvent(authorization, proposal, provider)
                    .setFileId(existing.map(FileRecord::getFileId).orElse(null))
                    .setStatus(ChangeEvent.Status.REJECTED)
                    .setRejectionReason(denial);
            ledger.append(event);
            return new MutationOutcome(event.getChangeId(), event.getFileId(),
                    ChangeEvent.Status.REJECTED, denial, null, null, null);
        }

        // 4. capture before state
        String beforeContent = Files.isRegularFile(absolute) ? read(absolute) : null;
        String beforeHash = beforeContent == null ? null : Hashing.sha256(beforeContent);

        // 4b. staleness. A transformer computes its replacement content from the file as it
        // stood when the transformer ran. If the tree has moved on since - because an earlier
        // proposal in this run already rewrote the same file - then applying this content would
        // silently discard that earlier, already-recorded change. Reject instead: a lost change
        // that the ledger still reports as APPLIED is the one failure the ledger cannot survive.
        String declaredBase = proposal.attributes() == null
                ? null : proposal.attributes().get("base_hash");
        if (declaredBase != null && beforeHash != null && !declaredBase.equals(beforeHash)) {
            return reject(authorization, proposal, provider,
                    existing.map(FileRecord::getFileId).orElse(null),
                    "STALE_BASE_CONTENT: this proposal was computed from " + declaredBase
                            + " but " + relativePath + " now hashes to " + beforeHash
                            + ". Applying it would discard an already-applied change.");
        }
        String changeId = ledger.nextChangeId();

        ChangeEvent.Operation resolvedOperation;
        String pathAfter = relativePath;
        String afterContent;
        String afterHash;
        String fileId;

        switch (operation) {
            case "DELETE" -> {
                if (existing.isEmpty()) {
                    return reject(authorization, proposal, provider, null,
                            "Cannot delete an unregistered path: " + relativePath);
                }
                deleteFile(absolute);
                fileId = existing.get().getFileId();
                registry.recordDelete(fileId, changeId);
                resolvedOperation = ChangeEvent.Operation.DELETE;
                afterContent = null;
                afterHash = null;
                pathAfter = null;
            }
            case "CREATE" -> {
                if (existing.isPresent() && existing.get().getStatus() == FileStatus.ACTIVE) {
                    return reject(authorization, proposal, provider, existing.get().getFileId(),
                            "Refusing to CREATE over an existing registered file: " + relativePath);
                }
                afterContent = proposal.newContent() == null ? "" : proposal.newContent();
                write(absolute, afterContent);
                afterHash = Hashing.sha256(afterContent);
                FileRegistry.ObservedFile observed = new FileRegistry.ObservedFile(
                        moduleOf(relativePath), relativePath, afterHash, roleOf(relativePath),
                        afterContent.length(), afterContent);
                String splitParent = proposal.attributes() == null ? null : proposal.attributes().get("split_from");
                if (splitParent != null) {
                    fileId = registry.recordSplit(splitParent, observed, changeId).fileId();
                } else {
                    fileId = registry.allocate(observed).fileId();
                    registry.byId(fileId).ifPresent(r -> r.setCreatedByChange(changeId));
                }
                resolvedOperation = ChangeEvent.Operation.CREATE;
            }
            case "RENAME" -> {
                if (existing.isEmpty()) {
                    return reject(authorization, proposal, provider, null,
                            "Cannot rename an unregistered path: " + relativePath);
                }
                pathAfter = FileRegistry.normalize(proposal.newPath());
                Path target = resolveInsideWorkspace(pathAfter);
                afterContent = proposal.newContent() != null ? proposal.newContent() : beforeContent;
                write(target, afterContent == null ? "" : afterContent);
                if (!target.equals(absolute)) {
                    deleteFile(absolute);
                }
                afterHash = Hashing.sha256(afterContent == null ? "" : afterContent);
                fileId = existing.get().getFileId();
                registry.appendVersion(fileId, changeId, pathAfter, afterHash, proposal.rationale());
                registry.byId(fileId).ifPresent(r -> {
                    r.setRenameSource(com.bootshift.core.identity.RenameSource.PROVIDER);
                    r.setRenameConfidence(1.0);
                });
                resolvedOperation = ChangeEvent.Operation.RENAME;
            }
            case "MERGE" -> {
                if (existing.isEmpty()) {
                    return reject(authorization, proposal, provider, null,
                            "Cannot merge into an unregistered path: " + relativePath);
                }
                afterContent = proposal.newContent();
                write(absolute, afterContent);
                afterHash = Hashing.sha256(afterContent);
                fileId = existing.get().getFileId();
                registry.appendVersion(fileId, changeId, relativePath, afterHash, proposal.rationale());
                String sources = proposal.attributes() == null ? null : proposal.attributes().get("merged_from");
                if (sources != null) {
                    for (String source : sources.split(",")) {
                        registry.byPath(source.trim()).ifPresent(r -> {
                            registry.recordMerge(r.getFileId(), existing.get().getFileId(), changeId);
                            deleteFile(resolveInsideWorkspace(r.getCurrentPath()));
                        });
                    }
                }
                resolvedOperation = ChangeEvent.Operation.MERGE;
            }
            default -> {
                if (existing.isEmpty()) {
                    return reject(authorization, proposal, provider, null,
                            "Cannot modify an unregistered path: " + relativePath);
                }
                afterContent = proposal.newContent();
                if (afterContent == null) {
                    return reject(authorization, proposal, provider, existing.get().getFileId(),
                            "MODIFY proposal carried no content");
                }
                afterHash = Hashing.sha256(afterContent);
                if (afterHash.equals(beforeHash)) {
                    ChangeEvent noop = baseEvent(authorization, proposal, provider)
                            .setFileId(existing.get().getFileId())
                            .setOperation(ChangeEvent.Operation.MODIFY)
                            .setPathBefore(relativePath)
                            .setPathAfter(relativePath)
                            .setBeforeSha256(beforeHash)
                            .setAfterSha256(afterHash)
                            .setStatus(ChangeEvent.Status.REJECTED)
                            .setRejectionReason("No-op: proposed content is identical to current content");
                    ledger.append(noop);
                    return new MutationOutcome(noop.getChangeId(), noop.getFileId(),
                            ChangeEvent.Status.REJECTED, noop.getRejectionReason(), beforeHash, afterHash, null);
                }
                write(absolute, afterContent);
                fileId = existing.get().getFileId();
                registry.appendVersion(fileId, changeId, relativePath, afterHash, proposal.rationale());
                resolvedOperation = ChangeEvent.Operation.MODIFY;
            }
        }

        // 8. identify changed symbols, 10. patch artifact
        List<String> changedSymbols = existing.map(r -> new ArrayList<>(r.getSymbolIds()))
                .orElseGet(ArrayList::new);
        String patchRef = writePatch(changeId, relativePath, pathAfter, beforeContent, afterContent);

        // 11-12. ChangeEvent and ledger append
        ChangeEvent event = baseEvent(authorization, proposal, provider)
                .setChangeId(changeId)
                .setFileId(fileId)
                .setOperation(resolvedOperation)
                .setPathBefore(beforeContent == null && resolvedOperation == ChangeEvent.Operation.CREATE
                        ? null : relativePath)
                .setPathAfter(pathAfter)
                .setBeforeSha256(beforeHash)
                .setAfterSha256(afterHash)
                .setSymbolsChanged(changedSymbols)
                .setPatchRef(patchRef)
                .setStatus(ChangeEvent.Status.APPLIED);
        if (proposal.attributes() != null && proposal.attributes().containsKey("split_from")) {
            event.setSplitFrom(proposal.attributes().get("split_from"));
        }
        ledger.append(event);
        if (registry.byId(fileId).isPresent()) {
            registry.primeContent(fileId, afterContent == null ? "" : afterContent);
        }
        return new MutationOutcome(event.getChangeId(), fileId, ChangeEvent.Status.APPLIED, null,
                beforeHash, afterHash, patchRef);
    }

    private MutationOutcome reject(Authorization authorization, TransformationPort.ProposedChange proposal,
                                   ChangeEvent.Provider provider, String fileId, String reason) {
        ChangeEvent event = baseEvent(authorization, proposal, provider)
                .setFileId(fileId)
                .setStatus(ChangeEvent.Status.REJECTED)
                .setRejectionReason(reason);
        ledger.append(event);
        return new MutationOutcome(event.getChangeId(), fileId, ChangeEvent.Status.REJECTED, reason,
                null, null, null);
    }

    private ChangeEvent baseEvent(Authorization authorization, TransformationPort.ProposedChange proposal,
                                  ChangeEvent.Provider provider) {
        return new ChangeEvent()
                .setRunId(runId)
                .setEdgeId(authorization.edgeId())
                .setAgent(authorization.agent())
                .setProvider(provider)
                .setRecipeId(proposal.recipeId())
                .setKnowledgeRefs(proposal.knowledgeRefs() == null ? List.of() : proposal.knowledgeRefs())
                .setImpactRefs(proposal.impactRefs() == null ? List.of() : proposal.impactRefs())
                .setPathBefore(proposal.path());
    }

    /**
     * Authorization check. A change is legal only when the target file is in the frozen scope for the
     * edge, and only when the operation class is permitted.
     */
    private String authorize(Authorization authorization, FileRecord record, String path,
                             String operation, TransformationPort.ProposedChange proposal) {
        if ("CREATE".equals(operation) && !authorization.allowCreate()) {
            return "Authorization does not permit file creation for this edge";
        }
        if ("DELETE".equals(operation) && !authorization.allowDelete()) {
            return "Authorization does not permit file deletion for this edge";
        }
        if ("RENAME".equals(operation) && !authorization.allowRename()) {
            return "Authorization does not permit renames for this edge";
        }
        boolean fileAuthorized = record != null
                && authorization.authorizedFileIds().contains(record.getFileId());
        boolean prefixAuthorized = authorization.authorizedPathPrefixes().stream()
                .anyMatch(path::startsWith);
        if (!fileAuthorized && !prefixAuthorized) {
            return "Path " + path + " is outside the authorized scope of edge " + authorization.edgeId()
                    + " (" + authorization.authorizedFileIds().size() + " authorized files, "
                    + authorization.authorizedPathPrefixes().size() + " authorized prefixes)";
        }
        if (authorization.maxChangedLines() > 0 && proposal.newContent() != null) {
            int changed = estimateChangedLines(record, proposal);
            if (changed > authorization.maxChangedLines()) {
                return "Patch changes " + changed + " lines, exceeding the authorized budget of "
                        + authorization.maxChangedLines();
            }
        }
        return null;
    }

    private int estimateChangedLines(FileRecord record, TransformationPort.ProposedChange proposal) {
        if (record == null) {
            return proposal.newContent().split("\n").length;
        }
        Path current = resolveInsideWorkspace(record.getCurrentPath());
        if (!Files.isRegularFile(current)) {
            return proposal.newContent().split("\n").length;
        }
        List<String> before = List.of(read(current).split("\n"));
        List<String> after = List.of(proposal.newContent().split("\n"));
        int max = Math.max(before.size(), after.size());
        int changed = 0;
        for (int i = 0; i < max; i++) {
            String left = i < before.size() ? before.get(i) : null;
            String right = i < after.size() ? after.get(i) : null;
            if (left == null || !left.equals(right)) {
                changed++;
            }
        }
        return changed;
    }

    /** Path traversal and symlink escape prevention (spec 48). */
    /**
     * Resolves a repository-relative path and refuses anything that leaves the workspace.
     *
     * <p>Two checks, because one is not enough. {@code normalize()} plus {@code startsWith} stops
     * {@code ../} traversal but says nothing about symlinks: a link inside the workspace pointing at
     * {@code /etc/passwd} has a path that is entirely inside the workspace, and writing through it
     * lands outside. The repository under analysis is untrusted input and can contain such a link,
     * so the resolved path is checked too - by {@code toRealPath}, which follows links, against the
     * real workspace root.
     *
     * <p>The target itself may legitimately not exist yet (a CREATE), so the nearest existing
     * ancestor is what gets resolved. A symlinked target is refused outright: writing through a link
     * would record a change against a file the registry does not describe.
     */
    private Path resolveInsideWorkspace(String relativePath) {
        Path root = migrationWorkspace.normalize();
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw HarnessException.refusal(
                    "Refusing to write outside the migration workspace: " + relativePath);
        }
        if (Files.isSymbolicLink(candidate)) {
            throw HarnessException.refusal(
                    "Refusing to write through a symbolic link: " + relativePath);
        }
        try {
            Path realRoot = root.toRealPath();
            Path existing = candidate;
            while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
                existing = existing.getParent();
            }
            if (existing == null) {
                throw HarnessException.refusal(
                        "Refusing to write to a path with no existing ancestor: " + relativePath);
            }
            if (!existing.toRealPath().startsWith(realRoot)) {
                throw HarnessException.refusal(
                        "Refusing to write outside the migration workspace via a symbolic link: "
                                + relativePath);
            }
        } catch (IOException e) {
            throw HarnessException.refusal(
                    "Cannot verify that " + relativePath + " stays inside the migration workspace: "
                            + e.getMessage());
        }
        return candidate;
    }

    private String writePatch(String changeId, String pathBefore, String pathAfter,
                              String before, String after) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- a/").append(pathBefore == null ? "/dev/null" : pathBefore).append('\n');
        sb.append("+++ b/").append(pathAfter == null ? "/dev/null" : pathAfter).append('\n');
        List<String> beforeLines = before == null ? List.of() : List.of(before.split("\n", -1));
        List<String> afterLines = after == null ? List.of() : List.of(after.split("\n", -1));
        int max = Math.max(beforeLines.size(), afterLines.size());
        for (int i = 0; i < max; i++) {
            String left = i < beforeLines.size() ? beforeLines.get(i) : null;
            String right = i < afterLines.size() ? afterLines.get(i) : null;
            if (left != null && left.equals(right)) {
                continue;
            }
            if (left != null) {
                sb.append("-").append(left).append('\n');
            }
            if (right != null) {
                sb.append("+").append(right).append('\n');
            }
        }
        Path patchFile = patchStore.resolve(changeId + ".patch");
        try {
            Files.createDirectories(patchStore);
            Files.writeString(patchFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write patch " + patchFile, e);
        }
        return patchFile.getFileName().toString();
    }

    @Override
    public void revertTo(String checkpointName, String reason) {
        scm.rollbackTo(migrationWorkspace, gitDir, checkpointName);
        for (ChangeLedger.Entry entry : ledger.entries()) {
            if (entry.event().getStatus() == ChangeEvent.Status.APPLIED) {
                ChangeEvent reverted = new ChangeEvent()
                        .setRunId(runId)
                        .setEdgeId(entry.event().getEdgeId())
                        .setFileId(entry.event().getFileId())
                        .setOperation(entry.event().getOperation())
                        .setPathBefore(entry.event().getPathAfter())
                        .setPathAfter(entry.event().getPathBefore())
                        .setBeforeSha256(entry.event().getAfterSha256())
                        .setAfterSha256(entry.event().getBeforeSha256())
                        .setAgent("mutation-gateway")
                        .setProvider(new ChangeEvent.Provider("REVERT", "checkpoint-rollback", "1.0"))
                        .setStatus(ChangeEvent.Status.REVERTED)
                        .setRejectionReason(reason);
                ledger.append(reverted);
            }
        }
    }

    /**
     * Detects writes that bypassed the gateway.
     *
     * <p>The registry holds the hash the gateway last wrote for each active file. Any difference on
     * disk means something else wrote into the migration workspace.
     */
    @Override
    public List<String> detectBypass() {
        List<String> violations = new ArrayList<>();
        Map<String, String> expected = new LinkedHashMap<>();
        for (FileRecord record : registry.active()) {
            expected.put(record.getCurrentPath(), record.getCurrentSha256());
        }
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Path file = migrationWorkspace.resolve(entry.getKey());
            if (!Files.isRegularFile(file)) {
                violations.add("MISSING: " + entry.getKey()
                        + " is registered as active but absent from the migration workspace");
                continue;
            }
            try {
                String actual = Hashing.sha256File(file);
                if (!actual.equals(entry.getValue())) {
                    violations.add("BYPASS: " + entry.getKey() + " content hash " + actual
                            + " does not match the gateway-recorded hash " + entry.getValue());
                }
            } catch (IOException e) {
                violations.add("UNREADABLE: " + entry.getKey() + " - " + e.getMessage());
            }
        }
        try (var stream = Files.walk(migrationWorkspace)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains(".git"))
                    .filter(p -> !p.toString().contains("target"))
                    .sorted()
                    .forEach(p -> {
                        String relative = FileRegistry.normalize(
                                migrationWorkspace.relativize(p).toString());
                        if (!expected.containsKey(relative) && registry.byPath(relative).isEmpty()) {
                            violations.add("UNTRACKED: " + relative
                                    + " exists in the migration workspace but has no registered identity");
                        }
                    });
        } catch (IOException e) {
            violations.add("SCAN_FAILED: " + e.getMessage());
        }
        return violations;
    }

    /** Highest-similarity descendant selection used when a transformation splits a file (spec 11.4). */
    public static Optional<String> selectSplitSurvivor(String originalContent, Map<String, String> candidates) {
        return candidates.entrySet().stream()
                .max(Comparator.comparingDouble(e -> Similarity.containment(e.getValue(), originalContent)))
                .map(Map.Entry::getKey);
    }

    private void write(Path target, String content) {
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + target, e);
        }
    }

    private void deleteFile(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + target, e);
        }
    }

    private String read(Path target) {
        try {
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + target, e);
        }
    }

    private static String moduleOf(String path) {
        int slash = path.indexOf('/');
        return slash < 0 ? "." : path.substring(0, slash);
    }

    private static FileRole roleOf(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith("pom.xml")) {
            return FileRole.MAVEN_BUILD;
        }
        if (lower.endsWith(".java")) {
            return lower.contains("/src/test/") ? FileRole.JAVA_TEST : FileRole.JAVA_MAIN;
        }
        if (lower.endsWith(".properties")) {
            return FileRole.CONFIG_PROPERTIES;
        }
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
            return FileRole.CONFIG_YAML;
        }
        if (lower.endsWith(".xml")) {
            return FileRole.CONFIG_XML;
        }
        return FileRole.RESOURCE;
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9._-]", "-");
    }
}
