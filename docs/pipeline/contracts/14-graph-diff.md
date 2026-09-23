# Stage 14-graph-diff

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `GraphDiffStage` |
| Output directory | `14-graph-diff` |
| Edge-scoped | yes |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `EDGE_SCOPE_VERIFIED` |

## Purpose

Rebuild the static graph and assert that every change stayed inside authorized scope

## Preconditions

- `EDGE_COMPILED`

## Input artifacts

- `13-build-repair/build-report.json`
- `11-plan/edge-plan.json`
- `02-build/build-model.json`

## Output artifacts

- `application-graph-current.json`
- `graph-diff.json`
- `scope-assertion.json`
- `last-good-graph.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `SCP-001` | Rebuild the application graph after mutation | The post-migration graph is rebuilt, never patched |
| `SCP-002` | Compare graphs and verify scope | Changed files are checked against the authorized set; anything outside it is a scope violation, not an incidental edit |
| `SCP-003` | Publish the graph diff and scope report | Structural change is separated from behavioural change |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/14-graph-diff/`, including attempts that refuse, fail or crash.
