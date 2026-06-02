/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.example.bodmas;

import io.github.argonizer.prooopt.annotation.CodeFunction;
import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.model.LogLevel;
import io.github.argonizer.prooopt.model.ModelTier;

/**
 * All functions available to the {@link BodmasSolver} orchestrator.
 *
 * <p><b>Zone breakdown:</b>
 * <ul>
 *   <li>{@code @CodeFunction} (static) — every arithmetic, validation, and formatting operation.
 *       Pure Java, deterministic, zero tokens. The LLM NEVER touches a number.</li>
 *   <li>{@code @PromptFunction} (instance, returns {@code null}) — structural analysis and
 *       plain-English interpretation only. No arithmetic.</li>
 * </ul>
 */
public class BodmasFunctions {

    // ================================================================
    // @CodeFunction zone — pure Java arithmetic, zero tokens
    // ================================================================

    @CodeFunction(
            description = "Adds two numbers: a + b",
            tags = {"arithmetic", "addition", "plus", "sum"})
    public static double add(double a, double b) {
        return a + b;
    }

    @CodeFunction(
            description = "Subtracts b from a: a - b",
            tags = {"arithmetic", "subtraction", "minus", "difference"})
    public static double subtract(double a, double b) {
        return a - b;
    }

    @CodeFunction(
            description = "Multiplies two numbers: a × b",
            tags = {"arithmetic", "multiplication", "times", "product"})
    public static double multiply(double a, double b) {
        return a * b;
    }

    @CodeFunction(
            description = "Divides a by b: a ÷ b. Throws if b is zero.",
            tags = {"arithmetic", "division", "quotient", "divided by"})
    public static double divide(double a, double b) {
        if (Math.abs(b) < 1e-15) {
            throw new ArithmeticException("Division by zero: cannot divide " + a + " by " + b);
        }
        return a / b;
    }

    @CodeFunction(
            description = "Raises base to the power of exponent: base^exponent",
            tags = {"arithmetic", "power", "exponent", "squared", "cubed", "orders"})
    public static double power(double base, double exponent) {
        return Math.pow(base, exponent);
    }

    @CodeFunction(
            description = "Computes the square root of radicand: √radicand",
            tags = {"arithmetic", "square root", "radical", "root", "orders"})
    public static double sqrt(double radicand) {
        if (radicand < 0) {
            throw new ArithmeticException("Square root of negative number: √" + radicand + " is not real");
        }
        return Math.sqrt(radicand);
    }

