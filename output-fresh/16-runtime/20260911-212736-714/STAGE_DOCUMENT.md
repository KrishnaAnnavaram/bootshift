# Stage 16-runtime — RuntimeValidationStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M295WTV68FHVKDHJZQA51KJM` |
| Edge | `EDGE-2-PATCH` |
| Status | **SUCCESS** |
| Start | 2026-09-11T21:27:36.550476200Z |
| End | 2026-09-11T21:32:33.040968400Z |
| Duration | 4m 56s |

> Edge EDGE-2-PATCH: 4/6 module(s) started, 268 bound property(ies), 280 runtime graph edge(s)

---

## 1. Purpose

Start the migrated application, observe runtime behaviour and enrich the runtime graph

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
| `state:EDGE_SCOPE_VERIFIED` | satisfied | No artifact proof declared for this state | — |
| `artifact:11-plan/edge-plan.json` | satisfied | Published and readable | — |
| `artifact:14-graph-diff/application-graph-current.json` | satisfied | Published and readable | — |
| `artifact:04-baseline/baseline-runtime.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `11-plan/edge-plan.json` | resolved | `0d78110cfed9c131…` |
| `14-graph-diff/application-graph-current.json` | resolved | `010f443728c2c103…` |
| `04-baseline/baseline-runtime.json` | resolved | `66cd94939775ab08…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `RUN-001` | Prepare the runtime workspace and toolchain | The migrated tree is started on the JDK the edge froze |
| `RUN-002` | Start modules and execute frozen scenarios | Missing infrastructure is recorded as NOT_AVAILABLE; it is never counted as a pass |
| `RUN-003` | Publish the runtime report | Observed, failed and unavailable stay distinguishable |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `RUN-001` | SUCCESS | 154 ms | Upstream inputs resolved |
| `RUN-002` | SUCCESS | 4m 56s | Completed; the stage reported SUCCESS |
| `RUN-003` | SUCCESS | 6 ms | Published and pointer advanced |

## 8. Tools and commands executed

| Id | Command | Exit | Timed out | Duration | Result |
| --- | --- | --- | --- | --- | --- |
| `CMD-000403` | `C:\Users\annav\tools\jdk-17\bin\java.exe -version` | 0 | no | 129 ms | SUCCESS |
| `CMD-000404` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -version` | 0 | no | 433 ms | SUCCESS |
| `CMD-000405` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -DskipTests package` | 0 | no | 7.6 s | SUCCESS |
| `CMD-000406` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\department-service\mvnw.cmd -B -version` | 0 | no | 462 ms | SUCCESS |
| `CMD-000407` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\department-service\mvnw.cmd -B -DskipTests package` | 0 | no | 7.6 s | SUCCESS |
| `CMD-000408` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\discovery-service\mvnw.cmd -B -version` | 0 | no | 607 ms | SUCCESS |
| `CMD-000409` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\discovery-service\mvnw.cmd -B -DskipTests package` | 0 | no | 10.2 s | SUCCESS |
| `CMD-000410` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\employee-service\mvnw.cmd -B -version` | 0 | no | 610 ms | SUCCESS |
| `CMD-000411` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\employee-service\mvnw.cmd -B -DskipTests package` | 0 | no | 14.3 s | SUCCESS |
| `CMD-000412` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\report-service\mvnw.cmd -B -version` | 0 | no | 692 ms | SUCCESS |
| `CMD-000413` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\report-service\mvnw.cmd -B -DskipTests package` | 0 | no | 12.6 s | SUCCESS |
| `CMD-000414` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\sheduler-service\mvnw.cmd -B -version` | 0 | no | 683 ms | SUCCESS |
| `CMD-000415` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\sheduler-service\mvnw.cmd -B -DskipTests package` | 0 | no | 11.1 s | SUCCESS |

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

- did not start: configuaration-server - PROCESS_EXITED: APPLICATION FAILED TO START
- did not start: employee-service - TIMEOUT: Caused by: com.mongodb.MongoTimeoutException: Timed out after 30000 ms while waiting to connect. Client view of cluster state is {type=UNKNOWN, servers=[{address=localhost:27017, type=UNKNOWN, state=CONNECTING, exception={com.mongodb.MongoSocketOpenException: Exception opening socket}, caused by {java.net.ConnectException: Connection refused: getsockopt}}]

## 18. Errors and blockers

No errors.

## 19. Blind spots and unknowns

This stage recorded no blind spots. That is a statement about this stage only.

## 20. Output artifacts

| Artifact | SHA-256 |
| --- | --- |
| `runtime-report.json` | `3f4ce123c12b4394…` |
| `scenario-observations-new.json` | `520218760c11fb98…` |
| `configuration-binding.json` | `95bf5e4ef4caacf3…` |
| `runtime-graph-current.json` | `d12a6fb2475e6f0f…` |
| `application-graph-current-enriched.json` | `eb9805cdcdcdb028…` |
| `silently-ignored-properties.json` | `0d8565f0b90333a4…` |
| `manifest.json` | `5aea5b93e3add5ba…` |

Attempt directory: `20260911-212736-714`

## 21. Result

**SUCCESS** — Edge EDGE-2-PATCH: 4/6 module(s) started, 268 bound property(ies), 280 runtime graph edge(s)

The stage did what it declared it would do.

## 22. Next action

Run: bootshift validate --edge <edge>

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M295WTV68FHVKDHJZQA51KJM` |
| Stage id | `16-runtime` |
| Edge id | `EDGE-2-PATCH` |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `2112bad6dc61d24961829d8f410f53ed9941d10d67f9928e92b4522eddc1731e` |
| Primary artifact hash | `1951193503` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
