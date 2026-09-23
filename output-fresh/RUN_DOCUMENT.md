# Bootshift Migration Run Document

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Source | `E:/Virtusa Projects/bootshift/src` |
| Started | 2026-09-11T20:47:35.941203100Z |
| Current status | **BLOCKED — human decision required** |
| Current stage | `17-differential` |
| Current edge | `EDGE-2-PATCH` |
| Migrating from | `2.7.12` |
| Landing target | `3.5.16` |
| Document generated | 2026-09-11T21:32:33.335866800Z |

> This document is rewritten after every stage attempt. It describes what has happened so far, not what is planned. If the run is still going, this is a snapshot.

---

## 1. Executive summary

| Measure | Value |
| --- | --- |
| Stage attempts | 24 |
| Succeeded | 23 |
| Degraded | 0 |
| Refused | 0 |
| Blocked | 1 |
| Failed or crashed | 0 |
| External commands run | 415 |
| Edges planned | 8 |

## 2. Current pipeline state

| Field | Value |
| --- | --- |
| Last stage attempted | `17-differential` |
| Attempt id | `01M2965WEJAND58F0PASTQANF8` |
| Outcome | **BLOCKED** |
| Run state after | `BLOCKED` |
| Stop reason | Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s) |

## 3. Source application summary

| Property | Value |
| --- | --- |
| Files | 99 |
| Modules | 6 |
| Build system | MAVEN |
| Build model authoritative | yes |
| Spring Boot | `2.7.12` |
| Spring Cloud | `2021.0.7` |
| Spring Framework | `5.3.27` |
| Java | `17` |

## 4. Target decision

| Field | Value |
| --- | --- |
| From | `2.7.12` on Java `17` |
| Landing target | `3.5.16` |
| Landing Java | `21.0.11` |
| Spring Cloud train | `2025.0.3` |
| Selection mode | AUTO_HIGHEST_SAFE_SUPPORTED |
| Support horizon | -2 month(s) |
| Lifecycle evidence | VERIFIED |
| Frozen | yes |

Why this target and not the others: section 9 of the `06-target` stage document records one decision per candidate, each with the rule that rejected it.

## 5. Migration path

| Edge | Class | From | To | Java | Risk | Validation depth |
| --- | --- | --- | --- | --- | --- | --- |
| `EDGE-1-PREP-TEST` | PREPARATORY | `2.7.12` | `2.7.12` | `17` | HIGH | FULL_DIFFERENTIAL |
| `EDGE-2-PATCH` | PATCH | `2.7.12` | `2.7.18` | `17` | HIGH | FULL_DIFFERENTIAL |
| `EDGE-3-MAJOR-3` | MAJOR_BOUNDARY | `2.7.18` | `3.0.13` | `17` | HIGH | FULL_DIFFERENTIAL |
| `EDGE-4-MINOR` | MINOR | `3.0.13` | `3.1.12` | `21` | HIGH | FULL_DIFFERENTIAL |
| `EDGE-5-MINOR` | MINOR | `3.1.12` | `3.2.12` | `21` | HIGH | FULL_DIFFERENTIAL |
| `EDGE-6-MINOR` | MINOR | `3.2.12` | `3.3.13` | `21` | HIGH | FULL_DIFFERENTIAL |
| `EDGE-7-MINOR` | MINOR | `3.3.13` | `3.4.13` | `21` | HIGH | FULL_DIFFERENTIAL |
| `EDGE-8-LANDING` | LANDING | `3.4.13` | `3.5.16` | `21` | HIGH | FULL_DIFFERENTIAL |

Each edge's full record - facts in force, impacts, recipes and what each stage did - is in `edges/<edge-id>/EDGE_DOCUMENT.md` once the edge has been attempted.

## 6. Stage timeline

