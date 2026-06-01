/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.example.linear;

import io.github.argonizer.prooopt.PrOOPt;
import io.github.argonizer.prooopt.PrOOPtRuntime;
import io.github.argonizer.prooopt.annotation.PromptOrchestrator;
import io.github.argonizer.prooopt.model.FunctionCall;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanMode;
import io.github.argonizer.prooopt.orchestrator.BaseOrchestrator;
import io.github.argonizer.prooopt.router.ModelRouter;

/**
 * Autonomous orchestrator that solves an n×n linear system. The dimension is inferred from the
 * flattened augmented matrix, so the same orchestrator solves a 2×2, 3×3, or larger system unchanged.
 *
 * <p>The bundled demo solves the canonical 3×3 system:
 *
 * <pre>
 *   x  +  y  +  z  =  25
 *   5x  + 3y  + 2z  =   0
 *        y  −  z  =   6
 * </pre>
 *
 * <p>Exact solution (verified by Gaussian elimination):
 * <ul>
 *   <li>x = −131/5 = <b>−26.2</b> (negative)</li>
 *   <li>y =  143/5 = 28.6</li>
 *   <li>z =  113/5 = 22.6</li>
 * </ul>
 *
 * <p><b>Zone governance</b> — enforced by the JVM, not the model:
 * <table>
 *   <tr><th>Zone</th><th>Functions</th><th>Execution</th></tr>
 *   <tr><td>Deterministic</td>
 *       <td>{@code gaussianElimination}, {@code verifySolution}, {@code computeResidual},
 *           {@code formatAsFraction}, {@code formatVectorAsFractions}, {@code formatAugmentedMatrix},
 *           {@code packageResult}</td>
 *       <td>Pure Java — zero tokens, sub-millisecond</td></tr>
 *   <tr><td>Bounded AI</td>
 *       <td>{@code interpretSolution}, {@code explainMethod}</td>
 *       <td>On-device LOCAL model — nothing leaves the JVM</td></tr>
 * </table>
 *
 * <p>The orchestrator uses {@link PlanMode#DYNAMIC} so the planner sees the full augmented matrix
 * embedded in the request on each invocation.
 */
@PromptOrchestrator(
        prompt = "You are a mathematical computation coordinator. Your job is to orchestrate the exact, "
                + "step-by-step solution of an n×n linear system, where n is inferred from the input "
                + "augmented matrix. "
                + "CRITICAL RULE: you must NEVER compute any arithmetic yourself. "
                + "All matrix operations, Gaussian elimination, back-substitution, verification, "
                + "residual computation, fraction formatting, and result packaging MUST be delegated "
                + "to the registered @CodeFunction tools. "
                + "Only @PromptFunction tools may produce natural-language text. "
                + "Orchestrate in this exact order: "
                + "(1) solve via gaussianElimination, "
                + "(2) verify via verifySolution, "
                + "(3) compute residual via computeResidual, "
                + "(4) format the solution vector via formatVectorAsFractions, "
                + "(5) interpret results via interpretSolution, "
                + "(6) package the final result via packageResult.",
        model = ModelTier.CLOUD_ADVANCED,
        planMode = PlanMode.DYNAMIC,
        parallel = false,
        name = "linear-system-solver",
        version = "0.2.0")
public class LinearSystemSolver extends BaseOrchestrator {

    /**
     * Default demo: augmented matrix [A|b] for the 3×3 system, row-major (length n*(n+1) = 12):
     * Row 0: 1x + 1y + 1z = 25  → [1, 1, 1, 25]
     * Row 1: 5x + 3y + 2z =  0  → [5, 3, 2,  0]
     * Row 2: 0x + 1y − 1z =  6  → [0, 1, −1,  6]
     */
    static final double[] DEMO_3X3 = {
            1, 1,  1, 25,
            5, 3,  2,  0,
            0, 1, -1,  6
    };

    // ---- BaseOrchestrator lifecycle hooks ----

    @Override
    protected void onRunStart(String traceId, Object input) {
        audit.info("[LINEAR-SOLVER][START] trace={} input={}", traceId, input);
    }

    @Override
    protected void beforeFunction(FunctionCall call) {
        audit.info("[LINEAR-SOLVER][BEFORE] function={} args={}",
                call.name(), call.args());
    }

