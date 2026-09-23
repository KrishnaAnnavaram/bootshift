# The runtime documentation architecture

How Bootshift documents itself while it runs, why it is built this way, and what it costs.

## The problem

Bootshift had strong final reporting and no runtime reporting. Everything a reader needed was
reconstructed at stage 19 from the artifact plane, which works exactly as long as the run reaches
stage 19.

Runs that fail do not. A run that stops at `13-build-repair` produced a scatter of stage artifacts
and no account of what happened, and it is precisely that run whose behaviour someone needs
explained. The documentation was weakest at the moment it mattered most.

A second, quieter problem: an artifact records results, and work that never ran produces no results.
Code that was implemented and never invoked therefore left no trace anywhere — a failure mode this
project has already had.

## The shape of the solution

```
Stage contract (design-time, generated into docs/pipeline/contracts/)
        |
        v
StageExecutor.run(stage, context)          <- the one door every stage is entered through
        |
        v
StageExecutionRecorder                     <- wraps the attempt in try/finally
        |
        +--> stage-execution.json          <- authoritative, mandatory
        |
        +--> STAGE_DOCUMENT.md             <- a rendering of the JSON, best-effort
        |
        +--> run-timeline.json  ---> RUN_DOCUMENT.md      (refreshed every attempt)
        |
        +--> edge-execution.json ---> EDGE_DOCUMENT.md    (edge-scoped stages)
        |
        v
19-evidence ---> MIGRATION_DOCUMENT.md + documentation-index.json
```

## Decisions, and what each one buys

### The recorder wraps `StageExecutor`, not each stage

`StageExecutor` is the single door every stage is entered from, so one integration point covers
twenty-one stages and every future one. No stage carries journalling code, and no new stage has to
remember to add any.

This required closing a hole: `StageRunner` in the CLI called `stage.execute(context)` directly,
bypassing `StageExecutor` entirely. Every single-stage CLI invocation therefore skipped precondition
checks — and would have produced no execution record. It now goes through `StageExecutor` like
everything else.

### The lifecycle is built on `finally`

A record created on the success path does not exist for the stage that crashed. Every exit —
success, refusal, block, failure, unhandled throwable — passes through the same finalization, and
the throwable is rethrown afterwards so behaviour is unchanged.

### Attempt directories are discovered, not passed

A stage opens its own `StageWriter`, and the recorder needs to write beside those artifacts.
`OutputLayout.open()` notifies a run-scoped listener, so the recorder learns the directory without
twenty-one stages handing it one. An attempt that refuses before opening a writer is journalled
under `<stage>/journal/<attempt-id>/` instead — `latest.json` never moves, so a refused attempt stays
invisible to every consumer that follows the pointer, while remaining fully documented.

### Attempt identity is a ULID, not a timestamp

Timestamps collide. Two attempts of a fast-failing stage land in the same millisecond, and a
colliding identifier makes retries indistinguishable in exactly the situation where telling them
apart matters. The output directory keeps its timestamp; identity does not depend on it.

### Steps are declared before the stage runs

`Stage.declaredSteps()` returns the plan. The journal marks each step as the stage settles it, and
anything left `PENDING` at the end is reported as a declared capability that was not exercised. A
step list accumulated during execution could only ever contain steps that executed, which is the
half that was already visible.

Steps are settled by the recorder at finalization, not optimistically inside the stage. A stage
cannot know its own outcome while still executing: the target resolver publishes its artifacts and
*then* reports a policy block, and its "select the landing target" step duly reported `SUCCESS` on a
run where no target was selected. Steps still `RUNNING` at the end now take the attempt's outcome.

### Commands are attributed by the journal, not by the runner

`ProcessRunner` knows what it ran; it does not know which stage, edge or attempt it ran for. Teaching
it would mean threading run context through every adapter that shells out. Instead the runner reports
facts to a `CommandObserver` and the journal supplies attribution. `ProcessRunner.observedBy(...)`
returns a runner with identical controls — allowlist, environment sanitisation, timeout, tree
termination are all untouched — that additionally reports what it ran.

Only environment variable *names* are recorded. The values are exactly what must never be written.

### JSON is authoritative; Markdown is a view

The renderers consume the serialized record, not the live object, so a stage document can be
regenerated from a `stage-execution.json` on disk months later — and there is no second reporting
code path that can drift. `RuntimeDocumentTest` asserts that rendering from the object and from
re-parsed JSON produce identical output.

## The documentation-failure policy

**Capturing execution truth is mandatory. Rendering it for humans is best-effort.**

`stage-execution.json` is written first. If Markdown rendering then fails, the failure is:

1. recorded on the record itself as `rendering_error`,
2. added to the journal's failure list,
3. surfaced in `RUN_DOCUMENT.md` section 14 (Gaps),
4. reported by `19-evidence` in `documentation-index.json` as a documentation gap.

It does **not** fail the stage. A formatting defect in a document generator must never turn a stage
that migrated an application correctly into a failed stage — the migration either worked or it did
not, and a renderer has no opinion on that. The defect is loud, attributable and non-fatal.

Two tests hold this policy in place: `renderingFailureDoesNotFailTheStage` and
`runDocumentSurvivesRenderingFailure`.

Schema violations follow the same reasoning. A malformed execution record is still the only durable
account of what a stage did, so it is written and the violation is recorded — refusing to write it
would destroy the evidence instead of reporting the defect. Stage *artifact* publishing keeps its
existing, stricter rule: schema errors there refuse to advance `latest.json`.

## Performance

The journal is an index of a run, not a second copy of it.

- Command output is never copied into a record — line counts and a reference to the redacted log.
- Reference lists are summarised in Markdown above forty entries, with the artifact named.
- The edge aggregate references stage execution records by path rather than inlining them.
- Step details take counts and short scalars; large collections belong in the stage's own artifact.

## What this does not do

- It does not make an undocumented stage self-documenting. A stage that declares no steps produces a
  document that says so — visible, but thin.
- It does not verify that a stage's declared steps are the steps it actually performs. It verifies
  that every declared step was settled, which is a weaker claim.
- It records decisions where stages record them. `06-target` records one per candidate; other stages
  currently record none, and their documents say "This stage recorded no decisions" rather than
  implying none were made.
