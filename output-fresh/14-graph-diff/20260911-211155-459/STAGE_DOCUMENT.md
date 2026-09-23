# Stage 14-graph-diff — GraphDiffStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M294ZYT6ZPC69CXAFB04E1CN` |
| Edge | `EDGE-1-PREP-TEST` |
| Status | **SUCCESS** |
| Start | 2026-09-11T21:11:50.342331300Z |
| End | 2026-09-11T21:11:56.057371100Z |
| Duration | 5.7 s |

> Edge EDGE-1-PREP-TEST graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK

---

## 1. Purpose

Rebuild the static graph and assert that every change stayed inside authorized scope

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `EDGE_COMPILED` | `EDGE_SCOPE_VERIFIED` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:EDGE_COMPILED` | satisfied | No artifact proof declared for this state | — |
| `artifact:13-build-repair/build-report.json` | satisfied | Published and readable | — |
| `artifact:11-plan/edge-plan.json` | satisfied | Published and readable | — |
| `artifact:02-build/build-model.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `13-build-repair/build-report.json` | resolved | `daa2906541d79943…` |
| `11-plan/edge-plan.json` | resolved | `0d78110cfed9c131…` |
| `02-build/build-model.json` | resolved | `13f63a0f2464ab7d…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `SCP-001` | Rebuild the application graph after mutation | The post-migration graph is rebuilt, never patched |
| `SCP-002` | Compare graphs and verify scope | Changed files are checked against the authorized set; anything outside it is a scope violation, not an incidental edit |
| `SCP-003` | Publish the graph diff and scope report | Structural change is separated from behavioural change |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `SCP-001` | SUCCESS | 5.1 s | Upstream inputs resolved |
| `SCP-002` | SUCCESS | 597 ms | Completed; the stage reported SUCCESS |
| `SCP-003` | SUCCESS | 10 ms | Published and pointer advanced |

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
| `application-graph-current.json` | `d972472c65ea38d5…` |
| `graph-diff.json` | `8897cc8a091e0583…` |
| `scope-assertion.json` | `ec5a61d71265656d…` |
| `last-good-graph.json` | `c5c1964bf24691a8…` |
| `manifest.json` | `7bb3e2dc328bab99…` |

Attempt directory: `20260911-211155-459`

## 21. Result

**SUCCESS** — Edge EDGE-1-PREP-TEST graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK

The stage did what it declared it would do.

## 22. Next action

Run: bootshift validate --edge <edge>

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M294ZYT6ZPC69CXAFB04E1CN` |
| Stage id | `14-graph-diff` |
| Edge id | `EDGE-1-PREP-TEST` |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `f1e161412e792a852993373902db0e8f920f74c792fb63bfc3c4c76b459d2513` |
| Primary artifact hash | `1887175783` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