| Stage | Edge | Status | Duration | Started |
| --- | --- | --- | --- | --- |
| `00-bootstrap` | — | SUCCESS | 1.9 s | 2026-09-11T20:47:35.941203100Z |
| `01-inventory` | — | SUCCESS | 355 ms | 2026-09-11T20:47:37.902532500Z |
| `02-build` | — | SUCCESS | 1m 59s | 2026-09-11T20:47:38.290444300Z |
| `03-graph` | — | SUCCESS | 4.3 s | 2026-09-11T20:49:37.632729400Z |
| `04-baseline` | — | SUCCESS | 9m 26s | 2026-09-11T20:49:42.006109100Z |
| `05-compatibility` | — | SUCCESS | 23.4 s | 2026-09-11T20:59:08.864803500Z |
| `06-target` | — | SUCCESS | 419 ms | 2026-09-11T20:59:32.257125700Z |
| `07-documentation` | — | SUCCESS | 12.2 s | 2026-09-11T20:59:32.692322800Z |
| `08-knowledge` | — | SUCCESS | 6m 46s | 2026-09-11T20:59:44.935608400Z |
| `09-impact` | — | SUCCESS | 2.8 s | 2026-09-11T21:06:31.684254Z |
| `10-characterization` | — | SUCCESS | 3m 47s | 2026-09-11T21:06:34.533091200Z |
| `11-plan` | — | SUCCESS | 2.9 s | 2026-09-11T21:10:21.894565600Z |
| `12-transformation` | `EDGE-1-PREP-TEST` | SUCCESS | 1.5 s | 2026-09-11T21:10:24.897174800Z |
| `13-build-repair` | `EDGE-1-PREP-TEST` | SUCCESS | 1m 23s | 2026-09-11T21:10:26.435953Z |
| `14-graph-diff` | `EDGE-1-PREP-TEST` | SUCCESS | 5.7 s | 2026-09-11T21:11:50.342331300Z |
| `15-test` | `EDGE-1-PREP-TEST` | SUCCESS | 5m 19s | 2026-09-11T21:11:56.205810700Z |
| `16-runtime` | `EDGE-1-PREP-TEST` | SUCCESS | 4m 39s | 2026-09-11T21:17:15.468935900Z |
| `17-differential` | `EDGE-1-PREP-TEST` | SUCCESS | 280 ms | 2026-09-11T21:21:55.145497400Z |
| `12-transformation` | `EDGE-2-PATCH` | SUCCESS | 1.5 s | 2026-09-11T21:21:55.686362400Z |
| `13-build-repair` | `EDGE-2-PATCH` | SUCCESS | 47.0 s | 2026-09-11T21:21:57.209499500Z |
| `14-graph-diff` | `EDGE-2-PATCH` | SUCCESS | 2.1 s | 2026-09-11T21:22:44.229246200Z |
| `15-test` | `EDGE-2-PATCH` | SUCCESS | 4m 50s | 2026-09-11T21:22:46.390741800Z |
| `16-runtime` | `EDGE-2-PATCH` | SUCCESS | 4m 56s | 2026-09-11T21:27:36.550476200Z |
| `17-differential` | `EDGE-2-PATCH` | **BLOCKED** | 193 ms | 2026-09-11T21:32:33.106283100Z |

Machine-readable form: `run-timeline.json`

## 7. Stage execution summaries

### 00-bootstrap — SUCCESS

Workspaces created under C:\Users\annav\AppData\Local\Temp\bootshift-workspaces\RUN-01M293KJ0RYV603B3JB0CCJJN9; OSS gate passed for 19 harness components

Duration 1.9 s · 0 command(s) · 0 decision(s) · 1 warning(s)

Detail: `00-bootstrap/20260911-204737-712/STAGE_DOCUMENT.md`

### 01-inventory — SUCCESS

99 files across 6 module(s); 104 migration signal(s); registry sealed as b3104303c3acb64fd340c733848a88ac76b26b2a8bc6a42892400ff870a6787d (content 3982fd4e3e180eeb)

Duration 355 ms · 0 command(s) · 0 decision(s)

Detail: `01-inventory/20260911-204738-128/STAGE_DOCUMENT.md`

### 02-build — SUCCESS

MAVEN model: 6 module(s), 962 dependency record(s), 9186 managed version(s)

Duration 1m 59s · 27 command(s) · 0 decision(s)

Detail: `02-build/20260911-204937-146/STAGE_DOCUMENT.md`

### 03-graph — SUCCESS

839 nodes / 1852 edges, 239 symbols, attribution 0.6858, content hash 5b764c29af1e

Duration 4.3 s · 0 command(s) · 0 decision(s)

Detail: `03-graph/20260911-204941-679/STAGE_DOCUMENT.md`

### 04-baseline — SUCCESS

Baseline sealed as dd491b5a6685134a (6 module(s) built, 12 test(s), 4/6 runtime probe(s) started)

