# Stage 01-inventory

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `InventoryStage` |
| Output directory | `01-inventory` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `FILE_REGISTRY_SEALED` |

## Purpose

Discover what repository was received and allocate permanent file identities

## Preconditions

- `OSS_POLICY_VERIFIED`

## Input artifacts

This stage consumes no published artifacts.

## Output artifacts

- `inventory-artifact.json`
- `file-registry.json`
- `inventory-signals.json`
- `inventory-issues.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `INV-001` | Resolve the scan root | Prefer the immutable original snapshot over the caller's own directory |
| `INV-002` | Load or create the file registry | A rescan reattaches existing identities instead of minting new ones |
| `INV-003` | Enumerate files | Walks the tree, applying the directory exclusion list |
| `INV-004` | Classify roles and hash content | Assigns a FileRole and a SHA-256 to every readable file |
| `INV-005` | Allocate or reattach permanent FILE_IDs | Identity has to survive a rename, or lineage breaks at the first move |
| `INV-006` | Collect migration-relevant signals | Records where migration-significant constructs appear, by FILE_ID |
| `INV-007` | Scan for exposed secrets | Findings are recorded as metadata; no secret value is ever stored |
| `INV-008` | Seal the file registry | Freezes identity for the run so later stages cannot renumber it |
| `INV-009` | Publish inventory artifacts | Schema-validated, then the pointer advances |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/01-inventory/`, including attempts that refuse, fail or crash.
