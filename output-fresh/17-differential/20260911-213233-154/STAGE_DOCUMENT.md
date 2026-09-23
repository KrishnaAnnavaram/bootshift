# Stage 17-differential — DifferentialStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M2965WEJAND58F0PASTQANF8` |
| Edge | `EDGE-2-PATCH` |
| Status | **BLOCKED** |
| Start | 2026-09-11T21:32:33.106283100Z |
| End | 2026-09-11T21:32:33.299891700Z |
| Duration | 193 ms |

> Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s)

---

## 1. Purpose

Compare original and migrated behaviour for the required dimensions

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `EDGE_COMPLETE` | `BLOCKED` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:EDGE_RUNTIME_GRAPH_ENRICHED` | satisfied | No artifact proof declared for this state | — |
| `artifact:11-plan/edge-plan.json` | satisfied | Published and readable | — |
| `artifact:10-characterization/characterization-contracts.json` | satisfied | Published and readable | — |
| `artifact:04-baseline/baseline-runtime.json` | satisfied | Published and readable | — |
| `artifact:16-runtime/runtime-report.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `11-plan/edge-plan.json` | resolved | `0d78110cfed9c131…` |
| `10-characterization/characterization-contracts.json` | resolved | `30475cb8381d558f…` |
| `04-baseline/baseline-runtime.json` | resolved | `66cd94939775ab08…` |
| `16-runtime/runtime-report.json` | resolved | `3f4ce123c12b4394…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `DIF-001` | Load frozen observations from both sides | A comparison needs a successful observation on OLD and on NEW |
| `DIF-002` | Normalise and compare | Normalisation is policy-driven and hashed, so a difference cannot be explained away by changing the rules afterwards |
| `DIF-003` | Publish the differential report | An unexplained difference blocks; that is the point of the stage |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `DIF-001` | SUCCESS | 33 ms | Upstream inputs resolved |
| `DIF-002` | BLOCKED | 145 ms | Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s) |
| `DIF-003` | SUCCESS | 7 ms | Published and pointer advanced |

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

- UNEXPLAINED CONFIGURATION_BINDING scenario SCN-00047: 1 difference(s) in /actuator/configprops with no verified migration fact and no recorded decision to explain them

## 18. Errors and blockers

No errors.

**What stopped the pipeline here:** Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s)

## 19. Blind spots and unknowns

This stage recorded no blind spots. That is a statement about this stage only.

## 20. Output artifacts

| Artifact | SHA-256 |
| --- | --- |
| `differential-report.json` | `df0d08917f68e2d0…` |
| `normalization-policy.json` | `296432adcd77d52e…` |
| `environment-equivalence-check.json` | `cd7a14a702dfa105…` |
| `manifest.json` | `8d807a809214f2a0…` |

Attempt directory: `20260911-213233-154`

## 21. Result

**BLOCKED** — Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s)

The stage ran and stopped on a finding it is not permitted to decide on its own. This is the harness working as designed.

## 22. Next action

This stage is blocked by the active policy. Read section 18, then either change the policy deliberately or accept that the migration cannot proceed as specified.

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M2965WEJAND58F0PASTQANF8` |
| Stage id | `17-differential` |
| Edge id | `EDGE-2-PATCH` |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `2df7641d219532f1ab7b4116d896d4e5fcf15111b407120531d80022ad8c445a` |
| Primary artifact hash | `-1596411896` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
