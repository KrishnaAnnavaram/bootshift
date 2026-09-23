# Stage 09-impact — ImpactStage

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M294P7M45YF96F39PCPF37CT` |
| Edge | Not an edge-scoped stage |
| Status | **SUCCESS** |
| Start | 2026-09-11T21:06:31.684254Z |
| End | 2026-09-11T21:06:34.503357700Z |
| Duration | 2.8 s |

> 2072 impact finding(s) across 27 file(s); {DEFINITELY_AFFECTED=502, POSSIBLY_AFFECTED=4, UNAFFECTED_WITHIN_OBSERVED_COVERAGE=1566}

---

## 1. Purpose

Determine which parts of this repository the verified migration facts affect

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `KNOWLEDGE_VERIFIED` | `IMPACT_ANALYZED` |

## 4. Preconditions

| Precondition | Result | Detail | Remediation |
| --- | --- | --- | --- |
| `state:KNOWLEDGE_VERIFIED` | satisfied | Proven by 08-knowledge/migration-knowledge.json | — |
| `artifact:08-knowledge/migration-knowledge.json` | satisfied | Published and readable | — |
| `artifact:03-graph/application-graph.json` | satisfied | Published and readable | — |
| `artifact:03-graph/file-registry.json` | satisfied | Published and readable | — |
| `artifact:01-inventory/inventory-signals.json` | satisfied | Published and readable | — |

## 5. Input artifacts

| Artifact | Status | Hash |
| --- | --- | --- |
| `08-knowledge/migration-knowledge.json` | resolved | `eb2230ecf64f1038…` |
| `03-graph/application-graph.json` | resolved | `f7a223fda7e01029…` |
| `03-graph/file-registry.json` | resolved | `74125cbe332c476c…` |
| `01-inventory/inventory-signals.json` | resolved | `eae4b4cf049f8114…` |

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
| `IMP-001` | Load verified facts and the application graph | Impact is resolved against this repository, not against a generic upgrade guide |
| `IMP-002` | Bind facts to files and symbols | Graph lookup first, source scanning as a declared fallback, then classification by certainty so a guess is never presented as a match |
| `IMP-003` | Publish the impact report | Every finding is traceable from fact to symbol to FILE_ID |

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `IMP-001` | SUCCESS | 64 ms | Upstream inputs resolved |
| `IMP-002` | SUCCESS | 2.7 s | Completed; the stage reported SUCCESS |
| `IMP-003` | SUCCESS | 0 ms | Published and pointer advanced |

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
| `impact-report.json` | `d4d1e5f89b22ba43…` |
| `impact-summary.json` | `9f18ffaf255cd72b…` |
| `blast-radius.json` | `996dc0163433cac5…` |
| `manifest.json` | `f940b8fd1525a083…` |

Attempt directory: `20260911-210631-780`

## 21. Result

**SUCCESS** — 2072 impact finding(s) across 27 file(s); {DEFINITELY_AFFECTED=502, POSSIBLY_AFFECTED=4, UNAFFECTED_WITHIN_OBSERVED_COVERAGE=1566}

The stage did what it declared it would do.

## 22. Next action

Run: bootshift characterize

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M294P7M45YF96F39PCPF37CT` |
| Stage id | `09-impact` |
| Edge id | — |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `619e7f87d359642616b946b48bdc7e3b5e7b6a044d2d25e35a1b45fc529b51c3` |
| Primary artifact hash | `1317236347` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
