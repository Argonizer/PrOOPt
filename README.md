# PrOOPt — Object-Oriented Prompt Engineering for Java

PrOOPt brings LLM calls under the same engineering discipline as ordinary Java code: scoped at the
function level, governed by annotations, and observable through a structured audit trail.

## Core idea

Every function in your codebase declares its own authority:

| Zone | Annotation | Execution | Example use |
|---|---|---|---|
| **Deterministic** | `@CodeFunction` | Pure Java | Date arithmetic, text normalisation |
| **Bounded AI** | `@PromptFunction(model = LOCAL)` | On-device (JLama) | Date extraction, classification |
| **Elevated AI** | `@PromptFunction(model = CLOUD_ADVANCED)` | Cloud API | Contract summarisation |

The JVM, not the model, enforces which tier a function may use.

## Model tiers

| `ModelTier` | Meaning |
|---|---|
| `LOCAL` | On-device inference via JLama — nothing leaves the JVM |
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

- **Java 21** (LTS). One-line bump to Java 25 when available: change `<java.release>21</java.release>` in the parent POM.
- Maven 3.9+

## Modules

| Module | Artifact | Purpose |
|---|---|---|
| `prooopt-core` | `prooopt-core` | Annotations, AOP, autoboxer, orchestrator, stream API |
| `prooopt-runtime-local` | `prooopt-runtime-local` | JLama on-device inference |
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

## Roadmap

- Streaming token output via `PromptStream.stream()`
- Encrypted credential store (AES-GCM, JAR-bundled)
- ONNX runtime for local inference as an alternative to JLama
- Maven Central publication (`io.github.argonizer:prooopt-core`)
