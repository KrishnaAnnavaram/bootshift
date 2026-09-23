# Stage 15-test

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `TestValidationStage` |
| Output directory | `15-test` |
| Edge-scoped | yes |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `EDGE_TESTED` |

## Purpose

Run the application test suite and classify results against baseline and previous edge

## Preconditions

- `EDGE_SCOPE_VERIFIED`

## Input artifacts

- `04-baseline/baseline-tests.json`
- `04-baseline/baseline-coverage.json`
- `11-plan/edge-plan.json`

## Output artifacts

- `test-report.json`
- `coverage-report.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `TST-001` | Resolve the toolchain and baseline results | Two baselines: the sealed pre-migration run and the edge-local previous state |
| `TST-002` | Run tests and classify outcomes | A pre-existing failure and a migration-caused regression are different findings and are never merged |
| `TST-003` | Publish the test report | Deleted, disabled and weakened tests are reported, not just pass counts |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/15-test/`, including attempts that refuse, fail or crash.
