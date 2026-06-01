# PrOOPt Example — Linear System Solver

Solves a 3×3 linear system of equations end-to-end using PrOOPt's three-zone control model.
All arithmetic is deterministic Java; language models contribute only prose interpretation.

## The problem

```
x  +  y  +  z  =  25
5x + 3y + 2z  =   0
     y  −  z  =   6
```

### Exact solution

| Variable | Fraction | Decimal |
|----------|----------|---------|
| x | **−131/5** | **−26.2** (negative) |
| y | 143/5 | 28.6 |
| z | 113/5 | 22.6 |

> **Note:** x is negative. The dominant coefficient 5x in the second equation forces x well below
> zero to satisfy 5x + 3y + 2z = 0 while y and z remain large enough to meet x + y + z = 25.

Verification: (−26.2) + 28.6 + 22.6 = 25 ✓ | 5(−26.2) + 3(28.6) + 2(22.6) = −131 + 85.8 + 45.2 = 0 ✓ | 28.6 − 22.6 = 6 ✓

## Three-zone breakdown

| Zone | Functions | Execution | Tokens |
|------|-----------|-----------|--------|
| **Deterministic** | `gaussianElimination`, `verifySolution`, `computeResidual`, `formatAsFraction`, `formatAugmentedMatrix`, `packageResult` | Pure Java — JVM enforced | 0 |
| **Bounded AI** | `interpretSolution`, `explainMethod` | On-device JLama (LOCAL) — nothing leaves the JVM | Minimal |
| **Elevated AI** | Orchestration planning only | CLOUD_ADVANCED (temperature 0.2) | Plan JSON only |

LLMs **never** compute arithmetic. The planner only decides *which* tools to call and in what order.

## Running the demo

```bash
# No API key needed — uses MockLinearRouter
mvn -pl prooopt-example-linear-system exec:java

# With a real Anthropic API key
export PROOOPT_ANTHROPIC_API_KEY=sk-ant-...
mvn -pl prooopt-example-linear-system exec:java \
    -Dexec.mainClass=io.github.argonizer.prooopt.example.linear.LinearSystemSolver
```

## Sample output

```
=== PrOOPt Linear System Solver ===
System:
  x  +  y  +  z  = 25
  5x + 3y + 2z  =  0
       y  −  z  =  6

Augmented matrix:
  [    1.0x     1.0y     1.0z |   25.0 ]
  [    5.0x     3.0y     2.0z |    0.0 ]
  [    0.0x     1.0y    -1.0z |    6.0 ]

[PROOOPT][CODE_FUNCTION][START]  traceId=a1b2c3 function=gaussianElimination
[PROOOPT][CODE_FUNCTION][END]    traceId=a1b2c3 function=gaussianElimination durationMs=0
[PROOOPT][CODE_FUNCTION][START]  traceId=a1b2c3 function=verifySolution
[PROOOPT][CODE_FUNCTION][END]    traceId=a1b2c3 function=verifySolution durationMs=0 result=true
[PROOOPT][PROMPT_FUNCTION][START] traceId=a1b2c3 function=interpretSolution tier=LOCAL
[PROOOPT][PROMPT_FUNCTION][END]   traceId=a1b2c3 function=interpretSolution durationMs=312
[PROOOPT][ORCHESTRATOR][SUMMARY]  traceId=a1b2c3 mode=DYNAMIC total=389ms functions=6 code=5 local=1 cloud=0

Solution:
  x = -131/5 = -26.2
  y = 143/5  =  28.6
  z = 113/5  =  22.6
Verified: true

Interpretation:
The system has a unique solution. Notably, x = −131/5 = −26.2 is negative, which reflects that
increasing x simultaneously satisfies the tight sum constraint (x+y+z=25) while offsetting the
dominant coefficient 5x in the second equation. Values y = 143/5 and z = 113/5 are both positive,
with their difference y−z = 6 confirming the third equation exactly.

All assertions passed.
```

## Module structure

```
prooopt-example-linear-system/
├── pom.xml
└── src/main/
    ├── java/io/github/argonizer/prooopt/example/linear/
    │   ├── LinearSystemResult.java   – Jackson-deserializable POJO for the solution
    │   ├── LinearSystemFunctions.java – @CodeFunction (6) + @PromptFunction (2)
    │   ├── LinearSystemSolver.java   – @PromptOrchestrator + main()
    │   └── MockLinearRouter.java     – Deterministic router for offline demo
    └── resources/
        └── application.yml           – Config (temperature: 0.2, DYNAMIC plan mode)
```
