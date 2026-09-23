# Edge EDGE-2-PATCH

| Field | Value |
| --- | --- |
| Source state | `2.7.12` |
| Target state | `2.7.18` |
| Java | `17.0.20.1` |
| Spring Cloud | `2021.0.9` |
| Class | PATCH |
| Validation depth | FULL_DIFFERENTIAL |
| Risk | HIGH |
| Mandatory checkpoint | no |
| Deterministic coverage | 0.822 |
| Expected residual | 0.17800000000000005 |
| Result | **BLOCKED** |

---

## Why this edge exists

Move to the latest patch of the current line before the boundary.

## Facts in force

1388 total, first 50 shown:

- `MK-00009`
- `MK-00010`
- `MK-00011`
- `MK-00012`
- `MK-00013`
- `MK-00014`
- `MK-00015`
- `MK-00016`
- `MK-00017`
- `MK-00018`
- `MK-00019`
- `MK-00020`
- `MK-00021`
- `MK-00022`
- `MK-00023`
- `MK-00024`
- `MK-00025`
- `MK-00026`
- `MK-00027`
- `MK-00028`
- `MK-00029`
- `MK-00030`
- `MK-00031`
- `MK-00032`
- `MK-00033`
- `MK-00034`
- `MK-00035`
- `MK-00036`
- `MK-00037`
- `MK-00038`
- `MK-00039`
- `MK-00040`
- `MK-00041`
- `MK-00042`
- `MK-00043`
- `MK-00044`
- `MK-00045`
- `MK-00046`
- `MK-00047`
- `MK-00048`
- `MK-00049`
- `MK-00050`
- `MK-00051`
- `MK-00052`
- `MK-00053`
- `MK-00054`
- `MK-00055`
- `MK-00056`
- `MK-00057`
- `MK-00058`

## Impacts

484 total, first 50 shown:

- `IMPACT-00001`
- `IMPACT-00002`
- `IMPACT-00003`
- `IMPACT-00004`
- `IMPACT-00005`
- `IMPACT-00006`
- `IMPACT-00007`
- `IMPACT-00008`
- `IMPACT-00009`
- `IMPACT-00010`
- `IMPACT-00011`
- `IMPACT-00012`
- `IMPACT-00013`
- `IMPACT-00014`
- `IMPACT-00015`
- `IMPACT-00016`
- `IMPACT-00017`
- `IMPACT-00018`
- `IMPACT-00019`
- `IMPACT-00020`
- `IMPACT-00021`
- `IMPACT-00022`
- `IMPACT-00023`
- `IMPACT-00024`
- `IMPACT-00025`
- `IMPACT-00026`
- `IMPACT-00027`
- `IMPACT-00028`
- `IMPACT-00029`
- `IMPACT-00030`
- `IMPACT-00031`
- `IMPACT-00032`
- `IMPACT-00033`
- `IMPACT-00034`
- `IMPACT-00035`
- `IMPACT-00036`
- `IMPACT-00037`
- `IMPACT-00038`
- `IMPACT-00039`
- `IMPACT-00040`
- `IMPACT-00041`
- `IMPACT-00042`
- `IMPACT-00043`
- `IMPACT-00044`
- `IMPACT-00045`
- `IMPACT-00046`
- `IMPACT-00047`
- `IMPACT-00048`
- `IMPACT-00049`
- `IMPACT-00050`

## Recipes planned

| Recipe | Provider | Capability | Why |
| --- | --- | --- | --- |
| `maven.parent-version` | BOOTSHIFT_MAVEN_POM | AVAILABLE | Move the parent POM to this edge's target version |
| `maven.managed-version` | BOOTSHIFT_MAVEN_POM | AVAILABLE | Move the Spring Cloud train to the one published for this Boot line |
| `java.remove-annotation` | BOOTSHIFT_REMOVED_ANNOTATION | AVAILABLE | Remove annotations deleted at this version |
| `config.property-migration` | BOOTSHIFT_CONFIG_PROPERTY | AVAILABLE | Apply property renames deprecated at this version |

## Stage 12 — Transformation

| Field | Value |
| --- | --- |
| Status | **SUCCESS** |
| Attempt | `01M295JDYYP49E1QNGJ48ZJ5FZ` |
| Duration | 1.5 s |
| Detail | `12-transformation/20260911-212155-953/STAGE_DOCUMENT.md` |