    @CodeFunction(
            description = "Computes n factorial: n! = n × (n-1) × ... × 1",
            tags = {"arithmetic", "factorial", "orders", "permutation"})
    public static double factorial(int n) {
        if (n < 0) {
            throw new ArithmeticException("Factorial of negative number: " + n + "! is undefined");
        }
        if (n > 20) {
            throw new ArithmeticException("Factorial overflow: " + n + "! exceeds double precision");
        }
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    @CodeFunction(
            description = "Computes a modulo b: a mod b (remainder)",
            tags = {"arithmetic", "modulo", "remainder", "mod"})
    public static double modulo(double a, double b) {
        if (Math.abs(b) < 1e-15) {
            throw new ArithmeticException("Modulo by zero");
        }
        return a % b;
    }

    @CodeFunction(
            description = "Negates a value: -value (unary minus)",
            tags = {"arithmetic", "negate", "negative", "unary minus"})
    public static double negate(double value) {
        return -value;
    }

    @CodeFunction(
            description = "Returns the absolute value: |value|",
            tags = {"arithmetic", "absolute", "abs", "magnitude"})
    public static double absolute(double value) {
        return Math.abs(value);
    }

    @CodeFunction(
            description = "Asserts computed answer equals expected within tolerance 1e-9",
            tags = {"validation", "assertion", "verify", "check answer"})
    public static boolean assertAnswer(double computed, double expected) {
        boolean passed = Math.abs(computed - expected) < 1e-9;
        if (passed) {
            System.out.printf("[PROOOPT][BODMAS][ASSERT] PASS ✓  "
                            + "computed=%.10f expected=%.10f diff=%.2e%n",
                    computed, expected, Math.abs(computed - expected));
        } else {
            System.out.printf("[PROOOPT][BODMAS][ASSERT] FAIL ✗  "
                            + "computed=%.10f expected=%.10f diff=%.2e%n",
                    computed, expected, Math.abs(computed - expected));
            throw new AssertionError(
                    "BODMAS assertion failed: expected " + expected
                            + " but computed " + computed
                            + " (diff=" + Math.abs(computed - expected) + ")");
        }
        return passed;
    }

    @CodeFunction(
            description = "Formats a double result, removing unnecessary decimal zeros",
            tags = {"formatting", "display", "result"})
    public static String formatResult(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        // Remove trailing zeros after the decimal point.
        return String.format("%.10f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    @CodeFunction(
            description = "Builds a step-by-step verification string for the result",
            tags = {"verification", "proof", "steps", "audit"})
    public static String buildVerificationSteps(String problem, double result) {
        return String.format(
                "Problem: %s%n"
                        + "Computed via DAG execution: %s%n"
                        + "All arithmetic performed by @CodeFunction — zero LLM computation%n"
                        + "Verification: answer is deterministic and reproducible",
                problem, formatResult(result));
    }

    /**
     * Deterministically packages the computed pieces into a {@link BodmasResult}. Like the
     * linear-system example's {@code packageResult}, this keeps result assembly in the @CodeFunction
     * zone so no number ever originates from the LLM.
     */
    @CodeFunction(
            description = "Assembles the final BodmasResult from computed values and LLM prose. "
                    + "Performs no arithmetic itself — only packaging.",
            tags = {"package", "result", "assemble", "wrap", "struct"},
            logLevel = LogLevel.SUMMARY)
    public static BodmasResult assembleResult(
            String problem,
            String bodmasAnalysis,
            String dagPlanSummary,
            double computedResult,
            String resultFormatted,
            double expectedResult,
            boolean assertionPassed,
            String verificationSteps,
            String interpretation,
            int totalDagSteps,
            int parallelWaves) {
        return new BodmasResult(problem, bodmasAnalysis, dagPlanSummary, computedResult,
                resultFormatted, expectedResult, assertionPassed, verificationSteps,
                interpretation, totalDagSteps, parallelWaves);
    }

    // ================================================================
    // @PromptFunction zone — instance, returns null, no arithmetic
    // ================================================================

    @PromptFunction(
            model = ModelTier.CLOUD_ADVANCED,
            prompt = """
                    Analyze this BODMAS/PEMDAS mathematical expression: {problem}

                    Identify:
                    1. All bracket groups and their nesting depth
                    2. All orders (powers, roots, factorials) and their operands
                    3. Which subexpressions can be computed in parallel (no shared deps)
                    4. The complete BODMAS precedence ordering of all operations
                    5. Total number of arithmetic operations required

                    Format your response as a structured analysis with clear sections.
                    Do NOT compute any values. Only identify and classify operations.
                    """,
            description = "Analyzes BODMAS expression structure and identifies parallelism opportunities",
            logLevel = LogLevel.FULL)
    public String analyzeBodmasProblem(String problem) {
        return null;
    }

    @PromptFunction(
            model = ModelTier.CLOUD_FAST,
            prompt = """
                    A BODMAS expression has been solved using a DAG execution plan
                    where every arithmetic step was a deterministic Java @CodeFunction.

                    Problem:    {problem}
                    DAG Plan:   {dagPlan}
                    Result:     {result}

                    Provide a clear plain-English explanation of:
                    1. How BODMAS precedence was applied
                    2. Which operations ran in parallel and why
                    3. Why the answer {result} is correct
                    4. The mathematical significance of the result

                    Be concise — 4 to 6 sentences maximum.
                    """,
            description = "Explains the BODMAS solution in plain English with mathematical commentary",
            logLevel = LogLevel.FULL)
    public String interpretSolution(String problem, String dagPlan, String result) {
        return null;
    }
}
