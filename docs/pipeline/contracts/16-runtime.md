# Stage 16-runtime

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `RuntimeValidationStage` |
| Output directory | `16-runtime` |
| Edge-scoped | yes |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `EDGE_RUNTIME_GRAPH_ENRICHED` |

## Purpose

Start the migrated application, observe runtime behaviour and enrich the runtime graph

## Preconditions

- `EDGE_SCOPE_VERIFIED`

## Input artifacts

- `11-plan/edge-plan.json`
- `14-graph-diff/application-graph-current.json`
- `04-baseline/baseline-runtime.json`

## Output artifacts

- `runtime-report.json`
- `scenario-observations-new.json`
- `configuration-binding.json`
- `runtime-graph-current.json`
- `application-graph-current-enriched.json`
- `silently-ignored-properties.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `RUN-001` | Prepare the runtime workspace and toolchain | The migrated tree is started on the JDK the edge froze |
| `RUN-002` | Start modules and execute frozen scenarios | Missing infrastructure is recorded as NOT_AVAILABLE; it is never counted as a pass |
| `RUN-003` | Publish the runtime report | Observed, failed and unavailable stay distinguishable |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/16-runtime/`, including attempts that refuse, fail or crash.
