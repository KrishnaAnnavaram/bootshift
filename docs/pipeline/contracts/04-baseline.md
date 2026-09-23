# Stage 04-baseline

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `BaselineStage` |
| Output directory | `04-baseline` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `BASELINE_SEALED` |

## Purpose

Observe and cryptographically seal the original application's behaviour

## Preconditions

- `GRAPH_VERIFIED`

## Input artifacts

- `02-build/build-model.json`
- `03-graph/application-graph.json`

## Output artifacts

- `baseline-build.json`
- `baseline-tests.json`
- `baseline-coverage.json`
- `baseline-configuration.json`
- `baseline-runtime.json`
- `environment-equivalence.json`
- `runtime-graph-baseline.json`
- `application-graph-baseline-enriched.json`
- `baseline-manifest.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `BAS-001` | Load graph, build model and registry | The baseline is measured against the structures the analysis half already established |
| `BAS-002` | Capture the immutable baseline | Toolchain selection, build, tests and runtime starts on the pre-migration tree, plus the pre-existing failures that must never be attributed to the migration |
| `BAS-003` | Seal and publish the baseline manifest | Once sealed the baseline cannot be re-measured; every later comparison binds to this hash |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/04-baseline/`, including attempts that refuse, fail or crash.
