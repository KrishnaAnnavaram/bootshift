# Stage 05-compatibility

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `CompatibilityStage` |
| Output directory | `05-compatibility` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `COMPATIBILITY_REGISTRY_READY` |

## Purpose

Build repository-independent Tier-1 compatibility and lifecycle knowledge

## Preconditions

- `BASELINE_SEALED`

## Input artifacts

- `02-build/build-model.json`
- `02-build/dependency-model.json`

## Output artifacts

- `compatibility-registry.json`
- `lifecycle-registry.json`
- `artifact-availability.json`
- `version-space-evidence.json`
- `internal-components.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `CMP-001` | Load the build model | Current Spring Boot, Spring Cloud and Java levels come from the resolved build model |
| `CMP-002` | Resolve version space and lifecycle facts | Published versions, support horizons and Spring Cloud trains, from their own sources |
| `CMP-003` | Publish the compatibility registry | UNKNOWN is preserved as UNKNOWN rather than being resolved to compatible |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/05-compatibility/`, including attempts that refuse, fail or crash.
