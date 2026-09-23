# LLM-as-Judge — the runtime documentation architecture, over a fresh full run

**Run judged:** `RUN-01M293KJ0RYV603B3JB0CCJJN9`
**Judged:** 2026-09-11
**Scope:** the execution journal and the four documentation levels added in this change
**Decision:** `ACCEPT_WITH_REPAIRS` — the architecture holds; three findings are material

Written by reading the published artifact plane, the execution journal, the change ledger and the
run workspace. Not the console log: the console is diagnostic output, and a critique built on it
would be judging what the harness *said* rather than what it *did*.

---

## 1. What the run actually did

| Half | Stages | Result |
|---|---|---|
| Analysis | 00 bootstrap → 11 plan | **All SUCCESS** |
| Edge loop | 12 → 17 for `EDGE-1-PREP-TEST` | **COMPLETE** |
| Edge loop | 12 → 17 for `EDGE-2-PATCH` | **BLOCKED at 17-differential** |
| Edge loop | `EDGE-3` … `EDGE-8` | **Never started** |
| Finalization | 18 approval, 19 evidence, 20 provenance | **Never executed** |

24 stage attempts. Two of eight planned edges attempted; one completed. Exit code 3.

The block is legitimate and reproducible: `SCN-00047`, `CONFIGURATION_BINDING`, `discovery-service`,
`/actuator/configprops`, field `body_field_paths` — the **same scenario, module and field** as the
project's earlier independent run. Two runs, months of code change apart, stopping on the identical
difference is a strong reproducibility signal for the differential layer.

## 2. Invariants: verified

| Invariant | Verdict | Evidence |
|---|---|---|
| `./src` never modified | **HOLDS** | `0d5d57f1…0501` identical before and after; `git status` clean |
| Every attempt is documented | **HOLDS** | 24/24 attempts have both `stage-execution.json` and `STAGE_DOCUMENT.md`; 0 missing |
| Journal never failed | **HOLDS** | `journal_failures: []` across 24 attempts |
| Gateway is the only writer | **HOLDS** | 19 ledger events, all `APPLIED`, all `agent=12-transformation`, all `provider=bootshift-transformers` |
| Every change carries full attribution | **HOLDS** | 19/19 carry `before_sha256`, `after_sha256`, `file_id`, `patch_ref`, `knowledge_refs`, `impact_refs` |
| AI proposes, never authorizes | **HOLDS** | 0 AI attempts used in repair; all 19 changes deterministic |
| Tests never weakened | **HOLDS** | 12 tests before and after; 2 classified `PRE_EXISTING_FAILURE`, not suppressed |
| Unexplained difference blocks | **HOLDS** | `UNEXPLAINED=1` → BLOCKED; run stopped rather than reporting success |
| No secret reaches a document | **HOLDS** | Credential-pattern scan over every `.md` and journal artifact: clean |
| Commands are attributable | **HOLDS** | 415 commands, each bound to stage + edge + attempt; 10 failed, 0 timed out |

## 3. Findings

### F1 — The gap reporter is unreachable on exactly the runs it was built for · **MATERIAL**

`documentation-index.json` is the artifact that reports missing or failed documents. It is written
by `19-evidence`. `19-evidence` never executes on a blocked run.

This run blocked. So the one artifact designed to say "these documents are missing" was not produced —
on the run type whose documentation was the entire justification for the change. The architecture
document I wrote claims documentation gaps are "reported by `19-evidence` in
`documentation-index.json`", and on a blocked run that sentence is false.

The gap-reporting is gated behind the happy path. It should be written by the journal after every
attempt, beside `run-timeline.json`, exactly as the run document is.

### F2 — Decisions are recorded by one stage out of twenty-one · **MATERIAL**

All 10 decisions in this run come from `06-target`. Every other stage recorded none, and their
documents say *"This stage recorded no decisions."*

That sentence is literally true and materially misleading. `08-knowledge` decided the status of 1,687
facts. `09-impact` classified certainty for 480+ findings. `11-plan` assigned `FULL_DIFFERENTIAL` and
computed a 0.822 coverage figure. `13-build-repair` decided to stop repairing after one round.
`17-differential` classified one difference as `UNEXPLAINED` and thereby halted the migration — the
single most consequential judgement in the run, recorded as a differential artifact but **not as a
decision**.

