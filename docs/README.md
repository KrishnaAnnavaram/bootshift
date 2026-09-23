# Bootshift documentation

Two kinds of documentation live in this repository, and keeping them apart is the point.

**Design-time** documentation describes what a stage is *supposed* to do. It is checked into the
repository, reviewed like code, and changes when the design changes.

**Runtime** documentation describes what a stage *actually did* during a specific run. It is
generated into `output/` by the execution journal, never written by hand, and is different for every
run.

Both are required. A design document cannot tell you why your migration stopped, and a run document
cannot tell you what the harness was trying to do.

## Design-time

| Path | What it is |
| --- | --- |
| [`pipeline/contracts/`](pipeline/contracts/) | One file per stage: purpose, preconditions, input and output artifacts, declared steps. **Generated from the code** by `bootshift stages --write-contracts` — do not edit by hand |
| [`adr/`](adr/) | Architecture decision records: the decisions that shaped the harness and what they cost |
| [`BUILD-AND-REVIEW-REPORT.md`](BUILD-AND-REVIEW-REPORT.md) | Build and review history |
| [`../README.md`](../README.md) | The complete technical reference, sections 1–63 |
| [`../schemas/`](../schemas/) | JSON schema for every published artifact |

The contracts are generated rather than written because a hand-maintained description of a pipeline
drifts at its own pace. This repository has already shipped documentation for a CLI command that was
never implemented; deriving the contract from the `Stage` interface means it cannot describe a stage
that does not exist, or omit one that does. CI fails if the committed contracts differ from what the
generator produces.

## Runtime

Generated per run, under the run's `output/` directory. Four levels, four scopes:

| Level | Document | Structured source | Scope |
| --- | --- | --- | --- |
| 1 | `STAGE_DOCUMENT.md` | `stage-execution.json` | One stage attempt |
| 2 | `EDGE_DOCUMENT.md` | `edge-execution.json` | One migration edge, stages 12–17 |
| 3 | `RUN_DOCUMENT.md` | `run-timeline.json` | Everything so far |
| 4 | `MIGRATION_DOCUMENT.md` | `migration-result.json`, `evidence-manifest.json` | The finished migration |

The JSON is authoritative in every case. The Markdown is a rendering of it and holds no facts of its
own, which is why a stage document can be regenerated from a `stage-execution.json` months later,
without the run that produced it.

Levels 1–3 are written from the first stage onwards, including for attempts that refuse, fail or
crash. Level 4 is written by `19-evidence` at finalization. That asymmetry is deliberate: a run that
stops at `13-build-repair` never reaches finalization, and a documentation system that only wrote at
the end would have nothing to say about exactly the runs that need explaining.

Find them with:

```bash
bootshift documents                      # everything this run produced
bootshift documents --stage 13-build-repair
bootshift documents --edge EDGE-2-PATCH
```

## Schemas for the journal

[`../schemas/journal/`](../schemas/journal/) holds `stage-execution.schema.json`,
`run-timeline.schema.json` and `edge-execution.schema.json`. Journal artifacts are validated against
them as they are written, and a violation is recorded as a journal failure rather than thrown: the
execution record is the only durable account of what a stage did, and refusing to write a malformed
one would destroy the evidence instead of reporting the defect.
