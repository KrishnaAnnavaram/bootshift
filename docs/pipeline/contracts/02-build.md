# Stage 02-build

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `BuildResolverStage` |
| Output directory | `02-build` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `BUILD_RESOLVED` |

## Purpose

Obtain the authoritative effective build model from Maven or Gradle itself

## Preconditions

- `FILE_REGISTRY_SEALED`

## Input artifacts

- `01-inventory/inventory-artifact.json`
- `01-inventory/file-registry.json`

## Output artifacts

- `build-model.json`
- `dependency-model.json`
- `bom-model.json`
- `plugin-model.json`
- `repository-model.json`
- `resolution-issues.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `BLD-001` | Load the inventory artifact | The build model is resolved over files inventory already gave identity to |
| `BLD-002` | Detect the build system | Maven, Gradle or a mixed composite; never flattened to one |
| `BLD-003` | Resolve the effective build model | Invokes the build tool so the model is authoritative, not descriptor-guessed |
| `BLD-004` | Classify dependency resolution | An unresolved coordinate makes version-space analysis unreliable |
| `BLD-005` | Derive Java levels and frameworks | Records the level each module declares, not the newest available |
| `BLD-006` | Publish the build, dependency and BOM models | One serialized contract every later stage rehydrates through |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/02-build/`, including attempts that refuse, fail or crash.
