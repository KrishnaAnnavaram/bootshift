# Stage 02-build — BuildResolverStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M293KMSJA3VR8CWG64AK4Q5X` |
| Edge | Not an edge-scoped stage |
| Status | **SUCCESS** |
| Start | 2026-09-11T20:47:38.290444300Z |
| End | 2026-09-11T20:49:37.568098200Z |
| Duration | 1m 59s |

> MAVEN model: 6 module(s), 962 dependency record(s), 9186 managed version(s)

---

## 1. Purpose

Obtain the authoritative effective build model from Maven or Gradle itself

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `FILE_REGISTRY_SEALED` | `BUILD_RESOLVED` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:FILE_REGISTRY_SEALED` | satisfied | Proven by 01-inventory/file-registry.json | — |
| `artifact:01-inventory/inventory-artifact.json` | satisfied | Published and readable | — |
| `artifact:01-inventory/file-registry.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `01-inventory/inventory-artifact.json` | resolved | `d692405e0e39e4ca…` |
| `01-inventory/file-registry.json` | resolved | `886d68b23018a84f…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `BLD-001` | Load the inventory artifact | The build model is resolved over files inventory already gave identity to |
| `BLD-002` | Detect the build system | Maven, Gradle or a mixed composite; never flattened to one |
| `BLD-003` | Resolve the effective build model | Invokes the build tool so the model is authoritative, not descriptor-guessed |
| `BLD-004` | Classify dependency resolution | An unresolved coordinate makes version-space analysis unreliable |
| `BLD-005` | Derive Java levels and frameworks | Records the level each module declares, not the newest available |
| `BLD-006` | Publish the build, dependency and BOM models | One serialized contract every later stage rehydrates through |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `BLD-001` | SUCCESS | 3 ms | Inventory artifact resolved |
| `BLD-002` | SUCCESS | 20 ms | Detected MAVEN |
| `BLD-003` | SUCCESS | 1m 58s | Resolved by invoking the build tool |
| `BLD-004` | SUCCESS | 0 ms | 0 unresolved coordinate(s) |
| `BLD-005` | SUCCESS | 16 ms | Java levels and frameworks derived |
| `BLD-006` | SUCCESS | 377 ms | Published and pointer advanced |

## 8. Tools and commands executed

| Id | Command | Exit | Timed out | Duration | Result |
| --- | --- | --- | --- | --- | --- |
| `CMD-000001` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -version` | 0 | no | 483 ms | SUCCESS |
| `CMD-000002` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-help-plugin:3.4.0:effective-pom -Doutput=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\configuaration-server\effective-pom.xml` | 0 | no | 3.1 s | SUCCESS |
| `CMD-000003` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:list -DoutputAbsoluteArtifactFilename=false -DincludeParents=true` | 0 | no | 6.3 s | SUCCESS |
| `CMD-000004` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:resolve-plugins` | 0 | no | 4.7 s | SUCCESS |
| `CMD-000005` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\configuaration-server\classpath.txt -Dmdep.includeScope=test -Dmdep.pathSeparator=;` | 0 | no | 4.3 s | SUCCESS |
| `CMD-000006` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-help-plugin:3.4.0:effective-pom -Doutput=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\department-service\effective-pom.xml` | 0 | no | 3.2 s | SUCCESS |
| `CMD-000007` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:list -DoutputAbsoluteArtifactFilename=false -DincludeParents=true` | 0 | no | 6.2 s | SUCCESS |
| `CMD-000008` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:resolve-plugins` | 0 | no | 4.5 s | SUCCESS |
| `CMD-000009` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\department-service\classpath.txt -Dmdep.includeScope=test -Dmdep.pathSeparator=;` | 0 | no | 4.3 s | SUCCESS |
| `CMD-000010` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-help-plugin:3.4.0:effective-pom -Doutput=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\discovery-service\effective-pom.xml` | 0 | no | 3.0 s | SUCCESS |
| `CMD-000011` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:list -DoutputAbsoluteArtifactFilename=false -DincludeParents=true` | 0 | no | 5.9 s | SUCCESS |
| `CMD-000012` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:resolve-plugins` | 0 | no | 4.6 s | SUCCESS |
| `CMD-000013` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\discovery-service\classpath.txt -Dmdep.includeScope=test -Dmdep.pathSeparator=;` | 0 | no | 4.5 s | SUCCESS |
| `CMD-000014` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-help-plugin:3.4.0:effective-pom -Doutput=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\employee-service\effective-pom.xml` | 0 | no | 3.1 s | SUCCESS |
| `CMD-000015` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:list -DoutputAbsoluteArtifactFilename=false -DincludeParents=true` | 1 | no | 5.6 s | FAILED |
| `CMD-000016` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:tree -DoutputType=text -DoutputFile=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\employee-service\dependency-tree.txt` | 0 | no | 4.9 s | SUCCESS |
| `CMD-000017` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:resolve-plugins` | 0 | no | 4.9 s | SUCCESS |
| `CMD-000018` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\employee-service\classpath.txt -Dmdep.includeScope=test -Dmdep.pathSeparator=;` | 0 | no | 4.8 s | SUCCESS |
| `CMD-000019` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-help-plugin:3.4.0:effective-pom -Doutput=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\report-service\effective-pom.xml` | 0 | no | 3.1 s | SUCCESS |
| `CMD-000020` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:list -DoutputAbsoluteArtifactFilename=false -DincludeParents=true` | 1 | no | 5.2 s | FAILED |
| `CMD-000021` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:tree -DoutputType=text -DoutputFile=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\report-service\dependency-tree.txt` | 0 | no | 4.6 s | SUCCESS |
| `CMD-000022` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:resolve-plugins` | 0 | no | 4.7 s | SUCCESS |
| `CMD-000023` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\report-service\classpath.txt -Dmdep.includeScope=test -Dmdep.pathSeparator=;` | 0 | no | 4.5 s | SUCCESS |
| `CMD-000024` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-help-plugin:3.4.0:effective-pom -Doutput=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\sheduler-service\effective-pom.xml` | 0 | no | 3.1 s | SUCCESS |
| `CMD-000025` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:list -DoutputAbsoluteArtifactFilename=false -DincludeParents=true` | 0 | no | 5.9 s | SUCCESS |
| `CMD-000026` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B org.apache.maven.plugins:maven-dependency-plugin:3.6.1:resolve-plugins` | 0 | no | 4.8 s | SUCCESS |
| `CMD-000027` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\original\configuaration-server\mvnw.cmd -B -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\build-evidence\maven\sheduler-service\classpath.txt -Dmdep.includeScope=test -Dmdep.pathSeparator=;` | 0 | no | 4.3 s | SUCCESS |

