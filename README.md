<div align="center">

# Bootshift — Evidence-Driven Spring Boot Migration Harness

**Bootshift is a migration harness for Spring Boot repositories. It takes an existing repository through these steps to a migrated source tree and an evidence trail that you can audit:**

`understand the repository` → `seal the baseline` → `decide the target` → `plan from verified facts` → `transform one edge at a time` → `build, test, run and compare` → `get human decisions` → `seal the evidence` → `answer provenance questions`.

![Agents](https://img.shields.io/badge/Agents-20_%2B_conductor-1F3864?style=for-the-badge)
![Skills](https://img.shields.io/badge/Skills-8-2E5FD9?style=for-the-badge)
![Phases](https://img.shields.io/badge/Phases-7-6E86E8?style=for-the-badge)
![Approval gates](https://img.shields.io/badge/Approval_gates-13-F5C542?style=for-the-badge)
![Exit codes](https://img.shields.io/badge/Exit_codes-5-C0392B?style=for-the-badge)
![Schemas](https://img.shields.io/badge/Artifact_schemas-21-1F3864?style=for-the-badge)
![Tests](https://img.shields.io/badge/Tests-190-3DA35B?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-A0399B?style=for-the-badge)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-migration_target-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36?style=flat-square&logo=apachemaven&logoColor=white)
![Git](https://img.shields.io/badge/JGit-6.10-F05032?style=flat-square&logo=git&logoColor=white)
![JavaParser](https://img.shields.io/badge/JavaParser-3.26.2-2C3E50?style=flat-square)
![OpenRewrite](https://img.shields.io/badge/OpenRewrite-core_8.90.4-2C3E50?style=flat-square)
![ArchUnit](https://img.shields.io/badge/ArchUnit-1.3.0-6E86E8?style=flat-square)
![AI](https://img.shields.io/badge/AI-local_OSS%2C_off_by_default-lightgrey?style=flat-square)
![Docs](https://img.shields.io/badge/Docs-ASD--STE100-5D6D7E?style=flat-square)

**[Summary](#1-summary)** ·
**[Agents](#25-the-20-agents)** ·
**[Workflow](#4-the-end-to-end-workflow)** ·
**[Evidence levels](#15-evidence-levels-and-outcomes)** ·
**[Run Bootshift](#20-how-to-run-bootshift)** ·
**[Known problems](#23-known-problems)** ·
**[Glossary](#25-glossary)**

</div>

> [!NOTE]
> This README uses ASD-STE100 Simplified Technical English. The writing rules and the project
> vocabulary are in [`docs/ste-style-guide.md`](docs/ste-style-guide.md). Each term in the
> [Glossary](#25-glossary) has only one meaning.

> [!CAUTION]
> Run Bootshift against an untrusted repository only in a disposable environment. Bootshift compiles,
> tests and starts the repository code. Commands have an allowlist and a timeout, but there is no
> kernel-level sandbox. A malicious Maven plugin that the repository already trusts runs with the
> privileges of the harness.

---

Bootshift migrates an existing Spring Boot repository to a supported target state. It records
evidence for each step. It is a **pipeline of twenty deterministic stages**, not twenty autonomous
agents. In this project, the word *agent* means a controlled stage. Each stage has declared inputs,
outputs, preconditions, postconditions and limits of authority. Most stages contain no AI. The full
pipeline runs with AI off, and AI is off by default.

Bootshift does not only change the Spring Boot version. It finds the structure of the repository and
plans the migration from evidence. It records each source change and validates the result for
structure and for behaviour. Its evidence trail tells what changed, why it changed, what it
affected and how Bootshift validated it.

This README is the **one location that explains all of Bootshift**. It gives these topics:

- the general design and the design rules
- each of the 20 agents: its purpose, its procedure, step by step, its outputs and its failure behaviour
- the graph, file identity, migration edge and evidence models
- the security, sensitive-data and retention rules
- the data map
- the runbook
- the validation results and the known problems

This README describes the harness. It does not describe the application in `./src`, which is only
input for the harness.

| If you are… | Read |
|---|---|
| A manager or reviewer | [1](#1-summary), [3](#3-design-rules), [4](#4-the-end-to-end-workflow), [15](#15-evidence-levels-and-outcomes), [22](#22-validation-results), [24](#24-key-points) |
| A developer who joins the project | All sections, in sequence. Keep [20](#20-how-to-run-bootshift), [21](#21-how-to-extend-bootshift) and [23](#23-known-problems) open while you work |
| An operator who runs a migration | [20](#20-how-to-run-bootshift), then the section for the phase that you run (sections [5](#5-the-input--source-intake-and-bootstrap) to [11](#11-phase-6--prove)) |
| An auditor who reads the evidence | [15](#15-evidence-levels-and-outcomes), [17](#17-security-sensitive-data-and-retention), [18](#18-data-and-file-map), then [Agent 19](#112-agent-19--evidence-and-report) and [Agent 20](#113-agent-20--provenance-graph-and-questions) |

---

## Table of contents

1. 🧭 [Summary](#1-summary)
2. 🏗️ [How Bootshift is built](#2-how-bootshift-is-built)
   - 2.1 [Components](#21-components)
   - 2.2 [System context](#22-system-context)
   - 2.3 [Modules and architecture rules](#23-modules-and-architecture-rules)
   - 2.4 [The three planes](#24-the-three-planes)
   - 2.5 [The 20 agents](#25-the-20-agents)
   - 2.6 [The 8 skills](#26-the-8-skills)
   - 2.7 [Architecture decision records](#27-architecture-decision-records)
   - 2.8 [Repository layout](#28-repository-layout)
3. 🛡️ [Design rules](#3-design-rules)
   - 3.1 [The 31 rules](#31-the-31-rules) · 3.2 [The three rules that do the most work](#32-the-three-rules-that-do-the-most-work) · 3.3 [What Bootshift can claim](#33-what-bootshift-can-claim) · 3.4 [The coverage statement rule](#34-the-coverage-statement-rule)
4. 🔄 [The end-to-end workflow](#4-the-end-to-end-workflow)
   - 4.1 [High-level flow](#41-high-level-flow) · 4.2 [Full flow with the edge loop](#42-full-flow-with-the-edge-loop) · 4.3 [Execution flow by phase](#43-execution-flow-by-phase) · 4.4 [The run state machine](#44-the-run-state-machine)
5. 📥 [The input — source intake and bootstrap](#5-the-input--source-intake-and-bootstrap)
   - 5.1 [Source input model](#51-source-input-model) · 5.2 [Workspace bootstrap](#52-workspace-bootstrap) · 5.3 [Environment provider contract](#53-environment-provider-contract)
6. 🔵 [Phase 1 — Understand](#6-phase-1--understand)
   - 6.1 [Agent 01 — Inventory](#61-agent-01--inventory) · 6.2 [Agent 02 — Build Resolver](#62-agent-02--build-resolver) · 6.3 [Agent 03 — Application Graph](#63-agent-03--application-graph)
7. 🟡 [Phase 2 — Baseline](#7-phase-2--baseline)
   - 7.1 [Agent 04 — Baseline](#71-agent-04--baseline)
8. 🧭 [Phase 3 — Decide](#8-phase-3--decide)
   - 8.1 [Agent 05 — Compatibility Registry](#81-agent-05--compatibility-registry) · 8.2 [Internal component onboarding](#82-internal-component-onboarding) · 8.3 [Agent 06 — Target Resolver](#83-agent-06--target-resolver)
9. 🧠 [Phase 4 — Plan](#9-phase-4--plan)
   - 9.1 [Agent 07 — Documentation Registry](#91-agent-07--documentation-registry) · 9.2 [Agent 08 — Migration Knowledge](#92-agent-08--migration-knowledge) · 9.3 [Agent 09 — Impact Analyzer](#93-agent-09--impact-analyzer) · 9.4 [Agent 10 — Characterization](#94-agent-10--characterization) · 9.5 [Agent 11 — Migration Planner](#95-agent-11--migration-planner)
10. 🛠️ [Phase 5 — Migration edge loop](#10-phase-5--migration-edge-loop)
    - 10.1 [Agent 12 — Transformation](#101-agent-12--transformation) · 10.2 [The FileMutationGateway](#102-the-filemutationgateway) · 10.3 [The change ledger](#103-the-change-ledger) · 10.4 [Agent 13 — Build and Repair](#104-agent-13--build-and-repair) · 10.5 [Agent 14 — Graph Rebuild and Graph Diff](#105-agent-14--graph-rebuild-and-graph-diff)
    - 10.6 [Agent 15 — Test Validation](#106-agent-15--test-validation) · 10.7 [Agent 16 — Runtime Validation](#107-agent-16--runtime-validation) · 10.8 [Runtime graph enrichment](#108-runtime-graph-enrichment) · 10.9 [Agent 17 — Differential Validation](#109-agent-17--differential-validation)
11. 🟣 [Phase 6 — Prove](#11-phase-6--prove)
    - 11.1 [Agent 18 — Approval](#111-agent-18--approval) · 11.2 [Agent 19 — Evidence and Report](#112-agent-19--evidence-and-report) · 11.3 [Agent 20 — Provenance Graph and Questions](#113-agent-20--provenance-graph-and-questions)
12. 🕸️ [The application graph model](#12-the-application-graph-model)
13. 🪪 [File identity and lineage](#13-file-identity-and-lineage)
14. 🪜 [Migration edges and validation depth](#14-migration-edges-and-validation-depth)
15. ⚖️ [Evidence levels and outcomes](#15-evidence-levels-and-outcomes)
16. 🔐 [OSS tooling and the AI boundary](#16-oss-tooling-and-the-ai-boundary)
17. 🔒 [Security, sensitive data and retention](#17-security-sensitive-data-and-retention)
18. 🗂️ [Data and file map](#18-data-and-file-map)
19. 📡 [Observability](#19-observability)
20. ▶️ [How to run Bootshift](#20-how-to-run-bootshift)
    - 20.1 [Prerequisites](#201-prerequisites) · 20.2 [Build the harness](#202-build-the-harness) · 20.3 [Run Bootshift](#203-run-bootshift) · 20.4 [CLI commands](#204-cli-commands) · 20.5 [Common options and exit codes](#205-common-options-and-exit-codes) · 20.6 [Policies and configuration](#206-policies-and-configuration) · 20.7 [Environment variables](#207-environment-variables) · 20.8 [Common faults](#208-common-faults)
21. 🧩 [How to extend Bootshift](#21-how-to-extend-bootshift)
22. ✅ [Validation results](#22-validation-results)
23. ⚠️ [Known problems](#23-known-problems)
24. 📌 [Key points](#24-key-points)
25. 📖 [Glossary](#25-glossary)
26. 📄 [License](#26-license)

---

## 1. Summary

**The problem.** A Spring Boot major upgrade is not a version change. It changes these items at the
same time:

- the namespace (`javax` to `jakarta`)
- the security configuration model
- the Java baseline
- the auto-configuration registration mechanism
- hundreds of configuration property names
- the transitive dependency graph

Each of these items can change behaviour, and no line of application code changes. The usual tools
answer a smaller question than the important one:

| A tool says | The important question |
|---|---|
| "It compiles." | Does it still do the same thing? |
| "Tests pass." | Do the tests cover the behaviour that changed? |
| "It starts." | Are the same beans active, and are the same properties bound? |
| "The recipe ran." | Which of my files did it change, and which rule permitted the change? |
| "Migration complete." | Complete to which standard, with which evidence, and what could it not see? |

Bootshift answers only the second column. To answer it, the harness must state what it did not
observe. This is a mechanical rule, not an aim:

- A claim without a coverage statement cannot go into the report.
- A behavioural difference with no verified explanation stops the run.
- A dimension that the environment cannot support gets the status `NOT_COMPARED`. It never gets a silent pass.

| Item | Value |
|---|---|
| Stages | **20** agents (`01` to `20`), plus the bootstrap conductor (`00`). 21 stage contracts are in `.claude/agents/` |
| Phases | **7**: Initialization (00) → Understand (01–03) → Baseline (04) → Decide (05–06) → Plan (07–11) → Migration edge loop (12–17) → Prove (18–20) |
| Mutating stages | **2**: Agent 12 (Transformation) and Agent 13 (Build and Repair). Both write only through `FileMutationGateway` |
| Human decisions | **13** approval gates. Bootshift never approves its own work |
| Input | A Spring Boot repository, as a Git repository or a plain directory. The default is `./src`. Bootshift never writes to it |
| Output | Schema-checked JSON artifacts in `output/<stage>/<timestamp>/`, a hash-chained change ledger, an evidence manifest, `migration-report.md`, `MIGRATION_DOCUMENT.md` and an export bundle with a CycloneDX 1.5 SBOM |
| Kinds of change | Deterministic transformers for Maven POMs, `javax` → `jakarta`, configuration properties, test frameworks and removed no-op annotations. OpenRewrite core recipes. Optional local AI repair proposals |
| Evidence levels | **6**: `E0` (asserted) to `E5` (differentially verified). Only `E3` and higher can authorize a change |
| Outcomes | **5** exit codes: `0` success, `1` failure, `2` refusal, `3` policy block, `4` human decision required |
| AI | Optional, local open-source models only, loopback endpoint only, off by default. The reference runs used zero AI |
| Tests | **190** test methods in 21 test classes (`tests/`) |

```mermaid
flowchart LR
    S["Source<br/><i>Git or plain directory</i>"] --> U["Understand<br/><i>inventory, build, graph</i>"]
    U --> B["Baseline<br/><i>observe and seal</i>"]
    B --> D["Decide<br/><i>compatibility, target</i>"]
    D --> P["Plan<br/><i>knowledge, impact, edges</i>"]
    P --> M["Migrate<br/><i>changes through the gateway</i>"]
    M --> V["Validate<br/><i>build, test, runtime, differential</i>"]
    V --> R["Prove<br/><i>claims and evidence levels</i>"]
    R --> Rep["Report<br/><i>sealed manifest and bundle</i>"]

    style B fill:#fff3cd,stroke:#856404
    style M fill:#f8d7da,stroke:#721c24
    style R fill:#d4edda,stroke:#155724
```

The yellow stage is the last point before any change. No component can change source before it. The
red stage is the only stage that changes source. In the green stage, each claim gets a level that a
reviewer can check.

| Phase | Agents | The questions that the phase answers |
|---|---|---|
| 🔵 **Understand** | 01 – 03 | Which application did we receive? Which files, modules, dependencies, beans, endpoints, repositories, entities and tests exist? How do they connect? What depends on a given file, symbol, bean, endpoint or property? |
| 🟡 **Baseline** | 04 | What was the behaviour before the migration? |
| 🧭 **Decide** | 05 – 06 | Which target version is safe and supported? Which migration path applies? |
| 🧠 **Plan** | 07 – 11 | Which official documents and artifact facts apply? Which files and symbols does each fact affect? Which behaviours have test protection, and which need characterization first? Which changes, in which sequence, by which tool? |
| 🛠️ **Migration edge loop** | 12 – 17 | Which files changed, why, and by which tool? What were the hashes before and after? Was a file renamed, split, merged, created or deleted? Did it compile? Did tests regress? Did runtime, API, security, serialization, binding or persistence behaviour change? |
| 🟣 **Prove** | 18 – 20 | Which changes are expected and have verified evidence? Which differences have no explanation and therefore block? Which evidence level did each dimension get? Which blind spots remain? |

**Findings from the reference corpus.** A run against the reference corpus in `./src` found these
facts. Nobody searched for them:

| Finding | How Bootshift found it |
|---|---|
| Lombok 1.18.24 cannot compile on JDK 21 | Toolchain hazard probe during baseline capture |
| MongoDB Atlas credentials are committed in four `application.properties` files | Inventory secret scan. Bootshift recorded only the location and a redacted sample |
| No GA Spring Cloud release train exists for Spring Boot 4.x | Artifact-channel probe of the published `spring-cloud-starter-parent` POMs |
| The only Boot line with Spring Cloud support is past its OSS support date | Lifecycle registry compared with the Spring Cloud mapping |
| `xml-apis:xml-apis-ext` has a malformed POM that breaks `dependency:list` | Build resolution, which then used `dependency:tree` as a fallback |

None of these findings is a defect in the harness. Each one is a fact about the repository and its
ecosystem. A plain version change finds each fact later, at a worse time.

---

## 2. How Bootshift is built

### 2.1 Components

Bootshift has five kinds of component. The job of each kind explains most rules in this README.

| Component | What it is | What it can do |
|---|---|---|
| **Stage (agent)** | A Java class in `stages/stageNN/` that implements `Stage`. It has declared inputs, outputs, preconditions and postconditions | Read upstream artifacts, call ports and publish its own schema-checked artifacts. Only stages 12 and 13 can propose source changes |
| **Stage contract** | A Markdown file in `.claude/agents/NN-<name>.md` with front matter (`stage`, `determinism`, `mutation_permission`, `exit_codes`) | State what the stage MAY and MUST NOT do. The file is normative. If the file and the code disagree, the code is wrong |
| **Port** | A Java interface in `ports/` | Give stages a boundary to the outside world |
| **Adapter** | A Java class in `adapters/` that implements a port | Run Maven, Gradle, Git, JavaParser, JDK tools and HTTP. Store artifacts, decisions and state. Hold the transformers and the single writer |
| **Skill** | A named, licence-checked, version-pinned capability, listed in `.claude/skills/README.md` | Compute intended content for a recipe. A skill never writes a file |

> **Bootshift is not an AI runtime.** The pipeline is Java code that runs from one executable jar.
> The `.claude/agents/` files are contracts for each stage. They are not prompts that an AI follows.
> The only AI component is the optional `LocalOssAIProvider`.

### 2.2 System context

```mermaid
flowchart TB
    subgraph REPO["Bootshift repository"]
      CLI["apps/migration-cli<br/>bootshift.jar (alias bsh)"]
      ST["stages/<br/>20 agents + orchestrator"]
      AD["adapters/<br/>Maven, Git, JavaParser, transformers, gateway"]
      POL["policies/ · schemas/ · migration-rules/"]
      SRC["./src<br/>application under analysis (read-only)"]
    end
    WS["External workspace<br/>BOOTSHIFT_WORKSPACE_ROOT/&lt;run_id&gt;/"]
    OUT["Artifact plane<br/>./output/&lt;stage&gt;/&lt;timestamp&gt;/"]
    DEC["Decision store<br/>~/.bootshift/decisions"]
    MVN["Maven / Gradle + installed JDKs"]
    NET["Allowlisted hosts<br/>Maven Central, docs.spring.io, …"]
    AI["Local OSS model<br/>(optional, loopback only)"]

    CLI --> ST --> AD
    POL --> ST
    SRC -->|"snapshot"| WS
    AD --> WS
    AD --> OUT
    AD --> MVN
    AD --> NET
    AD -.-> AI
    DEC --> ST
```

### 2.3 Modules and architecture rules

Bootshift uses ports and adapters. ArchUnit tests enforce the boundaries. A convention does not.

```mermaid
flowchart TD
    subgraph APPS["apps/"]
        CLI["migration-cli<br/><i>picocli, no migration semantics</i>"]
    end
    subgraph STAGES["stages/"]
        ORCH["PipelineOrchestrator<br/><i>sequence only</i>"]
        AG["Agents 01-20"]
    end
    subgraph PORTS["ports/"]
        P["17 port interfaces"]
    end
    subgraph ADAPTERS["adapters/"]
        A["Git, Maven, Gradle, JavaParser,<br/>JDK tools, HTTP, filesystem stores,<br/>transformers, environment, AI"]
    end
    subgraph CORE["core/"]
        C["domain, identity, graph, ledger,<br/>policy, state, evidence, provenance, security"]
    end

    CLI --> ORCH
    ORCH --> AG
    AG --> P
    AG --> C
    A -.implements.-> P
    P --> C
    A --> C

    style CORE fill:#d4edda,stroke:#155724
    style ADAPTERS fill:#fff3cd,stroke:#856404
```

| Module | Can depend on | Contents |
|---|---|---|
| `core` | Nothing in this repository. No I/O and no frameworks | Domain model |
| `ports` | `core` | Interfaces only |
| `adapters` | `core`, `ports`. Never `stages` | Implementations |
| `stages` | `core`, `ports`, `adapters` | The agents |
| `apps` | All | The CLI. It connects all modules |
| `tests` | All | The harness test suite |

| Rule | ArchUnit test |
|---|---|
| `core` depends on nothing above it | `coreIsIndependent` |
| `core` imports no concrete tool (JGit, JavaParser, Maven, picocli) | `coreImportsNoTooling` |
| `ports` never refer to adapters or stages | `portsAreInterfacesOverCore` |
| `adapters` never refer to stages or the CLI | `adaptersDoNotDependOnStages` |
| The CLI holds no migration semantics | `cliDelegates` |
| Only the gateway writes application source | `onlyGatewayWritesSource` |
| No source-available Spring recipe estate is on the classpath | `noForbiddenRecipeEstate` |
| Only the composition root builds the state adapter | `stagesUseContextForSharedPorts` |
| The harness never depends on the application under analysis | `noApplicationDependency` |

The last rule is more important than it looks. The core of the harness must not depend on the Spring
Boot version that it migrates. If it depends on that version, it cannot migrate across that boundary.
`ArchitectureTest` has 16 test methods. The rules above are 9 of them.

### 2.4 The three planes

```mermaid
flowchart TB
    subgraph CONTROL["CONTROL PLANE — decides, never runs repository code"]
        ORCH["Thin orchestrator"]
        SM["State machine"]
        POL["Policy engine"]
        LIC["OSS license gate"]
        IDR["Identity registry coordination"]
        APR["Approval policy"]
    end

    subgraph EXEC["EXECUTION PLANE — runs untrusted repository code, isolated"]
        MVN["Maven / Gradle"]
        AP["Annotation processors"]
        TST["Test suites"]
        RUN["Application startup"]
        ANA["Java static analysis"]
        TRF["Transformation tools"]
        AI["Local OSS inference (optional)"]
    end

    subgraph EVID["ARTIFACT / EVIDENCE PLANE — the machine-readable truth"]
        ART["Stage artifacts + latest.json"]
        REG["File and symbol registries"]
        GRAPH["Application graphs"]
        LED["Change ledger (hash chain)"]
        OBS["Observations"]
        MAN["Evidence manifest"]
    end

    CONTROL -->|"allowlisted commands,<br/>timeouts, resource limits"| EXEC
    EXEC -->|"results and raw captures"| EVID
    EVID -->|"state reconstruction (R23)"| CONTROL

    style CONTROL fill:#e7f3ff,stroke:#0366d6
    style EXEC fill:#f8d7da,stroke:#721c24
    style EVID fill:#d4edda,stroke:#155724
```

The control plane **never runs repository code in its own process**. Each Maven invocation, test run
and application start goes through `ProcessRunner`. `ProcessRunner` enforces these controls:

- a command allowlist
- a timeout
- an output cap
- an explicit working directory
- a deterministic locale and timezone

`ProcessRunner` never uses a shell to start a command. Thus, argument injection cannot escape the
argument vector.

### 2.5 The 20 agents

| # | Agent | Mutating | AI | Purpose | Postcondition |
|---|---|:--:|:--:|---|---|
| 00 | Pipeline Conductor (bootstrap) | no | no | Create the run and the workspaces, run the OSS gate, sequence the agents | `OSS_POLICY_VERIFIED` |
| 01 | Inventory | no | no | Find the repository contents, allocate permanent identities | `FILE_REGISTRY_SEALED` |
| 02 | Build Resolver | no | no | Get the authoritative effective build model | `BUILD_RESOLVED` |
| 03 | Application Graph | no | no | Build and verify the typed graph with ten views | `GRAPH_VERIFIED` |
| 04 | Baseline | no | no | Observe the original behaviour and seal it with hashes | `BASELINE_SEALED` |
| 05 | Compatibility | no | no | Tier-1 version-space knowledge with evidence quality | `COMPATIBILITY_REGISTRY_READY` |
| 06 | Target Resolver | no | no | Select the landing target and the transit checkpoints | `TARGET_FROZEN` |
| 07 | Documentation | no | no | Get authoritative documents and store them by content hash | `DOCUMENTATION_RETRIEVED` |
| 08 | Knowledge | no | opt | Change documentation and artifact facts into verified facts | `KNOWLEDGE_VERIFIED` |
| 09 | Impact | no | no | Find the repository locations that the facts affect | `IMPACT_ANALYZED` |
| 10 | Characterization | no | opt | Make behavioural contracts before any change | `CHARACTERIZATION_COMPLETE` |
| 11 | Planner | no | no | Freeze how the path runs | `PLAN_FROZEN` |
| **12** | **Transformation** | **yes** | no | Apply authorized deterministic transformations | `EDGE_TRANSFORMED` |
| **13** | **Build + Repair** | **yes** | **opt** | Compile, and repair residual failures within a budget | `EDGE_COMPILED` |
| 14 | Graph Diff | no | no | Build the graph again and assert the scope | `EDGE_SCOPE_VERIFIED` |
| 15 | Test Validation | no | no | Run tests, classify the results against two baselines | `EDGE_TESTED` |
| 16 | Runtime Validation | no | no | Start the application, observe it, enrich the runtime graph | `EDGE_RUNTIME_GRAPH_ENRICHED` |
| 17 | Differential | no | opt | Compare OLD and NEW for the required dimensions | `EDGE_DIFFERENTIAL_VALIDATED` |
| 18 | Approval | no | no | Raise gates and record human decisions | `FINAL_APPROVAL` |
| 19 | Evidence | no | no | Seal the manifest and write the reports | `EVIDENCE_SEALED` |
| 20 | Provenance | no | no | Provenance that you can query, and the question catalog | `MIGRATION_COMPLETE` |

Only two stages can change application source, and both use only `FileMutationGateway`. An ArchUnit
rule fails the build if either stage calls the filesystem write API directly. The `stages` command
prints this catalog with the mutating flag and the AI flag of each stage.

### 2.6 The 8 skills

A skill is a capability that Agent 11 finds at runtime and records in the transformation capability
registry. It is not a plugin system. Three rules apply to each skill:

1. **A skill is discovered, never assumed (R31).** Agent 11 probes each skill and records it as
   `AVAILABLE` or `UNAVAILABLE`, with the reason. A plan never depends on a capability that the run
   did not observe.
2. **A skill never writes (R13).** It computes the intended content and returns it.
   `FileMutationGateway` is the only writer.
3. **A skill is licence-checked before use (R8–R9).** An unknown licence is blocked.

| Skill | Provider | Used for |
|---|---|---|
| `bootshift.maven-pom` | `MavenPomTransformer` | Recipes `maven.parent-version`, `maven.property`, `maven.managed-version`, `maven.add-dependency`, `maven.remove-dependency` |
| `bootshift.jakarta-namespace` | `JakartaNamespaceTransformer` | The `javax` → `jakarta` relocation of 28 package prefixes |
| `bootshift.config-property` | `ConfigurationPropertyTransformer` | The 542 generated property rules |
| `bootshift.test-framework` | `TestFrameworkTransformer` | JUnit 4 → Jupiter and `@MockBean` → `@MockitoBean` |
| `bootshift.removed-annotation` | `RemovedAnnotationTransformer` | Removal of 3 curated no-op annotations |
| `openrewrite.core` | `OpenRewriteCoreProvider` | OpenRewrite core recipes (Apache-2.0 modules only) |
| `bootshift.javap-api-diff` | `JavapApiDiffAdapter` | The published-bytecode diff of Agent 08 |
| `bootshift.local-oss-ai` | `LocalOssAIProvider` | Optional repair proposals. Off by default |

`.claude/skills/README.md` gives the licence, the determinism and the recipes of each skill, and the
procedure to add a skill.

### 2.7 Architecture decision records

Seven decisions have important consequences. The full text is in [`docs/adr/`](docs/adr/).

| ADR | Decision | The cost that the project accepts |
|---|---|---|
| [**001**](docs/adr/ADR-001-persistent-file-identity.md) | Bootshift **allocates** file identity. It does not derive identity from the path or the content | Identity recovery depends on the integrity of the File Registry. Sealing, pointer-after-write, persistence after each stage and content-addressed storage decrease this risk |
| [**002**](docs/adr/ADR-002-artifact-plane-as-source-of-truth.md) | Artifacts are the truth. The state machine only tells where the run is | A stage cannot show "in progress". A partial result that nobody can see is safer than a partial result that a reader can see |
| [**003**](docs/adr/ADR-003-static-vs-runtime-graph.md) | Static and runtime graphs are separate layers. Bootshift never merges them | Each consumer must state which layer it uses. This is intentional |
| [**004**](docs/adr/ADR-004-strict-oss-transformation-boundary.md) | Automate where verification costs little. Measure the rest as residual | Complex framework API migrations show as diagnosed compile failures, not as automatic fixes |
| [**005**](docs/adr/ADR-005-full-graph-rebuild-v1.md) | Version 1 always builds the full graph again | One full parse for each edge. In exchange, the scope gate is reliable |
| [**006**](docs/adr/ADR-006-old-vs-new-differential-contract.md) | Environment equivalence is a contract. Normalization has a hash. An unexplained difference blocks | The harness blocks on some differences that are harmless. This asymmetry is intentional |
| [**007**](docs/adr/ADR-007-sequential-recipe-application.md) | Recipes apply one batch at a time. Each proposal carries its base hash | One commit for each recipe, not one for each edge. A file that three recipes change is written three times. In exchange, the ledger and the tree always agree |

### 2.8 Repository layout

```
bootshift/
├── pom.xml                      aggregator; packaging=pom; ./src is NOT a module
├── README.md                    this document
├── LICENSE                      MIT
│
├── src/                         ← THE APPLICATION UNDER ANALYSIS (read-only input)
│                                  never a Maven module of this build,
│                                  never written to, never downloaded again
│
├── core/                        domain model — no I/O, no frameworks
│   └── src/main/java/com/bootshift/core/
│       ├── domain/              Envelope, ExitCode, HarnessException, StageResult, RunContext, OutputLayout
│       ├── identity/            FileRecord, FileRegistry, FileRole, FileStatus, RenameSource
│       ├── graph/               NodeType, EdgeType, GraphNode, GraphEdge, ApplicationGraph, GraphDiff
│       ├── ledger/              ChangeEvent, ChangeLedger
│       ├── evidence/            EvidenceLevel, Claim, CoverageStatement, EvidenceManifest
│       ├── policy/              HarnessPolicy, LicensePolicy, ValidationDepth
│       ├── state/               RunState, StateMachine
│       ├── provenance/          ProvenanceGraph
│       ├── security/            SensitiveValues
│       └── util/                Hashing, Ids, Json, Similarity, SchemaValidator
│
├── ports/                       interfaces — the hexagon boundary (17 interfaces + 3 support types)
│
├── adapters/                    each outside-world implementation
│   └── src/main/java/com/bootshift/adapters/
│       ├── exec/                ProcessRunner (allowlist, timeout, output cap)
│       ├── scm/                 GitScmAdapter
│       ├── build/               MavenBuildAdapter, GradleBuildAdapter, BuildSystemResolver,
│       │                        JavaTargetSelector, ToolchainProbe
│       ├── analysis/            JavaParserCodeModelAdapter
│       ├── mutation/            FileMutationGateway  ← the single writer
│       ├── transform/           MavenPom, JakartaNamespace, ConfigurationProperty, TestFramework,
│       │                        RemovedAnnotation, OpenRewriteCoreProvider, YamlPropertyModel
│       ├── compat/              MavenCentralVersionSpaceAdapter
│       ├── docs/                HttpDocumentationAdapter, DocumentTextExtractor
│       ├── runtime/             SpringProcessRuntimeProbe, ScenarioHttpExecutor
│       ├── apidiff/             JavapApiDiffAdapter
│       ├── approval/            FilesystemDecisionStore
│       ├── environment/         Managed and Delegated providers, ContainerProvisioner
│       ├── evidence/            FilesystemEvidenceStore
│       ├── state/               FilesystemRunStateStore
│       ├── http/                HttpFetcher (egress allowlist + content cache)
│       ├── ai/                  LocalOssAIProvider (loopback only)
│       └── telemetry/           StructuredTelemetryAdapter
│
├── stages/                      the twenty agents + orchestration
│   └── src/main/java/com/bootshift/stages/
│       ├── Stage, StageContext, StageExecutor, StageSupport, EdgeSupport, ValidationSupport
│       ├── EdgeIndex, EdgeToolchain, RunFactory, PipelineOrchestrator
│       ├── bootstrap/           RunBootstrap
│       └── stage01/ … stage20/  one package for each agent
│
├── apps/migration-cli/          Picocli entry point → bootshift.jar
├── tests/                       the harness test suite (21 test classes, 190 test methods)
│
├── .claude/
│   ├── agents/                  21 stage contracts (00-pipeline-conductor + 01…20)
│   └── skills/                  skill catalog (README.md)
│
├── docs/
│   ├── adr/                     ADR-001 … ADR-007
│   ├── BUILD-AND-REVIEW-REPORT.md  build log and two judge-and-fix cycles
│   └── ste-style-guide.md       writing rules and project vocabulary
├── schemas/                     21 JSON Schemas for the published artifacts
├── policies/                    default, license, ai, evidence, normalization,
│                                persistence, retention, security, validation
├── migration-rules/             deterministic rules, including 542 generated property rules
├── fixtures/                    identity cases and held-out impact evaluation cases
├── reports/                     recorded pipeline runs, judge passes and the migration document
└── output/                      published artifacts (git ignores it)
    └── <stage>/<timestamp>/     immutable; `latest.json` is the pointer
```

**The one structural rule.** `./src/` is **input**. It is not in the `<modules>` of the aggregator.
No harness class is written into it. `MutationBoundaryTest` fails the build if any code path can
write there outside the snapshot copy. Each other directory is harness.

---

## 3. Design rules

### 3.1 The 31 rules

Thirty-one rules control the harness. Code enforces them. This README and the code use the same rule
numbers.

| # | Rule | Enforced by |
|---|---|---|
| R1 | Inventory runs first | Stage preconditions and the state machine |
| R2 | Inventory owns `FILE_ID` creation | Only `InventoryStage` calls `FileRegistry.allocate` on a new scan |
| R3–R4 | Identity stays the same after edits and renames. It is not a path and not a hash | `FileRegistry` reattachment order and `FileIdentityTest` |
| R5 | Build tools are authoritative | `BuildModel.authoritative` is false unless Maven or Gradle answered |
| R6 | The graph comes before migration decisions | `GRAPH_VERIFIED` is a precondition of baseline capture |
| **R7** | **No change before the baseline seal** | `StateMachine` refuses mutating transitions, and `FileMutationGateway` refuses to write |
| R8–R9 | Strict OSS. OpenRewrite core is a tool, not the product | `LicensePolicy` gate, forbidden coordinates, ArchUnit rule |
| R10 | Documentation is necessary but not sufficient | A fact is `VERIFIED` only with artifact-channel evidence |
| **R11** | **AI cannot authorize changes** | Each AI proposal passes deterministic verification before the gateway gets it |
| **R12** | **AI is optional** | AI is off by default. The reference runs used zero AI |
| **R13** | **No component writes source directly** | `FileMutationGateway` is the only writer. ArchUnit forbids a bypass |
| R14 | Bootshift records each attempted change | Rejected, failed and reverted attempts are ledger entries |
| R15–R16 | Migration runs edge by edge, with a frozen validation depth | `edge-plan.json` freezes the depth. Execution reads it |
| R17–R19 | Compilation, tests and startup are each one dimension. None of them alone is success | Evidence levels are for each dimension |
| **R20–R21** | **Differential validation is the strongest gate. An unexplained difference blocks** | `UNEXPLAINED > 0 => BLOCKED` |
| R22 | Logs and evidence are separate | `TelemetryPort` and `EvidenceObjectStore` |
| R23 | The artifact plane is the truth | Pointer-after-write. State is restored from artifacts |
| R24 | Each stage can run independently | One CLI command for each stage. The orchestrator holds no semantics |
| R25 | An automatic target is the highest **safe supported** stable GA version | The rank uses the support horizon and the evidence, not the version number |
| R26 | Bootshift cannot silently collapse a mandatory checkpoint | The planner decomposes, escalates or blocks |
| R27 | Static and runtime graphs are different layers | `EdgeType.isRuntimeObserved()` and ADR-003 |
| R28 | Bootshift never assumes that an unknown internal component is compatible | Existence probe, then `UNKNOWN`, then the policy action |
| R29 | A coverage regression is a gated signal | A default block at 5 percentage points. You can configure it |
| **R30** | **The sealed baseline cannot change** | A seal with a different hash throws. A new baseline needs a new run |
| R31 | Bootshift finds transformation capability at runtime | `transformation-capability-registry.json` is probed in each run |

### 3.2 The three rules that do the most work

> **R7 — no change before the baseline seal.**
> Without a sealed baseline there is nothing to compare against. Thus, nobody can prove a later
> claim false. The gateway refuses to write, and the state machine refuses to enter a mutating state.

> **R13 — one writer.**
> Transformation and repair make *proposals*. Only `FileMutationGateway` writes. This single control
> point lets Bootshift state, for each changed byte, which fact authorized the change and which tool
> made it.

> **R21 — an unexplained difference blocks.**
> It does not only warn or log. If a behavioural difference has no verified migration fact and no
> recorded approval, the run stops with exit code 3.

### 3.3 What Bootshift can claim

This section comes before the stage details because it is the most important fact about the tool.

<table>
<tr><th width="50%">Bootshift can claim</th><th width="50%">Bootshift never claims</th></tr>
<tr><td valign="top">

- These files exist, and this is their persistent identity
- This is the resolved dependency graph, from the build tool itself
- These are the beans, endpoints, repositories and properties, and how they connect
- This is what the original application did, sealed and hashed
- These migration facts are verified against published artifacts
- These repository locations are affected by those facts, and this graph path proves it
- This tool applied these changes, and that fact authorized them
- These dimensions were compared between OLD and NEW, for these scenarios
- This is the evidence level **of each dimension**, with a coverage statement
- **This is what the run could not see**

</td><td valign="top">

- That all business behaviour is equivalent
- That code paths without tests are safe
- That the analysis of reflection and dynamic configuration is complete
- That a dimension it could not observe is not affected
- That a green test suite means correctness
- That a successful startup means migration success
- That an AI proposal is correct because a model made it
- That documentation alone justifies a code change
- That a `DELEGATED` environment gives the same equivalence as a `MANAGED` one

</td></tr>
</table>

### 3.4 The coverage statement rule

A dimension assertion without a coverage statement is not permitted. `Claim.isPublishable()` refuses
it before Agent 19 writes the report.

```text
ILLEGAL:  Security = E4

LEGAL:    Security = E4
          Coverage: 41/44 protected endpoints observed
                    3 unobservable
                    GAP-021
```

---

## 4. The end-to-end workflow

### 4.1 High-level flow

The [summary diagram](#1-summary) shows the eight steps. Each step writes artifacts that the next step
reads. No stage reads the directory of another stage directly. Each stage uses
`StageSupport.upstream()`, which follows the `latest.json` pointer of the upstream stage.

### 4.2 Full flow with the edge loop

```mermaid
flowchart TD
    SRC["Input repository"] --> BOOT["Bootstrap<br/><i>preflight, not an agent</i>"]
    BOOT --> A01["01 Inventory"]
    A01 --> A02["02 Build Resolver"]
    A02 --> A03["03 Application Graph"]
    A03 --> A04["04 Baseline Capture + Seal"]

    A04 --> A05["05 Compatibility &amp; Lifecycle"]
    A05 --> A06["06 Target Resolver"]
    A06 --> A07["07 Documentation Registry"]
    A07 --> A08["08 Migration Knowledge"]
    A08 --> A09["09 Impact Analyzer"]
    A09 --> A10["10 Characterization"]
    A10 --> A11["11 Migration Planner"]

    A11 --> A12["12 Transformation"]
    A12 --> A13["13 Build + Repair"]
    A13 --> A14["14 Graph Rebuild + Diff"]
    A14 --> A15["15 Test Validation"]
    A15 --> A16["16 Runtime Validation"]
    A16 --> A17["17 Differential Validation"]

    A17 --> EDGE{"More migration edges?"}
    EDGE -->|Yes| A12
    EDGE -->|No| A18["18 Approval"]
    A18 --> A19["19 Evidence + Report"]
    A19 --> A20["20 Provenance Graph + Q&amp;A"]
    A20 --> DONE["MIGRATION_COMPLETE<br/>BLOCKED<br/>NEEDS_HUMAN"]

    style A04 fill:#fff3cd,stroke:#856404
    style A12 fill:#f8d7da,stroke:#721c24
    style A13 fill:#f8d7da,stroke:#721c24
    style A19 fill:#d4edda,stroke:#155724
```

### 4.3 Execution flow by phase

```mermaid
flowchart TD
    subgraph INIT["Phase 0 — Initialization"]
        I1["Create run id"] --> I2["Validate input path"]
        I2 --> I3["OSS policy gate<br/>on harness components"]
        I3 --> I4{"Gate passes?"}
        I4 -->|no| IBLOCK["exit 3 POLICY_BLOCK"]
        I4 -->|yes| I5["Create external workspaces"]
        I5 --> I6["Immutable original snapshot<br/>+ mutable migration worktree"]
        I6 --> I7["Init internal checkpoint git"]
    end

    subgraph UNDERSTAND["Phase 1 — Understand (read only)"]
        U1["01 Inventory<br/>allocate FILE_IDs"] --> U2["Seal file registry"]
        U2 --> U3["02 Build Resolver<br/>ask Maven or Gradle"]
        U3 --> U4["03 Build application graph"]
        U4 --> U5["Verify graph<br/>against inventory and build model"]
        U5 --> U6{"Integrity floors met?"}
        U6 -->|no| UBLOCK["exit 3 POLICY_BLOCK"]
    end

    subgraph BASE["Phase 2 — Baseline"]
        B1["Select a compatible toolchain"] --> B2["Build, test, measure coverage"]
        B2 --> B3["Start each module, observe runtime"]
        B3 --> B4["Enrich baseline runtime graph"]
        B4 --> B5["Establish environment<br/>equivalence contract"]
        B5 --> B6["SEAL baseline manifest"]
    end

    subgraph DECIDE["Phase 3 — Decide"]
        D1["05 Compatibility and lifecycle<br/>from artifact metadata"] --> D2["Probe internal components"]
        D2 --> D3["06 Rank candidate targets"]
        D3 --> D4{"Any viable<br/>landing target?"}
        D4 -->|no| DBLOCK["exit 3 with the trade-off explained"]
        D4 -->|yes| D5["FREEZE target and path"]
    end

    subgraph PLAN["Phase 4 — Plan"]
        P1["07 Pin authoritative documents"] --> P2["08 Documentation channel<br/>+ artifact channel"]
        P2 --> P3["Generate property rules<br/>from configuration metadata"]
        P3 --> P4["09 Locate impacts,<br/>measure analyzer accuracy"]
        P4 --> P5["10 Characterize behaviour"]
        P5 --> P6["11 Discover capabilities,<br/>compute residual"]
        P6 --> P7["Reconcile checkpoints"]
        P7 --> P8["FREEZE validation depth per edge"]
    end

    subgraph LOOP["Phase 5 — Migration edge loop"]
        L1["Checkpoint: edge start"] --> L2["12 Propose changes"]
        L2 --> L3["FileMutationGateway<br/><i>the only writer</i>"]
        L3 --> L4["13 Compile, cluster root causes, repair"]
        L4 --> L5["14 Full graph rebuild + diff"]
        L5 --> L6{"Scope authorized?"}
        L6 -->|no| LBLOCK["exit 3 SCOPE VIOLATION"]
        L6 -->|yes| L7["Read frozen validation depth"]
        L7 --> L8{"Tests required?"}
        L8 -->|yes| L9["15 Test + coverage gate"]
        L8 -->|no| L10
        L9 --> L10{"Runtime required?"}
        L10 -->|yes| L11["16 Runtime + binding provenance"]
        L10 -->|no| L12
        L11 --> L12{"Differential required?"}
        L12 -->|yes| L13["17 OLD vs NEW"]
        L12 -->|no| L14
        L13 --> L14["Checkpoint: edge complete"]
    end

    subgraph PROVE["Phase 6 — Prove"]
        V1["18 Raise approval gates"] --> V2{"Outstanding?"}
        V2 -->|yes| VHUMAN["exit 4 NEEDS_HUMAN"]
        V2 -->|no| V3["19 Build claims with coverage"]
        V3 --> V4["Verify ledger and index evidence"]
        V4 --> V5["SEAL evidence manifest"]
        V5 --> V6["20 Provenance graph + Q&amp;A"]
        V6 --> V7["Export validated bundle"]
    end

    INIT --> UNDERSTAND --> BASE --> DECIDE --> PLAN --> LOOP
    LOOP -->|more edges| LOOP
    LOOP -->|done| PROVE

    style BASE fill:#fff3cd,stroke:#856404
    style LOOP fill:#f8d7da,stroke:#721c24
    style PROVE fill:#d4edda,stroke:#155724
```

### 4.4 The run state machine

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> WORKSPACE_READY
    WORKSPACE_READY --> OSS_POLICY_VERIFIED
    OSS_POLICY_VERIFIED --> INVENTORY_COMPLETE
    INVENTORY_COMPLETE --> FILE_REGISTRY_SEALED
    FILE_REGISTRY_SEALED --> BUILD_RESOLVED
    BUILD_RESOLVED --> APPLICATION_GRAPH_BUILT
    APPLICATION_GRAPH_BUILT --> GRAPH_VERIFIED
    GRAPH_VERIFIED --> BASELINE_CAPTURED
    BASELINE_CAPTURED --> BASELINE_SEALED

    note right of BASELINE_SEALED
        R7: no mutation can occur
        before this point
    end note

    BASELINE_SEALED --> COMPATIBILITY_REGISTRY_READY
    COMPATIBILITY_REGISTRY_READY --> TARGET_RESOLVED
    TARGET_RESOLVED --> TARGET_FROZEN
    TARGET_FROZEN --> DOCUMENTATION_RETRIEVED
    DOCUMENTATION_RETRIEVED --> KNOWLEDGE_VERIFIED
    KNOWLEDGE_VERIFIED --> IMPACT_ANALYZED
    IMPACT_ANALYZED --> CHARACTERIZATION_COMPLETE
    CHARACTERIZATION_COMPLETE --> PLAN_FROZEN

    PLAN_FROZEN --> EDGE_TRANSFORMED
    state "per migration edge" as EDGELOOP {
        EDGE_TRANSFORMED --> EDGE_COMPILED
        EDGE_COMPILED --> REPAIRING : diagnostics remain
        REPAIRING --> EDGE_COMPILED : repair applied
        EDGE_COMPILED --> EDGE_GRAPH_REBUILT
        EDGE_GRAPH_REBUILT --> EDGE_SCOPE_VERIFIED
        EDGE_SCOPE_VERIFIED --> EDGE_TESTED
        EDGE_TESTED --> EDGE_RUNTIME_VALIDATED
        EDGE_RUNTIME_VALIDATED --> EDGE_RUNTIME_GRAPH_ENRICHED
        EDGE_RUNTIME_GRAPH_ENRICHED --> EDGE_DIFFERENTIAL_VALIDATED
        EDGE_DIFFERENTIAL_VALIDATED --> EDGE_COMPLETE
        EDGE_COMPLETE --> EDGE_TRANSFORMED : next edge
    }

    EDGE_COMPLETE --> FINAL_APPROVAL : last edge
    FINAL_APPROVAL --> EVIDENCE_SEALED
    EVIDENCE_SEALED --> MIGRATION_COMPLETE
    MIGRATION_COMPLETE --> [*]

    EDGE_TRANSFORMED --> ROLLED_BACK : gate failed
    ROLLED_BACK --> BLOCKED

    CREATED --> FAILED : stage error
    TARGET_RESOLVED --> BLOCKED : policy block
    EDGE_TESTED --> BLOCKED : unexplained regression
    EDGE_DIFFERENTIAL_VALIDATED --> NEEDS_HUMAN : approval gate
    FINAL_APPROVAL --> NEEDS_HUMAN : outstanding gates

    FAILED --> [*]
    BLOCKED --> [*]
    NEEDS_HUMAN --> [*]
    CANCELLED --> [*]
```

**The state machine is not the truth.** `RunState` tells **where** a run is. It never tells **what
is true**. The artifact plane holds the truth (R23). When a stage runs again, it reads the artifacts
again. It does not ask the state machine for the answer. The state machine lets a run continue after
a stop, and it refuses illegal sequences.

**Mutating states have a gate.** Each state has an `isMutating()` flag. `StateMachine` refuses each
transition into a mutating state until `BASELINE_SEALED` has a recorded baseline hash. This is R7 in
code:

```java
public void recordBaselineSeal(String hash) {
    if (baselineHash != null && !baselineHash.equals(hash)) {
        throw new HarnessException("Baseline already sealed with a different hash");
    }
    ...
}
```

**A stage can run again.** `alreadyReached(state)` lets a stage with a valid artifact run again with
the same result. A stage that runs again publishes a **new** directory with a timestamp and moves
`latest.json` to it. The previous directory does not change. Bootshift never overwrites an artifact.

The orchestrated route and the single-edge commands reach the same states. `migrate --edge <id>` and
`validate --edge <id>` reach `EDGE_COMPLETE` in the same way as the `run` command. In an earlier
version they did not. A run that went one edge at a time could not reach the state that approval
needs.

---

## 5. The input — source intake and bootstrap

### 5.1 Source input model

Bootshift accepts two intake shapes. Neither shape needs write access to the input.

```mermaid
flowchart TD
    IN{"Input path"}
    IN -->|"contains .git"| G["Git-backed intake"]
    IN -->|"plain directory"| P["Plain-directory intake"]

    G --> GP["Capture remote, branch,<br/>commit SHA, tree SHA"]
    P --> PP["Compute deterministic<br/>content-manifest hash"]

    GP --> SNAP["Immutable original snapshot"]
    PP --> SNAP
    SNAP --> RO["original/ marked read-only<br/>held for the whole run"]
    SNAP --> MIG["migration/ mutable worktree"]
    MIG --> CKPT["internal checkpoint Git repository<br/><i>inside the external workspace</i>"]

    style RO fill:#e7f3ff,stroke:#0366d6
    style CKPT fill:#fff3cd,stroke:#856404
```

**Bootshift never writes to the input path.** The internal checkpoint history is in the external run
workspace. Thus, a plain directory such as `./src` gets a full migration history, and it never gets a
`.git` folder.

For the reference corpus, Bootshift records the provenance as:

```json
{
  "kind": "PLAIN_DIRECTORY",
  "contentManifestHash": "743b1d181dcb80e00ef3f89d8e7a7f9037147c952595a17780b4b6229b619695",
  "capturedAt": "2026-09-10T05:39:41Z"
}
```

### 5.2 Workspace bootstrap

Bootstrap (Agent 00, `RunBootstrap`) is **infrastructure preflight**. It can create workspaces and
capture provenance. It cannot make a migration decision. Its contract is
`.claude/agents/00-pipeline-conductor.md`. The conductor owns the sequence and the state, never the
truth.

**Procedure**

1. Allocate the run id.
2. Validate the input path.
3. Run the OSS licence gate over the harness components. If the gate fails, stop with exit code 3.
4. Create the `original/`, `runtime-old/`, `migration/` and `runtime-new/` workspaces.
5. Take the read-only snapshot of the repository under analysis.
6. Start the internal checkpoint Git repository.
7. Advance `RunState`.

```mermaid
flowchart TD
    subgraph USER["User input (read-only, never written)"]
        SRC["./src"]
    end

    subgraph WS["External workspace root — BOOTSHIFT_WORKSPACE_ROOT/&lt;run_id&gt;/"]
        ORIG["original/<br/><i>immutable, read-only,<br/>held for the entire run</i>"]
        MIGR["migration/<br/><i>the mutable worktree</i>"]
        ROLD["runtime-old/<br/><i>writable OLD build and run</i>"]
        RNEW["runtime-new/<br/><i>NEW side runtime</i>"]
        CG["internal-checkpoint-git/"]
        EV["evidence/<br/><i>content-addressed</i>"]
        ST["state/"]
    end

    subgraph OUT["Artifact plane — ./output/"]
        STAGES["&lt;stage&gt;/&lt;timestamp&gt;/<br/>&lt;stage&gt;/latest.json"]
    end

    SRC -->|snapshot| ORIG
    SRC -->|snapshot| MIGR
    ORIG -->|seeded copy| ROLD
    MIGR --> CG
    MIGR -->|packaged| RNEW
    ORIG -.->|differential OLD side| ROLD

    style ORIG fill:#e7f3ff,stroke:#0366d6
    style MIGR fill:#f8d7da,stroke:#721c24
```

Two design points came from real runs:

1. **The baseline builds in `runtime-old/`, not in `original/`.** A build inside the original
   snapshot adds `target/` output to it. The files of the snapshot are read-only, so Maven copies
   that attribute into the copied resources, and the second build fails. `original/` does not change.
   `runtime-old/` is the writable copy that Bootshift builds, tests and runs.
2. **Snapshots include the wrapper jars.** The Maven wrapper jar is part of the repository, and the
   harness needs it to reproduce the build. When the snapshot excluded it, the wrapper did not work,
   and the build model silently became non-authoritative.

By default, the workspace root is `<java.io.tmpdir>/bootshift-workspaces`, outside the harness
repository. Thus, nobody commits a customer worktree by accident. `BOOTSHIFT_WORKSPACE_ROOT` and
`--workspace-root` change this location.

### 5.3 Environment provider contract

```mermaid
flowchart TD
    REQ["Environment requirement"] --> MODE{"Provider mode"}

    MODE -->|MANAGED| M["Harness owns creation"]
    MODE -->|DELEGATED| D["CI or infrastructure supplies it"]

    M --> MC["Controls locale, timezone, clock,<br/>encoding, egress policy"]
    M --> MP["Probes for a rootless OCI runtime"]
    MP -->|absent| MG["Records NO_OCI_RUNTIME as a gap;<br/>infrastructure dimensions are unobservable"]

    D --> DD["Reads declared attributes"]
    DD --> DA{"Attestation supplied<br/>for MUST_MATCH?"}
    DA -->|no| DG["UNATTESTED_MUST_MATCH gap:<br/>a delegated environment cannot<br/>self-certify equivalence"]
    DA -->|yes| DOK["Equivalence recorded as enforceable"]

    MC --> EV["Every runtime and differential result records:<br/>mode, implementation, version,<br/>fingerprint, checks, gaps"]
    MG --> EV
    DG --> EV
    DOK --> EV

    style MG fill:#fff3cd,stroke:#856404
    style DG fill:#fff3cd,stroke:#856404
```

Bootshift classifies each attribute before it measures anything:

| Classification | Examples | Effect on the comparison |
|---|---|---|
| `MUST_MATCH` | locale, timezone, clock strategy, file encoding, seed data, broker version, egress policy | A mismatch makes the affected dimensions `NOT_COMPARED` |
| `EXPECTED_TO_DIFFER` | JDK, Spring Boot, Spring Framework, Hibernate, Jackson, servlet container | The difference is expected. It does not make the comparison invalid |
| `UNCONSTRAINED` | OS name, CPU count | Not part of the contract |

A `DELEGATED` environment does not get the same evidence strength as a `MANAGED` one for the same
observations, unless you supply equivalent attested evidence. Bootshift records this asymmetry on
each result. It does not state it as a general rule.

`ContainerProvisioner` has a catalog of images. Each image is pinned by tag and digest: postgres
16.4-alpine, mysql 8.4, mariadb 11.4, mongo 7.0, redis 7.4-alpine, rabbitmq 3.13-alpine and
apache/kafka 3.8.0. It finds a working runtime when the **daemon** answers, not when the binary
exists. The environment artifact records `network.egress.harness = allowlist` and
`network.egress.child.processes = UNRESTRICTED`, because a Maven build that Bootshift starts can reach
the network by itself.

---

## 6. Phase 1 — Understand

Phase 1 is read only. Its three agents find what the repository contains and ask the build tool for
the effective build. Then they build the application graph that all later questions use.

### 6.1 Agent 01 — Inventory

```text
READ ONLY | DETERMINISTIC | ZERO LLM | FIRST STAGE
```

**Purpose.** Find exactly which repository arrived. Allocate the permanent file identities that all
later stages use.

**Why the stage exists.** It has two jobs that no later stage can take. First, classification: no
other stage scans the raw tree. Second, and more important, **identity allocation (R2)**. Assume that a later
stage assigns the identity after a transformation moves a file. Then the harness cannot state that
the file before and after is the same file.

| Input | Source |
|---|---|
| Repository root | Bootstrap snapshot (`original/`) |
| Run id | Run context |
| Exclude policy | `GitScmAdapter.DEFAULT_EXCLUDES` |
| Existing File Registry | Optional. It is present on a scan that runs again |

**Precondition:** `OSS_POLICY_VERIFIED`.

```mermaid
flowchart TD
    START["Enumerate the original snapshot"] --> EX["Apply exclude policy<br/><i>target, build, .git, node_modules</i>"]
    EX --> SL{"Symbolic link?"}
    SL -->|yes| SKIP["Skip: symlinks are an<br/>escape out of the workspace"]
    SL -->|no| CL["Classify role by extension and location"]
    CL --> HASH["SHA-256 the content"]
    HASH --> SCAN["Declare all observed paths<br/>before reattaching any of them"]
    SCAN --> RE{"Existing registry?"}
    RE -->|no| ALLOC["Allocate FILE-&lt;ULID&gt;"]
    RE -->|yes| ORDER["Reattachment order:<br/>1 exact path<br/>2 provider or Git rename<br/>3 exact content hash<br/>4 similarity above threshold<br/>5 allocate new"]
    ALLOC --> SIG
    ORDER --> SIG["Record migration SIGNALS<br/><i>never conclusions</i>"]
    SIG --> SEC["Scan for embedded credentials"]
    SEC --> SEAL["Seal the registry:<br/>hash over identity-to-baseline bindings"]
    SEAL --> PUB["Validate against schema, then publish"]

    style SEAL fill:#fff3cd,stroke:#856404
```

**Signals, not conclusions.** Bootshift records 27 signal definitions with
`"classification": "SIGNAL_NOT_CONCLUSION"`. A signal states *this file mentions
`javax.persistence`*. It never states *this file must be migrated*. Agent 09 makes that decision,
after verified knowledge exists.

Reference corpus result:

| Signal | Count | Signal | Count |
|---|--:|---|--:|
| `LOMBOK` | 24 | `MONGODB` | 9 |
| `SPRING_CLOUD` | 13 | `WAR_PACKAGING` | 6 |
| `WEBFLUX` | 11 | `CONFIG_SERVER` | 6 |
| `EUREKA` | 11 | `INTERNAL_STARTER` | 6 |
| `BOOTSTRAP_PROPERTIES` | 4 | `CUCUMBER` | 4 |
| `JUNIT4` | 3 | `JAVAX_NAMESPACE` | 2 |
| `MOCK_BEAN` | 2 | `SCHEDULING` | 2 |
| `RESILIENCE4J` | 1 | | |

**Outputs**

```text
output/01-inventory/<timestamp>/
├── inventory-artifact.json
├── file-registry.json
├── inventory-signals.json
├── inventory-issues.json
└── manifest.json
```

**Invariants**

- `FILE_ID != PATH` and `FILE_ID != CONTENT_HASH`.
- The reattachment order is fixed. Bootshift records the rule that decided each file.
- No artifact contains a secret value. An artifact contains only the presence, the location and a redacted sample.

| Situation | Behaviour | Exit |
|---|---|---|
| The input path is not a directory | Structured refusal | 2 |
| A file cannot be read | Recorded as `UNREADABLE`, gap `GAP-INV-001` raised | 0 |
| An artifact fails schema validation | `latest.json` does not move | 1 |

Example artifact:

```json
{
  "pipeline_stage": "01-inventory",
  "run_id": "RUN-01M24X67XDPR9YQXDK3QRS7FNB",
  "file_count": 99,
  "role_counts": {
    "JAVA_MAIN": 51, "JAVA_TEST": 12, "MAVEN_BUILD": 6,
    "CONFIG_PROPERTIES": 10, "SETTINGS": 12, "RESOURCE": 8
  },
  "stats": {
    "file_registry_seal": "743b1d181dcb80e0...",
    "signals_recorded": 104
  }
}
```

An inventory issue shows the credential policy:

```json
{
  "kind": "CREDENTIAL_IN_SOURCE",
  "path": "employee-service/src/main/resources/application.properties",
  "line": 3,
  "redacted_sample": "#spring.data.mongodb.uri=mongodb+srv://REDACTED:REDACTED@cluster0...",
  "evidence_policy": "NEVER_STORE_PLAINTEXT"
}
```

**Downstream consumers.** Agents 02, 03, 04, 09, 12, 13, 14, 19 and 20. Each of them addresses files
by `FILE_ID`.

### 6.2 Agent 02 — Build Resolver

**Purpose.** Ask the build tool what the build actually is. XML parsing is never the authority (R5).

**Why the stage exists.** A `pom.xml` describes the intent. Only Maven knows the effective model
after parent resolution, BOM imports, property interpolation and conflict mediation. On the reference
corpus, the declared dependencies are 49. The resolved graph has **962 records across 6 modules** and
**9,186 managed versions**. A plan that uses the first number does not see most of the migration.

**Inputs:** the inventory artifact and the file registry. **Precondition:** `FILE_REGISTRY_SEALED`.

```mermaid
flowchart TD
    D["Discover reactor roots"] --> C["Build executable candidate chain:<br/>1 repository wrapper<br/>2 mvn on PATH<br/>3 MAVEN_HOME / M2_HOME"]
    C --> PROBE["Probe each candidate with -version<br/><i>a wrapper is probed from its own module,<br/>because it resolves its base directory from cwd</i>"]
    PROBE --> OK{"Any candidate<br/>responded?"}
    OK -->|no| DEG["authoritative = false<br/>+ BLOCKING issue<br/>+ descriptor hints only"]
    OK -->|yes| EP["help:effective-pom<br/>-> managed versions"]
    EP --> DL["dependency:list"]
    DL --> DLOK{"Resolved<br/>artifacts?"}
    DLOK -->|no| DT["Fall back to dependency:tree<br/><i>tolerates a malformed upstream POM</i>"]
    DLOK -->|yes| PL
    DT --> DTOK{"Resolved?"}
    DTOK -->|no| BLOCK["BLOCKING issue;<br/>never substitute a version"]
    DTOK -->|yes| PL["resolve-plugins"]
    PL --> CP["build-classpath<br/><i>feeds the type solver in Agent 03</i>"]
    CP --> FW["Detect framework anchors:<br/>Boot, Cloud, Framework, Java, Jackson"]
    FW --> PUB["Publish"]

    style DEG fill:#f8d7da,stroke:#721c24
    style DT fill:#fff3cd,stroke:#856404
```

The `dependency:tree` fallback is necessary. On this corpus, `dependency:list` fails completely.
The cause is `xml-apis:xml-apis-ext:1.3.04`, which comes transitively through Apache POI. Its POM
declares `distributionManagement.status`, so Maven cannot build its model. `dependency:tree` walks
the resolved graph and accepts it. The fallback is the difference between **530** and **962**
dependency records.

`BuildSystemResolver` also finds Gradle builds and mixed builds (`BuildSystemPort.Kind.MIXED`).
`BuildModelCodec` gives the build model a versioned encode and decode contract with a fingerprint.
Before the codec, each stage built a partial model by hand and lost managed versions, plugins,
repositories and issues.

**Outputs**

```text
output/02-build/<timestamp>/
├── build-model.json          modules, toolchains, frameworks, authoritative flag
├── dependency-model.json     962 resolved records with scope and provenance
├── bom-model.json            9,186 managed versions, imported BOMs
├── plugin-model.json
├── repository-model.json
└── resolution-issues.json
```

**Invariants**

- `authoritative=false` when the tool did not answer, with a `degraded_reason`.
- An unresolved required artifact is a blocking issue or an explicit blind spot. Bootshift **never substitutes** a version.
- Bootshift captures the classpath, so that type attribution in later stages is real.

| Situation | Behaviour | Exit |
|---|---|---|
| No Maven or Gradle build found | Structured refusal | 2 |
| The tool is not available | Publishes a non-authoritative model and blind spot `BS-BUILD-001` | 0 |
| Some coordinates are not resolved | Gap `GAP-BUILD-001` | 0 |

Example artifact:

```json
{
  "kind": "MAVEN",
  "tool_version": "Apache Maven 3.8.7",
  "wrapper_used": true,
  "authoritative": true,
  "frameworks": {
    "spring-boot": "2.7.12",
    "spring-cloud": "2021.0.7",
    "spring-framework": "5.3.27",
    "jackson": "2.13.5",
    "java": "17"
  }
}
```

**Downstream consumers.** Agents 03, 04, 05, 08, 13, 14, 15 and 16.

### 6.3 Agent 03 — Application Graph

**Purpose.** Build a type-aware representation of the application with many views. Then verify it
against independent sources.

**Why the stage exists.** Each later question is a graph traversal. Examples: what depends on this,
what is the blast radius, and which validation dimensions does this impact need. Without the graph,
impact analysis is only a text search, and a text match cannot explain *why* something is affected.

**Inputs:** the file registry, the build model and the dependency model with the resolved classpath.
**Precondition:** `BUILD_RESOLVED`.

```mermaid
flowchart TD
    MOD["Module nodes from the<br/>authoritative build model"] --> LIB["Library nodes<br/>+ DEPENDS_ON_LIBRARY edges"]
    LIB --> FILES["File nodes from the registry"]
    FILES --> PARSE["JavaParser with symbol solver<br/><i>real dependency jars on the classpath</i>"]
    PARSE --> TYPES["Type nodes with Spring stereotype<br/>classification and attribution status"]
    TYPES --> MEM["Member nodes: methods,<br/>constructors, fields"]
    MEM --> AP["Synthesize annotation-processor members<br/><i>Lombok accessors, builder, log</i>"]
    AP --> LINK["Link relationships:<br/>EXTENDS, IMPLEMENTS, IMPORTS, USES_TYPE,<br/>CALLS, INJECTS, CALLS_SERVICE, CALLS_REPOSITORY"]
    LINK --> UNRES["Recover unresolved calls<br/>only when exactly one visible type matches;<br/>confidence 0.5, basis recorded"]
    UNRES --> EP["Endpoint nodes from<br/>request mapping annotations"]
    EP --> CFG["Configuration view:<br/>properties, profiles, placeholders"]
    CFG --> INT["Integration view: Eureka, Config Server,<br/>MongoDB, brokers, external HTTP"]
    INT --> VIEWS["Project ten view artifacts"]
    VIEWS --> VER["Independent verification"]

    style AP fill:#e7f3ff,stroke:#0366d6
    style VER fill:#d4edda,stroke:#155724
```

**Two details make the graph real.**

1. **The type solver gets the actual classpath.** Agent 02 captures the resolved compile and test
   classpath, and Agent 03 gives it to the symbol solver. On the reference corpus, this moved type
   attribution from **0.19 to 0.69**.
2. **Bootshift models annotation-processor members explicitly.** Lombok makes accessors, builders
   and the `log` field at compile time. They are not in the source, so a source parser cannot resolve
   a call to `employee.getName()` or `log.trace(...)`. Without them, most service-to-model
   relationships are not in the graph. Bootshift synthesizes them and marks them
   `synthetic=true, generated_by=lombok`. This took `CALLS` edges from 31 to 81 and `DECLARES` edges
   from 239 to 370.

**Graph verification.** The verification never uses the output of the graph to check the graph. Each
check uses an independent source.

| # | Check | Compared with | Reference result |
|---|---|---|---|
| 1 | Java file coverage | Inventory registry | 63/63 = 1.0 |
| 2 | Module coverage | Build model | 6/6 |
| 3 | Type coverage | Parser output | 63/63 |
| 4 | Endpoint coverage | Controller nodes | 5 controllers → 10 endpoints |
| 5 | Spring component coverage | Annotation extraction | 25 components |
| 6 | Resolved dependency coverage | Build model | 300/300 distinct libraries |
| 7 | Representative edge spot checks | Source file and line | 12 sampled with evidence |
| 8 | No contamination from harness source | Package prefix | 0 |
| 9 | Attribution ratio | Parser | 0.6858 |
| 10 | Graph query integrity | Live traversal | Blast radius and transitive dependencies both have results. Each result has a path |

If check 1, 2, 4, 6, 8 or 10 fails, the run stops. If check 9 is below the policy floor, Bootshift
raises `GAP-GRAPH-001` and caps the impact classification. The verification report also compares
the parsed compilation units with the type declarations from `FILE` nodes, and it reports a shortfall
as a gap.

**Outputs.** Thirteen kinds of artifact: `application-graph.json`, `file-registry.json`,
`symbol-registry.json`, ten view projections (`module-graph`, `file-graph`, `symbol-graph`,
`dependency-graph`, `spring-graph`, `configuration-graph`, `persistence-graph`, `endpoint-graph`,
`test-graph`, `integration-graph`), and `graph-summary.json`, `graph-issues.json` and the verification
report in JSON and Markdown.

**Reference corpus graph.** **839 nodes, 1,852 edges, 239 symbols**, structural hash `4bf6f2986e6d…`.

| Node type | n | Node type | n | Edge type | n |
|---|--:|---|--:|---|--:|
| LIBRARY | 300 | METHOD | 84 | DEPENDS_ON_LIBRARY | 962 |
| FILE | 99 | CONTROLLER | 5 | DECLARES | 370 |
| FIELD | 87 | SERVICE | 5 | CONTAINS | 172 |
| PACKAGE | 38 | REPOSITORY | 5 | IMPORTS | 90 |
| CLASS | 12 | MONGODB_DOCUMENT | 4 | CALLS | 81 |
| TEST | 12 | INTERFACE | 4 | USES_TYPE | 80 |
| ENDPOINT | 10 | ENUM | 6 | CONFIGURES | 21 |
| CONFIG_PROPERTY | 10 | MODULE | 6 | COVERED_BY_TEST | 18 |
| CONFIGURATION_CLASS | 8 | SPRING_BEAN | 2 | READS_FROM_CONFIG_SERVER | 10 |

**Invariants**

- The portable JSON is the truth. Correctness never needs a graph database.
- The static graph has no runtime facts. Blind spot `BS-GRAPH-RUNTIME` states this.
- Bootshift labels unresolved relations. It never shows them as high confidence.

| Situation | Behaviour | Exit |
|---|---|---|
| A verification floor is breached | Publishes the artifacts, blocks the baseline | 3 |
| Attribution is below the floor | Gap `GAP-GRAPH-001`, impact capped | 0 |
| Schema validation fails | The pointer does not move | 1 |

**Downstream consumers.** Agents 04, 09, 10, 14, 16, 17, 19 and 20, and the `graph` CLI queries.
[Section 12](#12-the-application-graph-model) describes the graph model in full.

---

## 7. Phase 2 — Baseline

### 7.1 Agent 04 — Baseline

**Purpose.** Observe the original application, then seal the observations.

**Why the stage exists.** This stage makes each later claim testable. After this stage, R7 permits
changes, and R30 forbids any change to what it recorded.

```mermaid
flowchart TD
    ENV["Establish the environment<br/>equivalence contract<br/><i>before anything is measured</i>"] --> TC["Discover installed JDKs"]
    TC --> SEL{"A JDK matching the<br/>declared language level?"}
    SEL -->|no| TCB["Blind spot BS-TOOLCHAIN-001:<br/>every observation below is unreliable"]
    SEL -->|yes| HAZ["Check known toolchain hazards<br/><i>for example Lombok before 1.18.30 on JDK 21</i>"]
    HAZ --> COPY["Seed runtime-old/ from original/<br/><i>never build inside the pristine snapshot</i>"]
    COPY --> BUILD["Compile every module"]
    BUILD --> TEST["Attach JaCoCo as a CLI goal<br/><i>a POM edit violates R7</i>"]
    TEST --> AGENT{"Did the agent attach?"}
    AGENT -->|no| RETRY["Re-run tests without coverage;<br/>record coverage unavailable with the reason"]
    AGENT -->|yes| COV["Parse Surefire and JaCoCo"]
    RETRY --> COV
    COV --> CFG["Capture configuration with<br/>sensitive values as metadata only"]
    CFG --> PKG["Package, then start each module<br/>with derived isolation settings"]
    PKG --> OBS["Observe context, health, beans, conditions,<br/>mappings, bound properties"]
    OBS --> RG["Enrich the baseline runtime graph"]
    RG --> SEAL["Hash every observation,<br/>the tree, the registry seal and the fingerprint"]
    SEAL --> IMMUT["BASELINE SEALED — immutable for this run"]

    style RETRY fill:#fff3cd,stroke:#856404
    style IMMUT fill:#fff3cd,stroke:#856404
```

**Three details came from real runs against the corpus.**

1. **Toolchain selection is environment provisioning, not a change.** Spring Boot 2.7.12 pins
   Lombok 1.18.24, which cannot run on JDK 21. It fails with `NoSuchFieldError` on
   `JCTree$JCImport.qualid`. A baseline compiled on JDK 21 gives a failure that tells nothing about
   the migration. The probe finds a JDK that agrees with the declared level, selects it and records
   the selection in the sealed manifest. If no such JDK exists, the probe states this. It does not
   give a result that has no meaning.
2. **Coverage instrumentation must not destroy the observation.** `employee-service` pins Surefire
   2.19.1, which does not evaluate the `argLine` property late. `jacoco:prepare-agent` sets that
   property. The fork stops with `processing of -javaagent failed` and writes **zero** test reports.
   Thus, the harness reported "0 tests" for a module that has tests. The fallback finds the agent
   failure in the Surefire dump stream, runs the tests again without instrumentation, and reports
   coverage as unavailable **with the reason**. The baseline now observes 12 tests, not 5.
3. **Runtime isolation settings come from each module.** It is correct to disable the Eureka client
   for a Eureka *client*. It is fatal for the Eureka *server*, because its own auto-configuration
   needs those beans. The first version of this code broke `discovery-service` for a reason that had
   no relation to the application.

| Dimension | What Bootshift captures | Reference result |
|---|---|---|
| Build | success, diagnostics, duration, toolchain | 6/6 modules compile on JDK 17.0.20.1 |
| Tests | pass, fail, error, skip, for each case | 12 tests, 2 MongoDB failures that existed before the migration |
| Coverage | instruction, branch, line, for each module | JaCoCo for 5 modules. 1 module unavailable, with the reason |
| Configuration | effective properties with provenance | 10 files, sensitive values as metadata |
| Runtime | context, beans, conditions, mappings, health | 5/6 modules started |
| Binding | canonical key, source, target, bound, defaulted | 368 bound properties |
| Integrations | Config Server, Eureka, MongoDB, schedulers | Recorded for each module |

**The seal**

```json
{
  "original_tree_hash": "743b1d181dcb80e0...",
  "old_workspace_hash": "743b1d181dcb80e0...",
  "old_workspace_matches_original": true,
  "file_registry_seal": "743b1d181dcb80e0...",
  "sealed_environment_fingerprint": "8f2c…",
  "environment_mode": "MANAGED",
  "observation_hashes": {
    "build": "…", "tests": "…", "coverage": "…", "configuration": "…",
    "runtime": "…", "runtime_graph": "…", "static_graph": "…", "toolchain": "…"
  },
  "normalization_policy_hash": "…",
  "sealed": true,
  "baseline_manifest_hash": "217d0c7f7d8e8a1a…"
}
```

**Invariants**

- A seal with a different hash throws. A new baseline needs a new run identity (R30).
- A module that does not start gives a blind spot. It never gives an assumed pass (R19).
- No artifact contains a sensitive value in plaintext.

| Situation | Behaviour | Exit |
|---|---|---|
| The original snapshot is missing | Structured refusal | 2 |
| A module does not build | Recorded as baseline debt that existed before the migration | 0 |
| A module does not start | Blind spot `BS-RUNTIME-<MODULE>` | 0 |
| A seal is attempted again with a different hash | Policy block | 3 |

**Downstream consumers.** Agents 05, 09, 10, 15, 16, 17 and 19, and the gateway. The gateway refuses
to write without the seal.

---

## 8. Phase 3 — Decide

### 8.1 Agent 05 — Compatibility Registry

**Purpose.** Build Tier-1 version-space knowledge that does not depend on the repository. The stage
runs **before** target resolution, so that the two stages do not depend on each other in a circle.

**Why the stage exists.** Target resolution must know which lines exist, which lines have support,
which Java versions they accept and which Spring Cloud train goes with them. If Bootshift took these
facts from the repository, the logic is circular. A hard-coded table becomes old.

```mermaid
flowchart TD
    META["maven-metadata.xml for spring-boot<br/><i>authoritative: which lines exist</i>"] --> LINES["Derive lines and newest stable patch"]
    LINES --> LC{"Curated lifecycle table<br/>still within its as-of window?"}
    LC -->|yes| VER["quality = VERIFIED"]
    LC -->|no| ADV["Consult the community aggregator<br/>quality = ADVISORY"]
    ADV --> NONE{"Still nothing?"}
    NONE -->|yes| UNK["quality = UNKNOWN"]
    VER --> CLOUD
    ADV --> CLOUD
    UNK --> CLOUD["For each Spring Cloud train line,<br/>read spring-cloud-starter-parent POM<br/>and take its Boot parent version"]
    CLOUD --> AVAIL["Probe artifact existence<br/>for each candidate"]
    AVAIL --> INT["Classify internal components<br/>by public-repository existence"]
    INT --> PUB["Publish registry"]

    style VER fill:#d4edda,stroke:#155724
    style ADV fill:#fff3cd,stroke:#856404
    style UNK fill:#f8d7da,stroke:#721c24
```

**Evidence quality controls what a fact can do.** A curated lifecycle table is correct only while a
person updates it. Thus, the table has an `as_of` date (`LifecycleSource.AS_OF` is `2026-09-01`), and
it becomes **stale** automatically. The quality then decides what the fact can do:

| Quality | Source | Can it eliminate a target? |
|---|---|---|
| `VERIFIED` | Curated Tier-1 table, inside its as-of window | **Yes** |
| `ADVISORY` | Community aggregator, or a stale curated entry | No. It raises an approval gate |
| `ESTIMATED` | Derived from the release cadence | No |
| `UNKNOWN` | No source was reachable | No |

This is the difference between "we know that this line is end of life" and "we think that it is".
Only the first can reject a target by itself. As the data gets older, the harness becomes *more*
careful.

**Bootshift verifies the Spring Cloud mapping from artifacts.** Bootshift does not remember the
train-to-Boot mapping. It reads the mapping from the published `spring-cloud-starter-parent` POM,
which declares `spring-boot-starter-parent` as its parent. One probe for each train line covers the
full mapping.

| Boot line | Spring Cloud train | Evidence |
|---|---|---|
| 3.2 | 2023.0.5 | Parent version in the starter-parent POM |
| 3.3 | 2023.0.6 | Parent version in the starter-parent POM |
| 3.4 | 2024.0.3 | Parent version in the starter-parent POM |
| 3.5 | 2025.0.3 | Parent version in the starter-parent POM |
| 4.0 | *none* | No GA train targets this line |
| 4.1 | *none* | No GA train targets this line |

**A probe classifies internal components. A name does not.** An earlier version used a group-id
allowlist and gave 31 false positives: `joda-time`, `junit:junit`, `xalan` and the full Sonatype Aether
stack. A name rule cannot tell an organization starter from a public library with an unusual
coordinate. The current test is existence. If the configured public repository resolves the
coordinate, it is a public component. If not, it is internal, and its compatibility is `UNKNOWN` until
a profile gives evidence. On the reference corpus, this correctly gives **zero** internal components.

**Outputs.** `compatibility-registry.json`, `lifecycle-registry.json`, `artifact-availability.json`,
`version-space-evidence.json`, `internal-components.json`.

**Invariants**

- No compatibility evidence means `UNKNOWN`, never "compatible" (R28).
- Community sources are only advisory. They never become hard assertions.
- Bootshift caches each fact from an outside source, with provenance and a content hash.

**Downstream consumers.** Agent 06 mainly, and Agents 08 and 18.

### 8.2 Internal component onboarding

A private starter without proof of compatibility is a migration risk. Usually nobody sees this risk
before runtime.

```mermaid
flowchart TD
    DEP["Resolved dependency"] --> MOD{"One of the<br/>repository modules?"}
    MOD -->|yes| SKIP["Not a third-party component"]
    MOD -->|no| PROF{"InternalComponentProfile<br/>on disk?"}
    PROF -->|yes| USE["Use the declared compatibility<br/>and its evidence references"]
    PROF -->|no| PROBE["Probe the public artifact repository"]
    PROBE --> EX{"Coordinate resolves?"}
    EX -->|yes| PUBLIC["Public ecosystem component"]
    EX -->|no| ONLINE{"Repository reachable?"}
    ONLINE -->|no| UNDET["UNDETERMINED<br/><i>cannot tell public from internal offline</i>"]
    ONLINE -->|yes| INTERNAL["INTERNAL_COMPONENT<br/>compatibility = UNKNOWN"]
    INTERNAL --> ACT{"Policy action"}
    ACT -->|BLOCK| B["Eliminates target candidates"]
    ACT -->|WARN| W["Caution + approval gate"]

    style INTERNAL fill:#fff3cd,stroke:#856404
    style B fill:#f8d7da,stroke:#721c24
```

An `InternalComponentProfile` is a file at
`policies/default/internal-components/<groupId>_<artifactId>.json`. The README in that folder gives
this example:

```json
{
  "group_id": "com.acme",
  "artifact_id": "acme-spring-starter",
  "component_version": "4.2.1",
  "compatibility": "SUPPORTED",
  "java_range": "17-21",
  "spring_boot_range": "3.2.0-3.5.999",
  "spring_framework_range": "6.1.0-6.2.999",
  "spring_security_range": "6.2.0-6.4.999",
  "transitive_managed_dependencies": ["com.acme:acme-core:4.2.1"],
  "migration_notes": "4.2.x drops the deprecated AcmeAutoConfiguration entry point.",
  "evidence_references": ["https://internal.acme/…", "sha256:…"],
  "owner_contact": "platform-team@acme.example",
  "confidence": "HIGH",
  "status": "VERIFIED"
}
```

`compatibility` must be `SUPPORTED`, `UNSUPPORTED` or `UNKNOWN`. A value other than `SUPPORTED`
constrains target resolution. A private starter never blocks graph resolution silently. Bootshift
always shows it as a named constraint.

### 8.3 Agent 06 — Target Resolver

**Purpose.** Decide **where** to go. It does not decide how to go there.

**Why the stage exists.** `--target auto` is the point where a migration tool is most frequently
wrong without notice. "Newest" is not "safe". R25 defines `auto` as the **highest safe supported
stable GA** state. The rank uses supportability and compatibility evidence. The version number only
breaks a tie.

```mermaid
flowchart TD
    LC["Lifecycle registry<br/><i>with evidence quality</i>"] --> CAND["Enumerate stable GA candidates"]
    COMPAT["Compatibility registry"] --> CAND
    AVAIL["Artifact availability"] --> CAND
    INT["Internal components"] --> CAND
    JDK["Installed JDKs"] --> CAND

    CAND --> E1{"Stable GA?"}
    E1 -->|no| X1["eliminate"]
    E1 -->|yes| E2{"Newer than<br/>the current state?"}
    E2 -->|no| X2["eliminate"]
    E2 -->|yes| E3{"Artifacts exist?"}
    E3 -->|no| X3["eliminate"]
    E3 -->|yes| E4{"App uses Spring Cloud<br/>and a GA train exists?"}
    E4 -->|no train| X4["eliminate:<br/>required artifacts do not exist"]
    E4 -->|ok| E5{"An installed JDK satisfies<br/>the range and the project level?"}
    E5 -->|no| X5["eliminate"]
    E5 -->|yes| E6{"Lifecycle evidence<br/>quality?"}
    E6 -->|VERIFIED + EOL| X6["eliminate"]
    E6 -->|ADVISORY + EOL| C1["caution -> approval gate"]
    E6 -->|ok| RANK
    C1 --> RANK["Rank by support horizon,<br/>GA status, availability,<br/>evidence quality, train presence"]
    RANK --> PICK{"Any viable?"}
    PICK -->|no| BLOCK["exit 3 with the closest<br/>candidate and the exact policy flag<br/>that admits it"]
    PICK -->|yes| LAND["Landing target"]
    LAND --> PATH["Compute transit checkpoints"]
    PATH --> FREEZE["FREEZE target and path"]

    style X4 fill:#f8d7da,stroke:#721c24
    style C1 fill:#fff3cd,stroke:#856404
    style FREEZE fill:#d4edda,stroke:#155724
```

**A transit checkpoint is not a landing target.** A version can be a correct step and not an
acceptable place to stop. Spring Boot 3.0 is end of life, and Bootshift never selects it as a landing
target. But it is a **mandatory** transit checkpoint, because it carries the Jakarta namespace
relocation, the Spring Security 6 configuration model and the Java 17 baseline. If the path skips
it, one diff hides three different failure modes.

**The result on the reference corpus.** Under the strict production policy, **each candidate was
eliminated**, and the harness gave the exact reason:

```text
POLICY BLOCK: No Spring Boot line satisfies the active policy as a landing target.
  2.7 -> the application uses Spring Cloud but no GA release train targets this Boot line…; open-source support has ended
  3.0 -> open-source support has ended
  3.1 -> …no GA release train…; open-source support has ended
  3.2 -> …no GA release train…
  3.3 -> open-source support has ended
  3.4 -> open-source support has ended
  3.5 -> open-source support has ended
  4.0 -> the application uses Spring Cloud but no GA release train targets this Boot line…
  4.1 -> the application uses Spring Cloud but no GA release train targets this Boot line…

Closest supportable candidate: 3.5.16 with Spring Cloud 2025.0.3.
It was rejected because: open-source support has ended.
If that is an accepted business risk, re-run with a policy that sets
allow_eol_landing_target=true, which records the exception explicitly rather than hiding it.
```

This is the correct answer for this application in September 2026. The newest Boot lines have no GA
Spring Cloud train yet. The newest line that has a train is past its open-source support date by a short time. A
tool that silently selects 4.1 gives a repository that cannot resolve
`spring-cloud-starter-netflix-eureka-client`.

With the documented exception policy, the resolution continues:

```text
Landing target Spring Boot 3.5.16 (Java 21, Spring Cloud 2025.0.3),
8 migration edge(s), -2 month support horizon
```

Agent 18 then raises a `SHORT_HORIZON_TARGET` gate for it.

**The frozen path (reference corpus)**

| Edge | From → To | Class | Mandatory | Landing |
|---|---|---|:--:|:--:|
| `EDGE-1-PREP-TEST` | 2.7.12 → 2.7.12 | PREPARATORY | ✅ | |
| `EDGE-2-PATCH` | 2.7.12 → 2.7.18 | PATCH | | |
| `EDGE-3-MAJOR` | 2.7.18 → 3.0.13 | **MAJOR_BOUNDARY** | ✅ | |
| `EDGE-4-MINOR` | 3.0.13 → 3.1.12 | MINOR | | |
| `EDGE-5-MINOR` | 3.1.12 → 3.2.12 | MINOR | | |
| `EDGE-6-MINOR` | 3.2.12 → 3.3.13 | MINOR | | |
| `EDGE-7-MINOR` | 3.3.13 → 3.4.13 | MINOR | | |
| `EDGE-8-MINOR` | 3.4.13 → 3.5.16 | MINOR | | ✅ |

The preparatory test-infrastructure edge runs **first**, before any framework change. It proves that
the pass, fail and skip semantics stay the same by themselves. Without it, nobody can tell a later
regression from an effect of the test runner.

The final edge has the class `LANDING`, not `MINOR`. A version can be a correct transit checkpoint and
an incorrect place to stop, and the two need different validation depths. The recorded run 2 in
`reports/` names the edges `EDGE-3-MAJOR-3` and `EDGE-8-LANDING`.

**The boundary rule.** **Each** major version that the path crosses gets its own mandatory
`MAJOR_BOUNDARY` edge. The path reaches the last supported line of a major before it enters the next
major.

The path builder once went directly to the lowest line of the *landing* major. For the 2.7 → 3.5
migration above, this gave the correct answer, because the path crosses only one major. For a
2.7 → 4.x migration it did not. It made one boundary edge into 4.0 and **no Boot 3 checkpoint**. Thus,
it skipped the transition that carries the Jakarta EE relocation, the Spring Security 6 configuration
change and the Java 17 baseline. A 2.7 → 4.1 path is now:

```text
PREPARATORY → PATCH(2.7.18) → MAJOR_BOUNDARY(3.0) → MINOR(3.1…3.5)
            → MAJOR_BOUNDARY(4.0) → LANDING(4.1)
```

The stage checks this before it freezes anything. If the number of `MAJOR_BOUNDARY` edges is less than
the number of majors between the source and the landing target, the stage fails. It does not publish a
path that skips a boundary. The path builder is the only control between a migration across two
majors and a rewrite in one step.

**Java target selection.** Bootshift does **not** take the Java level of the application from the JDK
that runs Bootshift. That JDK is a fact about the harness process, not about the application. With
that rule, the target depended on how the operator started the tool. On a machine with a newer JDK, it
silently raised the compiler target on an edge that did not carry that change.

`JavaTargetSelector` selects the Java level in this sequence:

1. Take the JDKs that are actually installed.
2. Keep only the majors that the target Boot line of the edge supports.
3. Remove each major below the current level of the project.
4. Apply the policy preference. The default is the **highest LTS** release.

A non-LTS release is not a defensible production landing target. Each edge records `edge_java`,
`java_vendor`, `java_version`, `java_home`, the selection reason and the evidence. If no installed JDK
satisfies an edge, Bootshift reports a blind spot. It never uses the harness JVM as a substitute.

| Situation | Behaviour | Exit |
|---|---|---|
| The source version cannot be found | Structured refusal | 2 |
| An explicit target is not a known line | Structured refusal | 2 |
| An explicit target is not viable | Policy block that names the eliminations | 3 |
| No candidate is viable | Policy block that explains the trade-off | 3 |

---

## 9. Phase 4 — Plan

### 9.1 Agent 07 — Documentation Registry

**Purpose.** Get the authoritative documents for the frozen path and pin them by content hash.

**Why the stage exists.** It runs **after** the path is frozen. Only then does Bootshift know which
edge-specific documents are necessary. If it gets all documents first, it pins documents for
edges that the plan never contains.

```mermaid
flowchart TD
    PATH["Frozen migration path"] --> PER["For each edge that changes version"]
    PER --> URL["Locate release notes<br/>and migration guide for the target line"]
    URL --> ALLOW{"Host on the<br/>egress allowlist?"}
    ALLOW -->|no| REFUSE["Egress refused"]
    ALLOW -->|yes| CACHE{"Already in the<br/>content-addressed cache?"}
    CACHE -->|yes| HIT["Serve from cache;<br/>the run stays reproducible offline"]
    CACHE -->|no| FETCH["Fetch and store"]
    FETCH --> RAW["Store the raw snapshot<br/><i>this is the authority</i>"]
    RAW --> TEXT["Store a readable rendition<br/><i>balanced-tag container extraction</i>"]
    HIT --> REG
    TEXT --> REG["Record id, publisher, component,<br/>versions, URL, timestamp, content hash,<br/>trust level, ETag, sizes"]
    REG --> COV["Report coverage and<br/>extraction usability"]

    style RAW fill:#d4edda,stroke:#155724
```

| Trust level | Use |
|---|---|
| `OFFICIAL_MIGRATION_GUIDE` | Candidate facts, highest documentation precedence |
| `OFFICIAL_RELEASE_NOTES` | Candidate facts |
| `OFFICIAL_METADATA` | Artifact-channel evidence |
| `OFFICIAL_API_DOC` / `OFFICIAL_GENERAL_DOC` | Candidate facts |
| `UPSTREAM_PROJECT_DOC` | Candidate facts |
| `COMMUNITY_ADVISORY` | **Advisory only**. Confidence 0.2. Never the only basis |

**The extraction is a rendition. It never replaces the source.** Spring publishes its official
documentation as HTML wiki pages. A prose pattern that reads the navigation of a page finds only
noise. Thus, Bootshift stores a readable rendition next to the raw snapshot and keeps both. The raw
snapshot stays the authority. No summary, from an LLM or from another source, can replace it.

The container extraction counts opening and closing tags. It does not use a lazy regex. A lazy match
stops at the first inner close tag. It silently cut a one-megabyte page to **430 characters** of
navigation. With depth counting, the same page gives **20,396 characters** of real text. Bootshift
reports `extracted_text_length` for each document, so that a reader can see which is which.

**Classification of components.** Resolution evidence classifies a component. The shape of its group
id does not. If the build resolver got a coordinate from a public repository, it is a public
open-source component, whatever its group name is. Only a coordinate that did not resolve, or that
came from a repository that is not clearly public, is reported as *possibly* internal. It is only
"possibly", because Bootshift cannot tell a private mirror of a public library from the library. The
registry keeps two gaps apart:

- `public_components_without_catalogued_document` is a gap in the catalogue of the harness.
- `possibly_internal_components` is a gap in what the harness can reach.

When the two lists were one list, about three dozen ordinary libraries hid the few entries that
needed a reader.

**Reference result:** **15 documents pinned across 7 edges, including 7 official migration guides.**
Recorded run 2 in `reports/` pinned 21 documents.

| Situation | Behaviour | Exit |
|---|---|---|
| The host is not on the allowlist | Refused. Recorded as an attempt that got no document | 0 |
| Offline | Cached documents still resolve. Blind spot `BS-DOC-001` | 0 |
| An edge has no guide | Gap `GAP-DOC-001`. The artifact channel covers the edge alone | 0 |
| A document was received but cannot be read | Gap `GAP-DOC-002` | 0 |

### 9.2 Agent 08 — Migration Knowledge

**Purpose.** Change official documentation and artifact facts into **verified** migration facts.

```mermaid
flowchart TD
    subgraph DOC["DOCUMENTATION CHANNEL — what the maintainers intended"]
        D1["Pinned document renditions"] --> D2["Sentence-level prose patterns:<br/>removed, deprecated, renamed to,<br/>replaced by, replacement introduced,<br/>no longer supported, default changed"]
        D2 --> D3["Subject filter:<br/>annotation, dotted name or CamelCase"]
        D3 --> D4["CANDIDATE facts<br/><i>never authorize a change</i>"]
    end

    subgraph ART["ARTIFACT CHANNEL — what actually changed in the bytes"]
        A1["Source and target BOM diff"] --> A2["Managed version changes<br/>restricted to what this app uses"]
        A1 --> A3["Artifacts removed from management"]
        A4["Artifact existence probes"] --> A5["Coordinates that no longer resolve"]
        A6["spring-configuration-metadata.json diff"] --> A7["Official deprecation entries<br/>with replacement keys"]
        A8["Structural edge facts"] --> A9["Jakarta relocation, Java baseline,<br/>Security 6 model, autoconfig imports"]
    end

    D4 --> MERGE{"Merge by subject and type"}
    A2 --> MERGE
    A3 --> MERGE
    A5 --> MERGE
    A7 --> MERGE
    A9 --> MERGE

    MERGE -->|"artifact evidence agrees"| V["VERIFIED<br/>may authorize a transformation"]
    MERGE -->|"channels disagree<br/>about the replacement"| C["CONFLICTING<br/>requires human resolution"]
    MERGE -->|"documentation only"| CAND["CANDIDATE<br/>cannot authorize a change"]

    A7 --> GEN["Generate property migration rules<br/>with evidence attached"]

    style V fill:#d4edda,stroke:#155724
    style C fill:#f8d7da,stroke:#721c24
    style CAND fill:#fff3cd,stroke:#856404
    style GEN fill:#e7f3ff,stroke:#0366d6
```

**Only artifact evidence authorizes a change.** `MigrationFact.verifyWithArtifactEvidence` is the only
path to `VERIFIED`, and it needs artifact-channel evidence. Documentation describes the intent.
Artifacts describe what was published. The second authorizes a code change (R10).

**Reference corpus knowledge: 1690 facts, 1683 `VERIFIED`, 7 `CANDIDATE`, 0 `CONFLICTING`.**

| Fact type | n | Source |
|---|--:|---|
| `API_REMOVED` | 1034 | **Published-bytecode diff**: types in the source jar that are not in the target jar |
| `PROPERTY_RENAMED` | 389 | Configuration metadata deprecation entries with a replacement |
| `PROPERTY_REMOVED` | 154 | Configuration metadata deprecations without a replacement |
| `MANAGED_VERSION_CHANGED` | 105 | BOM diff, limited to coordinates that this application uses |
| `ARTIFACT_REMOVED` | 3 | BOM diff and existence probes |
| `API_RENAMED` | 2 | Structural boundary fact and prose |
| `BASELINE_REQUIREMENT` | 1 | Java 17 baseline at the 3.x boundary |
| `COMPATIBILITY_REQUIREMENT` | 1 | Spring Cloud train lock |
| `BEHAVIOR_CHANGED_NO_API_CHANGE` | 1 | `spring.factories` to `AutoConfiguration.imports` |

Artifact-channel measurements: the source BOM has **1151** entries and the target BOM has **1471**
(both after Bootshift follows the imports). Bootshift read three configuration-metadata artifacts and
found **542 deprecated properties**. It compared **60 jar pairs**, which is the budget limit, and
found **1028 removed types**.

The seven `CANDIDATE` facts are real breaking changes that the documentation states clearly. Examples
are `RestHighLevelClient`, `YamlJsonParser`, `WebMvcMetricsFilter` and `banner.png`. They stay
`CANDIDATE` because no artifact observation in this run confirmed them. Thus, they cannot authorize a
transformation. This is the rule in operation, not a gap.

**The channel that opens the jar.** BOM entries, existence probes and configuration metadata describe
the *packaging* of a dependency. Only `javap` over two published jars can show that a **type** was
removed. "Documented as deprecated" is a different and weaker claim.

Agent 08 selects each coordinate that the build actually resolved and that the source and target BOMs
manage at different versions. For each one, it gets both jars and compares their public and protected
signatures. Agent 08 reads both BOM families. The Spring Boot BOM manages no Spring Cloud artifact. A
diff that read only the Boot BOM could not see the Spring Cloud half of a migration, which usually
breaks first.

Before this channel, `API_REMOVED` had **6** facts, all documentation candidates. Now it has hundreds
of artifact-verified facts. This channel is also the reason that `API_REMOVED` can need evidence level
`E3`. Without a channel that inspects published bytes, no such fact can reach `E3`. Then the gate
blocks each one from authorizing a change.

Agent 08 skips starter and BOM-aggregator artifacts. A starter ships an empty jar, because its only
purpose is to bring a dependency set. A diff of a starter always finds nothing and uses a budget slot.

**Bootshift reads BOMs transitively.** A BOM snapshot follows `<scope>import</scope>` entries to a
limited depth. It carries `${project.version}` down, so that the modules of a sub-BOM resolve
correctly. This is necessary. `spring-boot-dependencies` manages most of its entries directly, so a
read of the first level works for it. `spring-cloud-dependencies` is the opposite: **all seventeen** of
its dependency blocks are imports.

| | Managed entries | Usable Spring Cloud artifacts |
|---|---:|---:|
| First level only | 17 | **0** |
| Imports followed | 358 | **131** |

A read of only the first level recorded seventeen *aggregators* as dependencies and never opened one.
Thus, the snapshot had no real Spring Cloud coordinate. The managed-version diff could not see a
Spring Cloud version change. The bytecode diff skipped each Spring Cloud artifact as "not managed by
both BOMs".

The diff is limited to 60 jar pairs in each run. Coordinates beyond the budget, and coordinates that
only one BOM manages, are listed in `api_diff.not_diffed` and raise `GAP-KNOW-002`. Thus, the limit
shows as reduced coverage, not as facts that silently do not exist.

**Generated property rules.** Bootshift **generates** the property rules from the deprecation metadata
of the target release. Nobody writes them by hand. The file
`migration-rules/generated-properties/property-migration-rules.json` is committed so that a reviewer
can compare versions of it:

```json
{
  "generated_by": "bootshift 08-knowledge",
  "source_version": "2.7.12",
  "target_version": "3.5.16",
  "provenance": "spring-configuration-metadata.json deprecation entries published in the target release artifacts",
  "hand_written": false,
  "rule_count": 542,
  "rules": [
    { "from": "logging.file", "to": "logging.file.name", "action": "RENAME",
      "deprecation_level": "error",
      "evidence_ref": "META-INF/spring-configuration-metadata.json in spring-boot 3.5.16" }
  ]
}
```

The generated rules give deterministic coverage of the 542 property facts. A person cannot keep that
number of rules correct by hand. The generated rules are the reason that `PROPERTY_RENAMED` and
`PROPERTY_REMOVED` show full coverage in the residual report.

**Validity intervals.** Each migration fact has `component`, `valid_from`, `valid_to` and a
`validity_precision`. [Agent 11](#95-agent-11--migration-planner) uses them to give each edge only its
own facts.

**Optional AI role.** When AI is on, it can summarize, extract candidates, cluster and write draft
explanations. Bootshift records its output as evidence with the model identity, the prompt hash, the
context hash and the response hash. The output stays a hypothesis until deterministic verification
succeeds. The reference runs used **zero** AI.

### 9.3 Agent 09 — Impact Analyzer

**Purpose.** Answer this question: which parts of **this** repository do **these** verified facts
affect?

```mermaid
flowchart TD
    F["VERIFIED migration facts only"] --> LOC{"Fact type"}
    LOC -->|API| L1["Graph nodes referencing the type<br/>+ textual scan as a capped fallback"]
    LOC -->|PROPERTY| L2["CONFIG_PROPERTY nodes,<br/>exact then prefix"]
    LOC -->|ARTIFACT| L3["LIBRARY nodes and the module<br/>descriptors that resolve them"]
    LOC -->|BASELINE| L4["Build descriptors"]
    LOC -->|BEHAVIOR| L5["Named resource, else type reference"]

    L1 --> ATTR{"Was the relationship<br/>type-resolved?"}
    L2 --> CLASS
    L3 --> CLASS
    L4 --> CLASS
    L5 --> ATTR
    ATTR -->|no| CAP["CAP at POSSIBLY_AFFECTED<br/><i>a textual match is not a fact</i>"]
    ATTR -->|yes| CLASS["Classify"]
    CAP --> CLASS

    CLASS --> BR["Blast radius traversal<br/>with the explaining path"]
    BR --> DIM["Derive required validation dimensions<br/>from fact type and node kind"]
    DIM --> TESTS["Find covering tests"]
    TESTS --> BS["Attach inherited blind spots"]
    BS --> ACC["Measure precision and recall<br/>on held-out fixtures"]
    ACC --> FLOOR{"Recall below<br/>the policy floor?"}
    FLOOR -->|yes| ESC["Gap GAP-IMPACT-001;<br/>validation breadth escalates"]

    style CAP fill:#fff3cd,stroke:#856404
    style ESC fill:#fff3cd,stroke:#856404
```

| Classification | Meaning |
|---|---|
| `DEFINITELY_AFFECTED` | A type-resolved reference, or an exact configuration key match |
| `LIKELY_AFFECTED` | Strong structural evidence, a prefix match |
| `POSSIBLY_AFFECTED` | Found by text, or through an unresolved relation |
| `UNAFFECTED_WITHIN_OBSERVED_COVERAGE` | Nothing in the observed graph matched |

**Bootshift never gives a general `UNAFFECTED` result.** The strongest negative statement names its own
limit, and the artifact gives this rationale:

> "No node, symbol or configuration key in the observed graph matched this fact. This is not a claim
> of universal safety: reflection, dynamic configuration and unresolved types are outside observed
> coverage."

The attribution cap is mechanical:
`match.typeResolved ? classification : classification.capAt(POSSIBLY_AFFECTED)`.

**Reference corpus impact: 2072 findings across 27 files.** 502 `DEFINITELY_AFFECTED`,
5 `POSSIBLY_AFFECTED`, 1565 `UNAFFECTED_WITHIN_OBSERVED_COVERAGE`.

The name of the third class is intentional. `UNAFFECTED_WITHIN_OBSERVED_COVERAGE` is a claim about
what the analysis could see, not about the file. The name `UNAFFECTED` asserts something that the
harness cannot prove.

Each affected finding has a graph path:

```json
{
  "impact_id": "IMPACT-00042",
  "knowledge_id": "MK-00218",
  "classification": "DEFINITELY_AFFECTED",
  "path": "employee-service/src/main/resources/application.properties",
  "rationale": "Configuration key spring.data.mongodb.uri is exactly the affected property",
  "confidence": 0.98,
  "attribution_capped": false,
  "blast_radius_size": 7,
  "graph_path": [
    { "node": "MODULE:employee-service", "distance": 1,
      "explanation": "FILE:FILE-01M2… -[CONFIGURES]-> PROPERTY:spring.data.mongodb.uri" }
  ],
  "required_validation_dimensions": ["CONFIGURATION_BINDING"]
}
```

**Measured accuracy.** The `evaluate-impact` command measures the analyzer against held-out fixtures in
`fixtures/impact-evaluation/`. Fixtures marked `tuning` are not in the reported numbers. Thus, the
harness never reports an evaluation on data that it was tuned on. **If no held-out fixture exists,
the result is `UNMEASURED`, not perfect**, and `GAP-IMPACT-002` states this. A recall below
`impact_recall_floor` (default 0.80) increases the validation breadth for each edge.
[Section 22](#22-validation-results) gives the recorded accuracy.

### 9.4 Agent 10 — Characterization

**Purpose.** Protect migration-sensitive behaviour **before** it changes.

**Scenarios are executable, and Agent 10 executes them.** Agent 10 writes
`characterization-scenarios.json`. Each scenario is a concrete request. It has a method, a path with its
variables replaced, the headers that make the observation useful, and the exact facts to capture.
Agent 10 then runs the scenarios against the **original** application, before any migration, and
writes `characterization-old-observations.json`.

This execution makes an observation an oracle. Before, the stage wrote only a *description* of a
probe, and nothing ran it. Each contract stayed in `AWAITING_OLD_OBSERVATION` for the full run, while
the report counted it as protection. Differential validation had no scenario to run, so it compared
only Actuator metadata between the two sides.

```mermaid
flowchart TD
    I["Impact finding<br/><i>not UNAFFECTED</i>"] --> P{"Does adequate protection<br/>already exist?"}
    P -->|"a passing baseline test covers it"| MAP["MAPPED_TO_EXISTING_TEST"]
    P -->|no| OBS{"Can the behaviour be<br/>observed in this environment?"}
    OBS -->|no| UNOBS["UNOBSERVABLE<br/>+ characterization gap"]
    OBS -->|yes| GEN["Scaffold a harness-owned probe"]
    GEN --> WAIT["AWAITING_OLD_OBSERVATION<br/><i>cannot act as an oracle yet</i>"]
    WAIT --> RUN["Execute against the ORIGINAL application"]
    RUN --> OK{"Valid against OLD?"}
    OK -->|no| REJ["REJECTED"]
    OK -->|yes| FREEZE["FROZEN — observed behaviour<br/>becomes the oracle"]
    MAP --> FREEZE

    style WAIT fill:#fff3cd,stroke:#856404
    style FREEZE fill:#d4edda,stroke:#155724
    style UNOBS fill:#f8d7da,stroke:#721c24
```

**The rule**

> Expected behaviour comes from **observed original behaviour**, an **authoritative specification**,
> or an **explicit human decision**. It never comes from invention.

`AWAITING_OLD_OBSERVATION` is **not** a protected state. A scenario counts only when it is `FROZEN`,
`MAPPED_TO_EXISTING_VERIFIED_TEST`, `UNOBSERVABLE_WITH_EXPLICIT_GAP` or `HUMAN_EXCEPTION_REQUIRED`.
Bootshift never takes expected values from migrated code. If AI is on, it can scaffold a probe. It
can never supply the expected value.

**A scenario that ran and failed becomes a declared gap, not a pending scenario.**
`AWAITING_OLD_OBSERVATION` means "not run yet", a state that a later step can still resolve. If the
module was running and the request gave an unusable result, nothing in the run can resolve it. When
it stayed pending, it protected nothing, and it was also not in the declared-gap count. It now becomes
`UNOBSERVABLE_WITH_EXPLICIT_GAP`, with the execution failure as its reason. Only a scenario that
Bootshift never attempted stays pending.

**Scenario families.** From the endpoint graph, for each endpoint:

- the contract of the endpoint (`HTTP_API`)
- the same endpoint with no credential and with an incorrect credential (`SECURITY_AUTHORIZATION`)
- the field paths of the response (`SERIALIZATION`)

For each module that starts:

- `CONFIGURATION_BINDING` over `/actuator/configprops` and `/actuator/env`
- `SPRING_CONTEXT` over `/actuator/beans`, `/actuator/conditions`, `/actuator/mappings` and `/actuator/health`

If the environment provider cannot supply a datastore or a broker, the dimensions that need it are
written as `UNOBSERVABLE_WITH_EXPLICIT_GAP`. Thus, the coverage statement describes the full migration,
not a smaller one.

**Contracts do not depend on a test framework.** A contract describes a **scenario and its observed
result**, not a JUnit test:

```json
{
  "scenario_id": "SCN-00031",
  "dimension": "HTTP_API",
  "node_id": "ENDPOINT:GET /api/v1/employee/search",
  "state": "AWAITING_OLD_OBSERVATION",
  "oracle_source": "OBSERVED_ORIGINAL_BEHAVIOUR_PENDING",
  "probe": {
    "kind": "HTTP_API", "harness_owned": true, "transport": "HTTP",
    "method": "GET", "path": "/api/v1/employee/search",
    "capture": ["status_code", "content_type", "body_shape", "error_payload_shape"]
  }
}
```

Because the oracle was never a JUnit test, the test-infrastructure edge can migrate JUnit 4 to Jupiter
and keep the behavioural oracle.

**Reference results.** The earlier reference run in this README recorded **517 contracts**, all
`AWAITING_OLD_OBSERVATION`. They covered each impacted dimension, plus an HTTP contract for each of the
10 observed endpoints. That run came before scenario execution. Recorded run 2 in `reports/` built
82 scenarios, froze 36 against the original application and declared 46 as
`UNOBSERVABLE_WITH_EXPLICIT_GAP`. Zero stayed pending.

### 9.5 Agent 11 — Migration Planner

**Purpose.** The Target Resolver tells where to go. The Planner tells exactly **how**, and it freezes
the answer.

**Everything in an edge plan is for that edge.** For each edge, Agent 11 calculates these items. It
never copies them from the migration as a whole:

- facts and impact findings
- affected `FILE_ID`s and symbols
- required validation dimensions
- risk, residual and deterministic coverage
- the Spring Cloud train and the Java target
- the characterization scenarios and the approval obligations

An earlier version copied the fact set, the impact set and the file set of the full run into each
edge. Thus, a patch edge claimed authority over each impacted file in the repository, and its coverage
figure described a different edge.

**Validity intervals put facts on edges.** A fact reaches an edge only when its validity interval
intersects the `(from, to]` span of that edge. Bootshift publishes the precision:

| `validity_precision` | Meaning |
|---|---|
| `EDGE_EXACT` | The evidence names the exact version pair |
| `ARTIFACT_VERSION_WINDOW` | The change is inside the edges on which the managed version of the owning artifact moved |
| `SPAN_ONLY` | The evidence covers the full migration. Bootshift cannot put the fact on one edge |

A span-only fact that is shown as edge-exact is an overstated claim.

**Capability matching is for each recipe.** A plan entry with a `capability_id` names the capability
that actually implements that recipe. Bootshift asks the owning provider. The planner once attached
"the first `AVAILABLE` capability whose declared fact types overlap anything in the run". That paired
a Maven POM recipe with a JUnit capability. A coverage number from such a pairing has no meaning. If
no capability claims a recipe, Bootshift records `NO_CAPABILITY_CLAIMS_THIS_RECIPE`. It does not
supply a substitute.

```mermaid
flowchart TD
    CAP["Probe every transformation provider<br/>for what it can actually do right now"] --> REG["TransformationCapabilityRegistry<br/><i>with license evidence per capability</i>"]
    REG --> COV["Deterministic coverage =<br/>facts handled / verified facts"]
    COV --> RES["Residual: fact types with<br/>no AVAILABLE capability"]
    RES --> EDGE["For each edge in the frozen path"]

    EDGE --> TRAIN["Resolve the Spring Cloud train<br/>for THIS edge target line"]
    TRAIN --> JAVA["Resolve the Java level for THIS edge:<br/>highest installed JDK the line supports"]
    JAVA --> RECIPES["Select ordered transformations;<br/>omit managed-version when no train exists"]
    RECIPES --> SCOPE["Scope: impacted files + build descriptors,<br/>plus every test source on a PREPARATORY edge"]
    SCOPE --> RECON{"Does a composite capability<br/>span multiple checkpoints?"}
    RECON -->|no| DEC["DECOMPOSED"]
    RECON -->|"yes, policy allows"| COL["COLLAPSE_WITH_ESCALATED_VALIDATION"]
    RECON -->|"yes, policy forbids"| BLK["BLOCK"]
    DEC --> DEPTH
    COL --> DEPTH["depth = MAX(class, residual, impact, policy)"]
    DEPTH --> FREEZE["FREEZE the depth into the edge plan"]
    BLK --> STOP["exit 3"]

    style FREEZE fill:#d4edda,stroke:#155724
    style BLK fill:#f8d7da,stroke:#721c24
```

**Resolution is for each edge, not for each run.** End-to-end runs found two defects. Both came from
the use of a landing-target value on a transit checkpoint:

1. **The Spring Cloud train.** The first version installed the *landing* train (2025.0.3) on the
   2.7.18 patch edge. Spring Cloud removed `@EnableEurekaClient` in 2022.0, so the application did not
   compile: `cannot find symbol: class EnableEurekaClient`. The harness alone caused this failure. Each
   edge now resolves the train for **its own** Boot line. If no train targets that line, the plan omits
   the managed-version transformation. It does not guess.
2. **The Java level.** The landing Java level (21) on the 3.0 edge sets a compiler target that Spring
   Boot 3.0 does not support. Each edge now takes the highest installed JDK that **its** Boot line
   accepts and that is not below the project level.

**Why the list of one transformer is curated.** `java.remove-annotation` exists because the reference
corpus showed the gap. Spring Cloud 2022.0 deleted `@EnableEurekaClient`, and no deterministic
transformer could remove it. The major edge stopped with sixteen `cannot find symbol` errors. A person
fixes them with the deletion of one line in each module.

On the reference corpus, the transformer now removes the annotation and its import from four classes.
It does this on the **patch** edge, not the major edge. When the fact existed, the impact analysis put
those classes in the scope of that edge. Nobody scheduled this by hand.

It is easy to drive that transformer from each `API_REMOVED` fact of the bytecode diff. It is also
wrong. **"This type no longer exists" does not mean "it is safe to delete the reference."** For most
removed types, the reference is necessary. Its deletion changes behaviour silently, and that is the
worst result that this harness prevents.

A removal is safe only for an annotation whose *full* effect was to opt into behaviour that the target
version now does always. That is a claim about semantics, and no diff can prove it. Thus, each entry
on the list carries the evidence that its removal changes nothing. An annotation that is not on the
list stays residual: Bootshift reports it and does not guess. The list has three entries:
`org.springframework.cloud.netflix.eureka.EnableEurekaClient`,
`org.springframework.cloud.client.discovery.EnableDiscoveryClient` and
`org.springframework.cloud.netflix.hystrix.EnableHystrix`. `TransformerTest` asserts that each entry
gives its justification, and that the capability does not claim `API_REMOVED` facts outside its list.

**Capability discovery (R31).** Bootshift assumes no capability for an edge. The earlier reference run
in this README recorded this registry:

| Capability | Provider | License | Status on that run |
|---|---|---|---|
| `CAP-MAVEN-PARENT-VERSION` | `BOOTSHIFT_MAVEN_POM` | MIT (harness code) | AVAILABLE |
| `CAP-MAVEN-DEPENDENCY` | `BOOTSHIFT_MAVEN_POM` | MIT (harness code) | AVAILABLE |
| `CAP-JAKARTA-NAMESPACE` | `BOOTSHIFT_JAKARTA` | MIT (harness code) | AVAILABLE at the 2.x→3.x boundary |
| `CAP-JUNIT4-JUPITER` | `BOOTSHIFT_TEST_FRAMEWORK` | MIT (harness code) | AVAILABLE |
| `CAP-MOCKBEAN-MOCKITOBEAN` | `BOOTSHIFT_TEST_FRAMEWORK` | MIT (harness code) | AVAILABLE |
| `CAP-CONFIG-PROPERTY` | `BOOTSHIFT_CONFIG_PROPERTY` | MIT (harness code) | AVAILABLE, 542 generated rules |
| `CAP-REMOVE-NOOP-ANNOTATION` | `BOOTSHIFT_REMOVED_ANNOTATION` | MIT (harness code) | AVAILABLE, 3 annotations |
| `CAP-OPENREWRITE-CORE` | `OPENREWRITE_CORE` | Apache-2.0 | **UNAVAILABLE** at that time. Not on the classpath, counted as residual |

The OpenRewrite row shows the purpose of R31. The registry states the truth, and the missing coverage
goes into the residual calculation. It does not become a silent hole. The latest commit adds the
OpenRewrite core modules as dependencies of `adapters` ([Section 16](#16-oss-tooling-and-the-ai-boundary)).

**Validation depth is calculated one time and frozen (R16).**

```text
depth(edge) = MAX(
    class_depth,              PATCH -> tests, MINOR -> runtime,
                              PREPARATORY -> impacted differential,
                              MAJOR_BOUNDARY -> full differential
    residual_depth,           escalated by deterministic coverage
    impact_required_depth,    escalated when any impact is HIGH risk
    policy_required_depth     escalated when impact recall is below floor,
                              or a checkpoint was collapsed
)
```

Execution **reads** this value. It never decides for itself how much validation an edge needs.
[Section 14](#14-migration-edges-and-validation-depth) gives the full depth rules.

**Reference corpus plan: 8 edges frozen, deterministic coverage 0.3886.** 654 of 1683 verified facts
have a transformer that claims their subject. Read the breakdown, not the single number:

| Fact type | Facts | Covered | Not covered |
|---|---:|---:|---:|
| `API_REMOVED` | 1029 | 1 | 1028 |
| `PROPERTY_RENAMED` | 389 | 389 | 0 |
| `PROPERTY_REMOVED` | 153 | 153 | 0 |
| `MANAGED_VERSION_CHANGED` | 105 | 105 | 0 |
| `ARTIFACT_REMOVED` | 3 | 3 | 0 |
| `API_RENAMED` | 1 | 1 | 0 |
| `BASELINE_REQUIREMENT` | 1 | 1 | 0 |
| `BEHAVIOR_CHANGED_NO_API_CHANGE` | 1 | 0 | 1 |
| `COMPATIBILITY_REQUIREMENT` | 1 | 1 | 0 |

The single covered `API_REMOVED` fact is `@EnableEurekaClient`, the one annotation on the justified
list of the transformer that the run needed. The other **1028** are the real residual. The
published-bytecode diff proved that these types are gone, and no transformer claims a safe rewrite for
them. The residual report names them with example subjects, so that an operator can see what a person
must still do. `spring.factories` stays uncovered, because no safe mechanical equivalent exists.

An earlier version of this harness reported **0.9983** for the same corpus. The transformers did not
change. That number came from a fact set that never inspected a jar. It also came from a capability
model in which the claim of a JUnit rewriter on the `API_REMOVED` *type* absorbed each removed API. The
migration did not become more difficult. The number became correct.

Recorded run 2 in `reports/` gives the per-edge plan:

| Edge | Class | From → to | Java | Cloud | Recipes | Facts | Coverage |
|---|---|---|---|---|---|---|---|
| EDGE-1-PREP-TEST | PREPARATORY | 2.7.12 → 2.7.12 | 17 | 2021.0.9 | 1 | 692 | 0.9697 |
| EDGE-2-PATCH | PATCH | 2.7.12 → 2.7.18 | 17 | 2021.0.9 | 4 | 1388 | 0.8220 |
| EDGE-3-MAJOR-3 | MAJOR_BOUNDARY | 2.7.18 → 3.0.13 | 17 | 2022.0.5 | **33** | 1682 | 0.6795 |
| EDGE-4-MINOR | MINOR | 3.0.13 → 3.1.12 | **21** | 2022.0.5 | 4 | 1551 | 0.7357 |
| EDGE-5-MINOR | MINOR | 3.1.12 → 3.2.12 | 21 | 2023.0.5 | 4 | 1420 | 0.8035 |
| EDGE-6-MINOR | MINOR | 3.2.12 → 3.3.13 | 21 | 2023.0.6 | 4 | 1459 | 0.7820 |
| EDGE-7-MINOR | MINOR | 3.3.13 → 3.4.13 | 21 | 2024.0.3 | 4 | 1578 | 0.7231 |
| EDGE-8-LANDING | LANDING | 3.4.13 → 3.5.16 | 21 | 2025.0.3 | 4 | 1576 | 0.7240 |

Java moves from 17 to 21 exactly at EDGE-4, the first edge whose Boot line supports 21. The boundary
edge has 33 recipes and the lowest coverage. Facts reach an edge through two channels. `fact_scoping` records
them for each edge: declared attribution from the BOM diff of each edge (0–990 for each edge),
and validity-interval intersection for facts without attribution (692–698 for each edge).

---

## 10. Phase 5 — Migration edge loop

Agents 12 to 17 run once for each edge of the frozen path. Each edge has checkpoints at its start and
at its end. If a gate inside an edge fails, the working tree goes back to the start checkpoint of the
edge ([Section 15.5](#155-refusals-rollback-and-publication)).

### 10.1 Agent 12 — Transformation

**Purpose.** Apply only the deterministic transformations that the frozen plan authorized for this
edge.

**What it does not do.** **Agent 12 never writes a file.** It calculates proposals and gives them to
the gateway. An ArchUnit rule fails the build if code in `stages.stage12`, `stages.stage13` or
`adapters.transform` calls the filesystem write API.

```mermaid
flowchart TD
    SEAL{"Baseline sealed?"} -->|no| REFUSE["POLICY BLOCK (R7)"]
    SEAL -->|yes| CP["Checkpoint: edge start"]
    CP --> LOAD["Load the frozen edge plan"]
    LOAD --> REG["Register the transformers<br/>that handle its recipes"]
    REG --> EACH["For each ordered transformation"]
    EACH --> NARROW["Narrow target paths by role:<br/>descriptors, Java sources, tests, config"]
    NARROW --> PROPOSE["Provider computes ProposedChange objects"]
    PROPOSE --> COLLECT["Collect proposals across recipes"]
    COLLECT --> GATE["FileMutationGateway.apply<br/><i>the only writer</i>"]
    GATE --> PERSIST["Persist the live registry"]
    PERSIST --> REPORT["Report applied, rejected, failed,<br/>residual recipes, ledger head"]

    style REFUSE fill:#f8d7da,stroke:#721c24
    style GATE fill:#f8d7da,stroke:#721c24
```

Agent 12 applies **one recipe in each batch** ([ADR-007](docs/adr/ADR-007-sequential-recipe-application.md)).
Thus, each transformer sees the output of the previous recipe, and the git history has one commit for
each recipe. Each commit names the recipe that made it.

| Recipe | What it does | Limit |
|---|---|---|
| `maven.parent-version` | Sets the Boot parent version | Verifies the artifact id first. Exact text edit |
| `maven.property` | Sets a property, and adds it if it is absent | Only the named property |
| `maven.managed-version` | Sets a managed BOM version | Only inside `dependencyManagement` |
| `maven.dependency-coordinate` | Relocates a coordinate | Only an exact group and artifact match |
| `java.remove-annotation` | Deletes annotations that became no-ops and were then removed | **3 curated entries only**, each with the evidence that the removal changes nothing |
| `jakarta.namespace` | Rewrites relocated Jakarta EE packages | **28 relocated prefixes only**. It never changes the 26 preserved packages |
| `test.junit4-to-jupiter` | Mechanical JUnit 4 constructs | Rules, runners and `ExpectedException` stay residual |
| `test.mockbean-to-mockitobean` | Bean-override annotations for Boot 3.4 | Import and annotation only |
| `config.property-migration` | Applies generated property rules | Keeps comments. Comments out a removal, with the reason |

The skills catalog also lists `maven.add-dependency` and `maven.remove-dependency` for
`MavenPomTransformer`. `YamlPropertyModel` parses YAML structurally with SnakeYAML line marks, in
place of line-based text edits.

**The Jakarta limit shows the full design in one rule.** A general `javax` to `jakarta` rewrite can quickly
make a working application fail to compile. The cause then has no relation to the migration. `javax.sql`, `javax.net`, `javax.crypto`, `javax.naming`, `javax.management`,
`javax.xml.parsers` and twenty other packages **did not move**.

```java
// rewritten — these relocated in Jakarta EE 9
import javax.persistence.Entity;   ->  jakarta.persistence.Entity
import javax.servlet.Filter;       ->  jakarta.servlet.Filter
import javax.xml.bind.JAXBContext; ->  jakarta.xml.bind.JAXBContext

// untouched — these are still JDK or unrelated specs
import javax.sql.DataSource;
import javax.crypto.Cipher;
import javax.xml.parsers.SAXParser;
```

`TransformerTest` asserts both halves. It checks that `javax.xml.bind` moves while `javax.xml.parsers`
stays, and that the rewrite gives the same result when it runs again. It also pins the sizes of the
two lists: `JakartaNamespaceTransformer.RELOCATED` has 28 entries and `PRESERVED` has 26.

**Removals are commented out, not deleted.**

```properties
# [bootshift] removed property server.max-http-header-size: no replacement in the target version
#server.max-http-header-size=16KB
```

A reviewer can see what was there. A deleted line is not visible in a review.

**Forbidden by construction:** source-available Spring recipe packs, proprietary engines, direct
source writes, general reformatting of unrelated files and unverified dependency substitution.

| Situation | Behaviour | Exit |
|---|---|---|
| The baseline is not sealed | Policy block | 3 |
| A change is outside the authorized scope | The gateway rejects it and the ledger records it | 0 |
| No provider exists for a recipe | Recorded as residual | 0 |
| A path traversal attempt | Structured refusal | 2 |

### 10.2 The FileMutationGateway

`FileMutationGateway` is the central control point of the architecture. **Each change to application
source goes through it.**

```mermaid
flowchart TD
    IN["ProposedChange from Agent 12 or 13"] --> S1["1 Verify the baseline seal"]
    S1 -->|not sealed| BLOCK["POLICY BLOCK (R7)"]
    S1 -->|sealed| S2["2 Resolve FILE_ID from the path"]
    S2 --> S3["3 Verify authorization:<br/>file in scope, operation permitted,<br/>line budget respected"]
    S3 -->|denied| REJ["ChangeEvent status REJECTED<br/><i>appended to the ledger anyway</i>"]
    S3 -->|allowed| S4["4 Capture before path, hash and content"]
    S4 --> S4B{"4b Does the proposal's base_hash<br/>still match the file on disk?"}
    S4B -->|no| STALE["REJECTED — STALE_BASE_CONTENT<br/><i>it discards an<br/>already-applied change</i>"]
    S4B -->|yes| S5["5 Resolve inside the workspace<br/><i>traversal and symlink escape refused</i>"]
    S5 --> S6["6 Apply by operation:<br/>MODIFY, CREATE, DELETE, RENAME, MERGE"]
    S6 --> S7["7 Handle identity:<br/>append version, record split,<br/>merge or delete"]
    S7 --> S8["8 Capture after path and hash"]
    S8 --> S9["9 Identify changed SYMBOL_IDs"]
    S9 --> S10["10 Write the patch artifact"]
    S10 --> S11["11 Build the ChangeEvent"]
    S11 --> S12["12 Append to the hash chain"]
    S12 --> S13["13 Checkpoint the repository state"]
    S13 -->|checkpoint failed| CPF["Record a FAILED_VALIDATION event:<br/>rollback is unavailable for this batch"]
    S13 -->|ok| DONE["BatchOutcome"]
    CPF --> DONE

    style BLOCK fill:#f8d7da,stroke:#721c24
    style REJ fill:#fff3cd,stroke:#856404
    style STALE fill:#f8d7da,stroke:#721c24
    style S12 fill:#d4edda,stroke:#155724
```

The gateway also rejects a change that is over the budget. It checks path authorization for `RENAME`
destinations and validates `MERGE` sources. It writes unified diffs with an LCS algorithm, and it
rolls back to a scoped checkpoint.

**Step 4b: why the gateway refuses a stale proposal.** A transformer calculates its new content from
the file at the time when the transformer ran. Two recipes often target the same `pom.xml`, for
example a parent-version change and a managed-version change. If both proposals come from the tree
before the edge and go to the gateway together, the second write silently removes the first. **The
ledger still records both as `APPLIED`.**

This happened. On the reference corpus, an edge reported `12 applied`, and the commit had six file
changes. A managed-version change to the same file overwrote each parent-version change. Thus, the
project stayed on Spring Boot 2.7.12 while the ledger stated otherwise. The next edge then changed
`javax.servlet` to `jakarta.servlet` on a project that was still on Boot 2.7. The compiler
reported that `jakarta.servlet.http` does not exist.

The ledger cannot survive a lost change that it reports as applied. Thus, there are now two controls:

1. Agent 12 applies **one recipe in each batch**, so each transformer sees the output of the previous recipe.
2. Each proposal carries the hash of its source content. The gateway rejects it as `STALE_BASE_CONTENT` if the file changed after that.

`MutationBoundaryTest` covers both directions.

**Why a checkpoint failure does not throw.** If the checkpoint fails, the changes are already applied
and already in the ledger. An exception leaves a changed workspace with no record of the reason.
Thus, the failure becomes its own ledger event. The event states that deterministic rollback is not
available for that batch. The operator must see this fact. `MutationBoundaryTest.checkpointFailureIsRecorded`
asserts this.

**Two independent controls against a bypass.**

- **Static.** An ArchUnit rule forbids mutation-capable packages to call `Files.write*`,
  `Files.delete*`, `Files.move` or `Files.copy`.
- **Dynamic.** `detectBypass()` compares the content on disk with the hashes that the registry has as
  current. It reports three classes of violation:

```text
BYPASS:    <path> content hash <actual> does not match the gateway-recorded hash <expected>
UNTRACKED: <path> exists in the migration workspace but has no registered identity
MISSING:   <path> is registered as active but absent from the migration workspace
```

The dynamic check finds a write that skipped the gateway **even if it came from outside the JVM**.

Agent 12 calls `detectBypass()` after each edge. A `BYPASS` fails the stage. `UNTRACKED` and `MISSING`
are reported as gaps, because build output correctly appears in the workspace. A failure on it
teaches operators to ignore the check. `ControlsAreWiredTest` asserts that this call exists. For a
period, `detectBypass()` had three passing unit tests and no caller, so the dynamic control did not
run.

### 10.3 The change ledger

```mermaid
flowchart LR
    G["GENESIS<br/>0000…0000"] --> E1["EVENT 1<br/>CHANGE-000001"]
    E1 --> H1["hash₁ = SHA256(GENESIS ‖ canonical₁)"]
    H1 --> E2["EVENT 2<br/>CHANGE-000002"]
    E2 --> H2["hash₂ = SHA256(hash₁ ‖ canonical₂)"]
    H2 --> E3["EVENT 3<br/>CHANGE-000003"]
    E3 --> H3["hash₃ = SHA256(hash₂ ‖ canonical₃)"]
    H3 --> HEAD["HEAD<br/><i>sealed into the evidence manifest</i>"]

    style HEAD fill:#d4edda,stroke:#155724
```

Each link depends on the exact canonical bytes of each earlier event. Thus, Bootshift can find an
insertion, a deletion, a change of sequence and an edit in place. Verification calculates the chain
again from the persisted file. It never trusts the state in memory, so it can find offline tampering.

| Attack | Found by |
|---|---|
| Delete an event | A gap in the sequence and a broken chain link |
| Change the sequence of events | Previous-hash mismatch |
| Edit an event in place | The calculated event hash is different: "content was modified" |
| Insert a forged event | Chain break at the insertion point |
| Truncate the ledger **and** forge the head | The head declares more events than the ledger holds |

`ChangeLedgerTamperTest` has failure-injection tests for all five attacks.

Each ledger entry carries `change_id`, `edge_id`, `file_id`, `before_sha256`, `after_sha256`,
`provider`, `recipe_id`, `operation`, `patch_ref`, `knowledge_refs`, `impact_refs` and
`symbols_changed`.

**Each attempt is history (R14).**

```json
{ "change_id": "CHANGE-000004", "status": "APPLIED",  "file_id": "FILE-01M2…" }
{ "change_id": "CHANGE-000005", "status": "REJECTED", "rejection_reason": "Path … is outside the authorized scope of edge EDGE-1-PREP-TEST" }
{ "change_id": "CHANGE-000006", "status": "REVERTED", "rejection_reason": "rolled back to checkpoint" }
```

A rejected change is not a no-op. It is a recorded decision. `explain change` shows it with its full
provenance.

### 10.4 Agent 13 — Build and Repair

**Purpose.** Compile the migrated state and repair residual compile failures **within a budget**.

```mermaid
flowchart TD
    START["Select a toolchain for the edge target"] --> COMPILE["Compile every module"]
    COMPILE --> OK{"All modules<br/>compile?"}
    OK -->|yes| CP["Checkpoint: compiled"]
    OK -->|no| PARSE["Parse diagnostics"]
    PARSE --> CLUSTER["Cluster by root cause, in diagnosis order:<br/>1 dependency resolution<br/>2 plugin or toolchain<br/>3 missing type or package<br/>4 removed or renamed API<br/>5 generic or type mismatch<br/>6 namespace migration<br/>7 application specific"]
    CLUSTER --> PROG{"Did the error count fall?"}
    PROG -->|no| STOP["NO_PROGRESS -> NEEDS_HUMAN"]
    PROG -->|yes| ENV{"Environmental cause?"}
    ENV -->|yes| SKIP["NOT_REPAIRABLE_BY_SOURCE_EDIT<br/><i>no source edit can fix a broken toolchain</i>"]
    ENV -->|no| BUDGET{"Attempts left<br/>for this root cause?"}
    BUDGET -->|no| EXH["BUDGET_EXHAUSTED"]
    BUDGET -->|yes| R1["1 Verified deterministic rule"]
    R1 -->|none| R2["2 Knowledge-grounded template"]
    R2 -->|none| R3{"AI enabled and<br/>within budget?"}
    R3 -->|no| NONE["NO_REPAIR_AVAILABLE"]
    R3 -->|yes| AI["Bounded AI proposal"]
    AI --> VERIFY["Deterministic verification"]
    VERIFY -->|fails| REJECT["REJECTED_BEFORE_APPLY<br/><i>recorded with model provenance</i>"]
    VERIFY -->|passes| ACCEPT["ACCEPTED_FOR_GATEWAY"]
    R1 --> DUP
    R2 --> DUP
    ACCEPT --> DUP{"Same patch bytes<br/>proposed before?"}
    DUP -->|yes| LOOP["REPEATED_PATCH_DETECTED -> stop"]
    DUP -->|no| GATE["FileMutationGateway"]
    GATE --> COMPILE

    style STOP fill:#f8d7da,stroke:#721c24
    style REJECT fill:#fff3cd,stroke:#856404
    style GATE fill:#f8d7da,stroke:#721c24
```

**The root-cause sequence is the main point.** One unresolved dependency gives dozens of "cannot find
symbol" errors. A repair of each error alone uses all of the budget on symptoms. Bootshift assigns each
diagnostic to the **first** cause that explains it. Thus, a set of missing-symbol errors that an
unresolved dependency causes goes to the dependency.

Environmental causes (dependency resolution and plugin or toolchain failures) get the mark
`NOT_REPAIRABLE_BY_SOURCE_EDIT`. They never use repair budget. No source edit can repair a JDK
mismatch. `EdgeToolchain` freezes the JDK for each edge. It then **verifies** it with `java -version`.
A mismatch is a validation failure, because an observation on a toolchain that nobody selected
describes a system that nobody chose.

**AI verification gates (R11).** Each check runs **before** the proposal reaches the gateway. Each
check needs no answer from the model:

| Gate | Rejects |
|---|---|
| Not empty and different | A no-op or an empty response |
| Line budget | A patch larger than `ai_max_changed_lines_per_patch` |
| Half-file heuristic | A response that removed most of the file |
| Test weakening | A new `@Disabled`, `@Ignore` or `assumeTrue(false)` |
| Assertion count | A net loss of `assert` |
| Security and transactions | A net loss of `@PreAuthorize`, `@Secured`, `@RolesAllowed` or `@Transactional` |
| Forbidden dependency | A reference to a source-available recipe estate |
| Shape check | A Maven descriptor that no longer looks like one |

Bootshift records each rejection with the model identity, the prompt hash, the context hash and the
response hash. Thus, `explain change` can show what the model proposed and why Bootshift refused it.

| Budget | Default (`production`) |
|---|---|
| Attempts for each root cause | 4 |
| Total repair rounds | 10 |
| Total AI attempts | 12 |
| AI attempts for each root cause | 3 |
| AI files in each patch | 3 |
| AI changed lines in each patch | 80 |

No progress gives `NEEDS_HUMAN` (exit 4). It does not give another attempt.

**What a correct diagnosis looks like.** The major edge on the reference corpus once reported this:

```text
PLUGIN_OR_TOOLCHAIN     |  4 diagnostics | a build plugin or the toolchain itself failed;
                        |                | no source edit can repair it
MISSING_TYPE_OR_PACKAGE |  2 diagnostics | jakarta.servlet.http is not on the compile classpath
REMOVED_OR_RENAMED_API  | 18 diagnostics | Symbol unknown does not exist at the target version
APPLICATION_SPECIFIC    | 26 diagnostics | no framework-level cause explains this
```

Four clusters and fifty diagnostics, and almost all of it was wrong:

- The first cluster was the Maven summary line, not a toolchain fault. Its claim that no source edit
  can repair it stopped the repair loop after one round.
- `Symbol unknown` came from javac continuation lines that the parser read as separate diagnostics.
  The symbol name never reached the cluster, and each removed API went into one bucket.
- Twenty-six of the "application specific" entries were banners and help text.

The same edge now reports:

```text
REMOVED_OR_RENAMED_API  | 16 diagnostics | removed-api:EnableEurekaClient
                        |                | Symbol EnableEurekaClient does not exist at the
                        |                | target version
```

One cluster, with the correct name, points at exactly the annotation that Spring Cloud 2022.0
deleted. The residual became half the size, because the Maven framing was never a diagnostic, and the
repair budget uses that number. `CompilerDiagnosticsTest` protects this behaviour.

### 10.5 Agent 14 — Graph Rebuild and Graph Diff

**Purpose.** Build the static graph again and assert that each change stayed inside the authorized
scope, **before** expensive validation runs.

**Why its position in the pipeline is important.** If this stage runs after the tests, an out-of-scope
structural change shows one hour later. It runs immediately after the compile, so a scope
violation blocks in seconds.

```mermaid
flowchart TD
    COMP{"Did the edge compile?"} -->|no| PART["GRAPH_STATUS = PARTIAL<br/><i>full type attribution is impossible</i>"]
    COMP -->|yes| FULL["Full rebuild from the migration workspace"]
    PART --> FULL
    FULL --> DIFF["Diff against LAST_GOOD_GRAPH"]
    DIFF --> CHANGED["Changed file ids from the diff"]
    CHANGED --> AUTH{"Authorized by the plan,<br/>or changed through the ledger?"}
    AUTH -->|yes| OK["expected"]
    AUTH -->|no| CONSEQ{"Does this file depend on<br/>a file the ledger changed?"}
    CONSEQ -->|yes| EXP["Expected consequence<br/><i>a referenced type changed</i>"]
    CONSEQ -->|no| VIOL["SCOPE VIOLATION"]
    VIOL --> BLOCK["exit 3"]
    OK --> ADV
    EXP --> ADV["Advance LAST_GOOD_GRAPH"]
    ADV --> CP["Checkpoint: graph-verified"]

    style VIOL fill:#f8d7da,stroke:#721c24
    style PART fill:#fff3cd,stroke:#856404
```

**Always a full rebuild (ADR-005).** An incremental graph change is faster, and it is a usual source of
silent drift. A missed invalidation gives a graph that is slightly wrong. The diff then looks clean,
and the scope gate passes a change that it must block. Version 1 always builds the full graph. An
incremental mode can come later, behind a graph-equivalence test that proves
`INCREMENTAL_GRAPH == FULL_REBUILD_GRAPH` by structural hash.

**Expected consequence or scope violation.** Not each graph change in an unchanged file is a violation.
If `EmployeeService` refers to `Employee`, and `Employee` changed correctly, then facts about
`EmployeeService` change too. The stage asks if the file depends on a file that the ledger actually
changed. It reports expected consequences separately from violations.

**Degraded mode.** If the edge does not compile, Bootshift cannot build complete type attribution
again. The graph gets the mark `PARTIAL`, and Bootshift raises blind spot `BS-GRAPH-PARTIAL`. **The
harness does not claim that a full graph exists.**

**Outputs.** `application-graph-current.json`, `graph-diff.json`, `scope-assertion.json`,
`last-good-graph.json`.

### 10.6 Agent 15 — Test Validation

**Purpose.** Run the test suite of the application. Classify each result against **two** baselines.

**Why two baselines.** With only the sealed original, you cannot tell a regression from this edge from
a regression from three edges before. With only the previous edge, you cannot tell a migration
regression from debt that existed before the migration. Both are necessary.

```mermaid
flowchart TD
    DEPTH{"Frozen depth<br/>requires tests?"} -->|no| SKIP["Skip, recorded with the reason"]
    DEPTH -->|yes| RUN["Run tests with coverage instrumentation"]
    RUN --> AGENT{"Did the agent attach?"}
    AGENT -->|no| RETRY["Re-run without it;<br/>coverage unavailable with the reason"]
    AGENT -->|yes| PARSE
    RETRY --> PARSE["Parse Surefire reports"]
    PARSE --> EACH["For each test case"]
    EACH --> FAIL{"Failing now?"}
    FAIL -->|no| PASSED["PASSED"]
    FAIL -->|yes| BASE{"Failing at the<br/>sealed baseline?"}
    BASE -->|yes| PRE["PRE_EXISTING_FAILURE"]
    BASE -->|"did not exist"| UNEX1["UNEXPLAINED"]
    BASE -->|no| APPR{"Signed approval<br/>for this expectation?"}
    APPR -->|yes| INT["INTENTIONALLY_CHANGED_CONTRACT"]
    APPR -->|no| FACT{"Explained by a<br/>VERIFIED migration fact?"}
    FACT -->|yes| EXPF["EXPECTED_FRAMEWORK_CHANGE"]
    FACT -->|no| PREV{"Failing at the<br/>previous edge?"}
    PREV -->|yes| CUM["CUMULATIVE_REGRESSION"]
    PREV -->|no| LOC["EDGE_LOCAL_REGRESSION"]

    CUM --> BLOCKG["BLOCKS"]
    LOC --> BLOCKG
    UNEX1 --> BLOCKG

    PASSED --> COV["Coverage comparison"]
    PRE --> COV
    COV --> DROP{"Unexplained drop above<br/>the policy threshold?"}
    DROP -->|yes| BLOCKG
    DROP -->|no| CP["Checkpoint: tested"]

    style BLOCKG fill:#f8d7da,stroke:#721c24
```

**Two classifications need evidence.**

- `EXPECTED_FRAMEWORK_CHANGE` needs a **VERIFIED** migration fact whose subject is in the failure detail.
- `INTENTIONALLY_CHANGED_CONTRACT` needs a **recorded approval** that names that test.

Without one of these, a new failure is a regression or `UNEXPLAINED`. Both block.

**The probable-cause field.** The classification is about evidence. `probable_cause` is what an
operator reads first:

```json
{
  "test": "…EmployeeRepositoryTest#employeeSaveMethodTest",
  "outcome": "ERROR",
  "baseline_outcome": "ERROR",
  "classification": "PRE_EXISTING_FAILURE",
  "probable_cause": "INFRASTRUCTURE_UNAVAILABLE: MongoDB"
}
```

This field does not make the classification weaker. It is the difference between "the migration
broke persistence" and "MongoDB is not running".

**Coverage gate (R29).** Bootshift compares coverage with the sealed original **and** with the previous
successful edge. The default policy blocks an unexplained drop of more than **5 percentage points**.
You can configure this value. If Bootshift cannot compare coverage, it reports the gate as **not
evaluated**, never as passed, and raises a gap.

**Forbidden autonomous actions.** The harness never does these actions to hide a regression:
add `@Disabled`, delete tests, make assertions weaker, hide exceptions, exclude failing modules,
lower coverage gates, change expected values or change the instrumentation scope. This stage contains
no code for these actions.

**Reference corpus result.** `EDGE-1-PREP-TEST: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2}`. The
two failures are the MongoDB-dependent repository tests. Bootshift correctly assigned them to absent
infrastructure, not to the migration.

### 10.7 Agent 16 — Runtime Validation

**Purpose.** Start the migrated application and observe it. Startup is **one observation**. It is not
migration success (R19).

```mermaid
flowchart TD
    DEPTH{"Frozen depth<br/>requires runtime?"} -->|no| SKIP["Skip with the reason"]
    DEPTH -->|yes| ENV["Provision the environment;<br/>record mode and fingerprint"]
    ENV --> PKG["Package each module"]
    PKG --> SET["Derive isolation settings from<br/>what the module IS<br/><i>a discovery server keeps its client beans</i>"]
    SET --> START["Start the process"]
    START --> READY{"Reached readiness<br/>within the timeout?"}
    READY -->|no| FAIL["started=false<br/>+ blind spot with the failure reason<br/>+ every dimension unobservable"]
    READY -->|yes| PROBE["Probe: health, beans, conditions,<br/>env, mappings, configprops"]
    PROBE --> BIND["Capture bound-property provenance:<br/>canonical key, source, target field,<br/>bound, defaulted, deprecated, sensitive"]
    BIND --> SIL["Cross the static configuration graph<br/>with the runtime bound set"]
    SIL --> IGN{"Configured and bound at baseline,<br/>but not bound now?"}
    IGN -->|yes| PSI["PROPERTY_SILENTLY_IGNORED"]
    IGN -->|no| ENRICH
    PSI --> ENRICH["Enrich the runtime graph;<br/>every edge references its observation"]

    style FAIL fill:#f8d7da,stroke:#721c24
    style PSI fill:#fff3cd,stroke:#856404
```

**Binding provenance is the main point.** If Bootshift recorded only the values, it could not see the
important failure mode. For each property, it records:

- the canonical key
- the source file and the source type
- the target type and field
- whether it was **bound**, and whether it was **defaulted**
- whether it is deprecated, and its replacement
- whether it is sensitive

This record makes `PROPERTY_SILENTLY_IGNORED` visible. The static layer has a `CONFIGURES` edge. The
runtime layer has no `ACTUALLY_BINDS_PROPERTY` edge. Nothing failed, but the value has no effect now.

Reference corpus: **426 bound properties** captured across 5 started modules.

**A failure is an observation.** A module that does not start gives `started=false` and a failure reason.
It also gives a list of each unobservable dimension and blind spot `BS-RUNTIME-<MODULE>`. It never gives an assumed
pass.

### 10.8 Runtime graph enrichment

```mermaid
flowchart TD
    subgraph BASE["Baseline"]
        G0["G0_BASELINE_STATIC<br/><i>Agent 03</i>"] --> O1["Agent 04 runtime observations"]
        O1 --> G0E["G0_BASELINE_ENRICHED"]
    end

    subgraph EDGE["Per migration edge"]
        GE["G_EDGE_STATIC<br/><i>Agent 14 full rebuild</i>"] --> DIFF["Graph Diff<br/><i>static only</i>"]
        DIFF --> SCOPE["Scope gate"]
        SCOPE --> O2["Agent 16 runtime observations"]
        O2 --> GEE["G_EDGE_ENRICHED"]
    end

    G0E -->|"OLD side"| CMP["Agent 17 differential"]
    GEE -->|"NEW side"| CMP
    CMP --> FINAL["FINAL_GRAPH + provenance"]

    style DIFF fill:#e7f3ff,stroke:#0366d6
    style GEE fill:#d4edda,stroke:#155724
```

Each runtime edge type carries an `evidenceRef`:

| Runtime edge | Static counterpart | What the difference means |
|---|---|---|
| `ACTUALLY_INJECTED` | `INJECTS` | The bean was actually created and connected |
| `ACTIVE_UNDER_PROFILE` | `ACTIVATED_BY_PROFILE` | The profile was actually active |
| `ACTUALLY_HANDLES_ENDPOINT` | `HANDLES_ENDPOINT` | The mapping was actually registered |
| `ACTUALLY_BINDS_PROPERTY` | `USES_CONFIG_PROPERTY` | The value actually had an effect |
| `ACTUALLY_CALLS_EXTERNAL` | `CALLS_EXTERNAL_SERVICE` | The call actually occurred |
| `ACTUALLY_PUBLISHES_TO` | `PUBLISHES_TO` | The message was actually published |
| `ACTUALLY_CONSUMES_FROM` | `CONSUMES_FROM` | The subscription actually existed |

**A runtime edge never overwrites a static edge.** The two layers exist together. Thus, a report can
state whether a relationship was inferred, observed or both (R27).

Reference corpus: **441 runtime graph edges** added at edge 1.

### 10.9 Agent 17 — Differential Validation

**Purpose.** Run identical scenarios against the original and the migrated applications, and
classify each difference.

**The unit of comparison is the characterization scenario.** Bootshift runs the same scenario object
against the original application and the migrated one. It captures both results in the same way,
normalizes them with the same versioned policy and compares them.

A comparison of "module plus Actuator dimension" compares only the shape of two contexts. This stage
did that while no scenario ran. It cannot see that an endpoint changed its status, that an
unauthenticated caller now gets access, or that a response lost a field. The module-level comparison
stays only as a fallback for dimensions that no scenario covers, and it has the label of weaker
evidence.

**Bootshift compares each scenario that it observed on both sides.** The required dimensions of the
edge plan tell which dimensions the edge must *account for*. They do not limit which measurements
Bootshift *looks at*. The list of the plan comes from the impact set, which states what the harness
expects to change. A filter on that list discards observations that were already frozen against
OLD and already run against NEW. A difference blocks wherever Bootshift finds it. A comparison in a
dimension that the plan did not expect gets `plan_required_dimension: false`, and the report lists it
under `dimensions_compared_beyond_plan`.

```mermaid
flowchart TD
    SC["Characterized scenario"] --> EQ{"Environment equivalence<br/>satisfied for this dimension?"}
    EQ -->|no| NC["NOT_COMPARED<br/><i>a number is not evidence</i>"]
    EQ -->|yes| SPLIT

    SPLIT --> OLD["Original application<br/><i>runtime-old</i>"]
    SPLIT --> NEW["Migrated application<br/><i>runtime-new</i>"]
    OLD --> O1["Observation A"]
    NEW --> O2["Observation B"]

    O1 --> N["Versioned normalizer<br/><i>explicit, hashed, reviewable</i>"]
    O2 --> N
    N --> CMP["Structural comparator"]
    CMP --> DIFFS{"Any differences?"}
    DIFFS -->|no| IDENT["IDENTICAL"]
    DIFFS -->|yes| EXPL{"Explained by a VERIFIED fact<br/>or a signed approval?"}
    EXPL -->|"all of them"| EXP["EXPECTED"]
    EXPL -->|"some of them"| UNEXP1["UNEXPLAINED"]
    EXPL -->|none| UNEXP2["UNEXPLAINED"]
    EXPL -->|"known defect class"| UNEXPECTED["UNEXPECTED<br/><i>migration defect</i>"]

    UNEXP1 --> BLOCK["BLOCKS (R21)"]
    UNEXP2 --> BLOCK
    UNEXPECTED --> BLOCK

    style NC fill:#fff3cd,stroke:#856404
    style IDENT fill:#d4edda,stroke:#155724
    style EXP fill:#d4edda,stroke:#155724
    style BLOCK fill:#f8d7da,stroke:#721c24
```

**What Bootshift captures** is structural, not literal: status, content type, header names, security
header values, body shape and the sorted set of body field paths. A body that changed its shape shows
as a difference. A body in which only a `timestamp` moved does not.

A `SECURITY_AUTHORIZATION` scenario that differs without an explanation gets the class `UNEXPECTED`,
not `UNEXPLAINED`, because the difference **is** the change of the authorization decision.

Each comparison carries `scenario_id`, `dimension`, `old_evidence_ref`, `new_evidence_ref`, the
normalization policy hash, the differences, the classification, the explanation references and the
approval reference, if one applies.

**Dimensions:** `HTTP_API` · `SECURITY_AUTHORIZATION` · `SERIALIZATION` · `CONFIGURATION_BINDING` ·
`PERSISTENCE_STATE` · `QUERY_RESULT` · `TRANSACTION_EFFECT` · `CONTEXT_CAPABILITY` · `EVENT_MESSAGE` ·
`EXTERNAL_INTEGRATION` · `BATCH_RESULT` · `BUSINESS_RULE_OUTCOME`.

**Normalization is explicit, versioned and hashed.** `policies/normalization/normalization-policy.json`
has eleven rules. Each rule has an id, a scope, an action and a rationale. The policy hash goes into
the evidence manifest.

| Rule | Applies to | Action | Why |
|---|---|---|---|
| NORM-001/002 | each `timestamp`, `date` | drop | They differ between two runs by construction |
| NORM-003/004 | HTTP `Date`, `Server` | drop | Made for each response. Container identity is `EXPECTED_TO_DIFFER` |
| NORM-005/006 | `traceId`, `spanId` | drop | Made for each request |
| NORM-007 | ports in URLs | normalize | The harness allocates a free port for each side |
| NORM-008 | `uptime` | drop | Not behaviour |
| NORM-009 | `springBootVersion` | drop | It is the item that the migration changes |
| NORM-010 | hex strings of 32 to 64 characters | normalize | Content hashes of payloads that contain timestamps |
| NORM-011 | `generatedSql` | **demote to diagnostic** | Literal SQL equality is not the contract |

**Nothing is dropped implicitly.** Each comparison result lists the rules that fired.

**Persistence compares semantics, not SQL.** `policies/persistence/persistence-policy.json` defines
the contract. It contains query result sets, database and document state after the scenario, transaction
outcome, lock behaviour that the caller can see, and schema behaviour. The SQL text stays as
diagnostic evidence. Two statements can have different text and the same effect. Two statements can
also have the same text and behave differently under a new dialect. Bootshift compares a persistence
dimension only when the equivalence contract marks the database vendor and version as `MUST_MATCH`
and they match.

**Reference corpus result.** The earlier run in this README recorded
`EDGE-1-PREP-TEST: 6 comparison(s) across 1 dimension(s); {IDENTICAL=5, NOT_COMPARED=1}`. Five modules
compared as identical. The sixth was `NOT_COMPARED`, because `discovery-service` did not start on the
OLD side. Bootshift reported a gap, not a pass. [Section 22](#22-validation-results) gives the
scenario-based results of recorded run 2.

---

## 11. Phase 6 — Prove

### 11.1 Agent 18 — Approval

**Purpose.** Handle the decisions that a machine must not authorize for itself.

**Decisions are in a store, not in this stage.** Validation and approval once depended on each other.
The differential stage had to know whether an intentional-change decision existed. Only the approval
stage could tell it, but that stage runs *after* validation and takes its gates from the validation
output. Thus, a correctly recorded decision could not affect the validation that it was for.

`DecisionStore` breaks that cycle. An operator, a CI step or an enterprise approval system (through
another adapter) files decisions into a directory outside the run workspace. The directory is
`BOOTSHIFT_DECISIONS_DIR`, with the default `~/.bootshift/decisions`. Each stage can read the
decisions. Agent 18 keeps its real job:

- find which gates exist
- check the decisions on file against the gates
- report what is still outstanding

It never writes a decision for a person. The store refuses a decision with no actor or no rationale.

```mermaid
flowchart TD
    IN["Findings from Agents 13–17<br/><i>unexplained differences, AI patches,<br/>broad repairs, evidence shortfalls</i>"] --> MAP["Map each finding to its gate"]
    MAP --> OPEN["Open an approval request<br/><i>request_id, gate, evidence refs</i>"]
    OPEN --> WAIT{"A recorded decision<br/>exists for this request?"}
    WAIT -->|no| HUMAN["NEEDS_HUMAN — exit 4<br/><i>the run stops here</i>"]
    WAIT -->|yes| VAL{"Actor named AND<br/>rationale non-empty?"}
    VAL -->|no| THROW["ApprovalPort.record throws<br/><i>a decision without them is not a decision</i>"]
    VAL -->|yes| VERDICT{"Verdict"}
    VERDICT -->|APPROVED| REC["Record the decision;<br/>bind it to the findings it covers"]
    VERDICT -->|REJECTED| BLOCK["BLOCKED — exit 3"]
    REC --> LEDGER["Append to the ledger<br/>and the provenance graph"]
    LEDGER --> NEXT["The gate is closed for<br/>THESE findings only"]

    style HUMAN fill:#fff3cd,stroke:#856404
    style THROW fill:#f8d7da,stroke:#721c24
    style BLOCK fill:#f8d7da,stroke:#721c24
```

An approval applies only to the exact findings that it was raised for. A decision for the
`HIGH_RISK_AI_PATCH` of one edge does not close the same gate on the next edge. If one approval can
pre-authorize all later findings, R11 has no meaning.

| Gate | Raised when |
|---|---|
| `INTENTIONAL_SECURITY_CHANGE` | An unexpected or unexplained `SECURITY_AUTHORIZATION` difference |
| `PERSISTENCE_SCHEMA_CHANGE` | Unexplained persistence, query or transaction differences |
| `BUSINESS_OUTCOME_CHANGE` | Unexplained business-rule differences |
| `UNSUPPORTED_INTERNAL_STARTER` | Internal components with `UNKNOWN` compatibility |
| `BROAD_RESIDUAL_PATCH` | A repair patch that is larger than the normal budget |
| `HIGH_RISK_AI_PATCH` | Each accepted AI repair |
| `DOCUMENTATION_CONFLICT` | The channels disagree: a `CONFLICTING` fact |
| `SHORT_HORIZON_TARGET` | The landing target has little support time left |
| `NORMALIZATION_POLICY_CHANGE` | The normalization policy hash changed |
| `TEST_EXPECTATION_CHANGE` | A test expectation changes intentionally |
| `EVIDENCE_SHORTFALL` | A dimension did not get its required level |
| `CHECKPOINT_COLLAPSE` | A mandatory checkpoint was collapsed |
| `COVERAGE_REGRESSION` | The coverage gate fired |

**A decision is a stored artifact with an integrity hash.** `FilesystemDecisionStore` writes this
shape:

```json
{
  "decision_id": "DEC-00001",
  "request_id": "REQ-SHORT_HORIZON_TARGET-A3F91C2E",
  "gate": "SHORT_HORIZON_TARGET",
  "actor": "j.okafor",
  "role": "Principal Engineer, Platform",
  "scope": "…",
  "verdict": "APPROVED",
  "rationale": "3.5.16 is the newest line with a GA Spring Cloud train. Landing here and revisiting when 2025.1 reaches GA is a smaller risk than landing on 4.x with no ecosystem support.",
  "evidence_refs": ["…"],
  "policy_version": "1.0.0-eol-exception",
  "timestamp": "2026-09-10T06:12:44Z",
  "integrity_hash": "d41f8a…",
  "integrity_algorithm": "HmacSHA256",
  "actor_authentication": "LOCALLY_ASSERTED"
}
```

**`integrity_hash`, not `signature`.** The field had the name `signature` before. It is an HMAC over
the fields of the decision, with a key that is stored next to the store. It finds modification. It
does **not** authenticate a person, because each person who can write the file can calculate it
again. The name "signature" claimed a property that the harness cannot give. Each stored decision
records its `actor_authentication` as `LOCALLY_ASSERTED`, `EXTERNAL_IDENTITY_PROVIDER` or
`CRYPTOGRAPHICALLY_SIGNED`. An enterprise identity integration replaces the store implementation, not
the port, and each caller continues to work.

`ApprovalPort.record` **throws** on an empty rationale or a missing actor. The verdict is `APPROVED`,
`REJECTED` or `DEFERRED`. The harness never approves itself. Decisions come from outside through the
`approve` command, and open gates give exit code 4.

### 11.2 Agent 19 — Evidence and Report

**Purpose.** Assemble the evidence manifest, state the coverage correctly and write the reports that
a reviewer, an auditor and an operator each need.

**The final evidence includes each planned edge.** Agent 19 once read `output/13-build-repair/latest.json`,
`output/15-test/latest.json` and similar pointers. Each pointer names one directory: the directory
that the stage published last. In a run with eight planned edges, that is the eighth edge. The report
described that edge as if it described the full migration. An edge that did not compile halfway
through was not in the evidence.

An explicit **edge index** (`edge-index.json` in the run workspace) now records which directory each
stage published for each edge. `edge-evidence.json` then proves, for each edge, these facts:

- it was planned, transformed, compiled, graph-verified and scope-verified
- it was tested, run and compared where the plan required it
- its residuals have an account
- a checkpoint exists

**Evidence levels are mechanical.** A dimension reaches `E4` only when all of these conditions are
true:

- OLD/NEW scenario comparisons were run for it across the planned edges
- each comparison that ran gave `IDENTICAL` or `EXPECTED`
- no required comparison stayed `NOT_COMPARED`

`E4` never comes from a successful startup, a passing test suite or two equal graphs. Each of those is
evidence about a different thing.

**Absence is never a pass.**

- A missing approval report means that the approval stage never ran. This is not the same as "no gate
  is open". Bootshift records it as an evidence shortfall.
- A required comparison that was `NOT_COMPARED` is a shortfall.
- A missing `final_source_tree_hash` is a shortfall, and the export refuses it.

```mermaid
flowchart TD
    COLLECT["Collect every artifact from every stage"] --> HASH["Content-address each one<br/>SHA-256"]
    HASH --> MAN["Evidence manifest<br/><i>id, hash, producer, stage, run</i>"]
    MAN --> CLAIMS["Assemble claims"]
    CLAIMS --> LEVEL["Assign an evidence level per claim,<br/>from its supporting artifacts"]
    LEVEL --> REQ{"Level reached >=<br/>level required?"}
    REQ -->|yes| PUB["Publishable claim"]
    REQ -->|no| SHORT["EVIDENCE_SHORTFALL<br/><i>stated, not hidden</i>"]
    PUB --> COV["Coverage statement per dimension"]
    SHORT --> COV
    COV --> BS["Merge blind spots and gaps from every stage"]
    BS --> REP["migration-report.md<br/>evidence-manifest.json<br/>coverage-statement.json<br/>claims.json"]
    REP --> VER["Self-verification: re-hash every artifact"]

    style SHORT fill:#fff3cd,stroke:#856404
```

A claim is publishable only if its evidence reaches the required level:

```java
public boolean isPublishable() {
    return levelReached.atLeast(levelRequired);
}
```

`EvidenceManifest.verify()` calculates the hash of each referenced artifact again and returns one of
these verdicts:

| Verdict | Meaning |
|---|---|
| `VERIFIED` | Each artifact is present and its hash matches |
| `ARCHIVED` | The retention policy pruned some artifacts. The manifest itself is intact |
| `TAMPERED` | An artifact is present, but its content does not match its recorded hash |

**The report states what was NOT covered.** Each report has a mandatory coverage statement for each
dimension. A dimension with no observation shows as `UNOBSERVED`, with the reason. Bootshift never
omits it and never implies that it passed. This is R10 and R21 in a document.

**The migration document.** Agent 19 also writes **`MIGRATION_DOCUMENT.md`**, the end-to-end record of
what occurred. It writes it from the artifacts in each run. The report answers "is this defensible?".
The document answers the first question of a reviewer: *what actually occurred?* It has a table of
contents and fifteen sections:

1. the executive summary
2. how to read it
3. the architecture **before** the migration, with a Mermaid topology diagram
4. the architecture **after** the migration, with its diagram
5. what changed in the architecture and why
6. the migration path, with the reason for each checkpoint and its toolchain
7. a stage-by-stage record of all twenty stages
8. the detail of each edge
9. each change, with its authorizing facts and impacts
10. the behavioural validation results
11. evidence levels and coverage
12. residuals and gaps
13. human decisions
14. provenance and integrity seals
15. the limits of that specific run

The document goes with the exported bundle. Thus, a reviewer who gets only the export can still see
what was done. `reports/MIGRATION_DOCUMENT.md` is the document from recorded run 2.

**Outputs.** `evidence-manifest.json`, `claims.json`, `coverage-statement.json`, `migration-result.json`,
`file-lineage.json`, `symbol-lineage.json`, `migration-report.md`, `edge-evidence.json` and
`MIGRATION_DOCUMENT.md`. Agent 20 publishes `blind-spots.json` and `gaps.json`, because Agent 20
assembles the run-wide catalogs of what Bootshift could not observe and could not explain. The
residual coverage is in `residual-report.json` of Agent 11, next to the plan that it limits.

### 11.3 Agent 20 — Provenance Graph and Questions

**Purpose.** Make each artifact answerable: *why does this file look like this?*

```mermaid
flowchart TD
    LED["Change Ledger"] --> N1["Change nodes"]
    PLAN["Edge plans"] --> N2["Plan nodes"]
    FACTS["Migration facts"] --> N3["Knowledge nodes"]
    IMP["Impact records"] --> N4["Impact nodes"]
    DEC["Decisions"] --> N5["Decision nodes"]
    OBS["Validation observations"] --> N6["Observation nodes"]

    N1 --> G["Provenance graph"]
    N2 --> G
    N3 --> G
    N4 --> G
    N5 --> G
    N6 --> G

    G --> Q["Query interface"]
    Q --> Q1["explain change CHG-…"]
    Q --> Q2["explain impact IMP-…"]
    Q --> Q3["lineage FILE-…"]
    Q --> Q4["gaps · blind-spots"]

    style G fill:#e7f3ff,stroke:#0366d6
```

**Answers are traversals, not prose.** Bootshift reads each field below from a recorded artifact. It
does not summarize or infer.

```
$ bootshift explain change CHG-00007

  CHG-00007  [APPLIED]
  sequence       7
  event hash     9a3c…
  previous hash  4f1e…
  edge           EDGE-2-PATCH
  file           FILE-01M251AT6T287A1WRY6NWQTP81
  operation      MODIFY
  path before    report-service/pom.xml
  path after     report-service/pom.xml
  sha before     3160a4d…
  sha after      2afca57…
  agent          12-transformation
  provider       BOOTSHIFT_DETERMINISTIC bootshift-transformers
  recipe         maven.parent-version
  knowledge refs [MK-00031]
  impact refs    [IMP-00014]
  patch          patches/CHG-00007.patch
```

The reverse direction starts from a finding:

```
$ bootshift explain impact IMP-00014

  IMP-00014  [DEFINITELY_AFFECTED]
  subject        org.springframework.cloud.netflix.eureka.EnableEurekaClient
  fact           MK-00218 (API_REMOVED)
  path           configuaration-server/src/main/java/…/ConfiguarationServerApplication.java
  risk           HIGH
  confidence     0.95
  capped         false
  rationale      Graph node … references the subject with attribution RESOLVED
  blast radius   6
  dimensions     ["CONTEXT_CAPABILITY"]
```

A change with empty `knowledge refs` has no justifying fact. A fact with no supporting artifact could
never reach `E3` and authorize the change. The `gaps` and `blind-spots` commands publish the run-wide
catalogs of what Bootshift could not explain and could not observe. The `lineage` command gives the
identity, split, merge, rename and delete history of one `FILE_ID`.

---

## 12. The application graph model

```mermaid
flowchart TD
    subgraph SRC["Sources"]
        FS["Filesystem<br/><i>inventory</i>"]
        BLD["Build tools<br/><i>effective POM, dependency tree</i>"]
        AST["JavaParser + symbol solver<br/><i>real dependency classpath</i>"]
        RT["Runtime probes<br/><i>actuator</i>"]
    end

    FS --> CORE["Single typed graph<br/><i>nodes + edges, every edge evidenced</i>"]
    BLD --> CORE
    AST --> CORE
    RT --> CORE

    CORE --> V1["module view"]
    CORE --> V2["file view"]
    CORE --> V3["symbol view"]
    CORE --> V4["dependency view"]
    CORE --> V5["spring view"]
    CORE --> V6["configuration view"]
    CORE --> V7["persistence view"]
    CORE --> V8["endpoint view"]
    CORE --> V9["test view"]
    CORE --> V10["integration view"]

    style CORE fill:#e7f3ff,stroke:#0366d6
```

**Ten views, one graph.** A view is a projection. It is never a separate store. Thus, you can ask a
question in the endpoint view and answer it in the persistence view.

**Node types.** `core/graph/NodeType.java` defines 28 node types:

| Category | Node types |
|---|---|
| Structure | `MODULE`, `FILE`, `PACKAGE` |
| Type | `CLASS`, `INTERFACE`, `ENUM`, `RECORD` |
| Member | `METHOD`, `CONSTRUCTOR`, `FIELD` |
| Build | `LIBRARY`, `DEPENDENCY` |
| Spring | `SPRING_BEAN`, `CONFIGURATION_CLASS`, `CONTROLLER`, `SERVICE`, `REPOSITORY` |
| Web | `ENDPOINT` |
| Data | `ENTITY`, `MONGODB_DOCUMENT`, `DATABASE_TABLE` |
| Configuration | `CONFIG_PROPERTY`, `PROFILE` |
| Test | `TEST` |
| Integration | `MESSAGE_DESTINATION`, `EXTERNAL_SYSTEM`, `CONFIG_SERVER`, `DISCOVERY_SERVER` |

**Edge types.** `core/graph/EdgeType.java` defines 31 edge types.

- Static (24): `CONTAINS`, `DECLARES`, `IMPORTS`, `CALLS`, `EXTENDS`, `IMPLEMENTS`, `USES_TYPE`,
  `INJECTS`, `DEPENDS_ON`, `DEPENDS_ON_LIBRARY`, `DECLARES_BEAN`, `CONFIGURES`, `HANDLES_ENDPOINT`,
  `CALLS_SERVICE`, `CALLS_REPOSITORY`, `MANAGES_ENTITY`, `MAPS_TO_TABLE`, `USES_CONFIG_PROPERTY`,
  `ACTIVATED_BY_PROFILE`, `COVERED_BY_TEST`, `CALLS_EXTERNAL_SERVICE`, `REGISTERS_WITH_DISCOVERY`,
  `READS_FROM_CONFIG_SERVER`, `PUBLISHES_TO`, `CONSUMES_FROM`.
- Runtime-observed (7): the `ACTUALLY_*` and `ACTIVE_UNDER_PROFILE` edges of
  [Section 10.8](#108-runtime-graph-enrichment).

`EdgeType.isRuntimeObserved()` keeps the two layers separate in each query (R27).

**Each edge carries evidence.**

```json
{
  "edgeId": "EDGE-…",
  "type": "INJECTS",
  "from": "…EmployeeController",
  "to": "…EmployeeService",
  "evidence": "AST_FIELD_INJECTION",
  "confidence": 1.0,
  "source_layer": "STATIC",
  "attributes": { "field": "employeeService", "synthetic": false }
}
```

An unresolved call that Bootshift recovers by a name rule has `confidence: 0.5` and
`evidence: "AST_NAME_HEURISTIC"`. Lombok members have `"synthetic": true, "generated_by": "lombok"`.
No edge claims more certainty than it has.

**Blast radius.** The `graph blast-radius` command takes a `FILE_ID` or a path.
`ApplicationGraph.blastRadius` does a reverse traversal over each edge type except `CONTAINS`. It
returns the transitive dependents at each depth, with the path that explains each one. The default depth is 5
and the default row limit is 40. Impact scoring and test selection use this result.

```
$ bootshift graph blast-radius employee-service/src/main/java/com/aura/vihanga/employeeservice/service/EmployeeService.java --depth 3
```

**Two hashes, and why both exist.**

| Hash | Includes | Can you compare it across runs? |
|---|---|---|
| `structural_hash` | Nodes, edges **and their identities** | **No** |
| `content_hash` | Nodes and edges by fully qualified name. Each run-scoped id is removed | **Yes** |

Bootshift allocates new ULIDs as `FILE_ID`s in each run, and node ids come from them. Thus, a hash over
identities is different between two runs over a byte-identical repository. This makes it useful to
find identity changes *inside* a run, and misleading in a report. Two runs gave
`839 nodes / 1852 edges / attribution 0.6858` and the structural hashes `a362dad34459` and
`55ee8290493a`. A reader who compares these values thinks that the application changed, but
nothing changed.

Compare two runs with `content_hash`. `GraphHashTest` checks both directions. The content hash stays
the same across runs. It still changes when a node is added or an edge is connected differently.
Recorded runs 1 and 2 in `reports/` both gave the content hash `5b764c29af1e`.

**Reference corpus.** **839 nodes, 1852 edges, 239 symbols**, type attribution **0.6858**.

---

## 13. File identity and lineage

```mermaid
flowchart TD
    SCAN["Inventory scan"] --> KNOWN{"Path already registered?"}
    KNOWN -->|yes| SAME{"Content hash unchanged?"}
    SAME -->|yes| R1["1. Same path + same hash<br/>→ same FILE_ID"]
    SAME -->|no| R2["2. Same path, new hash<br/>→ same FILE_ID, new version"]
    KNOWN -->|no| GIT{"Git rename detection<br/>finds a source?"}
    GIT -->|yes| R3["3. Renamed → same FILE_ID<br/>renameSource=GIT"]
    GIT -->|no| HASH{"An absent file has<br/>this exact hash?"}
    HASH -->|yes| R4["4. Moved → same FILE_ID<br/>renameSource=CONTENT_HASH"]
    HASH -->|no| SIM{"Similarity above<br/>the policy threshold?"}
    SIM -->|yes| R5["5. Similar → same FILE_ID<br/>renameSource=SIMILARITY"]
    SIM -->|no| NEW["New allocation<br/>FILE-&lt;ULID&gt;"]

    R1 --> REG["File registry"]
    R2 --> REG
    R3 --> REG
    R4 --> REG
    R5 --> REG
    NEW --> REG
    REG --> ABSENT{"Registered file not<br/>seen in this scan?"}
    ABSENT -->|yes| DEL["status = DELETED<br/><i>the id is never reused</i>"]

    style NEW fill:#d4edda,stroke:#155724
    style DEL fill:#f8d7da,stroke:#721c24
```

**The rules**

- **R2:** the inventory agent, and only the inventory agent, allocates a `FILE_ID`.
- **R3–R4:** a `FILE_ID` is not a path and not a content hash. A file that moves keeps its identity.
  Two files with identical content have different identities.
- Bootshift tries reattachment **in the sequence 1 → 5**. The first rule that matches wins. The record
  stores the matching rule as `renameSource`, so that a reviewer can see *why* the harness thinks that
  two paths are the same file.
- Bootshift **never reuses** an id. The record of a deleted file stays in the registry with
  `status: DELETED`.
- Rule 5 uses `similarity_threshold` from the policy (default 0.72).

| Event | Recorded as |
|---|---|
| Split | The original keeps its id and gets `splitInto: [FILE-…, FILE-…]`. Each new part records `splitFrom` |
| Merge | Each source records `mergedInto`. The file that stays records `mergedFrom: [FILE-…, …]` |
| Delete | `status: DELETED`, `deletedAt`, and the full version history |

These records let the `lineage` command answer questions about a file that is no longer at the path
that a reviewer remembers.

**Sealing.** `FileRegistry.seal()` freezes the registry for the run. After the seal, an attempt to
allocate a new id or change a record throws (R30).

The registry publishes two hashes for the same reason as the graph:

- `seal_hash` covers `FILE_ID : path : content`, so it is scoped to the run. It makes an identity
  change after the seal visible.
- `content_manifest_hash` covers only `path : content`. Two runs over the same repository give the same
  value. A difference means that the repository changed, not that a new run allocated new identifiers.

---

## 14. Migration edges and validation depth

### 14.1 Edge classes

```mermaid
flowchart LR
    S["Source state<br/>2.6.8 / Java 8"] --> E1
    subgraph PATH["Migration path"]
        E1["EDGE-1<br/>PREPARATORY<br/>test framework"] --> E2["EDGE-2<br/>PATCH<br/>2.6.8 → 2.6.15"]
        E2 --> E3["EDGE-3<br/>MINOR<br/>2.6 → 2.7"]
        E3 --> E4["EDGE-4<br/>PREPARATORY<br/>deprecation cleanup"]
        E4 --> E5["EDGE-5<br/>MAJOR<br/>2.7 → 3.0<br/><i>javax → jakarta</i>"]
        E5 --> E6["EDGE-6<br/>MINOR<br/>3.0 → 3.2"]
        E6 --> E7["EDGE-7<br/>MINOR<br/>3.2 → 3.4"]
        E7 --> E8["EDGE-8<br/>MINOR<br/>3.4 → 3.5.16"]
    end
    E8 --> T["Landing target<br/>3.5.16 / Java 21"]

    style E5 fill:#f8d7da,stroke:#721c24
    style E1 fill:#e7f3ff,stroke:#0366d6
    style E4 fill:#e7f3ff,stroke:#0366d6
```

This diagram is an illustration of path decomposition. The reference corpus path is in
[Section 8.3](#83-agent-06--target-resolver).

| Class | Meaning | Validation depth floor (`validation-depth-policy.json`) |
|---|---|---|
| `PREPARATORY` | Changes at the current version that make a later edge possible | `TESTS` |
| `PATCH` | Inside a minor line | `TESTS` |
| `MINOR` | Across a minor line | `RUNTIME` |
| `MAJOR` | Across a major line | `DIFFERENTIAL` |
| `PLATFORM` | Java baseline change | `RUNTIME` |
| `ECOSYSTEM` | Spring Cloud train change | `RUNTIME` |

The target resolver also uses the classes `MAJOR_BOUNDARY` and `LANDING` for the path that it freezes.

### 14.2 An edge is a self-contained unit

Each edge has its **own** target state, its **own** Java level and its **own** Spring Cloud train. It
does not use the values of the landing target. Two defects that the harness found in its own
construction show why:

- The landing Spring Cloud train (`2025.0.3`) on a 2.7 edge removed `@EnableEurekaClient`, which does
  not exist in that train. The result was 33 compile errors. The planner now resolves the train for
  the Boot line of the edge. If no GA train targets that line, it **omits the managed-version
  transformation**. It does not install a train that does not agree.
- The landing Java level (21) on a patch edge changed the compiler target for a reason that the edge
  does not carry. The Java level now moves only at major boundaries. Each other edge uses the highest
  installed JDK that its own Boot line supports.

### 14.3 Train resolution from artifacts

The harness does not read a compatibility table. For each candidate Spring Cloud release, it gets
`spring-cloud-dependencies-<version>.pom` from Maven Central and reads the declared
`spring-boot-starter-parent` version. That version is the **minimum** Boot version of the train. The
harness then selects the newest train whose parent line is at or below the target line, **in the same
Boot major**. It marks the fact `VERIFIED` on an exact line match and `ADVISORY` when it infers the
match:

```
2.7 -> 2021.0.9  (parent 2.6 — newest train at or below 2.7 within major 2)   ADVISORY
3.0 -> 2022.0.5  (parent 3.0 — matches this line exactly)                     VERIFIED
3.1 -> 2022.0.5  (parent 3.0 — newest train at or below 3.1 within major 3)   ADVISORY
3.2 -> 2023.0.5  (parent 3.2 — matches this line exactly)                     VERIFIED
3.3 -> 2023.0.6  (parent 3.3 — matches this line exactly)                     VERIFIED
3.4 -> 2024.0.3  (parent 3.4 — matches this line exactly)                     VERIFIED
3.5 -> 2025.0.3  (parent 3.5 — matches this line exactly)                     VERIFIED
4.0 -> None      (no train declares a parent within Boot major 4)             BLOCKING
4.1 -> None      (no train declares a parent within Boot major 4)             BLOCKING
```

The last two lines are a real statement about the ecosystem at the date of the run, and anyone can
check them. They are the reason that the harness refuses to land this corpus on Boot 4.x.

### 14.4 Composite transformation and checkpoint reconciliation

**Why Bootshift cannot silently collapse a checkpoint (R26).** Bootshift divides a migration path so
that it validates each edge independently. Assume that it joins two edges into one because the tools support
the combined step. Then it loses the information that the division gives: which edge caused a
regression.

The harness permits a **composite transformation** only when all of these conditions are true:

1. The plan declares the composite explicitly and names the edges in it.
2. The ledger still records the transformation of each edge in it individually.
3. A **reconciliation record** shows, for each edge in it, the state that the edge must reach and the
   state that the run actually reached.
4. Validation runs at the **maximum** depth of the edges in it, never the minimum.
5. A `CHECKPOINT_COLLAPSE` approval gate is raised if the mandatory checkpoint of one of the edges was
   not independently observable.

```json
{
  "composite_id": "COMP-EDGE-6-7",
  "constituent_edges": ["EDGE-6-MINOR-3.0-3.2", "EDGE-7-MINOR-3.2-3.4"],
  "reason": "The recipe estate applies both minor migrations as one atomic unit",
  "per_edge_reconciliation": [
    {
      "edge": "EDGE-6-MINOR-3.0-3.2",
      "expected_state": "3.2.x",
      "observed_independently": false,
      "checkpoint": "COLLAPSED",
      "gate": "CHECKPOINT_COLLAPSE"
    }
  ],
  "validation_depth": "DIFFERENTIAL",
  "depth_source": "MAX(constituent edges)"
}
```

A composite is not forbidden. Bootshift **records it as less evidence**, raises a gate for approval
and shows it in the coverage statement. The policy flag `allow_checkpoint_collapse` controls it.

### 14.5 Adaptive validation depth

**Bootshift calculates the depth, then freezes it (R16).**

```
DEPTH = MAX(
    edge class floor,      // MAJOR ⇒ differential
    residual risk,         // AI-repaired code ⇒ differential
    impact severity,       // HIGH impact on security/persistence ⇒ differential
    policy minimum         // production policy ⇒ at least runtime
)
```

When Bootshift calculates the depth for an edge, it **freezes** the value into the plan before any
change. No later event can lower it: not a slow test suite, a container that fails at random, or a
short schedule. To lower the depth, you need a new run with a new plan and a recorded decision.

```mermaid
flowchart LR
    D0["NONE<br/><i>static only</i>"] --> D1["BUILD<br/><i>compiles</i>"]
    D1 --> D2["TESTS<br/><i>+ suite + coverage</i>"]
    D2 --> D3["RUNTIME<br/><i>+ starts + binds</i>"]
    D3 --> D4["DIFFERENTIAL<br/><i>+ OLD vs NEW</i>"]

    style D4 fill:#d4edda,stroke:#155724
```

| Trigger | Raised to |
|---|---|
| Edge class `MAJOR` | `DIFFERENTIAL` |
| Each accepted AI repair on the edge | `DIFFERENTIAL` |
| `HIGH` impact on security, persistence or transactions | `DIFFERENTIAL` |
| Namespace relocation applied (`javax` → `jakarta`) | `DIFFERENTIAL` |
| A configuration property migration that affects a bound key | `RUNTIME` |
| Measured impact recall below the policy floor | One level |
| Deterministic coverage below `residual_one_level_threshold` | One level |
| Policy `production` | At least `RUNTIME` |

**What an unreachable depth means.** If an edge needs `RUNTIME` and no module starts, the edge does
**not** pass at a lower depth. Bootshift raises an `EVIDENCE_SHORTFALL` gate and blind spots that name
each dimension that it could not observe. Bootshift always reports a skipped level with its reason.
This is R21 for validation.

---

## 15. Evidence levels and outcomes

### 15.1 The evidence ladder

```mermaid
flowchart TD
    E0["E0 — ASSERTED<br/><i>a statement with no artifact</i>"] --> E1
    E1["E1 — DOCUMENTED<br/><i>vendor documentation snapshot</i>"] --> E2
    E2["E2 — STATIC<br/><i>source, AST or build model</i>"] --> E3
    E3["E3 — ARTIFACT_VERIFIED<br/><i>the published artifact was inspected</i>"] --> E4
    E4["E4 — EXECUTED<br/><i>it compiled / the tests ran</i>"] --> E5
    E5["E5 — DIFFERENTIALLY_VERIFIED<br/><i>OLD and NEW behaved the same</i>"]

    CLAIM["Claim"] --> REQ["levelRequired<br/><i>set by the claim's kind</i>"]
    CLAIM --> GOT["levelReached<br/><i>from its supporting artifacts</i>"]
    REQ --> CMP{"reached >= required?"}
    GOT --> CMP
    CMP -->|yes| PUBOK["published"]
    CMP -->|no| PUBNO["EVIDENCE_SHORTFALL<br/><i>the claim is reported as unproven</i>"]

    style E0 fill:#f8d7da,stroke:#721c24
    style E5 fill:#d4edda,stroke:#155724
    style PUBNO fill:#fff3cd,stroke:#856404
```

| Level | Name | Can authorize a change | Can be published as fact |
|---|---|:--:|:--:|
| `E0` | `ASSERTED` | no | no |
| `E1` | `DOCUMENTED` | no | no |
| `E2` | `STATIC` | no | yes |
| `E3` | `ARTIFACT_VERIFIED` | yes | yes |
| `E4` | `EXECUTED` | yes | yes |
| `E5` | `DIFFERENTIALLY_VERIFIED` | yes | yes |

`policies/evidence/evidence-policy.json` defines these levels.

### 15.2 The level that a claim needs

The level depends on what the claim authorizes.

| Claim kind | Minimum level | Why |
|---|---|---|
| "This version exists" (`VERSION_EXISTS`) | `E3` | Documentation comes later than releases (R10) |
| "This API was removed" (`API_REMOVED`, `API_RENAMED`) | `E3` | `javap` on the published jar, not a migration guide |
| "This property was renamed or removed" | `E3` | `spring-configuration-metadata.json` in the published jar |
| "This artifact was removed" (`ARTIFACT_REMOVED`) | `E3` | BOM diff and existence probe |
| "This edge compiles" (`EDGE_COMPILES`) | `E4` | The build tool said so (R5) |
| "This edge does not regress the suite" (`SUITE_NOT_REGRESSED`) | `E4` | Surefire reports |
| "The application starts" (`APPLICATION_STARTS`) | `E4` | Runtime probe |
| "Behaviour is unchanged" (`BEHAVIOUR_UNCHANGED`) | `E5` | Only OLD-vs-NEW can support it |
| "This component is compatible" (`COMPONENT_COMPATIBLE`) | `E3` + policy | R28: Bootshift never assumes that an unknown internal component is compatible |

`E0` and `E1` never authorize a change. `Claim.isPublishable()` is the only gate that lets a statement
into the report as fact. The two documentation-only levels are below each important threshold.

### 15.3 Coverage statements

Each dimension gets a statement, observed or not:

```json
{
  "dimension": "SECURITY_AUTHORIZATION",
  "level_required": "E5",
  "level_reached": "E2",
  "covered": false,
  "reason": "No characterized security scenario could be executed: the OLD side did not expose an authenticated endpoint under the MANAGED environment",
  "blind_spots": ["BS-DIFF-SECURITY-UNOBSERVED"]
}
```

An uncovered dimension is a full report entry. The harness never reports on twelve dimensions with a
description of only the four that it observed.

### 15.4 The five outcomes and exit codes

| Outcome | Exit | When | What the operator does |
|---|---|---|---|
| `SUCCESS` | 0 | Each gate passed at the frozen depth | Review the report and the diff |
| `FAILURE` | 1 | A stage threw: a tool stopped, a path could not be read | Repair the environment and run the stage again |
| `REFUSAL` | 2 | The evidence does not support the next step | Supply the missing evidence, or accept the blind spot explicitly |
| `POLICY_BLOCK` | 3 | The only available path violates the policy | Change the policy with a decision, or change the target |
| `NEEDS_HUMAN` | 4 | Approval gates are open | Use the `approve` command with a rationale |

Exit codes `1` and `2` are different on purpose. A crash and a refusal are different events, and
automation must treat them differently.

### 15.5 Refusals, rollback and publication

**A refusal is structured.** A refusal always names the rule that it enforces and a concrete remedy.
"It did not work" is not an acceptable output. The previous README gave this example of a refusal:

```json
{
  "outcome": "REFUSAL",
  "stage": "08-knowledge",
  "reason": "DOCUMENTATION_ONLY_FACT_WOULD_AUTHORIZE_MUTATION",
  "detail": "FACT-00218 (spring.redis.* → spring.data.redis.*) is CANDIDATE: the documentation channel asserts it but no artifact-verified metadata entry confirms it for 3.0.0",
  "rule": "R10",
  "remedy": [
    "Provide spring-boot-autoconfigure-3.0.0.jar so its spring-configuration-metadata.json can be read, or",
    "Record a decision accepting the documentation-only fact, naming the reviewer"
  ],
  "blocking": true
}
```

**Rollback.** Each edge has a checkpoint at its start and at its end. If a gate inside an edge fails,
the working tree goes back to the start checkpoint of the edge, and Bootshift records `ROLLED_BACK`.
The applied changes stay in the ledger as attempted and reverted (R14), because "we tried this and it
failed validation" is evidence.

There is one intentional exception. If the **checkpoint itself** fails, the gateway keeps the applied
work. It records a `FAILED_VALIDATION` ledger event with `checkpointRef: "CHECKPOINT_FAILED"` and
returns. Thus, the operator can examine what occurred and does not lose it. `MutationBoundaryTest`
covers this path.

**Nothing partial is published.** `OutputLayout.publish()` validates first and writes the pointer
last. A failed stage does not move `latest.json`. Thus, downstream stages see the last good artifacts,
and they refuse to continue. They never read a half-written artifact.

---

## 16. OSS tooling and the AI boundary

### 16.1 The strict-OSS rule (R8–R9)

Each runtime dependency must have a verified permissive or weak-copyleft OSS licence. **An unknown
licence is blocked.** It is not permitted until somebody proves otherwise.

These items are forbidden as runtime dependencies:

- `rewrite-spring` and the wider source-available recipe estates
- MSAL and other proprietary identity SDKs
- proprietary transformation or analysis engines
- proprietary hosted LLM APIs

This rule has a technical reason. If the evidence chain of a harness depends on a component that
nobody can inspect, the harness cannot claim that its conclusions are reproducible.

### 16.2 Runtime dependencies

The parent `pom.xml` pins these versions:

| Component | Version | Licence | Role |
|---|---|---|---|
| Jackson (`databind`, `jsr310`, `dataformat-xml`) | 2.17.2 | Apache-2.0 | Canonical JSON and XML models |
| Picocli | 4.7.6 | Apache-2.0 | CLI |
| JGit | 6.10.0.202406032230-r | BSD-3-Clause (EDL) | Snapshots, checkpoints, rename detection, patch series |
| JavaParser (`symbol-solver-core`) | 3.26.2 | Apache-2.0 / LGPL-3.0 dual | AST and symbol resolution |
| networknt `json-schema-validator` | 1.5.1 | Apache-2.0 | Artifact schema conformance |
| SLF4J (`api`, `simple`) | 2.0.13 | MIT | Logging facade |
| OpenRewrite (`rewrite-core`, `rewrite-java`, `rewrite-java-21`, `rewrite-maven`, `rewrite-yaml`, `rewrite-properties`) | 8.90.4 | Apache-2.0 | Transformation engine (core modules only) |
| SnakeYAML | 2.2 | Apache-2.0 | Structural YAML property parsing |
| JUnit 5 | 5.10.3 | EPL-2.0 | Harness tests |
| AssertJ | 3.26.3 | Apache-2.0 | Harness test assertions |
| ArchUnit | 1.3.0 | Apache-2.0 | Architecture rules as tests |

JavaParser has two licences. The harness selects **Apache-2.0** and records the selection in
`policies/license/license-policy.json`. The earlier reference run reported that the OSS gate passed
for **12 of 12** harness components, with none unknown and none forbidden. That run came before the
OpenRewrite and SnakeYAML dependencies.

### 16.3 Tools that Bootshift starts as processes

Bootshift starts Maven, Gradle, the JDK tools (`javap`, `jar`, `java`, `jdeps`, `jdeprscan`) and Git as
external processes, through the command allowlist. JaCoCo runs as a Maven goal. These tools are not
linked into the harness. Thus, the dependency surface of the harness stays small, and the build tools
stay authoritative (R5). The harness does not implement them again.

`javap` is necessary. It is the only channel that can tell "documented as deprecated" from "actually
gone". For this reason, `API_REMOVED` needs evidence level `E3`, and Agent 08 runs a published-bytecode
diff over each declared coordinate whose managed version moves.

### 16.4 OpenRewrite core

`OpenRewriteCoreProvider` is a real transformation provider. It parses sources into an LST and runs
recipes through `InMemoryLargeSourceSet`. Its predecessor only asked whether `org.openrewrite.Recipe`
could load. It declared a capability when it could, and then returned no changes when asked to apply
anything. Thus, the capability registry claimed coverage that the transformation stage could not give.
That is worse than a declaration that the tool is absent.

Four Bootshift recipe ids map to concrete OpenRewrite recipes:

| Bootshift recipe | OpenRewrite recipe | Used for |
|---|---|---|
| `openrewrite.java.change-package` | `org.openrewrite.java.ChangePackage` | The `javax` → `jakarta` relocation |
| `openrewrite.java.remove-annotation` | `org.openrewrite.java.RemoveAnnotation` | Annotations deleted at the target version |
| `openrewrite.maven.change-parent-pom` | `org.openrewrite.maven.ChangeParentPom` | The parent POM version |
| `openrewrite.maven.change-property` | `org.openrewrite.maven.ChangePropertyValue` | The declared Java level |

Four constraints control the integration:

- **OpenRewrite is not the orchestrator.** Bootshift decides which recipe runs on which edge, from
  verified migration facts. It gives OpenRewrite one named transformation over one explicit file set.
- **OpenRewrite never writes to the migration workspace.** It parses sources into a model in memory.
  The results come back as text and go through `FileMutationGateway`, the same as all other changes.
- **OpenRewrite never bypasses evidence.** Each change carries these items: the engine version, the module
  versions, the recipe class, the input and output hashes, the edge id, and the knowledge and impact
  references.
- **Bootshift enforces strict OSS.** If a source-available Spring recipe estate can load, the provider
  reports `LICENSE_BLOCK` and does not run *at all*.

The forbidden list has exactly one definition, in `LicensePolicy`: the forbidden packages
`org.openrewrite.java.spring`, `org.openrewrite.recipe.spring` and `io.moderne`, and marker classes.
The provider probes against that list. An ArchUnit rule (`forbiddenRecipePolicyHasOneSourceOfTruth`)
fails the build if the list is copied anywhere else. Agent 00 probes the runtime classpath for it. If
three copies of a list exist, one of them can silently stop agreeing with the other two.

If the OpenRewrite Java module is available, the planner schedules the namespace relocation through
`ChangePackage`, not through the textual transformer of the harness. A textual rewrite matches the
token wherever it is, also inside comments and string literals. `ChangePackage` works on a parsed
model and changes only declarations, imports and type references. If the module is absent, Bootshift
uses its own transformer and records the reduced precision in the plan.

### 16.5 The licence gate

There is no separate licence command, because the gate is not optional. Agent 00 runs it **before
each other stage**, and a failure stops the run. A harness with an unverified dependency cannot make
verifiable claims. Agent 00 publishes the verdict as an artifact:

```
$ cat output/00-bootstrap/<timestamp>/oss-license-gate.json

  "gate": "PASSED",
  "allowlist": ["APACHE-2.0", "MIT", "BSD-2-CLAUSE", "BSD-3-CLAUSE", "EPL-2.0", …],
  "denylist":  ["PROPRIETARY", "SOURCE-AVAILABLE", "MSAL",
                "MODERNE SOURCE AVAILABLE LICENSE", "BUSL-1.1", "ELASTIC LICENSE", …],
  "forbidden_artifacts": ["org.openrewrite.recipe:rewrite-spring",
                          "org.openrewrite.recipe:rewrite-migrate-java-spring",
                          "io.moderne:moderne-recipe", "io.moderne.recipe:rewrite-spring"],
  "findings": [
    { "component": "com.fasterxml.jackson.core:jackson-databind",
      "version": "2.17.2",
      "declaredLicense": "Apache-2.0",
      "verdict": "ALLOWED",
      "reason": "License matches allowlist entry APACHE-2.0" },
    …
  ]
```

`policies/license/license-policy.json` lists the allowed SPDX identifiers, the forbidden classes
(`source-available`, `commercial-only`, `unknown`) and the licence selection for each component, for
example Apache-2.0 for JavaParser.

The same policy applies to the **AI model** when a local model is on. Bootshift checks the licence of
the model separately from the licence of its runtime. A permissively licensed server does not
make its weights permissively licensed. `policies/ai/ai-policy.json` sets
`unknown_model_license: BLOCK`.

### 16.6 The AI boundary

These rules from [Section 3.1](#31-the-31-rules) control AI:

- **R11 — AI cannot authorize.** No AI output is ever an authorization for a change.
- **R12 — AI is optional.** The harness runs from end to end with AI off.
- **R13 — single writer.** AI output reaches the filesystem only through `FileMutationGateway`, on the
  same 13-step path as each other change.

| Permitted | Forbidden |
|---|---|
| Suggest a repair for a compiler diagnostic | Decide that a version is compatible |
| Cluster diagnostics by probable root cause | Authorize a change |
| Write a draft rationale for a person to review | Approve its own patch |
| Summarize a documentation snapshot | Classify a test failure as expected |
| Propose characterization scenarios | Decide that a difference is acceptable |
| Explain a graph query result | Write to disk directly |

```mermaid
flowchart TD
    DIAG["Unresolved compiler diagnostic cluster"] --> EN{"AI enabled by policy?"}
    EN -->|no| STOP["Residual recorded; edge blocks"]
    EN -->|yes| CTX["Build context:<br/>diagnostic + surrounding source +<br/>VERIFIED facts. NO SECRETS."]
    CTX --> RED["Redact through SensitiveValues"]
    RED --> LLM["Local OSS provider<br/><i>loopback only</i>"]
    LLM --> PATCH["Proposed patch"]
    PATCH --> G1{"Inside the authorized<br/>edge scope?"}
    G1 -->|no| REJ["REJECTED"]
    G1 -->|yes| G2{"Parses?"}
    G2 -->|no| REJ
    G2 -->|yes| G3{"Compiles?"}
    G3 -->|no| REJ
    G3 -->|yes| G4{"Tests still pass?"}
    G4 -->|no| REJ
    G4 -->|yes| G5{"Within the patch budget?"}
    G5 -->|no| GATE1["BROAD_RESIDUAL_PATCH gate"]
    G5 -->|yes| ACC["Applied through the gateway,<br/>author=AI, ledger-recorded"]
    ACC --> GATE2["HIGH_RISK_AI_PATCH gate<br/>+ depth raised to DIFFERENTIAL"]

    style REJ fill:#f8d7da,stroke:#721c24
    style GATE2 fill:#fff3cd,stroke:#856404
```

Bootshift records each attempt, accepted or rejected (R14). A rejected AI patch is evidence about the
migration.

**The provider.** `LocalOssAIProvider` refuses each endpoint that is not loopback. No code path in the
harness sends repository content to a remote inference service. `AiBoundaryTest` has seven tests,
and one of them fails the build if Bootshift accepts a non-loopback endpoint. The defaults are
endpoint `http://127.0.0.1:11434`, runtime `ollama`, model `qwen2.5-coder` and model licence
`Apache-2.0` ([Section 20.7](#207-environment-variables)).

**Attribution.** `policies/ai/ai-policy.json` defines what happens to an accepted AI patch. The ledger
event gets `author: AI`, and the file appears in `ai-attributed-changes.json`. Bootshift raises
`HIGH_RISK_AI_PATCH`, and the validation depth of the edge goes to `DIFFERENTIAL`. Thus, a reviewer can
always ask "what here did a model write?".

---

## 17. Security, sensitive data and retention

### 17.1 The threat model

**The repository under analysis is untrusted input.** It contains code that the harness compiles and
runs. All controls below come from this assumption.

| Threat | Control |
|---|---|
| A build script of the repository runs arbitrary commands | Command allowlist in `ProcessRunner`. Only `mvn`, `mvnw`, `gradle`, `gradlew`, `java`, `javap`, `jdeps`, `jdeprscan` and `git` (and their Windows forms) can start. `cmd.exe` is not on the list |
| A build that runs for a long time or does not stop | Timeout for each invocation. Bootshift destroys the process tree when the time ends |
| Output floods memory or disk | Output cap for each invocation. Bootshift records the truncation |
| Repository code sends data over the network | Egress allowlist in `HttpFetcher`. Bootshift checks the allowlist again on each redirect |
| Path traversal through a crafted file path | Bootshift normalizes each path and asserts that it stays inside the workspace root. This check says nothing about symlinks, so the next row is separate |
| Symlink escape from the workspace | Bootshift rejects a write target that is a symlink. The `toRealPath()` of the nearest existing ancestor must resolve inside the real workspace root |
| A change to the real source tree of the user | Bootshift copies `./src/` and never writes to it. The original workspace is read-only |
| A bypass of the single writer | `detectBypass()` and the ArchUnit rule ([Section 10.2](#102-the-filemutationgateway)) |
| Tampering with recorded history | Hash-chained append-only ledger. `verify` finds modification, insertion, deletion, a change of sequence and truncation |
| Secrets in evidence or prompts | `SensitiveValues` redaction at each boundary. `NEVER_STORE_PLAINTEXT` |
| Secrets in the environment of child processes | `ProcessRunner` passes only an allowlist of variables (`PATH`, `HOME`, `JAVA_HOME`, `M2_HOME`, `MAVEN_HOME`, `GRADLE_USER_HOME`, `LANG`, `TZ` and similar). It drops names that contain `TOKEN`, `SECRET`, `PASSWORD`, `API_KEY`, `AWS_`, `GITHUB_` and similar fragments. It redacts secret-shaped values before it writes logs |
| A malicious or compromised AI response | Deterministic gates before the change: scope, parse, compile, tests, budget |
| Supply-chain injection through a recipe estate | Strict-OSS policy. `OpenRewriteCoreProvider` blocks recipe estates that are not core |

The default egress allowlist is `repo1.maven.org`, `repo.maven.apache.org`, `search.maven.org`,
`docs.spring.io`, `spring.io`, `github.com`, `raw.githubusercontent.com`, `api.github.com`,
`endoflife.date` and `central.sonatype.com`.

**Isolation.** Execution occurs in a workspace outside the repository, under the temporary directory
of the OS:

```
%TEMP%/bootshift-workspaces/<runId>/
    original/      read-only sealed copy
    runtime-old/   writable copy for OLD-side execution
    migration/     the mutable working tree
    runtime-new/   writable copy for NEW-side execution
```

Bootshift reads the `./src/` folder of the user one time, at snapshot time, and never writes to it.

**Out of scope.** The harness has no kernel-level sandbox. It does not defend against a repository
that attacks the host through a JDK zero-day. It does not defend against a malicious Maven plugin that
the build already trusted before the harness ran. Run Bootshift against an untrusted repository only
in a disposable environment.

### 17.2 Sensitive data

**The rule: metadata represents a sensitive value. Bootshift never stores the value.**

```json
{
  "secretRef": "SECRET-8F2A…",
  "kind": "MONGODB_CONNECTION_STRING",
  "location": { "fileId": "FILE-…", "line": 3, "key": "spring.data.mongodb.uri" },
  "evidence_policy": "NEVER_STORE_PLAINTEXT",
  "shape": { "scheme": "mongodb+srv", "has_credentials": true, "host_class": "EXTERNAL_MANAGED" },
  "detector": "URI_WITH_INLINE_CREDENTIALS",
  "confidence": 1.0
}
```

`shape` is structural on purpose. It tells a reviewer *what kind of item* is there and *why it is
important*. It does not give enough to use the secret.

The detectors in `policies/security/sensitive-data-policy.json` are `URI_WITH_INLINE_CREDENTIALS`,
`PRIVATE_KEY_BLOCK`, `AWS_ACCESS_KEY_ID`, `BEARER_TOKEN`, `BASIC_AUTH_HEADER`, `JDBC_PASSWORD_PROPERTY`
and `HIGH_ENTROPY_ASSIGNMENT`.

**Where redaction applies.** At each boundary, with no exception: artifacts, the ledger, logs,
telemetry, AI prompts, exported bundles and reports. `SensitiveValues.describe()` returns only
metadata. That class has no method that returns a detected plaintext secret.

**Found on the reference corpus.** The harness found **MongoDB Atlas credentials committed in four
`application.properties` files**. The report shows them as four `SECRET_REFERENCE` findings with
locations and shapes. The credentials are not in `output/`. `PublishedArtifactSecretScanTest` checks
that the real credential components of the corpus are in no published artifact, and that both halves
of a credential pair are redacted. `SchemaConformanceTest` checks that no plaintext secret reaches
`output/`.

**Keyed hashes.** Where the policy explicitly permits correlation across runs, Bootshift can store a
**keyed** hash. It is an HMAC-SHA256 with a key that is local to the deployment. With it, you can answer "the
same secret is in these five files". Unkeyed hashes of secrets are forbidden, because a person can
reverse them for low-entropy values.

### 17.3 Retention

| Tier | Contents | Default |
|---|---|---|
| `EVIDENCE_MANIFEST` | `evidence-manifest.json`, `change-ledger.jsonl`, `change-ledger-head.json`, `approval-report.json`, `migration-report.md`, `migration-result.json` | Indefinite |
| `PRIMARY_ARTIFACTS` | `output/<stage>/<timestamp>/*.json` | 365 days |
| `LARGE_BLOBS` | Documentation snapshots, fetched jars, build logs, Surefire dump streams | 90 days |
| `WORKSPACES` | `original/`, `runtime-old/`, `migration/`, `runtime-new/` | 7 days |

**Pruning does not hide anything.** When Bootshift prunes blobs, it does **not** write the manifest
again. `EvidenceManifest.verify()` then returns `ARCHIVED`, not `VERIFIED`, and the report states which
artifacts are not present now. If Bootshift silently removes a reference so that a later
verification looks clean, the manifest has no purpose. `TAMPERED` and `ARCHIVED` are different
verdicts for this reason: absence is not the same as alteration.

---

## 18. Data and file map

| Path | Committed? | Contents |
|---|---|---|
| `src/` | Yes | The reference corpus: a six-module Spring Boot 2.7.12 / Java 17 / Spring Cloud 2021.0.7 microservice estate. Input only |
| `policies/` | Yes | `default/` (production, production-eol-exception, development, internal-components), `ai/`, `evidence/`, `license/`, `normalization/`, `persistence/`, `retention/`, `security/`, `validation/` |
| `schemas/` | Yes | 21 JSON Schemas for published artifacts |
| `migration-rules/generated-properties/property-migration-rules.json` | Yes | 542 generated property rules. Committed so that a reviewer can compare them |
| `fixtures/` | Yes | 4 identity cases and 16 impact-evaluation cases |
| `reports/` | Yes | Recorded runs: pipeline logs, judge passes, final report, migration document, `./src` hashes, impact accuracy |
| `docs/adr/` | Yes | ADR-001 to ADR-007 |
| `.claude/agents/`, `.claude/skills/` | Yes | 21 stage contracts and the skill catalog |
| `output/<stage>/<timestamp>/` | No (git ignores it) | Published artifacts of one stage run. Immutable |
| `output/<stage>/latest.json` | No (git ignores it) | The pointer to the last complete directory of a stage |
| `output-run-*/`, `output-strict-probe/` | No (git ignores them) | Archived artifact planes of earlier runs |
| `<workspace root>/<run_id>/` | No (outside the repository) | `original/`, `migration/`, `runtime-old/`, `runtime-new/`, `internal-checkpoint-git/`, `evidence/`, `state/`, `http-cache/`, `telemetry/`, `edge-index.json` |
| `~/.bootshift/decisions/` | No (outside the repository) | The decision store (`BOOTSHIFT_DECISIONS_DIR`) |
| `<output>/validated-migration/` | No | The default target of `export` |

**Each published artifact is schema-checked before publication.** `OutputLayout.publish()` refuses to
write `latest.json` if one artifact in the directory fails validation. A failed validation is a stage
failure, not a warning. A malformed artifact damages each downstream stage that reads it.

**The envelope.** Each artifact uses one envelope, so that provenance is uniform:

```json
{
  "schema_version": "1.0.0",
  "artifact_type": "application-graph",
  "run_id": "RUN-01M2550D2MSSFRN1FJD72QAMK3",
  "stage": "03-graph",
  "produced_at": "2026-09-10T06:06:33.116Z",
  "producer": "com.bootshift.stages.stage03.ApplicationGraphStage",
  "inputs": [
    { "artifact": "inventory-artifact", "hash": "50e47c58f78a…" },
    { "artifact": "build-model", "hash": "8c1e7b09a44f…" }
  ],
  "edge_id": null,
  "environment_fingerprint": "ENV-9d31…",
  "blind_spots": [],
  "gaps": [],
  "payload": { }
}
```

`Envelope.RESERVED_KEYS` names each field that a stage must **not** shadow in its payload.
`StageSupport.compose()` throws on a collision. This found four real reserved-key defects
(`environment_fingerprint`, `run_id`, `gaps`, `edge_id`) during construction, before a downstream
consumer saw them.

**The 21 schemas**

| Group | Schema |
|---|---|
| common | `artifact-envelope` |
| inventory | `inventory-artifact` |
| file-registry | `file-registry` |
| build | `build-model` |
| graph | `application-graph`, `graph-diff` |
| symbol-registry | `symbol-registry` |
| baseline | `baseline-manifest` |
| compatibility | `lifecycle-registry` |
| migration-knowledge | `migration-knowledge` |
| impact | `impact-report` |
| characterization | `characterization-report`, `characterization-scenarios` |
| migration-plan | `migration-plan`, `edge-plan` |
| change-event | `change-event` |
| validation | `test-report`, `runtime-report` |
| differential | `differential-report` |
| evidence | `approval-report`, `migration-result` |

**Pointer-after-write**

```
output/03-graph/
├── 20260910-060633-116/          ← immutable, complete, validated
│   ├── application-graph.json
│   ├── … 16 more artifacts …
│   └── manifest.json
└── latest.json                    ← written last, by atomic move:
                                       {"directory": "20260910-060633-116", …}
```

A reader that follows `latest.json` never sees a stage that is partly written. A crash in a stage
leaves an orphan directory that no pointer refers to, and the previous `latest.json` still resolves to
a complete set.

---

## 19. Observability

**Progress output.** The CLI prints the status of each stage and the paths of its artifacts while the
run continues. Thus, you can follow a long run while it runs. Each path in this output is a published
file, because the pointer is written after the content.

**Structured telemetry.** `StructuredTelemetryAdapter` implements `TelemetryPort`. It writes one
canonical JSON line for each record to `<run workspace>/telemetry/telemetry.jsonl`. Each record has
`timestamp`, `kind` (`span.start`, `span.event`, `span.end` or `metric`), `trace_id`, `span_id`,
`run_id`, `status` and `attributes`. It supports spans, counters and gauges. Values pass through
`SensitiveValues` before emission. Logs and evidence are separate (R22): telemetry never goes into the
evidence store.

> **Note.** `StageContext` connects the telemetry adapter, but no stage calls it in the current code.
> Thus, a run writes no telemetry records today. The previous README described per-stage telemetry
> files and the counters `graph.attribution_ratio`, `knowledge.deterministic_coverage`,
> `mutation.bypass_detections`, `ledger.chain_verified`, `validation.unexplained`,
> `ai.patches_rejected` and `ai.patches_accepted`. The code does not emit these counters.
> [Known problems](#23-known-problems) lists this.

The stage artifacts contain the values that those counters were to track:

| Value | Where to read it | Why it is important |
|---|---|---|
| Graph attribution ratio | `03-graph` verification report | A drop means that the symbol solver lost the classpath |
| Deterministic coverage | `11-plan` `residual-report.json` | A drop means that rules are missing |
| Bypass detections | `12-transformation` report | Must be zero. Each other value is an integrity failure |
| Ledger chain verification | `verify` command | Must be true |
| Unexplained differences | `17-differential` `differential-report.json` | The number that blocks. If it goes up, the plan is wrong |
| AI patches accepted and rejected | `13-build-repair` report | The real hit rate of the AI, not its claimed rate |

---

## 20. How to run Bootshift

### 20.1 Prerequisites

| Need | Version | For |
|---|---|---|
| JDK | 21 | Build and run the harness (`maven.compiler.release` is 21) |
| Maven | 3.9+ | Build the harness |
| Git | 2.30+ | Snapshots, checkpoints, rename detection |
| An additional JDK | 8, 11 or 17 | Only if the application under analysis needs it. Bootshift finds it automatically |
| Network access to the allowlisted hosts | — | Maven Central metadata, BOMs, jars and Spring documentation. Use `--offline` to use only cached content |
| A rootless OCI runtime (Docker or similar), optional | — | Datastores and brokers for persistence and messaging dimensions. Without it, Bootshift records `NO_OCI_RUNTIME` |
| A local OSS model server, optional | — | AI repair proposals (`--ai`). Off by default |

The harness runs the build of the **application** on the JDK that the application needs. This is not
always the JDK that runs the harness. `ToolchainProbe` finds the installed JDKs in this sequence:

1. `BOOTSHIFT_JDK_<major>` variables, for example `BOOTSHIFT_JDK_17`. These overrides win.
2. The JDK that runs the harness.
3. `JAVA_HOME`.
4. Conventional install roots, including the macOS layout.

`JavaTargetSelector` then selects one JDK for each edge ([Section 8.3](#83-agent-06--target-resolver)).
`ToolchainProbe.knownHazard()` finds known hazards, for example Lombok 1.18.24 or earlier on JDK 21.
For such edges, it selects JDK 17.

`MavenBuildAdapter` finds Maven in this sequence: the repository wrapper, `mvn` on `PATH`, then
`MAVEN_HOME`, `M2_HOME` and `BOOTSHIFT_MAVEN_HOME`.

### 20.2 Build the harness

```bash
git clone https://github.com/KrishnaAnnavaram/bootshift.git
cd bootshift
mvn -q -DskipTests package
```

The build writes `apps/migration-cli/target/bootshift.jar`, one executable jar.

```bash
java -jar apps/migration-cli/target/bootshift.jar --help
```

The CLI command name is `bootshift`. Its alias is `bsh`. In the examples below, `bootshift` means
`java -jar apps/migration-cli/target/bootshift.jar`.

**Put the application under analysis in `./src`.**

```
bootshift/
└── src/          ← put the repository to migrate here
```

`./src/` is the default value of `--repo`. It is **input**. It is never a module of this build, and
Bootshift never writes to it. You can use each other path:

```bash
bootshift run --repo /path/to/other/repo --target auto
```

### 20.3 Run Bootshift

Run the full pipeline:

```bash
bootshift run --target auto --policy production
bsh run --target auto --policy production      # bsh is an alias for bootshift
```

`--target auto` means **the highest safe supported stable GA version** that the evidence permits
(R25). It does not mean the newest version that exists.

Run the analysis half only. Nothing changes:

```bash
bootshift run --target auto --analysis-only
```

Run with the EOL exception policy, as in the reference run:

```bash
java -jar apps/migration-cli/target/bootshift.jar run \
      --target auto --policy-file policies/default/production-eol-exception.json
```

Run one stage at a time. Each stage has its own command (R24):

```bash
bootshift inventory
bootshift resolve-build
bootshift graph
bootshift baseline
bootshift compatibility
bootshift resolve-target --target auto
bootshift documentation
bootshift knowledge
bootshift impact
bootshift characterize
bootshift plan
bootshift migrate --edge EDGE-1-PREP-TEST
bootshift validate --edge EDGE-1-PREP-TEST
bootshift approve
bootshift report
```

Record a human decision against an open gate:

```bash
bootshift approve --request REQ-SHORT_HORIZON_TARGET-A3F91C2E \
      --actor j.okafor --role "Principal Engineer, Platform" \
      --verdict APPROVED --rationale "Why this decision is made"
```

Without `--request`, `approve` raises the gates and lists the open requests. The store refuses an
empty rationale.

Inspect and export a run:

```bash
bootshift verify                       # the ledger chain and each artifact hash
bootshift evidence                     # the evidence manifest
bootshift gaps
bootshift blind-spots
bootshift lineage FILE-01M251AT6T287A1WRY6NWQTP81
bootshift explain change CHG-00007
bootshift explain impact IMP-00014
bootshift graph file FILE-01M251AT6T287A1WRY6NWQTP81
bootshift graph symbol com.aura.vihanga.employeeservice.service.EmployeeService
bootshift evaluate-impact
bootshift export --format repo --to ./validated-migration
bootshift export --format patch
bootshift export --diagnostic
```

**Export is validated by default.** `export` refuses each state except `MIGRATION_COMPLETE`. It
explains which item blocks it: unexplained differences, unexpected differences, open approvals,
evidence shortfalls or incomplete edges. A missing `final_source_tree_hash` is a failure, not a match.
Before, a null recorded hash compared as equal to each value, so a bundle could go out that nobody
checked against the validated tree. `--diagnostic` exports an incomplete run for inspection, and
labels the bundle `NOT VALIDATED` in `export-manifest.json`. If the exported tree hash and the recorded
hash do not match, `export` exits with code 3.

The export writes a CycloneDX 1.5 SBOM and a licence report. Bootshift makes them from the **final
migrated** dependency graph. It resolves that graph again from the migration workspace after the last
edge and seals it as `final-build-model.json`. An SBOM from the Stage 02 model describes the
dependency graph that the migration replaced. If Bootshift cannot resolve the migrated build
authoritatively, the `basis` field of the SBOM states this.

**Run offline.**

```bash
bootshift run --offline
```

The run uses only the content-addressed cache in `<run workspace>/http-cache/`. Facts that need a
network fetch are reported as not verifiable. Bootshift does not assume them. A fact that cannot reach
`E3` cannot authorize a change, so an offline run correctly blocks more often.

### 20.4 CLI commands

Twenty-five top-level commands plus `help`, and five nested commands under `graph` and `explain`.
`DocumentedCommandsExistTest` checks that each `bootshift` command in this README is declared by the
CLI.

| Command | What it does |
|---|---|
| `run` | The full pipeline, all twenty agents. Options `--target` (default `auto`) and `--analysis-only` |
| `stages` | Lists the stage catalog with the mutating and AI flags |
| `inventory` | Agent 01: scan, allocate `FILE_ID`s, seal the registry |
| `resolve-build` | Agent 02: authoritative build model |
| `graph` | Agent 03: build the graph |
| `graph file <FILE_ID>` | *(nested)* Each graph fact about one file |
| `graph blast-radius <FILE_ID or path> --depth N --limit N` | *(nested)* Transitive dependents with the explaining path. Defaults: depth 5, limit 40 |
| `graph symbol <fqn>` | *(nested)* Callers and callees of a symbol |
| `baseline` | Agent 04: capture and seal the baseline. Options `--skip-tests` and `--skip-runtime`. Bootshift records a skip as a blind spot, never silently |
| `compatibility` | Agent 05: lifecycle registry |
| `resolve-target` | Agent 06: resolve and freeze the landing target. Option `--target` |
| `documentation` | Agent 07: pin documentation snapshots |
| `knowledge` | Agent 08: migration facts from two channels |
| `impact` | Agent 09: impact analysis |
| `evaluate-impact` | Measure the impact precision and recall against held-out fixtures |
| `characterize` | Agent 10: behavioural contracts |
| `plan` | Agent 11: divide the path and freeze the plan |
| `migrate` | Agents 12–17 for one edge (`--edge`) or all edges |
| `validate` | Agents 15–17 for one edge (`--edge`, required), without changes |
| `approve` | Agent 18: raise gates, or record a decision (`--request`, `--actor`, `--role`, `--verdict`, `--rationale`) |
| `report` | Agents 19 and 20: evidence, coverage, reports and provenance |
| `evidence` | Inspect the evidence manifest |
| `verify` | Verify the ledger chain and each artifact hash |
| `explain` | Agent 20 provenance queries |
| `explain impact <IMPACT_ID>` | *(nested)* Why an impact finding exists |
| `explain change <CHG_ID>` | *(nested)* Why a change event was applied |
| `lineage <FILE_ID>` | Identity, split, merge, rename and delete history |
| `blind-spots` | Each item that the run could not observe |
| `gaps` | Each item that the run could not explain |
| `export` | Bundle and CycloneDX 1.5 SBOM. Options `--format repo\|patch`, `--to <dir>` (default `<output>/validated-migration`), `--diagnostic` |

### 20.5 Common options and exit codes

Each stage command accepts these options:

| Option | Meaning |
|---|---|
| `-r, --repo <path>` | The application under analysis. Default `src` |
| `--workspace-root <dir>` | External workspace root. Default `<java.io.tmpdir>/bootshift-workspaces`, or `BOOTSHIFT_WORKSPACE_ROOT` |
| `--output <dir>` | Artifact plane root. Default `output` |
| `--harness-root <dir>` | Harness root that holds `schemas/`, `policies/` and `migration-rules/`. Default `.` |
| `--policy <name>` | `production` or `development`. Default `production` |
| `--policy-file <path>` | An explicit policy JSON file |
| `--ai` | Turn on the optional local OSS AI provider. Default off |
| `--environment <managed\|delegated>` | Environment provider mode. Default `managed` |
| `--offline` | Stop all network egress. Use only cached documents and metadata |
| `--run-id <id>` | Continue a specific run, not the current one |
| `--env-attribute k=v` | Declare an environment attribute for the equivalence contract. You can use it more than one time |

| Code | Meaning |
|---|---|
| `0` | Success |
| `1` | Stage failure: something broke |
| `2` | Structured refusal: the harness does not continue on this evidence |
| `3` | Policy block: a policy forbids the only available path |
| `4` | Human decision required: approval gates are open |

[Section 15.4](#154-the-five-outcomes-and-exit-codes) tells what the operator does for each code.

### 20.6 Policies and configuration

A policy is a JSON document. Three policies are in `policies/default/`:

| Policy | Posture |
|---|---|
| `production` | Blocks each item that the harness cannot prove. No EOL landing target, no milestones, no AI, minimum support horizon 6 months, unexplained differences block |
| `production-eol-exception` | `production` with one recorded exception: `allow_eol_landing_target=true`, `minimum_support_horizon_months=-24` and `graph_attribution_floor=0.60`. The `exception` block records the rationale and the review date: "review at the next Spring Cloud GA release train". Agent 18 raises `SHORT_HORIZON_TARGET` for it |
| `development` | For harness development and fixture work. Never use it for a real migration. It permits an EOL target, AI, `WARN` for unknown internal components and lower floors. It sets `unexplained_difference_blocks=false` |

**Each setting**

| Setting | `production` | `development` | Meaning |
|---|---|---|---|
| `coverage_gate_enabled` | `true` | `true` | Coverage regression gate (R29) |
| `coverage_drop_block_percentage_points` | 5.0 | 10.0 | Unexplained coverage drop that blocks |
| `residual_no_escalation_threshold` | 0.90 | 0.90 | Deterministic coverage at or above this value does not raise the depth |
| `residual_one_level_threshold` | 0.60 | 0.60 | Coverage below this value raises the depth one level |
| `impact_recall_floor` | 0.80 | 0.60 | Impact analyzer accuracy floor, measured against held-out fixtures |
| `similarity_threshold` | 0.72 | 0.72 | File-identity reattachment rule 5 |
| `allow_milestone_targets` | `false` | `false` | Target selection (R25) |
| `allow_eol_landing_target` | `false` | `true` | Target selection (R25) |
| `minimum_support_horizon_months` | 6 | -60 | Target selection (R25) |
| `allow_checkpoint_collapse` | `true` | `true` | Checkpoint discipline (R26) |
| `unknown_internal_component_action` | `BLOCK` | `WARN` | Unknown internal components (R28) |
| `ai_allowed_in_production` | `false` | `true` | AI boundary (R11, R12) |
| `ai_max_total_attempts` | 12 | 20 | AI budget |
| `ai_max_attempts_per_root_cause` | 3 | 4 | AI budget |
| `ai_max_files_per_patch` | 3 | 5 | AI budget |
| `ai_max_changed_lines_per_patch` | 80 | 150 | AI budget |
| `repair_max_attempts_per_root_cause` | 4 | 5 | Deterministic repair budget |
| `repair_max_total_rounds` | 10 | 12 | Deterministic repair budget |
| `unexplained_difference_blocks` | `true` | `false` | R21 |
| `graph_java_coverage_floor` | 0.98 | 0.90 | Graph quality floor |
| `graph_attribution_floor` | 0.70 | 0.50 | Graph quality floor |

A value below a floor does not silently weaken the run. Bootshift raises a named gap (`GAP-GRAPH-001`
for attribution) and limits what the affected findings can claim. Unresolved relations can reach only
`POSSIBLY_AFFECTED`, never `DEFINITELY_AFFECTED`.

**Other policy files**

| File | Contents |
|---|---|
| `policies/license/license-policy.json` | Allowed SPDX identifiers, forbidden classes, licence selections |
| `policies/ai/ai-policy.json` | Permitted and forbidden AI uses, gates, budgets, prompt content rules |
| `policies/evidence/evidence-policy.json` | Evidence levels and the minimum level for each claim kind |
| `policies/normalization/normalization-policy.json` | The 11 normalization rules |
| `policies/persistence/persistence-policy.json` | The persistence comparison contract |
| `policies/retention/retention-policy.json` | Retention tiers and pruning rules |
| `policies/security/sensitive-data-policy.json` | Detectors, hashing rules, untrusted-repository controls |
| `policies/validation/validation-depth-policy.json` | Depth ladder, edge class floors, escalation triggers, test validation rules |
| `policies/default/internal-components/` | Organization starters ([Section 8.2](#82-internal-component-onboarding)). Each entry must give evidence of compatibility, not an assertion (R28) |

**Environment attributes**

```bash
bootshift run --environment delegated \
  --env-attribute database.vendor=postgresql \
  --env-attribute database.version=15.4 \
  --env-attribute jvm.vendor=temurin
```

Declared attributes go into the environment equivalence contract. If a `MUST_MATCH` attribute does
not match, the affected dimension becomes `NOT_COMPARED`. A number from a non-equivalent environment
is not evidence.

### 20.7 Environment variables

| Variable | Used by | Meaning |
|---|---|---|
| `BOOTSHIFT_WORKSPACE_ROOT` | `CommonOptions` | External workspace root. Default `<java.io.tmpdir>/bootshift-workspaces` |
| `BOOTSHIFT_DECISIONS_DIR` | `StageContext` | Decision store. Default `~/.bootshift/decisions` |
| `BOOTSHIFT_JDK_<major>` | `ToolchainProbe` | A specific JDK for a major, for example `BOOTSHIFT_JDK_17`. It wins over the other sources |
| `JAVA_HOME` | `ToolchainProbe`, child processes | One JDK source. Child processes inherit it |
| `MAVEN_HOME`, `M2_HOME`, `BOOTSHIFT_MAVEN_HOME` | `MavenBuildAdapter` | Maven candidates after the wrapper and `mvn` on `PATH` |
| `BOOTSHIFT_AI_ENDPOINT` | `RunFactory` | Local AI endpoint. Default `http://127.0.0.1:11434`. Must be loopback |
| `BOOTSHIFT_AI_RUNTIME` | `RunFactory` | AI runtime name. Default `ollama` |
| `BOOTSHIFT_AI_MODEL` | `RunFactory` | Model name. Default `qwen2.5-coder` |
| `BOOTSHIFT_AI_MODEL_LICENSE` | `RunFactory` | Declared model licence for the licence gate. Default `Apache-2.0` |
| `BOOTSHIFT_DEBUG` | `CliExceptionHandler` | If set, the CLI prints the full stack trace of an error |

Examples:

```bash
export BOOTSHIFT_JDK_17="C:\\Users\\<user>\\tools\\jdk-17"
export BOOTSHIFT_JDK_8="/usr/lib/jvm/temurin-8"
```

Internally, the build and runtime adapters receive the selected JDK as the stage option
`bootshift.javaHome`. Bootshift needs no credentials for its own operation.

### 20.8 Common faults

| Symptom | Cause | Action |
|---|---|---|
| `mvnw.cmd` is not found or cannot run | Windows wrappers are batch scripts, and `ProcessBuilder` cannot start them directly. `ProcessRunner.shellWrap()` adds `cmd.exe /c` for `.cmd` and `.bat`. A Maven wrapper resolves `.mvn/wrapper/maven-wrapper.properties` from the **current working directory** | Probe the wrapper from its own module. `MavenBuildAdapter.probeDirectory()` does this |
| `dependency:list` fails with a malformed POM | On the reference corpus, `xml-apis:xml-apis-ext:1.3.04` | The harness uses `dependency:tree` as a fallback. On the reference corpus this gives **962** records, not **530** |
| `dependency:tree` parses zero lines | Almost always CRLF. A trailing `\r` makes `Matcher.matches()` fail on each line while `find()` still seems to work | Split on `\R`, not `\n`, and use `stripTrailing()` before the match |
| `NoSuchFieldError: JCTree$JCImport.qualid` | Lombok 1.18.24 or earlier on JDK 21 | `ToolchainProbe.knownHazard()` selects JDK 17 for the affected edges. If no JDK 17 is installed, set `BOOTSHIFT_JDK_17` or install one. The harness never runs the build on an incompatible JDK silently |
| `processing of -javaagent failed` and zero test reports | Surefire 2.19.1 cannot fork with the JaCoCo agent | `ValidationSupport.runTests()` finds this in the dump stream and runs the tests again without coverage. On the reference corpus this gave **12** tests, not **5** |
| Type attribution is low | `dependency:build-classpath` failed, so the solver uses source only | Check `resolution-issues.json` in the build stage output. On the reference corpus, attribution goes from **0.6858** to **0.19** without the classpath |
| The discovery server does not start | A general `eureka.client.enabled=false` breaks a Eureka **server** | `ValidationSupport.runtimeSettings()` takes the isolation settings from what each module *is*. For a new module type, extend that method, not the global settings |
| Each result is `POLICY_BLOCK` | The strict production policy blocks EOL landing targets. At the time of writing, each Boot line with a GA Spring Cloud train is past its OSS support date | Read the elimination reasons. Use `production-eol-exception.json`, or record a decision |
| The report shows a dimension as `UNOBSERVED` | The harness works correctly | Find the blind spot id in `blind-spots.json`. Supply the missing capability (start the database, install the JDK, supply the jar) and run again |
| Stage 04 fails and the build is non-authoritative | The shell has no `JAVA_HOME`, so the `mvnw.cmd` of the project finds no JVM | Start Bootshift from a shell with a correct `JAVA_HOME`. Recorded run 2 had this problem on one aborted launch |

---

## 21. How to extend Bootshift

| You want to… | Do this | Code change? |
|---|---|---|
| Migrate a different repository | Put it in `./src` or use `--repo <path>` | No |
| Declare an organization starter | Add `<groupId>_<artifactId>.json` to `policies/default/internal-components/` with evidence | No |
| Change a gate or a budget | Make a policy file and use `--policy-file`. Do not change `production.json` for one run | No |
| Add a normalization rule | Add a rule with an id, scope, action and rationale to `normalization-policy.json`. The hash changes, and Bootshift raises `NORMALIZATION_POLICY_CHANGE` | No |
| Add a held-out impact case | Add a folder with `fixture.json` and a `tree/` to `fixtures/impact-evaluation/` | No |
| Add an adapter | Define the interface in `ports` first. Implement it in `adapters`. Connect it in `StageContext`. A stage must never import a concrete adapter type | Yes |
| Add a transformer | Write a pure, deterministic transformer that returns the intended content. Add a case in `TransformerTest`. Register the skill in `.claude/skills/README.md` | Yes |
| Add a removable no-op annotation | Add an entry to `RemovedAnnotationTransformer.NO_OP_ANNOTATIONS` with the evidence that the removal changes nothing | Yes, carefully |
| Add a stage | Follow the procedure below | Yes |

**Procedure: add a stage**

1. Make `stages/…/stageNN/` and implement `Stage`.
2. Declare the inputs with `StageSupport.upstream()` or `optionalUpstream()`. Never read the directory
   of another stage directly.
3. Build the envelope with `StageSupport.envelope()`. Compose the payload with
   `StageSupport.compose()`. It throws if the payload shadows a reserved key.
4. Write a JSON Schema in `schemas/<group>/`. Validate with `StageSupport.validate()`.
5. Register the stage in `PipelineOrchestrator` and add its CLI subcommand.
6. Write `.claude/agents/NN-<name>.md`, the stage contract.

**The one absolute rule.** If your code writes to the filesystem inside the migration workspace, it
must use `FileMutationGateway`. There is no exception. The architecture test enforces this statically,
and `detectBypass()` enforces it at runtime after each edge.

**Regex literals need care.** Java string literals that contain regexes are the most frequent source
of silent defects in this codebase. A lost backslash (`"\\s+"` that becomes `"\s+"`) is sometimes a
compile error and sometimes a *different regex that works*. When you edit a regex literal, verify the
compiled pattern, not the source line. Two real defects in this repository were of this kind:
`TREE_LINE` failed on CRLF, and `split("\\.")` became `split(".")`.

**Future work.** The project gives this sequence. The sequence is by the improvement to the evidence,
not by interest:

1. **Incremental graph rebuild, behind an equivalence proof.** Only with a test that asserts
   `INCREMENTAL_GRAPH == FULL_REBUILD_GRAPH` by structural hash across the fixture corpus. Without that
   test, the speed gives silent drift (ADR-005).
2. **Container-backed MANAGED environments.** Real PostgreSQL, MongoDB and Kafka for each side can
   move several dimensions from `NOT_COMPARED` to compared. This can close the largest blind spot on
   the reference corpus.
3. **Characterization from recorded traffic.** Contracts from captured production requests. The
   recording itself is evidence with a level and a provenance chain.
4. **Bytecode-level API differencing.** `javap` covers signatures. A comparison of method bodies can
   find the defect class in which a signature stays the same and the semantics change.
5. **Gradle parity.** The Gradle adapter resolves the model. It does not reach the Maven fidelity for
   plugin and BOM resolution, and no Gradle transformation exists.
6. **Migrations of more than one repository.** Today the unit is one repository. Contracts across
   repositories (a shared library and its consumers) need a graph that covers more than one repository.
7. **A signed evidence bundle.** The export is content-addressed and verifiable. A signature with an
   organizational key can let a third party verify provenance without trust in the exporter.

**Not planned:** autonomous approval, remote LLM inference on repository content, and confidence
scores that do not come from measured accuracy.

---

## 22. Validation results

All results below are recorded in this repository: in `reports/`, in `docs/BUILD-AND-REVIEW-REPORT.md`
or in the previous version of this README. They describe the reference corpus in `./src` and the
fixtures. They are not a general claim about other repositories.

**The harness test suite.** `tests/` has **190** test methods in 21 test classes.
`reports/BOOTSHIFT_FINAL_IMPLEMENTATION_AND_PIPELINE_REPORT.md` records **190 tests, 0 failures,
1 skipped, BUILD SUCCESS** on 2026-09-10. The skipped test is
`MutationBoundaryTest.symlinkEscapeIsRefused`. It must make a symbolic link, and Windows refuses this
without the create-symbolic-link privilege. The test skips with the reason recorded. It does not pass
without a check. The control that it covers is still in the gateway.

| Suite | Tests | What it protects |
|---|---:|---|
| `ArchitectureTest` | 16 | The hexagon: core depends on nothing, ports are interfaces, adapters never depend on stages, only the gateway writes, process execution is central, the forbidden list has one source |
| `ChangeLedgerTamperTest` | 8 | The chain finds modification, insertion, deletion, a change of sequence, truncation, and a reopen after the seal |
| `FileIdentityTest` | 12 | All five reattachment rules in sequence, split, merge and delete lineage, no id reuse, seal immutability |
| `MutationBoundaryTest` | 14 | Each change goes through the gateway. Bypass detection, scope enforcement, checkpoint-failure record, stale-proposal rejection, symlink escape |
| `MutationHardeningTest` | 10 | Budget rejection, `RENAME` destination authorization, `MERGE` source validation, scoped rollback |
| `AiBoundaryTest` | 7 | AI cannot authorize, cannot write and cannot reach a non-loopback endpoint. Each attempt is recorded |
| `TransformerTest` | 27 | POM edits, the 28 relocated and 26 preserved `javax` packages, property migration, test-framework rewrite, no-op annotation removal |
| `CoreDomainTest` | 27 | Hashing, canonical JSON, ULIDs, similarity, graph traversal, blast radius, evidence levels, state machine, artifact-directory isolation |
| `SchemaConformanceTest` | 4 | Each published artifact validates. Reserved keys are never shadowed. No plaintext secret reaches `output/` |
| `CompilerDiagnosticsTest` | 7 | javac continuation lines go into their diagnostic. The Maven framing is not a diagnostic. A real toolchain fault still is |
| `GraphHashTest` | 6 | The content hash can be compared across runs, the structural hash cannot, and both still find a real change |
| `ImpactAccuracyTest` | 5 | Accuracy is `UNMEASURED` with no fixtures. The prediction comes from the analyzer, never from the fixture. Tuning cases are excluded |
| `ImpactCorpusAccuracyTest` | 2 | Accuracy on the held-out fixture corpus |
| `MigrationPathTest` | 8 | Path construction, boundary edges, Java target selection |
| `BuildModelRoundTripTest` | 4 | `BuildModelCodec` encode and decode without loss |
| `JudgeRepairRegressionTest` | 13 | One group for each defect that the judge pass found and the repair pass corrected |
| `ControlsAreWiredTest` | 5 | Each control that the documentation claims has a real call site. Each planned recipe has a transformer |
| `DocumentedCommandsExistTest` | 2 | Each `bootshift` command in the docs is declared by the CLI. Nested commands are nested |
| `PublishedArtifactSecretScanTest` | 2 | The real credential components of the corpus are in no published artifact. Both halves of a credential pair are redacted |
| `ExecutionAndEgressBoundaryTest` | 6 | `cmd.exe` cannot start. The egress allowlist matches hosts. The keyed hash is an HMAC. An atomic write leaves no debris |
| `ArtifactChannelBudgetTest` | 6 | Packaging-only artifacts are not in the diff budget. A redirect off the allowlist is refused. A redirect loop stops. The entries of an imported BOM are resolved |

**Architecture rules are tests.**

```java
noClasses().that().resideInAPackage("..core..")
    .should().dependOnClassesThat().resideInAnyPackage("..adapters..", "..stages..");

noClasses().that().resideOutsideOfPackage("..adapters.mutation..")
    .should().callMethodWhere(writesToTheFilesystem());
```

The second rule makes "only the gateway writes" a fact about the codebase, not a convention. The rule
exempts `StageContext` and `RunFactory` as composition roots, and this exemption is explicit in the
rule.

**Why `ControlsAreWiredTest` exists.** Two capabilities in this repository were written to the
specification, had unit tests, and had no caller:

- `JavapApiDiffAdapter`. The artifact channel compared BOMs and read configuration metadata, but it
  never opened a jar, and only a jar shows a *removed type*. An edge reached the compiler with 33
  errors while the knowledge base reported 99.83% deterministic coverage.
- `detectBypass()`. It had three passing unit tests and no caller. ArchUnit enforced the single-writer
  rule statically, and nothing verified it at runtime.

No test at that time could find either problem, because both components passed their own tests. A
control with no call site is only documentation. This suite now asserts the call sites.

**What has no unit test on purpose.** The pipeline itself exercises the adapters that start Maven, Git
and the JDK, against the reference corpus. Mocks do not. A mocked `mvn` tests what the harness
believes about Maven, and R5 says not to trust that.

**Run the tests.**

```bash
mvn -q test                                      # all modules
mvn -q -pl tests test                            # the harness suite only
mvn -q -Dtest=ArchitectureTest -pl tests test
```

**The fixture corpus.**

```
fixtures/
├── README.md
├── identity/                  file identity and lineage cases
│   ├── rename-git/            a rename that git finds
│   ├── move-content-hash/     a move with identical content, no git history
│   ├── split/                 one class split into two
│   └── merge/                 two classes merged into one
└── impact-evaluation/         held-out ground truth for precision and recall
    ├── README.json
    └── <case>/fixture.json + <case>/tree/   16 cases
```

The 16 impact cases are: `ambiguous-simple-name`, `artifact-removed-from-bom`,
`enable-eureka-client-removed`, `hibernate-type-relocated`, `javax-persistence-relocation`,
`javax-preserved-package`, `javax-servlet-relocation`, `managed-version-change`,
`mockbean-deprecated`, `no-op-fact`, `reflective-usage-only`, `spring-redis-property-rename`,
`true-negative-unused-api`, `tuning-javax-annotation`, `websecurityconfigureradapter-removed` and
`yaml-property-removed`.

**Held-out impact accuracy.** `reports/impact-accuracy.json` (2026-09-10) records this result:

| Measure | Value |
|---|---|
| Held-out fixtures | 11 |
| Tuning fixtures excluded | 5 |
| True positives | 9 |
| False positives | 0 |
| False negatives | 0 |
| Precision / recall / F1 | 1.0 / 1.0 / 1.0 |

**Read this number for what it is.** Eleven held-out cases is a small sample. The result says that the
analyzer handles the failure modes that those fixtures encode. It does not say that the analyzer is
accurate in general. The artifact carries a `scope_warning` that says this. The earlier README recorded
6 held-out fixtures and 1 tuning fixture with the same perfect scores, before the corpus grew from 6
to 16 cases.

**The fixture never supplies the prediction.** It supplies a small source tree, one migration fact and
the ground truth. `AccuracyHarness` builds a file registry over that tree, runs the matcher of
`ImpactStage` and grades what the matcher returned. An earlier version read a `predicted_paths` array
from the fixture and compared it with `true_affected_paths` in the same file. That measured only that
the fixture author wrote two lists that agreed. This defect is the reason that `ImpactAccuracyTest`
exists, and why the `Fixture` record has no field for a prediction. If no held-out fixture is
present, the harness reports `UNMEASURED`, never a default or an estimate.

**The earlier reference run.** The previous README recorded this run against `./src` under
`production-eol-exception.json`. The repository holds no log file for it:

```
$ java -jar apps/migration-cli/target/bootshift.jar run \
      --target auto --policy-file policies/default/production-eol-exception.json

  00-bootstrap  [SUCCESS]
  Workspaces created under …/bootshift-workspaces/RUN-01M2550D2MSSFRN1FJD72QAMK3;
  OSS gate passed for 12 harness components

  01-inventory  [SUCCESS]
  99 files across 6 module(s); 104 migration signal(s); registry sealed as
  91b9b4ed1e141d6df29fa8a43462c21de210ff574106e3080b71791481714c2c (content 9fb5026060f8a88c)

  02-build  [SUCCESS]
  MAVEN model: 6 module(s), 962 dependency record(s), 9186 managed version(s)

  03-graph  [SUCCESS]
  839 nodes / 1852 edges, 239 symbols, attribution 0.6858, content hash 5b764c29af1e

  04-baseline  [SUCCESS]
  Baseline sealed as 1b8f054d5db20b16 (6 module(s) built, 12 test(s), 5/6 runtime probe(s)
  started)

  05-compatibility  [SUCCESS]
  Current state Spring Boot 2.7.12 / Java 17 / Spring Cloud 2021.0.7;
  9 candidate line(s), 0 unavailable, 0 unknown internal component(s)

  06-target  [SUCCESS]
  Landing target Spring Boot 3.5.16 (Java 21, Spring Cloud 2025.0.3),
  8 migration edge(s), -2 month support horizon

  07-documentation  [SUCCESS]
  15 document(s) pinned for 7 edge(s); 7 official migration guide(s)

  08-knowledge  [SUCCESS]
  1690 migration fact(s): 1683 VERIFIED, 7 CANDIDATE, 0 CONFLICTING for 2.7.12 -> 3.5.16

  09-impact  [SUCCESS]
  2072 impact finding(s) across 27 file(s); {DEFINITELY_AFFECTED=502, POSSIBLY_AFFECTED=5,
  UNAFFECTED_WITHIN_OBSERVED_COVERAGE=1565}

  10-characterization  [SUCCESS]
  517 characterization contract(s): 0 frozen, 0 mapped to existing tests, 517 awaiting OLD, 0
  unobservable

  11-plan  [SUCCESS]
  8 edge(s) frozen, deterministic coverage 0.3886, 1 differential dimension(s) required

  12-transformation  [SUCCESS]
  Edge EDGE-1-PREP-TEST: 3 applied, 0 rejected, 0 failed; ledger head 8e380a1b5b01

  13-build-repair  [SUCCESS]
  Edge EDGE-1-PREP-TEST compiles after 1 round(s); 0 AI attempt(s) used of 12

  14-graph-diff  [SUCCESS]
  Edge EDGE-1-PREP-TEST graph COMPLETE: +0/-0 nodes, +0/-0 edges,
  0 symbol(s) changed; scope OK

  15-test  [SUCCESS]
  EDGE-1-PREP-TEST: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2}

  16-runtime  [SUCCESS]
  EDGE-1-PREP-TEST: 5/6 module(s) started, 426 bound propert(ies),
  441 runtime graph edge(s)

  17-differential  [SUCCESS]
  EDGE-1-PREP-TEST: 6 comparison(s) across 1 dimension(s);
  {IDENTICAL=5, NOT_COMPARED=1}

  15-test  [SUCCESS]
  Edge EDGE-1-PREP-TEST: 12 test(s), {PASSED=10, PRE_EXISTING_FAILURE=2}

  16-runtime  [SUCCESS]
  Edge EDGE-1-PREP-TEST: 5/6 module(s) started, 426 bound propert(ies),
  441 runtime graph edge(s)

  17-differential  [SUCCESS]
  Edge EDGE-1-PREP-TEST: 6 comparison(s) across 1 dimension(s);
  {IDENTICAL=5, NOT_COMPARED=1}

  …edge 2 (PATCH → 2.7.18): 16 applied across three recipes, compiles,
    12 test(s) {PASSED=10, PRE_EXISTING_FAILURE=2}, 5/6 started, 5 IDENTICAL…

  12-transformation  [SUCCESS]
  Edge EDGE-3-MAJOR: 14 applied, 0 rejected, 0 failed; ledger head 4d8405eb6382

  13-build-repair  [SUCCESS]
  Edge EDGE-3-MAJOR compiles after 1 round(s); 0 AI attempt(s) used of 12

  14-graph-diff  [SUCCESS]
  Edge EDGE-3-MAJOR graph COMPLETE: +0/-0 nodes, +0/-0 edges, 0 symbol(s) changed; scope OK

  15-test  [POLICY_BLOCK]
  Edge EDGE-3-MAJOR: 2 blocking test or coverage finding(s)

  ! EDGE_LOCAL_REGRESSION: EmployeeControllerTest#getEmployee
      'void org.springframework.http.ResponseEntity.<init>(java.lang.Object,
       org.springframework.http.HttpStatus)'
  ! EDGE_LOCAL_REGRESSION: EmployeeControllerTest#createEmployee — same signature

  edge EDGE-3-MAJOR stopped at 15-test

exit 3
```

**Where that run stopped, and why.** The migration did **not** complete. The harness stopped at the
first edge where it could not justify the next step. Spring Framework 6 replaced
`ResponseEntity(T, HttpStatus)` with `ResponseEntity(T, HttpStatusCode)`. `EmployeeController` uses the
old form two times. The call site **still compiles**, because `HttpStatus` implements
`HttpStatusCode`. Thus, a gate that only builds passes this edge and ships a runtime failure. The test
suite found it. The classification is strict: new at this edge, absent from the sealed baseline,
explained by no `VERIFIED` fact and covered by no recorded approval. That is `EDGE_LOCAL_REGRESSION`,
and R21 blocks on it.

The run did establish these facts, verified against the tree and not the log. The application was on
`spring-boot-starter-parent` **3.0.13**. `javax.servlet` and `javax.persistence` were **fully
removed**, and the jakarta imports were in place. Deterministic transformers did all of this, with
**zero** AI attempts against a budget of twelve and no scope violations.

**How to read the numbers of that run**

- **`-2 month support horizon`.** The OSS support window of the landing target had *already closed*.
  Under the strict `production` policy, the harness gives `POLICY_BLOCK` (exit 3). The run used an
  explicit EOL exception policy, and the report states the negative horizon.
- **`5/6 runtime probe(s) started`.** One module did not start at baseline. Each value that comes from
  the runtime behaviour of that module is a blind spot, and the differential reports `NOT_COMPARED=1`.
- **`517 awaiting OLD`.** Contracts are declared before they are frozen. They freeze against observed
  OLD behaviour, not against expectations.
- **`{PASSED=10, PRE_EXISTING_FAILURE=2}`.** The two failures are MongoDB-dependent repository tests.
  They fail in the same way at the sealed baseline, with `probable_cause:
  INFRASTRUCTURE_UNAVAILABLE: MongoDB`. They are not migration regressions, and they are not hidden.
- **`0 AI attempt(s) used of 12`.** The deterministic transformers handled the edge. This is the
  intended usual case (R12).
- **`deterministic coverage`.** Read the breakdown for each fact type in `residual-report.json`. An
  earlier version reported `0.9983` for a run whose next edge gave 33 compile errors. Coverage is now
  calculated for each fact: a capability must claim the *subject* of the fact. The number measures
  rule availability for facts that the harness knows. It says nothing about whether the fact set is
  complete. The `api_diff` coverage of the artifact channel answers that second question.

The same corpus under the strict production policy:

```
$ bootshift run --target auto --policy production
…
  06-target  [POLICY_BLOCK]
  No candidate line satisfies the production policy.
    4.1  eliminated  NO_SPRING_CLOUD_TRAIN — no GA Spring Cloud train declares a
                     spring-boot-starter-parent within Boot major 4
    4.0  eliminated  NO_SPRING_CLOUD_TRAIN — (as above)
    3.5  eliminated  SUPPORT_WINDOW_CLOSED — OSS support ended 2026-06-24 (evidence: VERIFIED)
    3.4  eliminated  SUPPORT_WINDOW_CLOSED — OSS support ended 2025-12-24 (evidence: VERIFIED)
    …
  remedy: land on 3.5 with an explicit, signed EOL exception, or wait for the
          Spring Cloud 2025.1 GA train that targets Boot 4.0.

exit 3
```

This is the correct behaviour. The corpus has no landing target that agrees with the policy today.

**Recorded runs 1 and 2 (`reports/`).** After the earlier run, the project did one LLM-as-judge pass
(`reports/llm-judge-pass-1.md`, decision `REPAIR_ONCE`) and one consolidated repair pass
(`reports/judge-repair-pass.md`). Run 2 was the final verification. Nothing was judged or repaired
after it.

| Item | Run 1 (`RUN-01M267M0HBCW8KAGXQKF85WR67`) | Run 2 (`RUN-01M26B99S7GWBDP1CSDV6Q5QV6`) |
|---|---|---|
| Stages 00–11 | All `SUCCESS` | All `SUCCESS`. Build authoritative, 962 dependencies, 0 unresolved |
| Graph content hash | `5b764c29af1e` | `5b764c29af1e` (identical) |
| Baseline | — | Sealed. 12 tests, 2 failures that existed before. 4/6 modules started |
| Documents pinned | — | 21 |
| Facts | — | 1691, of which 1683 verified |
| Impact | — | 2072 findings, 27 affected files |
| Characterization | 12 scenarios left pending | 82 scenarios: 36 frozen, 46 declared gaps, 0 pending |
| EDGE-1-PREP-TEST | Complete | Complete. 82 comparisons: 36 `IDENTICAL`, 46 `NOT_COMPARED` |
| EDGE-2-PATCH | Refused: `Required state PLAN_FROZEN has not been reached` (defect J1-001) | 82 comparisons: 35 `IDENTICAL`, 46 `NOT_COMPARED`, **1 `UNEXPLAINED`** → `POLICY_BLOCK` |
| Stages 18–20 | Never ran | Never ran |
| Ledger | — | 19 entries, all `APPLIED`, all `BOOTSHIFT_DETERMINISTIC` |
| Scope violations | — | 0 on both edges |
| Toolchain | — | Frozen 17, executed `17.0.20.1`, `executed_version_verified: true` |
| `./src` hash | `f48b7888…8687b` before | Identical after (111 files) |

**The blocking finding of run 2.** Scenario `SCN-00047`, dimension `CONFIGURATION_BINDING`, module
`discovery-service`, target `/actuator/configprops`. The endpoint showed 236 property paths before and
237 after. One path appeared: `spring.cloud.loadbalancer.callGetWithRequestOnDelegates`
(`LoadBalancerClientsProperties`). The move of Spring Cloud from 2021.0.7 to 2021.0.9 with the Boot
patch made this property bindable. No verified fact named it and no decision covered it, so it was
`UNEXPLAINED`. The comparison had `plan_required_dimension: false`. It occurred only because the repair
pass made Bootshift compare each scenario that it observed on both sides. The decision store stayed
empty. Nobody approved the difference, weakened the policy or reclassified the finding.

**Judge findings and repair results**

| ID | Finding | Result in run 2 |
|---|---|---|
| J1-001 | `StageExecutor.hasReached` refused each edge after the first | **Fixed** |
| J1-002 | All 8 edges got identical facts and coverage | **Fixed**: 692–1682 facts, coverage 0.6795–0.9697 |
| J1-003 | Scenarios observed on both sides were discarded if the plan did not name their dimension | **Fixed**: 82 compared, 5 dimensions |
| J1-004 | 37 OSS libraries classified as internal by group-name prefix | **Fixed**: 0 possibly internal |
| J1-005 | Each edge planned at `FULL_DIFFERENTIAL` | **Not fixed**. The risk model does not separate edge classes |
| J1-006 | No final evidence, document or provenance graph | **Not fixed**. Stages 18–20 are gated behind edge completion |
| J1-010 | 12 scenarios pending, not declared as gaps | **Fixed**: 0 pending |

The run 2 dimensions that Bootshift compared were `HTTP_API`, `SECURITY_AUTHORIZATION`,
`SERIALIZATION`, `CONFIGURATION_BINDING` and `CONTEXT_CAPABILITY`. Seven of the twelve dimensions were
never compared. `docs/BUILD-AND-REVIEW-REPORT.md` records two earlier judge-and-fix cycles (J1-1 to
J1-8 and J2-1 to J2-18) during the construction of the harness.

---

## 23. Known problems

Read these problems before you use Bootshift on a real migration or extend it.

| # | Area | Problem | Impact and action |
|---|---|---|---|
| 1 | Migration result | No recorded run completed the migration. The latest run stopped at edge 2 of 8. The Jakarta boundary edge (33 recipes) never ran in the recorded runs | Do not treat a 3.x result as proved. Record a decision on `SCN-00047`, then run again |
| 2 | Final stages | Stages 18, 19 and 20 are written and connected, but no recorded run executed them. No sealed evidence manifest, provenance graph or export exists | Treat these stages as not verified in practice |
| 3 | Validation depth | Each edge is planned at the maximum depth (J1-005). Depth carries no information | Correct the risk model so that it separates edge classes |
| 4 | Infrastructure | No OCI daemon answered in the recorded runs. `PERSISTENCE_STATE`, `QUERY_RESULT` and `TRANSACTION_EFFECT` were never compared, and `employee-service` could not start without MongoDB | Start Docker or supply the datastores, then run again |
| 5 | Reference corpus debt | `configuaration-server` does not start, and two MongoDB tests fail, before any migration. Coverage cannot be measured for `employee-service` | These behaviours stay unverified on each edge |
| 6 | Telemetry | `StageContext` connects `StructuredTelemetryAdapter`, but no stage calls it. The counters that the previous README named are not emitted | Add span and counter calls in `StageExecutor`, or read the values from the stage artifacts ([Section 19](#19-observability)) |
| 7 | Gradle | `BuildSystemResolver` finds Gradle and mixed builds, but no Gradle transformation exists. A Gradle project is analysed and not migrated | Use Maven projects, or implement Gradle transformation |
| 8 | Attribution | Symbol attribution is 0.6858 on the reference corpus. About one third of symbol relations are not resolved, and their findings are capped at `POSSIBLY_AFFECTED`. This is below the `production` floor of 0.70, so `GAP-GRAPH-001` applies under that policy | Check the classpath capture. Accept the gap explicitly |
| 9 | Not implemented | Security and persistence graph enrichment, explicit ambiguity modelling in the symbol graph, artifact signature verification, an SBOM cross-check, content verification of AI repair requests, and one sealed tool and environment provenance record | Do not rely on these features |
| 10 | Reflection | Static analysis cannot resolve `Class.forName(config.get("handler"))`. Runtime observation recovers some of these sites, not all. The previous README described `DYNAMIC_DISPATCH` nodes, but `NodeType` has no such type | Read `UNAFFECTED_WITHIN_OBSERVED_COVERAGE` literally |
| 11 | Production traffic | Characterization comes from the graph and the test suite. A code path that only real users exercise is not visible | Add recorded-traffic characterization (future work 3) |
| 12 | Scope of the harness | It does not migrate across build systems, write missing tests or change business logic. It has no kernel-level sandbox | Run untrusted repositories in a disposable environment |
| 13 | Graph rebuild speed | Each edge builds the full graph again (ADR-005). This is correct and slow on large corpora | Accept the cost until the equivalence proof exists |
| 14 | Impact accuracy | Accuracy is measured only on 11 held-out fixtures. On a different corpus, the true figures are unknown | Read the `scope_warning`. Add held-out cases |
| 15 | Previous README | The previous README had facts that do not agree with the code: a `stages --show-toolchain` option that does not exist, `--ai <on\|off>` (the option is a flag), a `graph blast-radius --symbol` form (the command takes a `FILE_ID` or path), a cache in `~/.bootshift/cache/` (the code uses `<run workspace>/http-cache/`), node and edge type lists that are not in the enums, a `target-state` schema that does not exist, fixture folders `transform/`, `ledger/` and `schema/` that do not exist, and rule numbers that differ from the rule table | This README uses the code. Check other documents against the code |
| 16 | Rule numbers in policies | `policies/ai/ai-policy.json` uses `rule_r9`, `rule_r10` and `rule_r11` for the AI rules, and `validation-depth-policy.json` uses `rule_r14` for frozen depth. The code and [Section 3.1](#31-the-31-rules) use R11, R12, R13 and R16 | Align the policy files with the rule table |
| 17 | Hard-coded paths | `reports/*.md`, `reports/*.log` and `reports/src-*.sha256` contain absolute paths of the author machine | The paths are records. Do not use them as configuration |
| 18 | Reference corpus names | The corpus module names have spelling errors (`configuaration-server`, `sheduler-service`, package `utill`) | The names are input. Bootshift must not change them |
| 19 | Lifecycle data | The curated lifecycle table has `AS_OF` 2026-09-01. Rows older than the staleness window lose `VERIFIED` | Update `LifecycleSource` regularly. The harness becomes more careful as the data gets older |

---

## 24. Key points

1. **Twenty deterministic stages, not twenty autonomous agents.** A bootstrap conductor and seven
   phases: Initialization → Understand → Baseline → Decide → Plan → Migration edge loop → Prove.
2. **The input is a repository. Bootshift never writes to it.** All work occurs in an external
   workspace. The `./src` hash was identical before and after the recorded runs.
3. **Nothing changes before the baseline seal (R7),** and the sealed baseline cannot change (R30).
4. **One writer (R13).** Only `FileMutationGateway` writes source. ArchUnit and `detectBypass()`
   enforce this.
5. **Each attempt is in a hash-chained ledger (R14),** with the fact and the impact that authorized it.
6. **Only artifact evidence authorizes a change (R10).** Documentation gives `CANDIDATE` facts. `E3` is
   the minimum level that authorizes a change.
7. **`auto` means the highest safe supported stable GA target (R25),** not the newest. Each major
   boundary gets its own mandatory edge.
8. **Each edge is self-contained:** its own facts, Spring Cloud train, Java level and frozen
   validation depth.
9. **An unexplained difference blocks (R21).** Absence is never a pass: `NOT_COMPARED`, `UNOBSERVED`
   and `UNMEASURED` are explicit results.
10. **AI is optional, local and off by default.** It can propose. It can never authorize (R11, R12).
11. **People make the 13 gate decisions.** The harness never approves itself.
12. **Each claim has a level and a coverage statement.** The report states what the run could not see.

---

## 25. Glossary

| Term | Meaning |
|---|---|
| **Agent** | One of the 20 controlled pipeline stages (`01` to `20`), with declared inputs, outputs, preconditions and authority. Not an autonomous AI agent |
| **Stage contract** | The normative Markdown file of an agent in `.claude/agents/` |
| **Skill** | A named, licence-checked, version-pinned capability that a stage can use. It never writes |
| **Artifact** | A schema-checked JSON or Markdown file that a stage publishes in `output/<stage>/<timestamp>/` |
| **Artifact plane** | The set of published artifacts. It is the truth of a run |
| **Envelope** | The shared outer structure of each artifact, with provenance fields |
| **Pointer** | `latest.json` of a stage. It names the last complete directory |
| **Run** | One execution of the pipeline, with its own run id and workspace |
| **Workspace** | The external directory of a run, with `original/`, `migration/`, `runtime-old/` and `runtime-new/` |
| **Baseline** | The sealed observations of the original application |
| **Seal** | A hash that freezes a registry or a manifest. Bootshift cannot change it in the same run |
| **FILE_ID** | The permanent identity that Agent 01 allocates to a file. Not a path and not a hash |
| **Signal** | An inventory observation that does not make a decision |
| **Migration fact** | A statement about a version change, with a type, a status and evidence |
| **VERIFIED / CANDIDATE / CONFLICTING** | Fact status: confirmed by artifact evidence / documentation only / the channels disagree |
| **Artifact channel** | BOM diffs, existence probes, configuration metadata and the bytecode diff |
| **Documentation channel** | Facts from pinned official documents |
| **Impact finding** | A repository location that a verified fact affects, with a class and a graph path |
| **Blast radius** | The transitive dependents of a node, by reverse graph traversal |
| **Characterization scenario** | A concrete request that Bootshift runs against OLD and NEW |
| **Oracle** | The frozen observed behaviour of the original application |
| **Edge** | One step of the migration path, with a class, a target state and a frozen plan |
| **Landing target** | The final state of the path |
| **Transit checkpoint** | An edge target that is a step, not a place to stop |
| **Validation depth** | `NONE`, `BUILD`, `TESTS`, `RUNTIME` or `DIFFERENTIAL` for an edge |
| **Residual** | Verified facts that no transformer claims. A person must handle them |
| **Deterministic coverage** | The fraction of verified facts that a transformer claims |
| **Proposal** | A `ProposedChange` that a transformer or a repair makes. Only the gateway applies it |
| **Gateway** | `FileMutationGateway`, the only writer of application source |
| **Ledger** | The hash-chained, append-only record of each change attempt |
| **OLD / NEW** | The original application and the migrated application |
| **Dimension** | One kind of behaviour that Bootshift compares, for example `HTTP_API` |
| **Evidence level** | `E0` to `E5`, the strength of the evidence of a claim |
| **Coverage statement** | The statement of what a dimension covered and did not cover |
| **Blind spot** | Something that the run could not observe (`BS-…`) |
| **Gap** | Something that the run could not explain or complete (`GAP-…`) |
| **Gate** | An approval request that only a person can close |
| **Decision** | A stored human verdict (`APPROVED`, `REJECTED` or `DEFERRED`) on a gate, with an actor and a rationale |
| **Policy** | A JSON document with the gates, floors and budgets of a run |
| **MANAGED / DELEGATED** | Environment modes: the harness makes the environment / CI or infrastructure supplies it |
| **Reference corpus** | The six-module Spring Boot application in `./src` |
| **Problem** | A known fault or limit in Bootshift itself ([Section 23](#23-known-problems)) |

---

## 26. License

Bootshift is under the **MIT License**. The full text is in [LICENSE](LICENSE), and that file is
authoritative. Copyright (c) 2026 KrishnaAnnavaram.

MIT is on the allowlist of the harness in `policies/license/license-policy.json`. Thus, the OSS gate
that runs before each stage covers this project as well as its dependencies. The `export` command
writes a CycloneDX 1.5 SBOM with the evidence bundle. [Section 16.2](#162-runtime-dependencies) lists
each runtime dependency and its licence.

`./src/` is **not** part of this project, and this licence does **not** cover it. It is input that
the person who runs the harness supplies. Bootshift does not redistribute it or change it in place.
