# Stage 12-transformation

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `TransformationStage` |
| Output directory | `12-transformation` |
| Edge-scoped | yes |
| May write to application source | **yes** |
| May consult the optional AI provider | no |
| Postcondition | `EDGE_TRANSFORMED` |

## Purpose

Apply authorized deterministic transformations for the current migration edge

## Preconditions

- `PLAN_FROZEN`

## Input artifacts

- `11-plan/edge-plan.json`
- `06-target/target-state.json`
- `03-graph/file-registry.json`

## Output artifacts

- `transformation-report.json`
- `proposed-changes.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `TRF-001` | Load the frozen plan and open the mutation gateway | Only recipes the plan scheduled may run, and only through the gateway |
| `TRF-002` | Apply scheduled recipes | Every proposal is hashed before and after; the gateway decides, the recipe does not |
| `TRF-003` | Publish the transformation record | Applied, rejected and residual are recorded separately |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/12-transformation/`, including attempts that refuse, fail or crash.
