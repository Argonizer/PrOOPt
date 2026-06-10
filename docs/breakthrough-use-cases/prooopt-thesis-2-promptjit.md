# Thesis 2 — PromptJIT

## The self-cheapening AI runtime: a profile-guided JIT compiler for cognition

> *HotSpot made Java fast by noticing what runs often and compiling it down.
> PromptJIT makes AI cheap the same way — except what gets compiled is thought.*

---

## The claim

Every production AI system today has a property so universal nobody questions it:
**constant marginal cost**. The 10,000th near-identical request costs the same tokens,
the same latency, and the same cloud invoice as the first. The industry's answers —
token-level prompt caching, cheaper model tiers, distillation — attack the unit price,
not the structure. Distillation comes closest but requires training pipelines, GPU
budgets, and weeks of iteration, and produces an opaque artifact.

PrOOPt already contains, scattered across its feature set, every component of a far
more radical answer: a runtime in which **the cost of serving a request decays as a
function of how often the system has seen its kind** — asymptotically approaching the
electricity cost of local inference, and in the limit, the cost of running plain Java.

The components:

- a **semantic/intent plan cache** (`PlanCacheStrategy.SEMANTIC` / `INTENT`) that
  already detects "we have seen this kind of request before" — i.e., a *hot-path
  profiler for cognition*;
- **dynamic prompt functions** (`allowDynamic = true`) in which a cloud model already
  *writes new functions at runtime* when a capability gap is detected — i.e., a
  *code generator*;
- a **tier router** (`LOCAL` / `CLOUD_FAST` / `CLOUD_ADVANCED` / `AUTO`) — i.e., a
  choice of *execution engine* per function;
- an **autoboxer + `SchemaGenerator`** that validate model output against Java types —
  i.e., a *verification oracle* that can detect when a cheap execution failed.

PromptJIT composes them into the missing whole: **a profile-guided, tier-descending
optimising compiler for prompts**, where `CLOUD_ADVANCED` is the optimising compiler,
`LOCAL` is the CPU, and autoboxing failures are deoptimisation traps. This is the
user-visible promise: *the more you use it, the cheaper it gets — provably, in the
audit log.*

---

## Why this is unprecedented

The JIT analogy is not decoration; it is an exact structural correspondence, and no
framework has built it because no framework holds all four components in one governed
runtime:

| JVM HotSpot | PromptJIT |
|---|---|
| Bytecode interpretation | `DYNAMIC` plan mode — cloud model plans every request from scratch |
| Invocation counters find hot methods | `PlanCache` hit statistics + `INTENT` clustering find hot request families |
| C2 compiles hot paths to native code | `CLOUD_ADVANCED` compiles hot intents into: plan templates, rule-prompts for `LOCAL`, few-shot exemplars |
| Native execution | `LOCAL` ONNX inference following the compiled rules — zero cloud cost |
| Speculative optimisation + guards | Compiled rule applied only when input similarity ≥ `planCacheSimilarityThreshold` |
| **Deoptimisation trap** | `PrOOPtAutoBoxException` / schema-validation failure → fall back to cloud re-planning, recompile with the failure as a counter-example |
| Tiered compilation (C1 → C2) | Tier descent: `CLOUD_ADVANCED` → `CLOUD_FAST` → `LOCAL`+rules → `@CodeFunction` |
| Profile pollution / recompilation | Drift detection: rising deopt rate on an intent triggers recompilation against recent traffic |

Anthropic's and OpenAI's prompt caching caches **tokens** — the prefix bytes of a
conversation. PromptJIT caches and recompiles **strategy**: what to do, in what order,
with which tools, under which rules. Distillation transfers capability by gradient
descent over weeks into an unauditable artifact; PromptJIT transfers capability by
*writing legible instructions* — in minutes, reviewable in a diff, reversible by cache
eviction (`runtime.clearPlanCacheFor("…")`). It is **distillation without gradient
descent**, performed at the prompt-and-plan layer, with the governance story intact.