Duration 9m 26s · 28 command(s) · 0 decision(s) · 3 warning(s)

Detail: `04-baseline/20260911-204942-332/STAGE_DOCUMENT.md`

### 05-compatibility — SUCCESS

Current state Spring Boot 2.7.12 / Java 17 / Spring Cloud 2021.0.7; 9 candidate line(s), 0 unavailable, 0 unknown internal component(s)

Duration 23.4 s · 0 command(s) · 0 decision(s)

Detail: `05-compatibility/20260911-205908-963/STAGE_DOCUMENT.md`

### 06-target — SUCCESS

Landing target Spring Boot 3.5.16 (Java 21, Spring Cloud 2025.0.3), 8 migration edge(s), -2 month support horizon

Duration 419 ms · 2 command(s) · 10 decision(s) · 5 warning(s)

Detail: `06-target/20260911-205932-627/STAGE_DOCUMENT.md`

### 07-documentation — SUCCESS

21 document(s) pinned for 7 edge(s); 7 official migration guide(s)

Duration 12.2 s · 0 command(s) · 0 decision(s)

Detail: `07-documentation/20260911-205932-757/STAGE_DOCUMENT.md`

### 08-knowledge — SUCCESS

1691 migration fact(s): 1683 VERIFIED, 8 CANDIDATE, 0 CONFLICTING for 2.7.12 -> 3.5.16

Duration 6m 46s · 286 command(s) · 0 decision(s) · 1 warning(s)

Detail: `08-knowledge/20260911-205944-957/STAGE_DOCUMENT.md`

### 09-impact — SUCCESS

2072 impact finding(s) across 27 file(s); {DEFINITELY_AFFECTED=502, POSSIBLY_AFFECTED=4, UNAFFECTED_WITHIN_OBSERVED_COVERAGE=1566}

Duration 2.8 s · 0 command(s) · 0 decision(s)

Detail: `09-impact/20260911-210631-780/STAGE_DOCUMENT.md`

### 10-characterization — SUCCESS

82 executable scenario(s): 36 frozen against OLD, 46 unobservable, 0 unexecuted; 506 characterization contract(s): 0 frozen, 0 mapped to existing tests, 506 awaiting OLD, 0 unobservable

Duration 3m 47s · 0 command(s) · 0 decision(s) · 2 warning(s)

Detail: `10-characterization/20260911-210634-575/STAGE_DOCUMENT.md`

### 11-plan — SUCCESS

8 edge(s) frozen, deterministic coverage 0.6791, 1 differential dimension(s) required

Duration 2.9 s · 0 command(s) · 0 decision(s) · 1 warning(s)

Detail: `11-plan/20260911-211022-294/STAGE_DOCUMENT.md`

### 12-transformation · EDGE-1-PREP-TEST — SUCCESS

Edge EDGE-1-PREP-TEST: 3 applied, 0 rejected, 0 failed; ledger head f3c29169d33f

Duration 1.5 s · 0 command(s) · 0 decision(s) · 1 warning(s)

Detail: `12-transformation/20260911-211025-582/STAGE_DOCUMENT.md`

### 13-build-repair · EDGE-1-PREP-TEST — SUCCESS

Edge EDGE-1-PREP-TEST compiles after 1 round(s); 0 AI attempt(s) used of 12

Duration 1m 23s · 8 command(s) · 0 decision(s)

Detail: `13-build-repair/20260911-211026-740/STAGE_DOCUMENT.md`

### 14-graph-diff · EDGE-1-PREP-TEST — SUCCESS

Edge EDGE-1-PREP-TEST graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK

Duration 5.7 s · 0 command(s) · 0 decision(s)

Detail: `14-graph-diff/20260911-211155-459/STAGE_DOCUMENT.md`

### 15-test · EDGE-1-PREP-TEST — SUCCESS

Edge EDGE-1-PREP-TEST: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2}

Duration 5m 19s · 15 command(s) · 0 decision(s)

Detail: `15-test/20260911-211156-418/STAGE_DOCUMENT.md`

### 16-runtime · EDGE-1-PREP-TEST — SUCCESS

Edge EDGE-1-PREP-TEST: 4/6 module(s) started, 267 bound property(ies), 279 runtime graph edge(s)

Duration 4m 39s · 13 command(s) · 0 decision(s) · 2 warning(s)