The decision record was built to make "why did the harness do that" answerable in one place. With one
stage populating it, the journal answers that question for target selection and for nothing else.

### F3 — `unexecuted_step_count: 0` proves far less than it appears · **MATERIAL**

Every attempt reports zero unexecuted steps. That reads as a strong result. It is not.

Seventeen of twenty-one stages declare only a three-step spine — load inputs, do the work, publish.
A three-step plan is nearly impossible to leave unexecuted, so the planned-versus-executed contract
is satisfied in form while being weakest precisely where it would matter: `13-build-repair`'s repair
rounds, `17-differential`'s normalisation, `08-knowledge`'s verification channels are each a single
undifferentiated "work" step.

Only `00-bootstrap` (10 steps), `01-inventory` (9), `02-build` (6) and `03-graph` (7) carry plans
detailed enough for an omission to be visible. The mechanism is sound and proven by test; its
*coverage* is thin, and the headline number invites more confidence than the instrumentation earns.

### F4 — Planned-but-unattempted edges have no document · MINOR

The plan names 8 edges. `edges/` holds 2. A reader working from the run tree cannot see that six
edges were planned and never started without opening the plan artifact.

`EdgeExecutionAggregate` already models this correctly — it returns `NOT_STARTED` and the renderer
says "planned and never attempted" — and there is a passing test for it. Nothing calls it for an
unattempted edge, because edge documents are only refreshed by edge-scoped attempts. A capability
built, tested, and never invoked: the exact failure mode the declared-step mechanism exists to catch,
reproduced one level up.

### F5 — Floating-point noise in an audit document · MINOR

`Expected residual | 0.030299999999999994`. Raw double, rendered unformatted.

### F6 — The behavioural evidence base is thinner than the pass rate suggests · NOTED, not a defect

Edge 2's differential: 35 `IDENTICAL`, 46 `NOT_COMPARED`, 1 `UNEXPLAINED`. Only 43% of comparisons
produced a usable result, because 2 of 6 modules did not start — one application failure, one
MongoDB timeout with no container runtime available (Docker installed, daemon not answering).

The harness handles this correctly: `NOT_COMPARED` is not a pass, and the runtime report records
`PROCESS_EXITED` and `TIMEOUT` explicitly. It is recorded here so nobody reads "35 IDENTICAL" as
"35 of 35".

## 4. Claims stronger than evidence

Assessed against what I claimed for this change:

| Claim | Verdict |
|---|---|
| "Every stage attempt creates `stage-execution.json` and `STAGE_DOCUMENT.md`" | **Supported.** 24/24, including the blocked attempt |
| "Failed, refused and crashed stages are documented" | **Supported** for blocked and refused by real runs; crash path by test only — no stage crashed in any run |
| "Commands are traceable to stage, edge and attempt" | **Supported.** 415 commands |
| "Decisions are recorded" | **Overstated.** One stage of twenty-one. See F2 |
| "Documentation gaps are reported by the evidence stage" | **False on blocked runs.** See F1 |
| "Planned versus executed steps are visible" | **Supported mechanically, thin in coverage.** See F3 |
| "`EDGE_DOCUMENT.md` exists for attempted edges" | **Supported** — and correctly *not* claimed for unattempted ones, which is F4 |

## 5. Decision

`ACCEPT_WITH_REPAIRS`.

The architecture does what it was built to do. The run that blocked is fully explained by its own
artifacts, which is the thing that was missing before and is the whole point. Every invariant that
protects the migration held, and the two independent runs blocking on the identical difference is
better evidence for the differential layer than anything in the test suite.

Three repairs are warranted before this is presented as finished:

1. **F1** — move documentation-gap reporting out of `19-evidence` into the journal, written after
   every attempt. A gap reporter that requires a successful run is not a gap reporter.
2. **F2** — record decisions in at least `08-knowledge`, `11-plan`, `13-build-repair` and
   `17-differential`. The `UNEXPLAINED` classification that halted this migration must appear as a
   decision record.
3. **F3** — deepen the step plans for the edge stages, or stop reporting a zero count as though it
   were a strong signal.

F4 and F5 are small and should be fixed with them.

## 6. What a repair pass may not do

- It may not weaken the differential to clear `SCN-00047`. That block is correct.
- It may not make documentation rendering fatal to a stage.
- It may not record a decision the harness did not actually make, to improve a count.
- It may not claim the finalization stages work until a run reaches them.
