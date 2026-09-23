# Stage 09-impact

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `ImpactStage` |
| Output directory | `09-impact` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `IMPACT_ANALYZED` |

## Purpose

Determine which parts of this repository the verified migration facts affect

## Preconditions

- `KNOWLEDGE_VERIFIED`

## Input artifacts

- `08-knowledge/migration-knowledge.json`
- `03-graph/application-graph.json`
- `03-graph/file-registry.json`
- `01-inventory/inventory-signals.json`

## Output artifacts

- `impact-report.json`
- `impact-summary.json`
- `blast-radius.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `IMP-001` | Load verified facts and the application graph | Impact is resolved against this repository, not against a generic upgrade guide |
| `IMP-002` | Bind facts to files and symbols | Graph lookup first, source scanning as a declared fallback, then classification by certainty so a guess is never presented as a match |
| `IMP-003` | Publish the impact report | Every finding is traceable from fact to symbol to FILE_ID |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/09-impact/`, including attempts that refuse, fail or crash.
