# Stage 05-compatibility — CompatibilityStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M2948Q60E41CXQP1MNC6VAGY` |
| Edge | Not an edge-scoped stage |
| Status | **SUCCESS** |
| Start | 2026-09-11T20:59:08.864803500Z |
| End | 2026-09-11T20:59:32.225663900Z |
| Duration | 23.4 s |

> Current state Spring Boot 2.7.12 / Java 17 / Spring Cloud 2021.0.7; 9 candidate line(s), 0 unavailable, 0 unknown internal component(s)

---

## 1. Purpose

Build repository-independent Tier-1 compatibility and lifecycle knowledge

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `BASELINE_SEALED` | `COMPATIBILITY_REGISTRY_READY` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:BASELINE_SEALED` | satisfied | Proven by 04-baseline/baseline-manifest.json | — |
| `artifact:02-build/build-model.json` | satisfied | Published and readable | — |
| `artifact:02-build/dependency-model.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `02-build/build-model.json` | resolved | `13f63a0f2464ab7d…` |
| `02-build/dependency-model.json` | resolved | `056a8e2147f6e3a9…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `CMP-001` | Load the build model | Current Spring Boot, Spring Cloud and Java levels come from the resolved build model |
| `CMP-002` | Resolve version space and lifecycle facts | Published versions, support horizons and Spring Cloud trains, from their own sources |
| `CMP-003` | Publish the compatibility registry | UNKNOWN is preserved as UNKNOWN rather than being resolved to compatible |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `CMP-001` | SUCCESS | 60 ms | Upstream inputs resolved |
| `CMP-002` | SUCCESS | 23.3 s | Completed; the stage reported SUCCESS |
| `CMP-003` | SUCCESS | 0 ms | Published and pointer advanced |

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
| `compatibility-registry.json` | `de5e77f1943b2cd7…` |
| `lifecycle-registry.json` | `aa6b295b6f86bd38…` |
| `artifact-availability.json` | `94e67367b1af90b8…` |
| `version-space-evidence.json` | `83f5a4bfd1d9ff65…` |
| `internal-components.json` | `bd9646292336ac0b…` |
| `manifest.json` | `bce6d5914671ca53…` |

Attempt directory: `20260911-205908-963`

## 21. Result

**SUCCESS** — Current state Spring Boot 2.7.12 / Java 17 / Spring Cloud 2021.0.7; 9 candidate line(s), 0 unavailable, 0 unknown internal component(s)

The stage did what it declared it would do.

## 22. Next action

Run: bootshift resolve-target --target auto

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M2948Q60E41CXQP1MNC6VAGY` |
| Stage id | `05-compatibility` |
| Edge id | — |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `881f81b82db53be3744d209377614d57e55e0bda8c411b4b6fcf6db711fd05d0` |
| Primary artifact hash | `-266362163` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
