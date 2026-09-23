# Stage 00-bootstrap

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `RunBootstrap` |
| Output directory | `00-bootstrap` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `WORKSPACE_READY, OSS_POLICY_VERIFIED` |

## Purpose

Infrastructure preflight: validate the input path, gate the harness bill of materials against OSS policy, create the run identity and workspaces, snapshot the source and initialise the artifact, evidence and state planes. Makes no migration decision.

> Infrastructure preflight, explicitly NOT an agent. It may not perform migration analysis or make any migration decision. Inventory remains the first analysis stage.

## Preconditions

None declared.

## Input artifacts

This stage consumes no published artifacts.

## Output artifacts

- `bootstrap.json`
- `oss-license-gate.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `BOOT-001` | Resolve and validate source root | Refuse a path that is not a directory before anything is created |
| `BOOT-002` | Gate the harness bill of materials | Every component the harness itself loads is checked against the OSS policy |
| `BOOT-003` | Probe for forbidden recipe estates on the classpath | A declared coordinate list cannot see what arrived transitively |
| `BOOT-004` | Create run workspaces | original, migration, runtime-old, runtime-new, checkpoint, evidence and state |
| `BOOT-005` | Capture source provenance | Records what was handed to the harness, independently of what it becomes |
| `BOOT-006` | Snapshot source into original and migration workspaces | Both snapshots must hash identically or the run cannot be trusted |
| `BOOT-007` | Initialise the internal checkpoint repository | Checkpoint history lives in the external workspace, never in the user input |
| `BOOT-008` | Mark the original workspace read-only | The immutable side of the differential must not be writable |
| `BOOT-009` | Initialise the run state store | Creates the run record the state machine writes to |
| `BOOT-010` | Publish the bootstrap artifact and OSS licence gate | Proves WORKSPACE_READY and OSS_POLICY_VERIFIED to every later precondition |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/00-bootstrap/`, including attempts that refuse, fail or crash.
