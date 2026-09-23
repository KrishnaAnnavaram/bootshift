# Stage 19-evidence

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `EvidenceStage` |
| Output directory | `19-evidence` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `EVIDENCE_SEALED` |

## Purpose

Seal the evidence manifest and produce the defensible migration report

## Preconditions

- `FINAL_APPROVAL`

## Input artifacts

This stage consumes no published artifacts.

## Output artifacts

- `migration-report.md`
- `migration-result.json`
- `evidence-manifest.json`
- `documentation-index.json`
- `file-lineage.json`
- `symbol-lineage.json`
- `claims.json`
- `edge-evidence.json`
- `coverage-statement.json`
- `MIGRATION_DOCUMENT.md`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `EVD-001` | Aggregate every edge attempt | Resolved through the edge index so all edges are included, not only the one the stage pointer names |
| `EVD-002` | Verify the ledger and compute coverage | Evidence levels, claims, shortfalls and blind spots are derived, never asserted |
| `EVD-003` | Seal the evidence manifest and write the migration document | An incomplete run is reported as incomplete rather than presented as a migration |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/19-evidence/`, including attempts that refuse, fail or crash.
