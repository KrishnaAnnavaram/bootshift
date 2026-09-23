# Stage 13-build-repair — BuildRepairStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M294XCW3JGTTJF1Z37H4AV6X` |
| Edge | `EDGE-1-PREP-TEST` |
| Status | **SUCCESS** |
| Start | 2026-09-11T21:10:26.435953Z |
| End | 2026-09-11T21:11:50.300189Z |
| Duration | 1m 23s |

> Edge EDGE-1-PREP-TEST compiles after 1 round(s); 0 AI attempt(s) used of 12

---

## 1. Purpose

Compile the migrated state and repair bounded residual compile failures

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `EDGE_TRANSFORMED` | `EDGE_COMPILED` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:EDGE_TRANSFORMED` | satisfied | No artifact proof declared for this state | — |
| `artifact:11-plan/edge-plan.json` | satisfied | Published and readable | — |
| `artifact:08-knowledge/migration-knowledge.json` | satisfied | Published and readable | — |
| `artifact:02-build/build-model.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `11-plan/edge-plan.json` | resolved | `0d78110cfed9c131…` |
| `08-knowledge/migration-knowledge.json` | resolved | `eb2230ecf64f1038…` |
| `02-build/build-model.json` | resolved | `13f63a0f2464ab7d…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `RPR-001` | Resolve the frozen toolchain | Compiling on a different JDK than the edge froze would produce evidence about the wrong toolchain |
| `RPR-002` | Compile and repair within budget | Diagnostics are clustered by root cause; repairs are deterministic, and an AI proposal is verified deterministically before the gateway is ever asked |
| `RPR-003` | Publish the build and repair record | Records why repair stopped, which matters as much as whether it succeeded |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `RPR-001` | SUCCESS | 255 ms | Upstream inputs resolved |
| `RPR-002` | SUCCESS | 1m 23s | Completed; the stage reported SUCCESS |
| `RPR-003` | SUCCESS | 7 ms | Published and pointer advanced |

## 8. Tools and commands executed

| Id | Command | Exit | Timed out | Duration | Result |
| --- | --- | --- | --- | --- | --- |
| `CMD-000344` | `C:\Users\annav\tools\jdk-17\bin\java.exe -version` | 0 | no | 130 ms | SUCCESS |
| `CMD-000345` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -version` | 0 | no | 445 ms | SUCCESS |
| `CMD-000346` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 8.3 s | SUCCESS |
| `CMD-000347` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 15.1 s | SUCCESS |
| `CMD-000348` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 8.6 s | SUCCESS |
| `CMD-000349` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 21.6 s | SUCCESS |
| `CMD-000350` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 16.4 s | SUCCESS |
| `CMD-000351` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 12.6 s | SUCCESS |

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
| `build-report.json` | `daa2906541d79943…` |
| `repair-report.json` | `12242114d531199e…` |
| `diagnostics.json` | `a29e71c998267400…` |
| `manifest.json` | `4cd3b6278dadfc85…` |

Attempt directory: `20260911-211026-740`

## 21. Result

**SUCCESS** — Edge EDGE-1-PREP-TEST compiles after 1 round(s); 0 AI attempt(s) used of 12

The stage did what it declared it would do.

## 22. Next action

Run: bootshift validate --edge <edge>

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M294XCW3JGTTJF1Z37H4AV6X` |
| Stage id | `13-build-repair` |
| Edge id | `EDGE-1-PREP-TEST` |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `e8e6377380f3ecc1e86ffa487edb655d88b99edd59022a4c26437d8c6d0fbafc` |
| Primary artifact hash | `-120463906` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
