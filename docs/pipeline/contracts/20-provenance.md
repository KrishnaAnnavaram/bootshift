# Stage 20-provenance

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `ProvenanceStage` |
| Output directory | `20-provenance` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `MIGRATION_COMPLETE` |

## Purpose

Build the provenance graph and answer the deterministic question catalog

## Preconditions

- `EVIDENCE_SEALED`

## Input artifacts

This stage consumes no published artifacts.

## Output artifacts

- `provenance-graph.json`
- `question-catalog.json`
- `blind-spots.json`
- `gaps.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `PRV-001` | Load evidence, ledger and lineage | Provenance is assembled from records that already exist |
| `PRV-002` | Build the provenance graph | Connects file, symbol, fact, decision, validation and gap lineage |
| `PRV-003` | Publish the provenance graph | Supports asking why a specific file changed and what authorized it |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/20-provenance/`, including attempts that refuse, fail or crash.
