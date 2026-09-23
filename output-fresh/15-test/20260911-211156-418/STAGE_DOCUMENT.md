# Stage 15-test — TestValidationStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M29504HD7NAH4RX9NRSR9BX5` |
| Edge | `EDGE-1-PREP-TEST` |
| Status | **SUCCESS** |
| Start | 2026-09-11T21:11:56.205810700Z |
| End | 2026-09-11T21:17:15.441473600Z |
| Duration | 5m 19s |

> Edge EDGE-1-PREP-TEST: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2}

---

## 1. Purpose

Run the application test suite and classify results against baseline and previous edge

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `EDGE_SCOPE_VERIFIED` | `EDGE_TESTED` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:EDGE_SCOPE_VERIFIED` | satisfied | No artifact proof declared for this state | — |
| `artifact:04-baseline/baseline-tests.json` | satisfied | Published and readable | — |
| `artifact:04-baseline/baseline-coverage.json` | satisfied | Published and readable | — |
| `artifact:11-plan/edge-plan.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `04-baseline/baseline-tests.json` | resolved | `c1daa4e5ef919587…` |
| `04-baseline/baseline-coverage.json` | resolved | `a2cd30a501343e38…` |
| `11-plan/edge-plan.json` | resolved | `0d78110cfed9c131…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `TST-001` | Resolve the toolchain and baseline results | Two baselines: the sealed pre-migration run and the edge-local previous state |
| `TST-002` | Run tests and classify outcomes | A pre-existing failure and a migration-caused regression are different findings and are never merged |
| `TST-003` | Publish the test report | Deleted, disabled and weakened tests are reported, not just pass counts |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `TST-001` | SUCCESS | 205 ms | Upstream inputs resolved |
| `TST-002` | SUCCESS | 5m 19s | Completed; the stage reported SUCCESS |
| `TST-003` | SUCCESS | 2 ms | Published and pointer advanced |

## 8. Tools and commands executed

| Id | Command | Exit | Timed out | Duration | Result |
| --- | --- | --- | --- | --- | --- |
| `CMD-000352` | `C:\Users\annav\tools\jdk-17\bin\java.exe -version` | 0 | no | 147 ms | SUCCESS |
| `CMD-000353` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B -version` | 0 | no | 474 ms | SUCCESS |
| `CMD-000354` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\configuaration-server\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 31.2 s | SUCCESS |
| `CMD-000355` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\department-service\mvnw.cmd -B -version` | 0 | no | 460 ms | SUCCESS |
| `CMD-000356` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\department-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 33.0 s | SUCCESS |
| `CMD-000357` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\discovery-service\mvnw.cmd -B -version` | 0 | no | 482 ms | SUCCESS |
| `CMD-000358` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\discovery-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 23.6 s | SUCCESS |
| `CMD-000359` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\employee-service\mvnw.cmd -B -version` | 0 | no | 496 ms | SUCCESS |
| `CMD-000360` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\employee-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 1 | no | 6.3 s | FAILED |
| `CMD-000361` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\employee-service\mvnw.cmd -B test` | 1 | no | 1m 39s | FAILED |
| `CMD-000362` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\employee-service\mvnw.cmd -B test -Dtest=com.aura.vihanga.employeeservice.repository.EmployeeRepositoryTest#employeeSaveMethodTest,com.aura.vihanga.employeeservice.repository.EmployeeRepositoryTest#getEmployeeById -DfailIfNoSpecifiedTests=false` | 1 | no | 1m 16s | FAILED |
| `CMD-000363` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\report-service\mvnw.cmd -B -version` | 0 | no | 485 ms | SUCCESS |
| `CMD-000364` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\report-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 23.3 s | SUCCESS |
| `CMD-000365` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\sheduler-service\mvnw.cmd -B -version` | 0 | no | 428 ms | SUCCESS |
| `CMD-000366` | `C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9\migration\sheduler-service\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report` | 0 | no | 22.6 s | SUCCESS |

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
| `test-report.json` | `43d21ae77ae44940…` |
| `coverage-report.json` | `9abe73fd1e97e9c9…` |
| `manifest.json` | `f8ee28750b14a524…` |

Attempt directory: `20260911-211156-418`

## 21. Result

**SUCCESS** — Edge EDGE-1-PREP-TEST: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2}

The stage did what it declared it would do.

## 22. Next action

Run: bootshift validate --edge <edge>

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M29504HD7NAH4RX9NRSR9BX5` |
| Stage id | `15-test` |
| Edge id | `EDGE-1-PREP-TEST` |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `eb9cbd5e28223b12fbed14b7a42b3b335d45cbb21d081c5e9d085b15e8617a35` |
| Primary artifact hash | `-930947973` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
