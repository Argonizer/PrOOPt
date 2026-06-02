# PrOOPt Example — BODMAS/PEMDAS Solver

*Demonstrates: DAG execution, CLOUD_ADVANCED planning, @CodeFunction arithmetic*

## The PrOOPt principle applied

The LLM generates a **DAG execution plan** based on BODMAS/PEMDAS precedence
rules — it decides *which* operations to perform and *in what order*. Every
arithmetic operation is then executed by a deterministic `@CodeFunction`. Java
determines *what each operation produces*. The model reasons about structure
and precedence; it never computes a single number. This is the core PrOOPt
contract: scoped, auditable authority declared at the function level.

## Three-zone breakdown

| Zone | Functions | Execution |
|---|---|---|
| `@CodeFunction` (static, pure Java) | `add`, `subtract`, `multiply`, `divide`, `power`, `sqrt`, `factorial`, `modulo`, `negate`, `absolute`, `assertAnswer`, `formatResult`, `buildVerificationSteps`, `assembleResult` | Deterministic, zero tokens |
| `@PromptFunction` `CLOUD_ADVANCED` | `analyzeBodmasProblem` + DAG plan generation | Cloud API — structure & precedence reasoning |
| `@PromptFunction` `CLOUD_FAST` | `interpretSolution` | Cloud API — plain-English commentary |

> `assembleResult` is a deterministic packager (like the linear-system example's
> `packageResult`): it builds the final `BodmasResult` from already-computed
> values so that no number ever originates from the LLM.

## Why DAG matters for BODMAS

Many subexpressions are independent and have no shared dependencies, so the
`DagExecutor` runs them concurrently. For the primary problem
`((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)`:

```
Wave 1 (no deps — all fire simultaneously):
  add(8,4)→$b1   add(2,4)→$b2   add(16,9)→$b3
  power(3,2)→$o1   factorial(5)→$o2   power(4,2)→$o3

Wave 2:
  sqrt($b3)→$o4            [deps: add(16,9)]
  multiply($b1,$o1)→$dm1   [deps: add(8,4), power(3,2)]
  multiply($o3,5)→$dm2     [deps: power(4,2)]

Wave 3:
  subtract($dm1,6)→$dm3    [deps: $dm1]
  divide($o2,$dm2)→$dm5    [deps: factorial(5), $dm2]

Wave 4:
  divide($dm3,$b2)→$dm4    [deps: $dm3, add(2,4)]

Wave 5:
  add($dm4,$dm5)→$as1      [deps: $dm4, $dm5]

Wave 6:
  subtract($as1,$o4)→$result   [deps: $as1, $o4]   → 13.5 ✓
```

The orchestrator LLM produces this plan by reasoning about BODMAS precedence;
the `DagExecutor` runs it. No LLM touches a number.

## How to run

```bash
export PROOOPT_ANTHROPIC_API_KEY=your_key_here
mvn exec:java -pl prooopt-example-bodmas \
  -Dexec.mainClass="io.github.argonizer.prooopt.example.bodmas.BodmasSolver"
```

The bundled demo runs against a deterministic `MockBodmasRouter`, so it works
**without an API key**. Swap in a real `CloudModelRouter` to plan live.

## Sample output (primary assertion)

```
═══════════════════════════════════════════════════════════════
PrOOPt BODMAS Solver Result
═══════════════════════════════════════════════════════════════
Problem:          ((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)
Computed Result:  13.5
Expected Result:  13.5
Assertion:        PASS ✓
DAG Steps:        14
Parallel Waves:   6
═══════════════════════════════════════════════════════════════
```

## A note on `temperature: 0.1`

The orchestrator runs at very low temperature (`0.1`). Mathematical reasoning
about operator precedence must be deterministic: a high temperature would let
the model generate incorrect DAG step orderings — violating BODMAS precedence
and producing wrong (or non-reproducible) plans. Low temperature keeps the
planner's precedence reasoning tight and repeatable.

## Tests

- `BodmasFunctionsTest` — 31 pure-Java unit tests for every `@CodeFunction`,
  including `primaryAssertion_allStepsVerified` which manually chains all 14
  arithmetic calls as deterministic ground truth (= 13.5).
- `BodmasSolverIntegrationTest` — 9 tests driving the full orchestration
  pipeline against a mock router: operator precedence, brackets, the primary
  13.5 assertion, error propagation, parallel execution, cross-stream
  dependency resolution, and traceId propagation.

```bash
mvn test -pl prooopt-example-bodmas
```
