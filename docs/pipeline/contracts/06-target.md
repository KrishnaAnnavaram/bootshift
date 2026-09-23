# Stage 06-target

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `TargetResolverStage` |
| Output directory | `06-target` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `TARGET_FROZEN` |

## Purpose

Choose the landing target and the transit checkpoints that reach it

## Preconditions

- `COMPATIBILITY_REGISTRY_READY`

## Input artifacts

- `05-compatibility/lifecycle-registry.json`
- `05-compatibility/compatibility-registry.json`
- `05-compatibility/artifact-availability.json`
- `05-compatibility/internal-components.json`

## Output artifacts

- `target-state.json`
- `migration-path.json`
- `target-resolution-report.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `TGT-001` | Load compatibility registry and build model | Candidate targets are drawn from verified lifecycle data, not from a hard-coded list |
| `TGT-002` | Filter candidates and select the landing target | Every rejected candidate keeps the rule that rejected it, so the choice is auditable |
| `TGT-003` | Freeze the migration path | Splits the jump into edges at major boundaries and freezes the toolchain for each |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/06-target/`, including attempts that refuse, fail or crash.