The terminal rung is the most audacious: when an intent's compiled rule has executed
thousands of times with zero deoptimisations and its logic is fully mechanical, the
compiler emits a *candidate `@CodeFunction`* — generated Java, submitted as a pull
request, never hot-loaded. Intelligence that began as frontier-model reasoning ends as
compiled, tested, deterministic code. The system literally **crystallises cognition
into software**.

---

## Architecture

### The cognition de-escalation ladder

Every request family migrates down this ladder over its lifetime:

```
 novelty                                                            maturity
 ───────────────────────────────────────────────────────────────────────────▶

 CLOUD_ADVANCED          CLOUD_FAST            LOCAL + compiled        @CodeFunction
 full planning,          cached plan,          rules                   (via human-
 thinking=true           cheap execution       zero cloud cost         reviewed PR)
 ~$0.02/req              ~$0.002/req           ~$0.0000/req            $0, deterministic
```

### The runtime loop

```java
@PromptOrchestrator(
        prompt = "Resolve the customer-operations request using registered tools.",
        planMode = PlanMode.STATIC,                       // plans are compilable artifacts
        planCacheStrategy = PlanCacheStrategy.INTENT,     // cluster by intent, not bytes
        planCacheSize = 2000,
        planCacheTtl = -1,                                // evicted by drift, not time
        allowDynamic = true,                              // capability gaps get filled…
        dynamicFunctionModel = ModelTier.CLOUD_FAST,      // …by a cheap generator
        maxDynamicFunctions = 3
)
public Resolution handle(String request) { return null; }
```

Around this orchestrator, PromptJIT adds a **compiler daemon** — itself a PrOOPt
program — that runs on a schedule:

```java
public class PromptJitCompiler {

    @CodeFunction(description = "Scan the audit log for the window and compute, per "
            + "intent cluster: volume, tier mix, mean cost, deopt (autobox-failure) "
            + "rate, and cache hit ratio.")
    public List<IntentProfile> profile(Duration window) { /* deterministic log analytics */ }

    @PromptFunction(model = ModelTier.CLOUD_ADVANCED, thinking = true,
            prompt = "You are an optimising compiler. For hot intent {profile}, study "
                   + "these successful cloud-handled traces: {exemplars}. Write a "
                   + "rule-prompt that a small local model can follow to handle this "
                   + "intent: explicit steps, tool names, output schema, and the edge "
                   + "cases below which it must refuse and escalate: {deoptCases}")
    public String compileRuleForLocal(IntentProfile profile, String exemplars,
                                      String deoptCases) { return null; }

    @CodeFunction(description = "Run the compiled rule against the golden replay set "
            + "on the LOCAL tier; promote only if pass-rate >= 0.98 and autobox "
            + "failures == 0; otherwise file for recompilation with failures attached.")
    public PromotionVerdict verifyAndPromote(String candidateRule, String intent) { /* … */ }
}
```

Three details make this a *compiler* rather than a cache:

1. **Counter-example-driven recompilation.** Every deoptimisation (an
   `PrOOPtAutoBoxException`, a schema mismatch, an escalation) is logged with its
   trace. The nightly compile feeds exactly those failures back into
   `compileRuleForLocal` — profile-guided optimisation in the literal sense. Rules get
   *monotonically sharper about their own blind spots*, and the refusal clause means a
   compiled rule knows when to throw itself away and escalate.

2. **Guarded speculation.** A compiled rule serves an input only when the semantic
   cache deems it inside the intent's similarity threshold. Outside the guard, the
   request falls back up the ladder — the system is never *wrong quietly*; it is
   either fast or deliberate, exactly like speculative JIT code behind a type guard.

3. **Promotion gates with replay verification.** `verifyAndPromote` replays a golden
   set deterministically before any rule reaches production traffic — and the final
   rung (generated `@CodeFunction` Java) is gated by a human pull-request review.
   Governance never thins as cost falls.

### What the audit log shows

PrOOPt's existing audit summary line carries `estCost` per trace. PromptJIT's signature
artifact is therefore already built: a **cost-decay curve straight from the audit log**.