Detail: `16-runtime/20260911-211715-637/STAGE_DOCUMENT.md`

### 17-differential · EDGE-1-PREP-TEST — SUCCESS

Edge EDGE-1-PREP-TEST: 82 comparison(s) across 1 dimension(s); {IDENTICAL=36, NOT_COMPARED=46}

Duration 280 ms · 0 command(s) · 0 decision(s)

Detail: `17-differential/20260911-212155-178/STAGE_DOCUMENT.md`

### 12-transformation · EDGE-2-PATCH — SUCCESS

Edge EDGE-2-PATCH: 16 applied, 0 rejected, 0 failed; ledger head 90eb766c9357

Duration 1.5 s · 0 command(s) · 0 decision(s)

Detail: `12-transformation/20260911-212155-953/STAGE_DOCUMENT.md`

### 13-build-repair · EDGE-2-PATCH — SUCCESS

Edge EDGE-2-PATCH compiles after 1 round(s); 0 AI attempt(s) used of 12

Duration 47.0 s · 8 command(s) · 0 decision(s)

Detail: `13-build-repair/20260911-212157-428/STAGE_DOCUMENT.md`

### 14-graph-diff · EDGE-2-PATCH — SUCCESS

Edge EDGE-2-PATCH graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK

Duration 2.1 s · 0 command(s) · 0 decision(s)

Detail: `14-graph-diff/20260911-212246-099/STAGE_DOCUMENT.md`

### 15-test · EDGE-2-PATCH — SUCCESS

Edge EDGE-2-PATCH: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2}

Duration 4m 50s · 15 command(s) · 0 decision(s)

Detail: `15-test/20260911-212246-552/STAGE_DOCUMENT.md`

### 16-runtime · EDGE-2-PATCH — SUCCESS

Edge EDGE-2-PATCH: 4/6 module(s) started, 268 bound property(ies), 280 runtime graph edge(s)

Duration 4m 56s · 13 command(s) · 0 decision(s) · 2 warning(s)

Detail: `16-runtime/20260911-212736-714/STAGE_DOCUMENT.md`

### 17-differential · EDGE-2-PATCH — BLOCKED

Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s)

Duration 193 ms · 0 command(s) · 0 decision(s) · 1 warning(s)

Detail: `17-differential/20260911-213233-154/STAGE_DOCUMENT.md`

## 8. Edge timeline

| Edge | Stage attempts | Last stage | Last outcome | Edge document |
| --- | --- | --- | --- | --- |
| `EDGE-1-PREP-TEST` | 6 | `17-differential` | SUCCESS | `edges/EDGE-1-PREP-TEST/EDGE_DOCUMENT.md` |
| `EDGE-2-PATCH` | 6 | `17-differential` | **BLOCKED** | `edges/EDGE-2-PATCH/EDGE_DOCUMENT.md` |

## 9. Changes performed so far

Mutating stages have run. Per-attempt mutation counts are in each stage document's section 14; the authoritative record of every change is the append-only change ledger.

| Stage | Edge | Status | Summary |
| --- | --- | --- | --- |
| `12-transformation` | `EDGE-1-PREP-TEST` | SUCCESS | Edge EDGE-1-PREP-TEST: 3 applied, 0 rejected, 0 failed; ledger head f3c29169d33f |
| `13-build-repair` | `EDGE-1-PREP-TEST` | SUCCESS | Edge EDGE-1-PREP-TEST compiles after 1 round(s); 0 AI attempt(s) used of 12 |
| `12-transformation` | `EDGE-2-PATCH` | SUCCESS | Edge EDGE-2-PATCH: 16 applied, 0 rejected, 0 failed; ledger head 90eb766c9357 |
| `13-build-repair` | `EDGE-2-PATCH` | SUCCESS | Edge EDGE-2-PATCH compiles after 1 round(s); 0 AI attempt(s) used of 12 |

## 10. Validation performed so far

