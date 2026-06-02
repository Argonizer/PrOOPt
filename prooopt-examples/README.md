# PrOOPt Examples

Three worked, runnable PrOOPt examples in one module. Each runs against a deterministic mock router,
so **no API key is required**. Swap in a real `CloudModelRouter` to run any of them against a live model.

| Example | Package | Demonstrates | Run |
|---|---|---|---|
| **Legal Analyzer** | `io.github.argonizer.prooopt.example` | Mixed `@CodeFunction` + bounded/elevated `@PromptFunction`, three-zone governance | `LegalAnalyzerDemo` |
| **Linear System Solver** | `io.github.argonizer.prooopt.example.linear` | n×n Gaussian elimination; all arithmetic in `@CodeFunction`, prose via `@PromptFunction` | `LinearSystemSolver` |
| **BODMAS/PEMDAS Solver** | `io.github.argonizer.prooopt.example.bodmas` | DAG execution with `CLOUD_ADVANCED` planning; parallel arithmetic steps | `BodmasSolver` |

Detailed write-ups: [`README-linear.md`](README-linear.md) and [`README-bodmas.md`](README-bodmas.md).

## Running a demo

Each example exposes a `main`. Pick one with `-Dexec.mainClass`:

```bash
# Legal Analyzer
mvn exec:java -pl prooopt-examples \
  -Dexec.mainClass="io.github.argonizer.prooopt.example.LegalAnalyzerDemo"

# Linear System Solver (optionally pass a flattened [A|b] matrix as an argument)
mvn exec:java -pl prooopt-examples \
  -Dexec.mainClass="io.github.argonizer.prooopt.example.linear.LinearSystemSolver"

# BODMAS/PEMDAS Solver (the module's default mainClass)
mvn exec:java -pl prooopt-examples \
  -Dexec.mainClass="io.github.argonizer.prooopt.example.bodmas.BodmasSolver"
```

To run against a live model, set your key and swap the mock router for a `CloudModelRouter`:

```bash
export PROOOPT_ANTHROPIC_API_KEY=your_key_here
```

## Reference configuration

The bundled demos wire their routers in code, so the YAML files here are reference documentation for
the equivalent declarative configuration:

- `application-legal.yaml`
- `application-linear.yml`
- `application-bodmas.yml`

## Tests

```bash
mvn test -pl prooopt-examples
```

The BODMAS example ships 40 tests (31 `@CodeFunction` unit tests + 9 integration tests driving the
full orchestration pipeline against a mock router).
