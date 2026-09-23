# Stage 10-characterization — CharacterizationStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M294PAD5FG2YMJ1M6W52P4DT` |
| Edge | Not an edge-scoped stage |
| Status | **SUCCESS** |
| Start | 2026-09-11T21:06:34.533091200Z |
| End | 2026-09-11T21:10:21.862695400Z |
| Duration | 3m 47s |

> 82 executable scenario(s): 36 frozen against OLD, 46 unobservable, 0 unexecuted; 506 characterization contract(s): 0 frozen, 0 mapped to existing tests, 506 awaiting OLD, 0 unobservable

---

## 1. Purpose

Establish behavioural contracts for migration-sensitive impacts before they change

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `IMPACT_ANALYZED` | `CHARACTERIZATION_COMPLETE` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:IMPACT_ANALYZED` | satisfied | Proven by 09-impact/impact-report.json | — |
| `artifact:09-impact/impact-report.json` | satisfied | Published and readable | — |
| `artifact:03-graph/application-graph.json` | satisfied | Published and readable | — |
| `artifact:04-baseline/baseline-runtime.json` | satisfied | Published and readable | — |
| `artifact:04-baseline/baseline-tests.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `09-impact/impact-report.json` | resolved | `d4d1e5f89b22ba43…` |
| `03-graph/application-graph.json` | resolved | `f7a223fda7e01029…` |
| `04-baseline/baseline-runtime.json` | resolved | `66cd94939775ab08…` |
| `04-baseline/baseline-tests.json` | resolved | `c1daa4e5ef919587…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `CHR-001` | Identify unprotected behaviour | Behaviour with no test covering it is what a differential later has to speak for |
| `CHR-002` | Execute scenarios against the pre-migration application | A scenario is frozen only when it actually ran on OLD; anything else is NOT_EXECUTED |
| `CHR-003` | Publish characterization scenarios | The frozen observations become the oracle the migration is compared against |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `CHR-001` | SUCCESS | 22 ms | Upstream inputs resolved |
| `CHR-002` | SUCCESS | 3m 47s | Completed; the stage reported SUCCESS |
| `CHR-003` | SUCCESS | 2 ms | Published and pointer advanced |

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

- 46 scenario(s) are unobservable in this environment and are recorded as explicit gaps
- 506 probe(s) await execution against the original application; they cannot act as an oracle until then

## 18. Errors and blockers

No errors.

## 19. Blind spots and unknowns

This stage recorded no blind spots. That is a statement about this stage only.

## 20. Output artifacts

| Artifact | SHA-256 |
| --- | --- |
| `characterization-report.json` | `cb0ad5b597be2f0a…` |
| `characterization-contracts.json` | `30475cb8381d558f…` |
| `characterization-scenarios.json` | `841e790982dcf9d7…` |
| `characterization-old-observations.json` | `d4d76e8830472e66…` |
| `characterization-gaps.json` | `bcb29f4815338f06…` |
| `manifest.json` | `cd0202d4b1748cc9…` |

Attempt directory: `20260911-210634-575`

## 21. Result

**SUCCESS** — 82 executable scenario(s): 36 frozen against OLD, 46 unobservable, 0 unexecuted; 506 characterization contract(s): 0 frozen, 0 mapped to existing tests, 506 awaiting OLD, 0 unobservable

The stage did what it declared it would do.

## 22. Next action

Run: bootshift plan

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M294PAD5FG2YMJ1M6W52P4DT` |
| Stage id | `10-characterization` |
| Edge id | — |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `c190d634dd4f77bf0172b4d8ac2cbbd124fd0ab6d72964bc4ef7182c0cace199` |
| Primary artifact hash | `673705365` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
