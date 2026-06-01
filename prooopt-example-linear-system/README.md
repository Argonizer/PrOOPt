# PrOOPt Example — Linear System Solver

Solves an **n×n** linear system of equations end-to-end using PrOOPt's three-zone control model.
All arithmetic is deterministic Java; language models contribute only prose interpretation.

The dimension `n` is **inferred from the input** — a flattened augmented matrix `[A|b]` of length
`n*(n+1)` (n rows × n+1 columns, row-major). The same code solves a 2×2, 3×3, or 100×100 system
unchanged. The bundled demo solves the canonical 3×3 system below; pass your own matrix as a
command-line argument to solve any other.

## The demo problem (3×3)

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
| **Deterministic** | `gaussianElimination`, `verifySolution`, `computeResidual`, `formatAsFraction`, `formatVectorAsFractions`, `summarizeSolution`, `formatAugmentedMatrix`, `packageResult` | Pure Java — JVM enforced | 0 |
| **Bounded AI** | `interpretSolution`, `explainMethod` | On-device JLama (LOCAL) — nothing leaves the JVM | Minimal |
| **Elevated AI** | Orchestration planning only | CLOUD_ADVANCED (temperature 0.2) | Plan JSON only |

LLMs **never** compute arithmetic. The planner only decides *which* tools to call and in what order.

## Running the demo

```bash
# No API key needed — uses MockLinearRouter, solves the bundled 3×3 demo
mvn -pl prooopt-example-linear-system exec:java

# Solve any n×n system — pass a flattened augmented matrix [A|b] of length n*(n+1).
# Example: 2x + y = 5, x − y = 1  →  [2,1,5, 1,-1,1]  →  x=2, y=1
mvn -pl prooopt-example-linear-system exec:java -Dexec.args="[2,1,5,1,-1,1]"

# Example 4×4 (rows of 5 numbers each):
mvn -pl prooopt-example-linear-system exec:java \
    -Dexec.args="[1,1,1,1,10, 0,1,0,0,2, 0,0,1,0,3, 0,0,0,1,4]"

# With a real Anthropic API key
export PROOOPT_ANTHROPIC_API_KEY=sk-ant-...
mvn -pl prooopt-example-linear-system exec:java
```

The input layout is row-major: for an n×n system, supply n rows of (n+1) numbers — the n
coefficients followed by the right-hand-side value. The solver derives n from the array length
(`n*(n+1)`) and rejects any length that is not of that form.

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
    │   ├── LinearSystemFunctions.java – @CodeFunction (8, n-dimensional) + @PromptFunction (2)
    │   ├── LinearSystemSolver.java   – @PromptOrchestrator + main()
    │   └── MockLinearRouter.java     – Deterministic router for offline demo
    └── resources/
        └── application.yml           – Config (temperature: 0.2, DYNAMIC plan mode)
```
