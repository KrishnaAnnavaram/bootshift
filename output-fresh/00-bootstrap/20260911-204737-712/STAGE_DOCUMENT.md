# Stage 00-bootstrap — RunBootstrap

| Field | Value |
| --- | --- |
| Run | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt | `01M293KJG55B1BR5B5RV0V29VJ` |
| Edge | Not an edge-scoped stage |
| Status | **SUCCESS** |
| Start | 2026-09-11T20:47:35.941203100Z |
| End | 2026-09-11T20:47:37.802149700Z |
| Duration | 1.9 s |

> Workspaces created under C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9; OSS gate passed for 19 harness components

---

## 1. Purpose

Infrastructure preflight: validate the input path, gate the harness bill of materials against OSS policy, create the run identity and workspaces, snapshot the source and initialise the artifact, evidence and state planes. Makes no migration decision.

## 2. Why this stage ran

The pipeline orchestrator reached this stage in sequence.

## 3. State transition

| Before | After |
|  --- |  --- |
| `CREATED` | `OSS_POLICY_VERIFIED` |

## 4. Preconditions

This stage declared no preconditions.

## 5. Input artifacts

This stage consumes no upstream artifacts.

## 6. Planned execution steps

| Step | Name | Purpose |
| --- | --- | --- |
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

## 7. Actual execution steps

| Step | Status | Duration | Note |
| --- | --- | --- | --- |
| `BOOT-001` | SUCCESS | 0 ms | Input path accepted |
| `BOOT-002` | SUCCESS | 3 ms | Every declared component passes the OSS gate |
| `BOOT-003` | SUCCESS | 0 ms | No forbidden recipe estate is loadable |
| `BOOT-004` | SUCCESS | 0 ms | Run workspaces created |
| `BOOT-005` | SUCCESS | 111 ms | Source provenance captured |
| `BOOT-006` | SUCCESS | 456 ms | Original and migration snapshots agree |
| `BOOT-007` | SUCCESS | 1.2 s | Checkpoint repository initialised |
| `BOOT-008` | SUCCESS | 31 ms | Original workspace marked read-only |
| `BOOT-009` | SUCCESS | 8 ms | Run state store initialised |
| `BOOT-010` | SUCCESS | 83 ms | Published and pointer advanced |

## 8. Tools and commands executed

This stage launched no external processes.

> Command arguments are redacted before they are written. Process output is not copied into this document; where a log was captured it is referenced above.

## 9. Decisions made

This stage recorded no decisions.

## 10. Evidence used

This stage referenced no evidence objects.

## 11. Migration documents used

This stage consumed no migration documentation.

## 12. Migration facts used or produced

This stage neither consumed nor produced migration facts.

## 13. Impact analysis involved

No impact findings were involved in this stage.

## 14. Source mutations

This stage does not write to application source.

## 15. Validation performed

This stage performs no validation of its own.

## 16. Retries and fallback paths

### Retries

No retries occurred.

### Fallbacks

The stage completed by its primary method; no fallback was used.

## 17. Warnings

- Input path is inside the harness repository; harness modules are excluded from analysis by the module filter.

## 18. Errors and blockers

No errors.

## 19. Blind spots and unknowns

This stage recorded no blind spots. That is a statement about this stage only.

## 20. Output artifacts

| Artifact | SHA-256 |
| --- | --- |
| `bootstrap.json` | `252ceecb954491e0…` |

Attempt directory: `20260911-204737-712`

## 21. Result

**SUCCESS** — Workspaces created under C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9; OSS gate passed for 19 harness components

The stage did what it declared it would do.

## 22. Next action

Run: bootshift inventory --repo <path>

## 23. Integrity and provenance

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempt id | `01M293KJG55B1BR5B5RV0V29VJ` |
| Stage id | `00-bootstrap` |
| Edge id | — |
| Generated from | `stage-execution.json` |
| Execution record SHA-256 | `a18c4c50a407f40cfef8b180f8519fd6fd0af581d7e6712411afd48eaa1f83e2` |
| Primary artifact hash | `729611458` |
| Policy hash | `401ab5afe78e1845…` |
| Tool version | `1.0.0` |
| Schema version | `1.0.0` |

This document is generated from the execution record named above. The JSON is authoritative; this file is a rendering of it and holds no facts of its own.
