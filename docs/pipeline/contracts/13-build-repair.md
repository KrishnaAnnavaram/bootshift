# Stage 13-build-repair

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `BuildRepairStage` |
| Output directory | `13-build-repair` |
| Edge-scoped | yes |
| May write to application source | **yes** |
| May consult the optional AI provider | yes, and never as an authority |
| Postcondition | `EDGE_COMPILED` |

## Purpose

Compile the migrated state and repair bounded residual compile failures

## Preconditions

- `EDGE_TRANSFORMED`

## Input artifacts

- `11-plan/edge-plan.json`
- `08-knowledge/migration-knowledge.json`
- `02-build/build-model.json`

## Output artifacts

- `build-report.json`
- `repair-report.json`
- `diagnostics.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `RPR-001` | Resolve the frozen toolchain | Compiling on a different JDK than the edge froze would produce evidence about the wrong toolchain |
| `RPR-002` | Compile and repair within budget | Diagnostics are clustered by root cause; repairs are deterministic, and an AI proposal is verified deterministically before the gateway is ever asked |
| `RPR-003` | Publish the build and repair record | Records why repair stopped, which matters as much as whether it succeeded |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/13-build-repair/`, including attempts that refuse, fail or crash.
