# PrOOPt — Object-Oriented Prompt Engineering for Java

PrOOPt brings LLM calls under the same engineering discipline as ordinary Java code: scoped at the
function level, governed by annotations, and observable through a structured audit trail.

## Core idea

Every function in your codebase declares its own authority:

| Zone | Annotation | Execution | Example use |
|---|---|---|---|
| **Deterministic** | `@CodeFunction` | Pure Java | Date arithmetic, text normalisation |
| **Bounded AI** | `@PromptFunction(model = LOCAL)` | On-device (ONNX Runtime) | Date extraction, classification |
| **Elevated AI** | `@PromptFunction(model = CLOUD_ADVANCED)` | Cloud API | Contract summarisation |

The JVM, not the model, enforces which tier a function may use.

## Model tiers

| `ModelTier` | Meaning |
|---|---|
| `LOCAL` | On-device inference via ONNX Runtime — nothing leaves the JVM |
| `CLOUD_FAST` | Cloud API (cheap/fast model, e.g. Haiku / GPT-4o-mini) |
| `CLOUD_ADVANCED` | Cloud API (flagship model, e.g. Claude Opus / GPT-4o) |
| `AUTO` | Router selects tier based on estimated token count |

## Autoboxing

`PrOOPtAutoBoxer` converts raw model text to any Java type with lenient heuristics:

- **Primitives** — strips commas, strips trailing `.0`, extracts first number from prose
- **Boolean** — `yes/no`, `true/false`, `1/0`, case-insensitive
- **Enums** — case-insensitive, scans prose for the first matching constant name
- **`java.time`** — ISO-8601 parsing for `LocalDate`, `LocalDateTime`, `ZonedDateTime`
- **Collections / Maps** — Jackson JSON, with markdown fence stripping
- **POJOs** — Jackson deserialization after fence stripping

## Two-phase orchestration

```
User input
   │
   ▼ Phase 1 — Discovery (LOCAL model)
List<String> capabilities
   │
   ▼ Phase 2 — Matching (TF-IDF cosine similarity)
List<ToolDescriptor> (≤ maxTools)
   │
   ▼ Phase 3 — Planning (CLOUD_FAST model)
ExecutionPlan (JSON)
   │
   ▼ Wave-parallel execution
Result
```

Plans are memoised by input hash — identical requests skip discovery and planning entirely.

## PromptStream API

```java
String summary = PromptStream.of(contractText)
        .pipe(proxied::normalizeWhitespace)   // @CodeFunction
        .pipe(proxied::generateSummary)        // @PromptFunction CLOUD_ADVANCED
        .withTimeout(5_000)
        .execute();
```

## Quick start

```java
LegalAnalyzer analyzer = new LegalAnalyzer();

PrOOPtRuntime prooopt = PrOOPt.builder()
        .router(new CloudModelRouter(anthropicAdapter))
        .registerInstance(analyzer)
        .build();

Object result = prooopt.orchestrate(analyzer, contractText);
```

## Sensitive data

```java
@PromptFunction(model = ModelTier.CLOUD_ADVANCED, ...)
public String generateSummary(
        @SensitiveData(label = "***CONTRACT-TEXT***") String text) {
    return null; // PrOOPt supplies the result
}
```

Audit logs replace the argument with the label; the model still receives the real value.

## System requirements

| Resource | Requirement |
|---|---|
| **Java** | 17+ (ONNX Runtime has no preview API requirement); the project baseline is **Java 21** because the core orchestrator uses virtual threads |
| **RAM** | LOCAL tier: 3 GB minimum (Phi-3.5 Mini INT4 ≈ 2 GB model + JVM). CLOUD-only: 512 MB minimum |
| **GPU** | Not required (CPU inference via ONNX Runtime) |
| **OS** | Any (x86-64 or ARM64) — ONNX Runtime native libs are auto-selected |
| **Maven** | 3.9+ |

## Modules

| Module | Artifact | Purpose |
|---|---|---|
| `prooopt-core` | `prooopt-core` | Annotations, AOP, autoboxer, orchestrator, stream API |
| `prooopt-runtime-local-java17` | `prooopt-runtime-local-java17` | ONNX Runtime on-device inference (Java 17+) |
| `prooopt-runtime-cloud` | `prooopt-runtime-cloud` | Anthropic + OpenAI cloud adapters |
| `prooopt-example` | `prooopt-example` | `LegalAnalyzer` worked example |

## Audit log sample

```
[PROOOPT][PROMPT_FUNCTION][START] traceId=abc123 function=generateSummary tier=CLOUD_ADVANCED inputs={text=***CONTRACT-TEXT***}
[PROOOPT][PROMPT_FUNCTION][END]   traceId=abc123 function=generateSummary durationMs=842 result=This mutual non-disclosure...
[PROOOPT][ORCHESTRATOR][SUMMARY]  traceId=abc123 totalMs=1204 functions=5 code=2 local=2 cloud=1 tokens=~312 estCost=$0.0020
```

Logs are written by the `io.github.argonizer.prooopt.audit` logger (additivity=false) with daily + 50 MB rotation.

## Credentials

API keys are never hardcoded. Set them as environment variables or system properties:

```yaml
# prooopt.yaml
models:
  cloud:
    apiKey: ${ANTHROPIC_API_KEY}
```

