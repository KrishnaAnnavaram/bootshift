# Stage 08-knowledge

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `KnowledgeStage` |
| Output directory | `08-knowledge` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `KNOWLEDGE_VERIFIED` |

## Purpose

Turn official documentation plus artifact reality into verified migration facts

## Preconditions

- `DOCUMENTATION_RETRIEVED`

## Input artifacts

- `06-target/target-state.json`
- `07-documentation/document-registry.json`
- `02-build/dependency-model.json`
- `02-build/bom-model.json`

## Output artifacts

- `migration-knowledge.json`
- `artifact-channel.json`
- `documentation-channel.json`
- `knowledge-summary.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `KNW-001` | Load documents and artifact metadata | Facts are derived from published artifacts and official documents, never from a model |
| `KNW-002` | Derive and verify migration facts | A fact becomes VERIFIED only when a verification channel confirms it; the rest stay CANDIDATE and are visible as such |
| `KNW-003` | Publish migration knowledge | Each fact carries the channel and evidence that justify its status |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/08-knowledge/`, including attempts that refuse, fail or crash.
