# The writing standard: ASD-STE100 Simplified Technical English

Use these rules for every README and for `docs/ste-style-guide.md` in each repository. Copy this file
into the repository as `docs/ste-style-guide.md` and add a **project vocabulary** section (Section 3)
with the technical names and technical verbs of that project.

## 1. The writing rules

### Words

1. Use one word for one meaning, and one meaning for one word. Do not use synonyms for variety.
2. Use a word only as one part of speech. For example, `test` is a noun or a verb, `check` is a verb.
3. Do not use phrasal verbs (`set up`, `carry out`, `find out`, `pick up`, `look up`, `come up with`).
   Use one verb: `prepare`, `do`, `find`, `get`, `make`.
4. Do not use an `-ing` form as a noun or an adjective (`the running job`, `after indexing`).
   Exception: a technical name, a file name, a command or a status value.
5. Do not use contractions (`don't`, `it's`, `can't`). Do not use slang or idioms
   (`out of the box`, `under the hood`, `at a glance`, `gotcha`, `bells and whistles`).
6. Do not use `and/or`. Write `A, B or both`.
7. Do not use `should`, `could`, `would` or `may` for instructions. Use `must` for a rule, the
   imperative for a step and `can` for a possibility.
8. Keep the articles `a`, `an` and `the` in sentences.
9. Do not make a noun cluster of more than three words. A technical name is one word.

### Sentences

1. A procedural sentence (an instruction) has a maximum of **20 words**.
2. A descriptive sentence has a maximum of **25 words**.
3. Write one instruction in one sentence.
4. Use the imperative for an instruction: `Run the tests.` Not `The tests should be run.`
5. Use the active voice. Use the passive voice only when the agent of the action is not important.
6. Use only the simple present, the simple past and the simple future.
7. Put a condition before the instruction: `If the index is stale, build it again.`
8. Do not use semicolons in sentences. Write two sentences.

### Paragraphs, notes and warnings

1. A paragraph has one topic and a maximum of **6 sentences**. Start with the topic sentence.
2. A warning or a caution starts with a clear command. Then it gives the reason.
3. A note gives information. It does not give an instruction.
4. Use a vertical list for a sequence or a set of conditions. Each item of a numbered procedure is one step.

### Tables, headings and diagrams

1. A table cell can be a short phrase. If a cell has a sentence, the sentence obeys the rules.
2. A heading is a noun phrase (`The cost model`) or an imperative (`Run the demo`).
   Do not start a heading with an `-ing` form.
3. A diagram label is a short phrase. Use the same terms as the text.

### What STE does not change

Code, commands, file names, paths, field names, environment variables, status values, enum values,
product names and URLs stay exactly as they are. They are technical names. Put them in backticks.

## 2. General words to replace

| Do not use | Use |
|---|---|
| utilize, leverage | use |
| in order to | to |
| set up | prepare, install, configure |
| carry out, perform | do |
| make sure, ensure | make sure (allowed), or `check that` |
| a lot of, lots of | many, much |
| e.g., i.e. | for example, that is |
| should (instruction) | must (rule) / imperative (step) |
| might, may (possibility) | can |
| very, really, just, simply, easily | (delete) |
| seamless, robust, powerful, blazing | (delete or give a measured fact) |

## 3. Project vocabulary

This section gives the technical names and the technical verbs of Bootshift. The README uses each term with only this meaning.

### 3.1 Technical names (nouns)

| Term | Meaning | Do not use |
|---|---|---|
| **agent** | One of the 20 controlled pipeline stages (`01` to `20`). It is not an autonomous AI agent | bot, worker |
| **stage** | The Java class of an agent, or its output folder `output/<stage>/` | step (except in a procedure), job |
| **stage contract** | The normative file of an agent in `.claude/agents/` | prompt, persona |
| **skill** | A named, licence-checked capability that a stage can use. It never writes | plugin, extension |
| **run** | One execution of the pipeline, with one run id | session, job |
| **workspace** | The external run directory with `original/`, `migration/`, `runtime-old/`, `runtime-new/` | sandbox, temp folder |
| **artifact** | A schema-checked file that a stage publishes | output file, result |
| **artifact plane** | The set of published artifacts. It is the truth of a run | database, store |
| **pointer** | The `latest.json` file of a stage | link, symlink |
| **baseline** | The sealed observations of the original application | snapshot (except the source snapshot), reference |
| **seal** | A hash that freezes a registry or a manifest | lock, signature |
| **FILE_ID** | The permanent identity of a file | file key, file hash |
| **signal** | An inventory observation that makes no decision | finding (for inventory) |
| **migration fact** | A statement about a version change, with a status and evidence | rule, knowledge item |
| **impact finding** | A location that a verified fact affects | hit, match |
| **edge** | One step of the migration path | hop, stage (for a path step) |
| **landing target** | The final state of the path | destination, goal |
| **transit checkpoint** | An edge target that is a step, not a place to stop | waypoint |
| **validation depth** | `NONE`, `BUILD`, `TESTS`, `RUNTIME` or `DIFFERENTIAL` | test level, rigor |
| **proposal** | A `ProposedChange` from a transformer or a repair | patch (before the gateway applies it), suggestion |
| **gateway** | `FileMutationGateway`, the only writer | writer service |
| **ledger** | The hash-chained record of each change attempt | log, history file |
| **scenario** | A concrete request that Bootshift runs against OLD and NEW | test case, probe description |
| **oracle** | The frozen observed behaviour of the original application | expected result (from code) |
| **dimension** | One kind of behaviour that Bootshift compares, for example `HTTP_API` | aspect, area |
| **evidence level** | `E0` to `E5` | confidence, grade |
| **coverage statement** | What a dimension covered and did not cover | summary |
| **blind spot** | An item that the run could not observe (`BS-…`) | unknown, limitation |
| **gap** | An item that the run could not explain or complete (`GAP-…`) | issue, hole |
| **gate** | An approval request that only a person can close | check, approval step |
| **decision** | A stored human verdict on a gate | approval (for the record), sign-off |
| **residual** | Verified facts that no transformer claims | leftovers, remaining work |
| **policy** | A JSON document with the gates, floors and budgets of a run | config, profile |
| **reference corpus** | The six-module application in `./src` | sample app, demo |

### 3.2 Technical verbs

| Verb | Meaning |
|---|---|
| **allocate** | Give a new `FILE_ID` to a file |
| **reattach** | Give an existing `FILE_ID` to a file at a new path or with new content |
| **seal** | Freeze a registry or a manifest with a hash |
| **resolve** | Get the effective value from the build tool or from published artifacts |
| **verify** | Confirm a fact with artifact evidence, or confirm a hash chain |
| **characterize** | Run scenarios against the original application and freeze the results |
| **freeze** | Store a value in the plan so that no later step can change it |
| **propose** | Calculate the intended content of a file, without a write |
| **apply** | Write a proposal through the gateway |
| **classify** | Give a finding, a test result or a comparison its class |
| **normalize** | Change an observation with the versioned normalization rules |
| **compare** | Find the differences between an OLD and a NEW observation |
| **raise** | Open a gate, a gap or a blind spot |
| **publish** | Validate the artifacts of a stage and move its pointer |
| **export** | Write the validated bundle and the SBOM |