## Plan modes — STATIC vs DYNAMIC

`@PromptOrchestrator(planMode = ...)` controls whether the execution plan is cached and reused.

| Mode | Cloud plan-generation calls | Best for |
|---|---|---|
| `STATIC` (default) | 1 total (first call) | Batch/repetitive workloads — 10,000 loan applications share one plan |
| `DYNAMIC` | 1 per call | Conversational agents, genuinely varied inputs |

Under `STATIC`, the cached artifact is a **plan template** (`${input}` placeholders retained);
`PlanInstantiator` binds the live input on each run and assigns a fresh trace id. A warm hit skips
discovery and planning entirely:

```
[PROOOPT][PLAN_CACHE][HIT] key='Analyze this loan…' similarity=1.00 → skipping Cloud LLM
[PROOOPT][ORCHESTRATOR][SUMMARY] trace=… mode=STATIC cached=true plan_generation=0ms cloud_calls=1 …
```

### Cache strategies (`planCacheStrategy`)

| Strategy | Key | Reuse |
|---|---|---|
| `EXACT` | SHA-256 of trimmed input | Byte-identical input only |
| `SEMANTIC` (default) | Input embedding | Cosine similarity ≥ `planCacheSimilarityThreshold` (0.85) |
| `INTENT` | LOCAL-model intent label | All inputs of the same intent |

Cache is LRU-bounded (`planCacheSize`, default 500), TTL-expiring (`planCacheTtl`, seconds, `-1` =
never), and invalidated automatically when a new function is registered. Manual control:

```java
runtime.clearPlanCache();
runtime.clearPlanCacheFor("extractSigningDate");
```

## Dynamic prompt functions

Off by default — pure compile-time governance. Opt in per orchestrator:

```java
@PromptOrchestrator(prompt = "...", allowDynamic = true,
        maxDynamicFunctions = 3, dynamicFunctionModel = ModelTier.CLOUD_FAST)
```

When the matching phase finds no registered tool above the threshold for a capability, PrOOPt asks
the model to generate a minimal, **session-scoped** prompt function (always returns `String`). It is
registered in a `ThreadLocal` `DynamicFunctionCache`, discarded at run end, capped by
`maxDynamicFunctions`, and tagged `[DYNAMIC]` on every audit line:

```
[PROOOPT][DYNAMIC][GAP_DETECTED]    capability='validate SWIFT code' bestScore=0.21 threshold=0.40
[PROOOPT][DYNAMIC][GENERATING]      model=CLOUD_FAST remainingBudget=2
[PROOOPT][DYNAMIC][REGISTERED]  ⚠  name=validateSwiftCode model=LOCAL scope=SESSION
[PROOOPT][PROMPT_FUNCTION][START]   function=validateSwiftCode [DYNAMIC] model=LOCAL
```

Plans that reference dynamic functions are never cached (the functions don't survive the run).

## Standalone concurrency

`PrOOPt.builder()` wires everything without Spring. Always `shutdown()` to release resources:

```java
PrOOPtRuntime prooopt = PrOOPt.builder()
        .configFrom("application.yaml")
        .modelRouter(customRouter)
        .scan(MyFunctions.class)
        .threadPool(Executors.newFixedThreadPool(4))
        .build();
// ...
prooopt.shutdown();
```

Propagate the trace id across thread boundaries with `PrOOPtThreadPropagator`:

```java
executor.submit(PrOOPtThreadPropagator.propagate(() -> prooopt.orchestrate(bean, input)));
```

| Context | TraceId propagation |
|---|---|
| `main()` / single-threaded loop | Automatic |
| `ExecutorService` / `CompletableFuture` / `ForkJoinPool` | `PrOOPtThreadPropagator.propagate()` |
| Spring Boot / Spring Batch | Automatic (managed bean) |

## Performance

- **Virtual threads for cloud calls** — `routeAsync()` uses a virtual-thread pool for blocking cloud
  I/O and a bounded platform pool for CPU-bound LOCAL inference.
- **Async model loading** — `OnnxModelLoader.loadAsync()` loads the model on a virtual thread so
  startup never blocks; the first real request waits only if loading is still in flight.
- **Compiled prompt templates** — `PromptTemplate.compile()` parses `{placeholders}` once; resolution
  is an `O(n)` join, faster than repeated `String.replace` for 3+ variables.
- **Metadata caches** — `FunctionRegistry` caches parameter names/types, return types, and compiled
  templates so interception does zero reflection per call.
- **Constrained generation** — `SchemaGenerator` derives a JSON Schema (Anthropic/OpenAI structured
  output) from the return type, driving autoboxing retries toward zero.
- **Async audit logging** — the audit appender is wrapped in a 4096-entry async (LMAX Disruptor)
  buffer; the calling thread never blocks on disk I/O.
- **Embedding LRU cache** — `embed()` results are memoised (2,000 entries), cleared on re-fit.

## Roadmap

- Streaming token output via `PromptStream.stream()`
- Encrypted credential store (AES-GCM, JAR-bundled)
- AspectJ compile-time weaving (`prooopt-agent`) for zero-overhead AOP
- Maven Central publication (`io.github.argonizer:prooopt-core`)
