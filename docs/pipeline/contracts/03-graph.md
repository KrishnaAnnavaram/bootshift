# Stage 03-graph

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `ApplicationGraphStage` |
| Output directory | `03-graph` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `GRAPH_VERIFIED` |

## Purpose

Build and verify the type-aware, multi-view static application graph

## Preconditions

- `BUILD_RESOLVED`

## Input artifacts

- `01-inventory/file-registry.json`
- `02-build/build-model.json`
- `02-build/dependency-model.json`

## Output artifacts

- `application-graph.json`
- `file-registry.json`
- `symbol-registry.json`
- `module-graph.json`
- `file-graph.json`
- `symbol-graph.json`
- `dependency-graph.json`
- `spring-graph.json`
- `configuration-graph.json`
- `persistence-graph.json`
- `endpoint-graph.json`
- `test-graph.json`
- `integration-graph.json`
- `graph-summary.json`
- `graph-issues.json`
- `graph-verification-report.json`
- `graph-verification-report.md`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `GRF-001` | Load registry and build model | The graph is built over identities and modules that already exist |
| `GRF-002` | Select Java source roots per module | Kotlin, Groovy and Scala roots are excluded rather than mis-parsed |
| `GRF-003` | Analyse sources with symbol solving | Type resolution is what makes an edge a fact rather than a name match |
| `GRF-004` | Read configuration files | Configuration keys become graph nodes so property impact is traceable |
| `GRF-005` | Build nodes and edges | Beans, endpoints, entities, repositories, dependencies, configuration |
| `GRF-006` | Verify graph completeness and attribution | A partial graph must be declared partial, not published as complete |
| `GRF-007` | Publish the graph and verification report | Structural and content hashes are recorded separately |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/03-graph/`, including attempts that refuse, fail or crash.