| Stage | Edge | Status | Summary |
| --- | --- | --- | --- |
| `14-graph-diff` | `EDGE-1-PREP-TEST` | SUCCESS | Edge EDGE-1-PREP-TEST graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK |
| `15-test` | `EDGE-1-PREP-TEST` | SUCCESS | Edge EDGE-1-PREP-TEST: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2} |
| `16-runtime` | `EDGE-1-PREP-TEST` | SUCCESS | Edge EDGE-1-PREP-TEST: 4/6 module(s) started, 267 bound property(ies), 279 runtime graph edge(s) |
| `17-differential` | `EDGE-1-PREP-TEST` | SUCCESS | Edge EDGE-1-PREP-TEST: 82 comparison(s) across 1 dimension(s); {IDENTICAL=36, NOT_COMPARED=46} |
| `14-graph-diff` | `EDGE-2-PATCH` | SUCCESS | Edge EDGE-2-PATCH graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK |
| `15-test` | `EDGE-2-PATCH` | SUCCESS | Edge EDGE-2-PATCH: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2} |
| `16-runtime` | `EDGE-2-PATCH` | SUCCESS | Edge EDGE-2-PATCH: 4/6 module(s) started, 268 bound property(ies), 280 runtime graph edge(s) |
| `17-differential` | `EDGE-2-PATCH` | **BLOCKED** | Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s) |

## 11. Current blockers

| Stage | Status | Reason |
| --- | --- | --- |
| `17-differential` | BLOCKED | Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s) |

## 12. Human decisions required

- `17-differential`: Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s) — **This stage is blocked by the active policy. Read section 18, then either change the policy deliberately or accept that the migration cannot proceed as specified.**

## 13. Warnings

| Stage | Warnings |
| --- | --- |
| `00-bootstrap` | 1 |
| `04-baseline` | 3 |
| `06-target` | 5 |
| `08-knowledge` | 1 |
| `10-characterization` | 2 |
| `11-plan` | 1 |
| `12-transformation` | 1 |
| `16-runtime` | 2 |
| `16-runtime` | 2 |
| `17-differential` | 1 |

Warning text is in each stage document's section 17.

## 14. Gaps

No documentation or execution gaps detected in this run.

## 15. Blind spots

Blind spots are recorded per stage attempt, in section 19 of each stage document. The evidence stage consolidates them for the run.

## 16. Evidence produced so far

| Stage | Attempt directory | Status |
| --- | --- | --- |
| `00-bootstrap` | `20260911-204737-712` | SUCCESS |
| `01-inventory` | `20260911-204738-128` | SUCCESS |
| `02-build` | `20260911-204937-146` | SUCCESS |
| `03-graph` | `20260911-204941-679` | SUCCESS |
| `04-baseline` | `20260911-204942-332` | SUCCESS |
| `05-compatibility` | `20260911-205908-963` | SUCCESS |
| `06-target` | `20260911-205932-627` | SUCCESS |
| `07-documentation` | `20260911-205932-757` | SUCCESS |
| `08-knowledge` | `20260911-205944-957` | SUCCESS |
| `09-impact` | `20260911-210631-780` | SUCCESS |
| `10-characterization` | `20260911-210634-575` | SUCCESS |
| `11-plan` | `20260911-211022-294` | SUCCESS |
| `12-transformation` | `20260911-211025-582` | SUCCESS |
| `13-build-repair` | `20260911-211026-740` | SUCCESS |
| `14-graph-diff` | `20260911-211155-459` | SUCCESS |
| `15-test` | `20260911-211156-418` | SUCCESS |
| `16-runtime` | `20260911-211715-637` | SUCCESS |
| `17-differential` | `20260911-212155-178` | SUCCESS |
| `12-transformation` | `20260911-212155-953` | SUCCESS |
| `13-build-repair` | `20260911-212157-428` | SUCCESS |
| `14-graph-diff` | `20260911-212246-099` | SUCCESS |
| `15-test` | `20260911-212246-552` | SUCCESS |
| `16-runtime` | `20260911-212736-714` | SUCCESS |
| `17-differential` | `20260911-213233-154` | **BLOCKED** |

## 17. Next action

This stage is blocked by the active policy. Read section 18, then either change the policy deliberately or accept that the migration cannot proceed as specified.

## 18. Integrity information

| Field | Value |
| --- | --- |
| Run id | `RUN-01M293KJ0RYV603B3JB0CCJJN9` |
| Attempts recorded | 24 |
| Timeline artifact | `run-timeline.json` |
| Journal failures | 0 |
| Schema version | `1.0.0` |

Every row in the stage timeline is backed by a `stage-execution.json` in the named attempt directory. This document holds no facts that are not in those records.
