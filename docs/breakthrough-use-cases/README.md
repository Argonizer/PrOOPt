# PrOOPt Breakthrough Use Cases — Thesis Collection

This collection assesses PrOOPt's unique capabilities and proposes six breakthrough
projects — three built on **PrOOPt core** and three built on **PrOOPt States** — each
developed as a full thesis in its own document.

---

## What makes PrOOPt different (the raw material)

Any breakthrough use case must be *impossible or impractical* without PrOOPt's specific
primitives. The assessment below identifies the load-bearing capabilities.

### PrOOPt core

| Capability | Primitive | Why it matters |
|---|---|---|
| **JVM-enforced authority zones** | `@CodeFunction` / `@PromptFunction(model = LOCAL \| CLOUD_FAST \| CLOUD_ADVANCED)` | "What the AI may do" becomes a compile-time property, not a policy document |
| **Closed-book reasoning** | All arithmetic/deterministic logic locked in `@CodeFunction` | The LLM cannot interpolate an answer; it must reason toward one |
| **Knowledge injection layer** | `@CodeFunction` registry as tools | Proprietary models (never in any training corpus) become first-class reasoning instruments |
| **Plan compilation & caching** | `planMode = STATIC`, `PlanCacheStrategy.{EXACT, SEMANTIC, INTENT}`, `PlanInstantiator` | One expensive cloud plan amortised across thousands of executions |
| **Dynamic prompt functions** | `allowDynamic = true`, `DynamicFunctionCache` | The cloud model *writes new capabilities* on gap detection — session-scoped, budget-capped, audited |
| **Provable data custody** | `ModelTier.LOCAL` (ONNX in-JVM) + `@SensitiveData` masking | Sensitive data demonstrably never crosses the machine boundary; audits stay clean |
| **Structured audit trail** | `io.github.argonizer.prooopt.audit` logger, trace ids, per-run cost estimates | Every decision reconstructable — the compliance artifact is a by-product of execution |

### PrOOPt States

| Capability | Primitive | Why it matters |
|---|---|---|
| **Natural language as schema** | `@Persona("…")`, `@Trait("…")` — *the description is the rule* | State-transition logic is declared in prose and executed by an LLM, versioned like code |
| **Population-scale state** | `generate().limit(n).distribute(Segment…)`, `loadWhere("…")`, `broadcast(…)` | Thousands of LLM-managed entities, queryable like a database |
| **Time as a first-class force** | `evolutionSchedule`, `evolutionDescription` | Entities drift, age, and decay on cron schedules — without external stimulus |
| **Autonomous introspection** | `internalLoop`, `LoopDepth.{SHALLOW, MODERATE, DEEP}`, quiet periods | Entities consolidate experience and revise beliefs while the system is idle |
| **Social physics for events** | `EventPipeline` + `BlastRadius.{WITNESSED, FACTION, RELATIONSHIP, PROXIMITY}` + `AttenuationModel.RUMOUR` | Information propagates, attenuates, and *distorts* across a population — natively |
| **Longitudinal analytics** | `EvolutionReport`: `InflectionPoint`, `TraitCorrelation`, `ForwardProjection`, `TrajectoryAssessment` | Every entity's full history is analysable: when it changed, why, and where it's heading |
| **Reactive governance** | `@OnPersonaEvent`, `PersonaMetric` thresholds, `PersonaUpdateVetoException` | Deterministic Java reacts to — and can veto — LLM-driven state change |

### The shared optimisation vocabulary

A pattern recurs across all six theses, and it is PrOOPt's most under-exploited idea:

> **Cloud models as compilers, local models as CPUs.**
> `CLOUD_ADVANCED` is invoked rarely — to *write* plan templates, rule-prompts, and
> population rules. `LOCAL` (ONNX, in-JVM, zero marginal cost) is invoked constantly —
> to *follow* them. Frontier intelligence enters the system as reviewed, cached,
> versioned artifacts rather than per-request API calls. The marginal cost of
> intelligence decays toward the cost of electricity.

Supporting techniques used throughout: `STATIC` plan mode with `INTENT` caching,
the `AUTO` tier router, escalation ladders (LOCAL → CLOUD only on threshold/inflection
events), blast-radius fan-out bounding, quiet-period scheduling, and `@CodeFunction`
pre-digestion so models only ever see cheap, structured summaries.

---

## The six theses

### PrOOPt core

| # | Thesis | One-line claim |
|---|---|---|
| 1 | [**Iron Interlock** — governed conversational autonomy for safety-critical industrial control](prooopt-thesis-1-iron-interlock.md) | The first credible safety case for an LLM inside the control loop of physical infrastructure — because the safety case is the architecture |
| 2 | [**PromptJIT** — the self-cheapening AI runtime](prooopt-thesis-2-promptjit.md) | A profile-guided JIT compiler for cognition: every repeated request makes the system cheaper, until intelligence costs almost nothing |
| 3 | [**The Boundary Refinery** — a reasoning copilot for proprietary science](prooopt-thesis-3-boundary-refinery.md) | Frontier models reasoning *on top of* knowledge they can never contain, with proof the knowledge never left |

### PrOOPt States

| # | Thesis | One-line claim |
|---|---|---|
| 4 | [**The World That Remembers** — persistent, society-scale NPC populations](states-thesis-1-living-world.md) | The first game world with memory, gossip, and an audit trail — a society as a database |
| 5 | [**The Policy Wind Tunnel** — synthetic societies for decision rehearsal](states-thesis-2-policy-wind-tunnel.md) | Wind-tunnel testing for policies, prices, and messages — including the epidemiology of misinformation — on populations that cost nothing to convene |
| 6 | [**Machine Diaries** — anthropomorphic observability for distributed systems](states-thesis-3-machine-diaries.md) | Every service keeps a diary: qualitative, longitudinal, queryable state for machine fleets — the biography layer observability never had |

Each thesis follows the same structure: the claim, why it is unprecedented, the
architecture mapped to real PrOOPt APIs, the optimisation engineering, what success
measures look like, and an honest account of limits.
