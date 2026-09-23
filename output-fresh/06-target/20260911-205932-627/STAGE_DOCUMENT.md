# Stage 06-target — TargetResolverStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M2949E11X9R1AZCPVGM1MT01` |
| Edge | Not an edge-scoped stage |
| Status | **SUCCESS** |
| Start | 2026-09-11T20:59:32.257125700Z |
| End | 2026-09-11T20:59:32.676273Z |
| Duration | 419 ms |

> Landing target Spring Boot 3.5.16 (Java 21, Spring Cloud 2025.0.3), 8 migration edge(s), -2 month support horizon

---

## 1. Purpose

Choose the landing target and the transit checkpoints that reach it

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `COMPATIBILITY_REGISTRY_READY` | `TARGET_FROZEN` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:COMPATIBILITY_REGISTRY_READY` | satisfied | Proven by 05-compatibility/compatibility-registry.json | — |
| `artifact:05-compatibility/lifecycle-registry.json` | satisfied | Published and readable | — |
| `artifact:05-compatibility/compatibility-registry.json` | satisfied | Published and readable | — |
| `artifact:05-compatibility/artifact-availability.json` | satisfied | Published and readable | — |
| `artifact:05-compatibility/internal-components.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `05-compatibility/lifecycle-registry.json` | resolved | `aa6b295b6f86bd38…` |
| `05-compatibility/compatibility-registry.json` | resolved | `de5e77f1943b2cd7…` |
| `05-compatibility/artifact-availability.json` | resolved | `94e67367b1af90b8…` |
| `05-compatibility/internal-components.json` | resolved | `bd9646292336ac0b…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `TGT-001` | Load compatibility registry and build model | Candidate targets are drawn from verified lifecycle data, not from a hard-coded list |
| `TGT-002` | Filter candidates and select the landing target | Every rejected candidate keeps the rule that rejected it, so the choice is auditable |
| `TGT-003` | Freeze the migration path | Splits the jump into edges at major boundaries and freezes the toolchain for each |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `TGT-001` | SUCCESS | 361 ms | Upstream inputs resolved |
| `TGT-002` | SUCCESS | 48 ms | Completed; the stage reported SUCCESS |
| `TGT-003` | SUCCESS | 0 ms | Published and pointer advanced |

## 8. Tools and commands executed

| Id | Command | Exit | Timed out | Duration | Result |
| --- | --- | --- | --- | --- | --- |
| `CMD-000056` | `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe -version` | 0 | no | 197 ms | SUCCESS |
| `CMD-000057` | `C:\Users\annav\tools\jdk-17\bin\java.exe -version` | 0 | no | 152 ms | SUCCESS |

> Command arguments are redacted before they are written. Process output is not copied into this document; where a log was captured it is referenced above.

## 9. Decisions made

### DEC-00001 — LANDING_TARGET_CANDIDATE

| Field | Value |
| --- | --- |
| Subject | `Spring Boot 2.7` |
| Decision | REJECTED |
| Reason | support horizon of -33 month(s) is below the policy minimum -24 |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

*Alternatives considered:* `3.0`, `3.1`, `3.2`, `3.3`, `3.4`, `3.5`, `4.0`, `4.1`

*Policies:* `selection_mode=AUTO_HIGHEST_SAFE_SUPPORTED`

### DEC-00002 — LANDING_TARGET_CANDIDATE

| Field | Value |
| --- | --- |
| Subject | `Spring Boot 3.0` |
| Decision | REJECTED |
| Reason | support horizon of -33 month(s) is below the policy minimum -24 |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

*Alternatives considered:* `2.7`, `3.1`, `3.2`, `3.3`, `3.4`, `3.5`, `4.0`, `4.1`

*Policies:* `selection_mode=AUTO_HIGHEST_SAFE_SUPPORTED`

### DEC-00003 — LANDING_TARGET_CANDIDATE

| Field | Value |
| --- | --- |
| Subject | `Spring Boot 3.1` |
| Decision | REJECTED |
| Reason | support horizon of -27 month(s) is below the policy minimum -24 |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

*Alternatives considered:* `2.7`, `3.0`, `3.2`, `3.3`, `3.4`, `3.5`, `4.0`, `4.1`

*Policies:* `selection_mode=AUTO_HIGHEST_SAFE_SUPPORTED`

### DEC-00004 — LANDING_TARGET_CANDIDATE

| Field | Value |
| --- | --- |
| Subject | `Spring Boot 3.2` |
| Decision | VIABLE_NOT_CHOSEN |
| Reason | Viable, but scored -40.8 against the selected candidate |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

*Alternatives considered:* `2.7`, `3.0`, `3.1`, `3.3`, `3.4`, `3.5`, `4.0`, `4.1`

*Policies:* `selection_mode=AUTO_HIGHEST_SAFE_SUPPORTED`

### DEC-00005 — LANDING_TARGET_CANDIDATE

| Field | Value |
| --- | --- |
| Subject | `Spring Boot 3.3` |
| Decision | VIABLE_NOT_CHOSEN |
| Reason | Viable, but scored 19.3 against the selected candidate |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

*Alternatives considered:* `2.7`, `3.0`, `3.1`, `3.2`, `3.4`, `3.5`, `4.0`, `4.1`

*Policies:* `selection_mode=AUTO_HIGHEST_SAFE_SUPPORTED`

### DEC-00006 — LANDING_TARGET_CANDIDATE

| Field | Value |
| --- | --- |
| Subject | `Spring Boot 3.4` |
| Decision | VIABLE_NOT_CHOSEN |
| Reason | Viable, but scored 79.4 against the selected candidate |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

*Alternatives considered:* `2.7`, `3.0`, `3.1`, `3.2`, `3.3`, `3.5`, `4.0`, `4.1`

*Policies:* `selection_mode=AUTO_HIGHEST_SAFE_SUPPORTED`

### DEC-00007 — LANDING_TARGET_CANDIDATE

| Field | Value |
| --- | --- |
| Subject | `Spring Boot 3.5` |
| Decision | SELECTED |
| Reason | Highest-scoring viable candidate under AUTO_HIGHEST_SAFE_SUPPORTED; score 139.5, support horizon -2 month(s) |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

*Alternatives considered:* `2.7`, `3.0`, `3.1`, `3.2`, `3.3`, `3.4`, `4.0`, `4.1`

*Policies:* `selection_mode=AUTO_HIGHEST_SAFE_SUPPORTED`

### DEC-00008 — LANDING_TARGET_CANDIDATE

| Field | Value |
| --- | --- |
| Subject | `Spring Boot 4.0` |
| Decision | REJECTED |
| Reason | the application uses Spring Cloud but no GA release train targets this Boot line, so the required artifacts do not exist |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

*Alternatives considered:* `2.7`, `3.0`, `3.1`, `3.2`, `3.3`, `3.4`, `3.5`, `4.1`

*Policies:* `selection_mode=AUTO_HIGHEST_SAFE_SUPPORTED`

### DEC-00009 — LANDING_TARGET_CANDIDATE

| Field | Value |
| --- | --- |
| Subject | `Spring Boot 4.1` |
| Decision | REJECTED |
| Reason | the application uses Spring Cloud but no GA release train targets this Boot line, so the required artifacts do not exist |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

*Alternatives considered:* `2.7`, `3.0`, `3.1`, `3.2`, `3.3`, `3.4`, `3.5`, `4.0`

*Policies:* `selection_mode=AUTO_HIGHEST_SAFE_SUPPORTED`

### DEC-00010 — LANDING_TARGET

| Field | Value |
| --- | --- |
| Subject | `auto` |
| Decision | 3.5.16 |
| Reason | Selection mode AUTO_HIGHEST_SAFE_SUPPORTED over 9 evaluated candidate(s) |
| Confidence | VERIFIED |
| Made by | DETERMINISTIC_RULE |
| Authorized by | DETERMINISTIC_RULE |

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

- eliminated 2.7: support horizon of -33 month(s) is below the policy minimum -24
- eliminated 3.0: support horizon of -33 month(s) is below the policy minimum -24
- eliminated 3.1: support horizon of -27 month(s) is below the policy minimum -24
- eliminated 4.0: the application uses Spring Cloud but no GA release train targets this Boot line, so the required artifacts do not exist
- eliminated 4.1: the application uses Spring Cloud but no GA release train targets this Boot line, so the required artifacts do not exist

## 18. Errors and blockers

No errors.

## 19. Blind spots and unknowns

This stage recorded no blind spots. That is a statement about this stage only.

## 20. Output artifacts

| Artifact | SHA-256 |
| --- | --- |
| `target-state.json` | `93b39e231b7ae4b4…` |
| `migration-path.json` | `513d19c5ddd7089e…` |
| `target-resolution-report.json` | `03882e561054d620…` |
| `manifest.json` | `3f912bd5c8831772…` |

Attempt directory: `20260911-205932-627`

## 21. Result

**SUCCESS** — Landing target Spring Boot 3.5.16 (Java 21, Spring Cloud 2025.0.3), 8 migration edge(s), -2 month support horizon

The stage did what it declared it would do.

## 22. Next action

Run: bootshift documentation

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M2949E11X9R1AZCPVGM1MT01` |
| Stage id | `06-target` |
| Edge id | — |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `ed8d8498f929ea87e5cd0e7787a6e65a4a3e2fb7ac6729d8d359ad6b1ec8e98d` |
| Primary artifact hash | `-416705828` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
