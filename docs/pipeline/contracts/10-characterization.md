# Stage 10-characterization

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `CharacterizationStage` |
| Output directory | `10-characterization` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `CHARACTERIZATION_COMPLETE` |

## Purpose

Establish behavioural contracts for migration-sensitive impacts before they change

## Preconditions

- `IMPACT_ANALYZED`

## Input artifacts

- `09-impact/impact-report.json`
- `03-graph/application-graph.json`
- `04-baseline/baseline-runtime.json`
- `04-baseline/baseline-tests.json`

## Output artifacts

- `characterization-report.json`
- `characterization-contracts.json`
- `characterization-scenarios.json`
- `characterization-old-observations.json`
- `characterization-gaps.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `CHR-001` | Identify unprotected behaviour | Behaviour with no test covering it is what a differential later has to speak for |
| `CHR-002` | Execute scenarios against the pre-migration application | A scenario is frozen only when it actually ran on OLD; anything else is NOT_EXECUTED |
| `CHR-003` | Publish characterization scenarios | The frozen observations become the oracle the migration is compared against |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/10-characterization/`, including attempts that refuse, fail or crash.