```
week 1:  [SUMMARY] intent=refund-status  mode=DYNAMIC cloud_calls=2 estCost=$0.0214
week 2:  [SUMMARY] intent=refund-status  cached=true  cloud_calls=1 estCost=$0.0019
week 5:  [SUMMARY] intent=refund-status  cached=true  cloud_calls=0 tier=LOCAL estCost=$0.0000
week 9:  intent=refund-status  →  promoted to RefundStatusFunctions.java (PR #214, merged)
```

---

## The optimisation engineering

- **The user-visible pattern is exactly "cloud writes rules, local follows them"** —
  but industrialised: rules are versioned, replay-tested, guarded, and audited. The
  `DynamicFunctionCache` provides the in-session version of this reflex (gap → cloud
  writes function → session uses it); PromptJIT extends it across sessions with
  persistence and promotion gates, which is precisely the maturity step the
  session-scoped design anticipates.
- **`AUTO` tier as the safety net**: unprofiled traffic routes by estimated token
  count, so even cold paths are never naively expensive.
- **Embedding LRU + compiled `PromptTemplate`s** keep the hot path allocation-light;
  a warm LOCAL hit performs zero reflection (`FunctionRegistry` metadata caches) and
  zero cloud I/O.
- **Wave-parallel DAG execution on virtual threads** means even multi-step compiled
  plans execute at in-process speeds; the JIT reduces *steps that need a model* as well
  as *which model*.
- **Economic instrumentation is free**: the integral between the week-1 cost line and
  the current cost line — *dollars not spent* — is computable from logs the framework
  already writes.

---

## Why it is breathtaking

There is a moment in every demo of this system that lands like a magic trick: you
handle a request live, see `estCost=$0.0214`, handle twenty similar ones, re-run the
compiler, handle the twenty-first — and the audit line reads `cloud_calls=0
estCost=$0.0000` *with the same answer quality, verified by replay*. The system did not
get cheaper because anyone optimised it. It got cheaper **because it was used**.

Economically, this redraws the AI deployment map. Today, unit economics gate LLMs out
of high-volume/low-value workflows (logistics exceptions, claims triage, KYC checks,
internal ticketing) — precisely the workflows enterprises run at millions of events per
month. A runtime whose marginal cost decays toward zero makes those workflows viable
not by negotiating token prices but by *structurally needing fewer tokens every week*.

Intellectually, it demonstrates something the field talks about and does not build:
**experience compilation**. The frontier model's role shifts from *answering questions*
to *teaching a cheaper system how to answer them* — with the teaching materialised as
legible, diffable, revocable text artifacts rather than weights. And because every rung
of the ladder is a PrOOPt annotation, the entire learning loop remains inside the
governance regime: every compiled rule is audited, every promotion is gated, every
deoptimisation is on the record. Self-optimising AI with a paper trail — the
combination is the breakthrough.

---

## Measurable success criteria

1. **Cost-decay curve**: ≥90% reduction in mean cost-per-request on the top 20 intent
   clusters within 8 weeks at production volume, measured purely from audit logs.
2. **Quality floor**: compiled-rule answers match cloud-tier answers on the golden
   replay set ≥98%, with deoptimisation (not silent degradation) covering the
   remainder.
3. **Deopt rate trend**: per-intent deoptimisation rate falls monotonically across
   recompilations (evidence the counter-example loop works).
4. **Crystallisation count**: number of intents promoted all the way to merged
   `@CodeFunction` PRs — cognition converted into code, the ultimate compression.
5. **Throughput/latency**: p50 latency for hot intents at LOCAL tier under 300 ms on
   CPU-only hardware (no GPU in the serving path).

## Honest limits

- The ladder's bottom rungs only fit *recurring, structured* work; genuinely novel or
  open-ended requests will (correctly) live at the cloud tiers forever. PromptJIT's
  promise is about the distribution's fat head, which in operations workloads is most
  of the volume.
- Compiled rules inherit the LOCAL model's ceiling; the promotion gate, not optimism,
  decides what it can carry.
- Intent drift is the moral equivalent of profile pollution — the drift detector and
  TTL-by-evidence eviction are mandatory components, not nice-to-haves.
