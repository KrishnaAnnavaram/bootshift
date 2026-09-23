# Stage 04-baseline — BaselineStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M293QDKPGGA1R7G0H3VWVF0K` |
| Edge | Not an edge-scoped stage |
| Status | **SUCCESS** |
| Start | 2026-09-11T20:49:42.006109100Z |
| End | 2026-09-11T20:59:08.809173100Z |
| Duration | 9m 26s |

> Baseline sealed as dd491b5a6685134a (6 module(s) built, 12 test(s), 4/6 runtime probe(s) started)

---

## 1. Purpose

Observe and cryptographically seal the original application's behaviour

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `GRAPH_VERIFIED` | `BASELINE_SEALED` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:GRAPH_VERIFIED` | satisfied | Proven by 03-graph/graph-verification-report.json | — |
| `artifact:02-build/build-model.json` | satisfied | Published and readable | — |
| `artifact:03-graph/application-graph.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `02-build/build-model.json` | resolved | `13f63a0f2464ab7d…` |
| `03-graph/application-graph.json` | resolved | `f7a223fda7e01029…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `BAS-001` | Load graph, build model and registry | The baseline is measured against the structures the analysis half already established |
| `BAS-002` | Capture the immutable baseline | Toolchain selection, build, tests and runtime starts on the pre-migration tree, plus the pre-existing failures that must never be attributed to the migration |
| `BAS-003` | Seal and publish the baseline manifest | Once sealed the baseline cannot be re-measured; every later comparison binds to this hash |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `BAS-001` | SUCCESS | 303 ms | Upstream inputs resolved |
| `BAS-002` | SUCCESS | 9m 26s | Completed; the stage reported SUCCESS |
| `BAS-003` | SUCCESS | 4 ms | Published and pointer advanced |

## 8. Tools and commands executed

| Id | Command | Exit | Timed out | Duration | Result |
| --- | --- | --- | --- | --- | --- |
| `CMD-000028` | `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe -version` | 0 | no | 137 ms | SUCCESS |
| `CMD-000029` | `C:\Users\annav\tools\jdk-17\bin\java.exe -version` | 0 | no | 137 ms | SUCCESS |
| `CMD-000030` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B -version` | 0 | no | 449 ms | SUCCESS |
| `CMD-000031` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 6.0 s | SUCCESS |
| `CMD-000032` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 8.2 s | SUCCESS |
| `CMD-000033` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 5.9 s | SUCCESS |
| `CMD-000034` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 11.9 s | SUCCESS |
| `CMD-000035` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 9.9 s | SUCCESS |
| `CMD-000036` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B -DskipTests test-compile` | 0 | no | 8.7 s | SUCCESS |
| `CMD-000037` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B -version` | 0 | no | 421 ms | SUCCESS |
| `CMD-000038` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 24.7 s | SUCCESS |
| `CMD-000039` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\department-service\mvnw.cmd -B -version` | 0 | no | 568 ms | SUCCESS |
| `CMD-000040` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\department-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 32.2 s | SUCCESS |
| `CMD-000041` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\discovery-service\mvnw.cmd -B -version` | 0 | no | 670 ms | SUCCESS |
| `CMD-000042` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\discovery-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 28.2 s | SUCCESS |
| `CMD-000043` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\employee-service\mvnw.cmd -B -version` | 0 | no | 639 ms | SUCCESS |
| `CMD-000044` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\employee-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 1 | no | 9.0 s | FAILED |
| `CMD-000045` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\employee-service\mvnw.cmd -B test` | 1 | no | 1m 55s | FAILED |
| `CMD-000046` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\report-service\mvnw.cmd -B -version` | 0 | no | 757 ms | SUCCESS |
| `CMD-000047` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\report-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 32.4 s | SUCCESS |
| `CMD-000048` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\sheduler-service\mvnw.cmd -B -version` | 0 | no | 798 ms | SUCCESS |
| `CMD-000049` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\sheduler-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 32.2 s | SUCCESS |
| `CMD-000050` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\configuaration-server\mvnw.cmd -B -DskipTests package` | 0 | no | 10.9 s | SUCCESS |
| `CMD-000051` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\department-service\mvnw.cmd -B -DskipTests package` | 0 | no | 11.0 s | SUCCESS |
| `CMD-000052` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\discovery-service\mvnw.cmd -B -DskipTests package` | 0 | no | 10.9 s | SUCCESS |
| `CMD-000053` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\employee-service\mvnw.cmd -B -DskipTests package` | 0 | no | 14.1 s | SUCCESS |
| `CMD-000054` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\report-service\mvnw.cmd -B -DskipTests package` | 0 | no | 12.7 s | SUCCESS |
| `CMD-000055` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\runtime-old\sheduler-service\mvnw.cmd -B -DskipTests package` | 0 | no | 10.1 s | SUCCESS |

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

- 2 pre-existing test failure(s) recorded as baseline debt
- BLIND SPOT BS-RUNTIME-CONFIGUARATION-SERVER: Module configuaration-server did not start during baseline capture
- BLIND SPOT BS-RUNTIME-EMPLOYEE-SERVICE: Module employee-service did not start during baseline capture

## 18. Errors and blockers

No errors.

## 19. Blind spots and unknowns

This stage recorded no blind spots. That is a statement about this stage only.

## 20. Output artifacts

| Artifact | SHA-256 |
| --- | --- |
| `baseline-build.json` | `f633e7bb7ff5287b…` |
| `baseline-tests.json` | `c1daa4e5ef919587…` |
| `baseline-coverage.json` | `a2cd30a501343e38…` |
| `baseline-configuration.json` | `77f24a3fe727a56b…` |
| `baseline-runtime.json` | `66cd94939775ab08…` |
| `environment-equivalence.json` | `6fa9c18e3e3e1e47…` |
| `runtime-graph-baseline.json` | `9feab404b93035f6…` |
| `application-graph-baseline-enriched.json` | `329be53cb2348c67…` |
| `baseline-manifest.json` | `273f3255e4981e6d…` |
| `manifest.json` | `5fd6a421657e2621…` |

Attempt directory: `20260911-204942-332`

## 21. Result

**SUCCESS** — Baseline sealed as dd491b5a6685134a (6 module(s) built, 12 test(s), 4/6 runtime probe(s) started)

The stage did what it declared it would do.

## 22. Next action

Run: bootshift compatibility

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M293QDKPGGA1R7G0H3VWVF0K` |
| Stage id | `04-baseline` |
| Edge id | — |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `f1c3e53242f325d6254d3134a7062fdd0a5c5cf346503a6e219825d59a2d6d04` |
| Primary artifact hash | `1622115155` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
