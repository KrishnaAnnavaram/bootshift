# Stage 18-approval

> Generated from the stage contract in code by `bootshift stages --write-contracts`.
> Do not edit by hand; edit the stage and regenerate.

| | |
|  --- |  --- |
| Implementation | `ApprovalStage` |
| Output directory | `18-approval` |
| Edge-scoped | no |
| May write to application source | no |
| May consult the optional AI provider | no |
| Postcondition | `FINAL_APPROVAL` |

## Purpose

Raise and record the human decisions the harness must not make for itself

## Preconditions

- `EDGE_COMPLETE`

## Input artifacts

This stage consumes no published artifacts.

## Output artifacts

- `approval-report.json`
- `approval-requests.json`
- `manifest.json`

## Declared steps

Declared before the stage runs, so a step that never executes is reported rather than absent.

| Step | Name | Purpose |
|  --- |  --- |  --- |
| `APR-001` | Derive approval gates from the evidence so far | Gates come from what validation found, not from a fixed checklist |
| `APR-002` | Match gates against filed human decisions | Decisions arrive from outside the run and are integrity-hashed; the harness never files one for itself |
| `APR-003` | Publish the approval report | Outstanding requests are listed explicitly |

## Runtime documentation

Every attempt at this stage writes `stage-execution.json` and `STAGE_DOCUMENT.md` into its own attempt directory under `output/18-approval/`, including attempts that refuse, fail or crash.
