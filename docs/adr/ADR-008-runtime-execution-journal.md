# ADR-008: Runtime documentation is a journal, not a report

**Status:** Accepted
**Date:** 2026-09-10

## Context

The harness produced excellent documentation at stage 19 and almost none before it. Everything a
reader needed — what ran, in what order, on what evidence, with what result — was reconstructed at
finalization from the artifact plane.

That works for runs that finish. It fails for runs that do not, and those are the runs that need
explaining. A run stopping at `13-build-repair` left a scatter of stage artifacts and no account of
what had happened; the reader most in need of an explanation was the one guaranteed not to get one.

There was a second gap with the same root. An artifact records *results*. Work that never ran
produces no results, so a stage step that was implemented and never invoked left no trace anywhere.
This project has already shipped exactly that defect.

Both are the same mistake: deriving the record of a run from its outputs, when the thing that needs
recording is its *execution*.

## Decision

Documentation is produced **as the run happens**, by a journal wrapped around every stage attempt,
rather than reconstructed afterwards by a reporter.

Four consequences follow, and each is load-bearing:

1. **One integration point.** The journal wraps `StageExecutor`, the single door every stage is
   entered through, in a `try/finally`. Success, refusal, policy block, failure and unhandled
   throwable all finalize the same way. No stage carries journalling code.

2. **Steps are declared, not accumulated.** `Stage.declaredSteps()` states the plan before the stage
   runs. Anything unsettled at the end is reported as a declared capability that was not exercised.
   This is the only mechanism in the harness that can see work which did not happen.

3. **Structured record is authoritative; Markdown is a view.** `stage-execution.json` is written
   first and the document is rendered from it, so there is no second reporting path to drift, and a
   document can be regenerated from a record on disk without the run that produced it.

4. **Truth capture and rendering have different failure policies.** The record is mandatory. The
   rendering is best-effort: a formatting defect must never turn a stage that migrated an
   application correctly into a failed one. A rendering failure is recorded on the record, surfaced
   in the run document and reported by stage 19 as a documentation gap.

The final `MIGRATION_DOCUMENT.md` is retained unchanged in role. It is now backed by the journal
rather than being the only thing that exists.

## Consequences

- A run that stops anywhere has a complete account up to the stop, including the stop itself, its
  cause and the remediation. `RUN_DOCUMENT.md` exists from the first stage onward.
- Every external process is attributable to a stage, edge and attempt, with exit code, duration and
  working directory, and with arguments redacted before they are written.
- Attempts are immutable and identified by ULID rather than by the timestamp of their output
  directory, so retries in the same millisecond stay distinguishable.
- Instrumenting the recorder found two real defects that had been invisible: single-stage CLI
  invocations bypassed `StageExecutor` entirely and so skipped precondition checks, and stages that
  publish before reporting a policy block were marking their work steps successful on runs where the
  work had not succeeded.
- The cost is roughly a millisecond and a few kilobytes per attempt, plus a declared step plan per
  stage. Command output is referenced rather than copied, and long reference lists are summarised.
- Design-time stage contracts are now generated from the `Stage` interface into
  `docs/pipeline/contracts/`, and CI fails if the committed contracts differ from what the generator
  produces. A hand-maintained contract is a second description of the pipeline that drifts at its
  own pace, which is the same failure this repository already had with CLI documentation.

## Alternatives considered

**Extend `StageResult`.** Rejected. `StageResult` is the stage's answer to its caller — an exit code,
a summary, artifacts — returned by value through the whole CLI. Widening it into an audit object
pushes the cost of auditing onto every path that only wanted to know whether the stage worked, and it
still would not exist for a stage that threw.

**Derive documentation from telemetry.** Rejected. Telemetry is diagnostic output and is allowed to
be lossy, sampled or absent. Artifacts are the source of truth in this harness (R23), and a document
built from a channel that may drop events cannot be evidence.

**Reconstruct at stage 19 from richer artifacts.** Rejected. It preserves the original defect: the
run that never reaches 19 gets nothing, and no artifact can record work that did not happen.
