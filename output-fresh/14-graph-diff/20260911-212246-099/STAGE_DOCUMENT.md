# Stage 14-graph-diff — GraphDiffStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M295KXC5BP978Z6FF8KHQ8WD` |
| Edge | `EDGE-2-PATCH` |
| Status | **SUCCESS** |
| Start | 2026-09-11T21:22:44.229246200Z |
| End | 2026-09-11T21:22:46.367571900Z |
| Duration | 2.1 s |

> Edge EDGE-2-PATCH graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK

---

## 1. Purpose

Rebuild the static graph and assert that every change stayed inside authorized scope

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `EDGE_COMPLETE` | `EDGE_COMPLETE` |

The run state did not advance in this attempt.

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
| `13-build-repair/build-report.json` | resolved | `5fa2deb696f46e20…` |
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
| `SCP-001` | SUCCESS | 1.9 s | Upstream inputs resolved |
| `SCP-002` | SUCCESS | 268 ms | Completed; the stage reported SUCCESS |
| `SCP-003` | SUCCESS | 2 ms | Published and pointer advanced |

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
| `application-graph-current.json` | `010f443728c2c103…` |
| `graph-diff.json` | `d44e112ef1824183…` |
| `scope-assertion.json` | `dc85a064145d8f57…` |
| `last-good-graph.json` | `3cf58380051e089e…` |
| `manifest.json` | `d3f9d2b561ee32fa…` |

Attempt directory: `20260911-212246-099`

## 21. Result

**SUCCESS** — Edge EDGE-2-PATCH graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK

The stage did what it declared it would do.

## 22. Next action

Run: bootshift validate --edge <edge>

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M295KXC5BP978Z6FF8KHQ8WD` |
| Stage id | `14-graph-diff` |
| Edge id | `EDGE-2-PATCH` |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `e062a29a7aa1f67a80e45dcf3dabb588c2bbb78c61c2a829361166d91651fd5d` |
| Primary artifact hash | `-1014085234` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
