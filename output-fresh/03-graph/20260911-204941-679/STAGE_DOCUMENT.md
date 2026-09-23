# Stage 03-graph — ApplicationGraphStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M293Q9B0ZHX41A7WA4JRKYRX` |
| Edge | Not an edge-scoped stage |
| Status | **SUCCESS** |
| Start | 2026-09-11T20:49:37.632729400Z |
| End | 2026-09-11T20:49:41.971161700Z |
| Duration | 4.3 s |

> 839 nodes / 1852 edges, 239 symbols, attribution 0.6858, content hash 5b764c29af1e

---

## 1. Purpose

Build and verify the type-aware, multi-view static application graph

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `BUILD_RESOLVED` | `GRAPH_VERIFIED` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:BUILD_RESOLVED` | satisfied | Proven by 02-build/build-model.json | — |
| `artifact:01-inventory/file-registry.json` | satisfied | Published and readable | — |
| `artifact:02-build/build-model.json` | satisfied | Published and readable | — |
| `artifact:02-build/dependency-model.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `01-inventory/file-registry.json` | resolved | `886d68b23018a84f…` |
| `02-build/build-model.json` | resolved | `13f63a0f2464ab7d…` |
| `02-build/dependency-model.json` | resolved | `056a8e2147f6e3a9…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `GRF-001` | Load registry and build model | The graph is built over identities and modules that already exist |
| `GRF-002` | Select Java source roots per module | Kotlin, Groovy and Scala roots are excluded rather than mis-parsed |
| `GRF-003` | Analyse sources with symbol solving | Type resolution is what makes an edge a fact rather than a name match |
| `GRF-004` | Read configuration files | Configuration keys become graph nodes so property impact is traceable |
| `GRF-005` | Build nodes and edges | Beans, endpoints, entities, repositories, dependencies, configuration |
| `GRF-006` | Verify graph completeness and attribution | A partial graph must be declared partial, not published as complete |
| `GRF-007` | Publish the graph and verification report | Structural and content hashes are recorded separately |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `GRF-001` | SUCCESS | 206 ms | Registry and build model loaded |
| `GRF-002` | SUCCESS | 3.6 s | All source roots are Java |
| `GRF-003` | SUCCESS | 3.6 s | Symbol solving complete |
| `GRF-004` | SUCCESS | 6 ms | 10 configuration file(s) read |
| `GRF-005` | SUCCESS | 200 ms | 839 node(s), 1852 edge(s) |
| `GRF-006` | SUCCESS | 21 ms | Graph verified as complete |
| `GRF-007` | SUCCESS | 275 ms | Published and pointer advanced |

## 8. Tools and commands executed

This stage launched no external processes.

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
| `application-graph.json` | `f7a223fda7e01029…` |
| `file-registry.json` | `74125cbe332c476c…` |
| `symbol-registry.json` | `360e63f5b9169ad4…` |
| `module-graph.json` | `c60ef5d70ac6bfcb…` |
| `file-graph.json` | `1cadab052da945ef…` |
| `symbol-graph.json` | `8359ff6a4a0f9c26…` |
| `dependency-graph.json` | `20d6dff47d583aef…` |
| `spring-graph.json` | `df9f2cc42ca167fa…` |
| `configuration-graph.json` | `6fd9c79f94df7983…` |
| `persistence-graph.json` | `7db6f51f0e9f0477…` |
| `endpoint-graph.json` | `2d0772eec66fa564…` |
| `test-graph.json` | `026b07d4317c4206…` |
| `integration-graph.json` | `34c843991cc9fede…` |
| `graph-summary.json` | `cbd6ec30fd9a77ce…` |
| `graph-issues.json` | `59340da0b3d64389…` |
| `graph-verification-report.json` | `fa9af077b5444582…` |
| `graph-verification-report.md` | `d0117c5c7498cb73…` |
| `manifest.json` | `d7d63e36e2cae31d…` |

Attempt directory: `20260911-204941-679`

## 21. Result

**SUCCESS** — 839 nodes / 1852 edges, 239 symbols, attribution 0.6858, content hash 5b764c29af1e

The stage did what it declared it would do.

## 22. Next action

Run: bootshift baseline

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M293Q9B0ZHX41A7WA4JRKYRX` |
| Stage id | `03-graph` |
| Edge id | — |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `4b8361feb2e0a5a83dcfdae627ba483034cc3fd34a12e7a9f044c95e6914e253` |
| Primary artifact hash | `-1830851390` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
