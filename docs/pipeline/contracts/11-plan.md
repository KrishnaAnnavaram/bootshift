# Stage 11-plan

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `PlannerStage` |
| Output directory | `11-plan` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `PLAN_FROZEN` |

## Purpose

Plan and freeze how the frozen migration path will actually be executed

## Preconditions

- `CHARACTERIZATION_COMPLETE`

## Input artifacts

- `06-target/migration-path.json`
- `08-knowledge/migration-knowledge.json`
- `09-impact/impact-report.json`
- `10-characterization/characterization-contracts.json`

## Output artifacts

- `migration-plan.json`
- `edge-plan.json`
- `transformation-capability-registry.json`
- `residual-report.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `PLN-001` | Load facts, impacts and the frozen path | The plan is assembled from verified knowledge and measured impact |
| `PLN-002` | Compute coverage and build the edge plan | Facts with a transformer, facts without one and unknowns are counted separately, so deterministic coverage can never be read as knowledge completeness |
| `PLN-003` | Freeze the plan | Recipes, ordering, validation depth and toolchain stop being negotiable here |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/11-plan/`, including attempts that refuse, fail or crash.