Edge EDGE-2-PATCH: 16 applied, 0 rejected, 0 failed; ledger head 90eb766c9357

## Stage 13 — Build and repair

| Field | Value |
| --- | --- |
| Status | **SUCCESS** |
| Attempt | `01M295JFES4Y2QK2ZT64KF4PBB` |
| Duration | 47.0 s |
| Detail | `13-build-repair/20260911-212157-428/STAGE_DOCUMENT.md` |

Edge EDGE-2-PATCH compiles after 1 round(s); 0 AI attempt(s) used of 12

## Stage 14 — Graph rebuild and scope verification

| Field | Value |
| --- | --- |
| Status | **SUCCESS** |
| Attempt | `01M295KXC5BP978Z6FF8KHQ8WD` |
| Duration | 2.1 s |
| Detail | `14-graph-diff/20260911-212246-099/STAGE_DOCUMENT.md` |

Edge EDGE-2-PATCH graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK

## Stage 15 — Test validation

| Field | Value |
| --- | --- |
| Status | **SUCCESS** |
| Attempt | `01M295KZFM4T5Y2X0C42WSHSWD` |
| Duration | 4m 50s |
| Detail | `15-test/20260911-212246-552/STAGE_DOCUMENT.md` |

Edge EDGE-2-PATCH: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2}

## Stage 16 — Runtime validation

| Field | Value |
| --- | --- |
| Status | **SUCCESS** |
| Attempt | `01M295WTV68FHVKDHJZQA51KJM` |
| Duration | 4m 56s |
| Detail | `16-runtime/20260911-212736-714/STAGE_DOCUMENT.md` |

Edge EDGE-2-PATCH: 4/6 module(s) started, 268 bound property(ies), 280 runtime graph edge(s)

## Stage 17 — Differential validation

| Field | Value |
| --- | --- |
| Status | **BLOCKED** |
| Attempt | `01M2965WEJAND58F0PASTQANF8` |
| Duration | 193 ms |
| Detail | `17-differential/20260911-213233-154/STAGE_DOCUMENT.md` |

Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s)

## Applied changes

The authoritative record of every applied change is the append-only change ledger; the counts above are the journal's view of it.

## Rejected changes

Mutations the gateway refused are counted in the mutation summaries above and recorded individually in each stage's own artifact. A rejection is a control working, not a failure.

## Residuals

Facts with no deterministic transformer are recorded as residual by the planner and raise the validation depth for this edge. See the plan artifact and stage 11 for the residual calculation.

## Validation results

| Stage | Status | Result |
| --- | --- | --- |
| `14-graph-diff` | SUCCESS | Edge EDGE-2-PATCH graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK |
| `15-test` | SUCCESS | Edge EDGE-2-PATCH: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2} |
| `16-runtime` | SUCCESS | Edge EDGE-2-PATCH: 4/6 module(s) started, 268 bound property(ies), 280 runtime graph edge(s) |
| `17-differential` | BLOCKED | Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s) |

## Evidence

| Stage | Attempt | Execution record |
| --- | --- | --- |
| `12-transformation` | `01M295JDYYP49E1QNGJ48ZJ5FZ` | `12-transformation/20260911-212155-953/stage-execution.json` |
| `13-build-repair` | `01M295JFES4Y2QK2ZT64KF4PBB` | `13-build-repair/20260911-212157-428/stage-execution.json` |
| `14-graph-diff` | `01M295KXC5BP978Z6FF8KHQ8WD` | `14-graph-diff/20260911-212246-099/stage-execution.json` |
| `15-test` | `01M295KZFM4T5Y2X0C42WSHSWD` | `15-test/20260911-212246-552/stage-execution.json` |
| `16-runtime` | `01M295WTV68FHVKDHJZQA51KJM` | `16-runtime/20260911-212736-714/stage-execution.json` |
| `17-differential` | `01M2965WEJAND58F0PASTQANF8` | `17-differential/20260911-213233-154/stage-execution.json` |

## Blockers

**Edge EDGE-2-PATCH: 1 unexplained behavioural difference(s)**

## Edge result

**BLOCKED** — the edge stopped on a finding that requires a human decision.

Generated from `edge-execution.json`, which is itself derived from the per-attempt execution records.