    @Override
    protected void afterFunction(FunctionCall call, Object result) {
        String resultStr = result != null ? result.toString() : "null";
        if (resultStr.length() > 120) {
            resultStr = resultStr.substring(0, 120) + "…";
        }
        audit.info("[LINEAR-SOLVER][AFTER]  function={} result={}", call.name(), resultStr);
    }

    @Override
    protected void onError(FunctionCall call, Throwable error) {
        audit.error("[LINEAR-SOLVER][ERROR]  function={} error={}", call.name(), error.getMessage());
    }

    @Override
    protected void onRunComplete(String traceId, long totalDurationMs, int functionsCount) {
        audit.info("[LINEAR-SOLVER][COMPLETE] trace={} totalMs={} functions={}",
                traceId, totalDurationMs, functionsCount);
    }

    // ---- Entry point ----

    public static void main(String[] args) {
        // Pick the system to solve: command-line override or the bundled 3×3 demo.
        double[] augmented = args.length > 0 ? parseMatrix(args[0]) : DEMO_3X3;
        int n = LinearSystemFunctions.dimensionOf(augmented);

        LinearSystemFunctions functions = new LinearSystemFunctions();
        ModelRouter mockRouter = new MockLinearRouter();

        LinearSystemSolver solver = new LinearSystemSolver();

        PrOOPtRuntime runtime = PrOOPt.builder()
                .router(mockRouter)
                .includeStdlib(false)
                .registerInstance(functions)
                .registerInstance(solver)
                .build();

        try {
            System.out.printf("=== PrOOPt Linear System Solver (%d×%d) ===%n", n, n);
            System.out.println("Augmented matrix [A|b]:");
            System.out.print(LinearSystemFunctions.formatAugmentedMatrix(augmented));
            System.out.println();

            Object rawResult = runtime.orchestrate(solver, serializeMatrix(augmented));

            LinearSystemResult result = (rawResult instanceof LinearSystemResult r)
                    ? r
                    : fallbackSolve(augmented);

            System.out.println("Solution:");
            for (int i = 0; i < result.getN(); i++) {
                System.out.printf("  %s = %s = %.4f%n",
                        LinearSystemFunctions.varName(i), result.fraction(i), result.value(i));
            }
            System.out.printf("Verified: %s%n", result.isVerified());
            System.out.printf("%nInterpretation:%n%s%n", result.getInterpretation());

            // Hard assertions for the bundled 3×3 demo only.
            if (augmented == DEMO_3X3) {
                if (result.value(0) >= 0) {
                    throw new AssertionError(
                            "Expected x < 0 (x = −131/5 = −26.2), got " + result.value(0));
                }
                double eps = 1e-6;
                assertClose("x", result.value(0), -131.0 / 5.0, eps);
                assertClose("y", result.value(1),  143.0 / 5.0, eps);
                assertClose("z", result.value(2),  113.0 / 5.0, eps);
                System.out.println("\nAll assertions passed.");
            }
        } finally {
            runtime.shutdown();
        }
    }

    // ---- Helpers ----

    /** Serializes a matrix to a compact JSON array literal for the orchestrator input. */
    static String serializeMatrix(double[] m) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < m.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(m[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /** Parses a "[a,b,c,...]" or "a,b,c,..." literal into a double array. */
    static double[] parseMatrix(String literal) {
        String body = literal.trim().replaceAll("^\\[|\\]$", "").trim();
        if (body.isEmpty()) {
            return new double[0];
        }
        String[] parts = body.split("\\s*,\\s*");
        double[] m = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            m[i] = Double.parseDouble(parts[i]);
        }
        return m;
    }

    /** Deterministic fallback used when running against the mock router. */
    private static LinearSystemResult fallbackSolve(double[] augmented) {
        double[] solution = LinearSystemFunctions.gaussianElimination(augmented);
        boolean verified = LinearSystemFunctions.verifySolution(augmented, solution);
        return LinearSystemFunctions.packageResult(solution, verified,
                "Solution computed by Gaussian elimination with partial pivoting.");
    }

    private static void assertClose(String var, double actual, double expected, double eps) {
        if (Math.abs(actual - expected) > eps) {
            throw new AssertionError(String.format(
                    "Assertion failed for %s: expected %.6f, got %.6f", var, expected, actual));
        }
    }
}
