# Stage 07-documentation

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `DocumentationStage` |
| Output directory | `07-documentation` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `DOCUMENTATION_RETRIEVED` |

## Purpose

Fetch and content-address the authoritative documents for the frozen migration path

## Preconditions

- `TARGET_FROZEN`

## Input artifacts

- `06-target/target-state.json`
- `06-target/migration-path.json`
- `02-build/build-model.json`

## Output artifacts

- `document-registry.json`
- `document-coverage.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `DOC-001` | Determine which components and edges need documentation | Retrieval is driven by the frozen path, not by a fixed list of URLs |
| `DOC-002` | Retrieve and verify authoritative sources | Allowlist, version applicability and content hash decide acceptance; a failed retrieval is recorded as a failure rather than silently skipped |
| `DOC-003` | Publish the document registry | Every accepted document is pinned by hash so a later run can prove what it read |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/07-documentation/`, including attempts that refuse, fail or crash.
