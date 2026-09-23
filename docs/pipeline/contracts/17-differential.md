# Stage 17-differential

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `DifferentialStage` |
| Output directory | `17-differential` |
| Edge-scoped | yes |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `EDGE_DIFFERENTIAL_VALIDATED` |

## Purpose

Compare original and migrated behaviour for the required dimensions

## Preconditions

- `EDGE_RUNTIME_GRAPH_ENRICHED`

## Input artifacts

- `11-plan/edge-plan.json`
- `10-characterization/characterization-contracts.json`
- `04-baseline/baseline-runtime.json`
- `16-runtime/runtime-report.json`

## Output artifacts

- `differential-report.json`
- `normalization-policy.json`
- `environment-equivalence-check.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `DIF-001` | Load frozen observations from both sides | A comparison needs a successful observation on OLD and on NEW |
| `DIF-002` | Normalise and compare | Normalisation is policy-driven and hashed, so a difference cannot be explained away by changing the rules afterwards |
| `DIF-003` | Publish the differential report | An unexplained difference blocks; that is the point of the stage |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/17-differential/`, including attempts that refuse, fail or crash.