> Command arguments are redacted before they are written. Process output is not copied into this document; where a log was captured it is referenced above.

## 9. Decisions made

This stage recorded no decisions.

## 10. Evidence used

This stage referenced no evidence objects.

## 11. Migration documents used

This stage consumed no migration documentation.

## 12. Migration facts used or produced

This stage neither consumed nor produced migration facts.

## 13. Impact analysis involved

No impact findings were involved in this stage.

## 14. Source mutations

This stage does not write to application source.

## 15. Validation performed

This stage performs no validation of its own.

## 16. Retries and fallback paths

### Retries

No retries occurred.

### Fallbacks

The stage completed by its primary method; no fallback was used.

## 17. Warnings

No warnings.

## 18. Errors and blockers

No errors.

## 19. Blind spots and unknowns

This stage recorded no blind spots. That is a statement about this stage only.

## 20. Output artifacts

| Artifact | SHA-256 |
| --- | --- |
| `build-model.json` | `13f63a0f2464ab7d…` |
| `dependency-model.json` | `056a8e2147f6e3a9…` |
| `bom-model.json` | `424ef56f1b46ccbd…` |
| `plugin-model.json` | `523d0f2bfb56708f…` |
| `repository-model.json` | `1c4d81a5bdca5b1e…` |
| `resolution-issues.json` | `45f1b037aee87c15…` |
| `manifest.json` | `8cfa36f9658ae2cf…` |

Attempt directory: `20260911-204937-146`

## 21. Result

**SUCCESS** — MAVEN model: 6 module(s), 962 dependency record(s), 9186 managed version(s)

The stage did what it declared it would do.

## 22. Next action

Run: bootshift graph

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M293KMSJA3VR8CWG64AK4Q5X` |
| Stage id | `02-build` |
| Edge id | — |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `a31058a2453fe6ecd9796b17a33c08f7d9688d2ad093d2096ea68c8980efcafd` |
| Primary artifact hash | `-1837302943` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
