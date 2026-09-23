# Stage 16-runtime — RuntimeValidationStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M2959WACG2QJ144B1HHCGHSB` |
| Edge | `EDGE-1-PREP-TEST` |
| Status | **SUCCESS** |
| Start | 2026-09-11T21:17:15.468935900Z |
| End | 2026-09-11T21:21:55.132594100Z |
| Duration | 4m 39s |

> Edge EDGE-1-PREP-TEST: 4/6 module(s) started, 267 bound property(ies), 279 runtime graph edge(s)

---

## 1. Purpose

Start the migrated application, observe runtime behaviour and enrich the runtime graph

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `EDGE_TESTED` | `EDGE_RUNTIME_GRAPH_ENRICHED` |

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
| `14-graph-diff/application-graph-current.json` | resolved | `d972472c65ea38d5…` |
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
| `RUN-001` | SUCCESS | 159 ms | Upstream inputs resolved |
| `RUN-002` | SUCCESS | 4m 39s | Completed; the stage reported SUCCESS |
| `RUN-003` | SUCCESS | 1 ms | Published and pointer advanced |

## 8. Tools and commands executed

| Id | Command | Exit | Timed out | Duration | Result |
| --- | --- | --- | --- | --- | --- |
| `CMD-000367` | `C:\Users\annav\tools\jdk-17\bin\java.exe -version` | 0 | no | 128 ms | SUCCESS |
| `CMD-000368` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -version` | 0 | no | 468 ms | SUCCESS |
| `CMD-000369` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -DskipTests package` | 0 | no | 7.1 s | SUCCESS |
| `CMD-000370` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\department-service\mvnw.cmd -B -version` | 0 | no | 521 ms | SUCCESS |
| `CMD-000371` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\department-service\mvnw.cmd -B -DskipTests package` | 0 | no | 6.9 s | SUCCESS |
| `CMD-000372` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\discovery-service\mvnw.cmd -B -version` | 0 | no | 769 ms | SUCCESS |
| `CMD-000373` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\discovery-service\mvnw.cmd -B -DskipTests package` | 0 | no | 11.2 s | SUCCESS |
| `CMD-000374` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\employee-service\mvnw.cmd -B -version` | 0 | no | 603 ms | SUCCESS |
| `CMD-000375` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\employee-service\mvnw.cmd -B -DskipTests package` | 0 | no | 15.0 s | SUCCESS |
| `CMD-000376` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\report-service\mvnw.cmd -B -version` | 0 | no | 701 ms | SUCCESS |
| `CMD-000377` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\report-service\mvnw.cmd -B -DskipTests package` | 0 | no | 13.4 s | SUCCESS |
| `CMD-000378` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\sheduler-service\mvnw.cmd -B -version` | 0 | no | 634 ms | SUCCESS |
| `CMD-000379` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\sheduler-service\mvnw.cmd -B -DskipTests package` | 0 | no | 10.5 s | SUCCESS |

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
| `runtime-report.json` | `5b83a832a07c2cf9…` |
| `scenario-observations-new.json` | `de423ccf84d4e04e…` |
| `configuration-binding.json` | `c473746c881f1efe…` |
| `runtime-graph-current.json` | `bb92d3a02d440fff…` |
| `application-graph-current-enriched.json` | `b6cff6f4eeed2d1f…` |
| `silently-ignored-properties.json` | `38fe72869f30200c…` |
| `manifest.json` | `4340d96963e62208…` |

Attempt directory: `20260911-211715-637`

## 21. Result

**SUCCESS** — Edge EDGE-1-PREP-TEST: 4/6 module(s) started, 267 bound property(ies), 279 runtime graph edge(s)

The stage did what it declared it would do.

## 22. Next action

Run: bootshift validate --edge <edge>

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M2959WACG2QJ144B1HHCGHSB` |
| Stage id | `16-runtime` |
| Edge id | `EDGE-1-PREP-TEST` |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `4cd675478ac7d59dcf4733c86dac1ca784e0223f0e1626937b9e6798d6093568` |
| Primary artifact hash | `-636725168` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
