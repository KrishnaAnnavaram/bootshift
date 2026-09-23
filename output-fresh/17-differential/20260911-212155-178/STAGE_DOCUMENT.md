# Stage 17-differential — DifferentialStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M295JDE9JBKRDVSRHMQE65AJ` |
| Edge | `EDGE-1-PREP-TEST` |
| Status | **SUCCESS** |
| Start | 2026-09-11T21:21:55.145497400Z |
| End | 2026-09-11T21:21:55.426180900Z |
| Duration | 280 ms |

> Edge EDGE-1-PREP-TEST: 82 comparison(s) across 1 dimension(s); {IDENTICAL=36, NOT_COMPARED=46}

---

## 1. Purpose

Compare original and migrated behaviour for the required dimensions

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `EDGE_RUNTIME_GRAPH_ENRICHED` | `EDGE_DIFFERENTIAL_VALIDATED` |

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
| `16-runtime/runtime-report.json` | resolved | `5b83a832a07c2cf9…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `DIF-001` | Load frozen observations from both sides | A comparison needs a successful observation on OLD and on NEW |
| `DIF-002` | Normalise and compare | Normalisation is policy-driven and hashed, so a difference cannot be explained away by changing the rules afterwards |
| `DIF-003` | Publish the differential report | An unexplained difference blocks; that is the point of the stage |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `DIF-001` | SUCCESS | 15 ms | Upstream inputs resolved |
| `DIF-002` | SUCCESS | 247 ms | Completed; the stage reported SUCCESS |
| `DIF-003` | SUCCESS | 8 ms | Published and pointer advanced |

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
| `differential-report.json` | `98a18227dec4a852…` |
| `normalization-policy.json` | `283a5bd7299899bf…` |
| `environment-equivalence-check.json` | `0fdd5837733d1a5d…` |
| `manifest.json` | `9a2c4aae62fe1e09…` |

Attempt directory: `20260911-212155-178`

## 21. Result

**SUCCESS** — Edge EDGE-1-PREP-TEST: 82 comparison(s) across 1 dimension(s); {IDENTICAL=36, NOT_COMPARED=46}

The stage did what it declared it would do.

## 22. Next action

Run: bootshift approve

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M295JDE9JBKRDVSRHMQE65AJ` |
| Stage id | `17-differential` |
| Edge id | `EDGE-1-PREP-TEST` |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `9a84c3ce6bc3ef21ede9c3ed1672bd330e62a95fbede04f8e92fdca71583bb81` |
| Primary artifact hash | `-530823312` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
