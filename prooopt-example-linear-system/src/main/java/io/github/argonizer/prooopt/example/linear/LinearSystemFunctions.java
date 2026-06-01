/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.example.linear;

import io.github.argonizer.prooopt.annotation.CodeFunction;
import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.model.ModelTier;

/**
 * All functions for the n×n linear-system solver.
 *
 * <p>Every function operates on a <b>flattened augmented matrix</b> [A|b] of length {@code n*(n+1)},
 * stored row-major (n rows × (n+1) columns). The dimension {@code n} is inferred from the array
 * length, so the same code solves a 2×2, 3×3, or 100×100 system unchanged.
 *
 * <p><b>Zone breakdown:</b>
 * <ul>
 *   <li>{@code @CodeFunction} (static) — Gaussian elimination, back-substitution, verification,
 *       fraction formatting, residual computation, matrix pretty-printing. All arithmetic is exact
 *       Java floating-point; LLMs NEVER touch a number.</li>
 *   <li>{@code @PromptFunction} (instance, returns {@code null}) — interpretation of a
 *       numerically-verified solution and a natural-language explanation of the method used.</li>
 * </ul>
 */
public class LinearSystemFunctions {

    // ================================================================
    // @CodeFunction zone — pure Java, deterministic, zero tokens
    // ================================================================

    /**
     * Solves an n×n system Ax = b using Gaussian elimination with partial pivoting.
     *
     * <p>Input is a flattened augmented matrix [A|b] of length {@code n*(n+1)} (n rows × (n+1)
     * columns, row-major). The dimension {@code n} is derived from the array length. Returns a
     * {@code double[n]} solution vector, or throws if the system is singular.
     */
    @CodeFunction(
            description = "Solve an n×n linear system via Gaussian elimination with partial pivoting. "
                    + "Input: flattened augmented matrix [A|b] of n*(n+1) doubles (row-major); n is "
                    + "inferred from the length. Output: solution vector of n doubles.",
            tags = {"solve", "linear", "system", "gaussian", "elimination", "matrix", "algebra", "numeric"})
    public static double[] gaussianElimination(double[] augmented) {
        int n = dimensionOf(augmented);
        int cols = n + 1;

        // Copy into an n×(n+1) working matrix to avoid mutating the caller's array.
        double[][] m = new double[n][cols];
        for (int r = 0; r < n; r++) {
            System.arraycopy(augmented, r * cols, m[r], 0, cols);
        }

        // Forward elimination with partial pivoting.
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
                    pivot = row;
                }
            }
            double[] tmp = m[col];
            m[col] = m[pivot];
            m[pivot] = tmp;

            double pivotVal = m[col][col];
            if (Math.abs(pivotVal) < 1e-12) {
                throw new ArithmeticException("Singular matrix: no unique solution exists.");
            }
            for (int row = col + 1; row < n; row++) {
                double factor = m[row][col] / pivotVal;
                for (int k = col; k < cols; k++) {
                    m[row][k] -= factor * m[col][k];
                }
            }
        }

        // Back-substitution.
        double[] solution = new double[n];
        for (int row = n - 1; row >= 0; row--) {
            double sum = m[row][n];
            for (int k = row + 1; k < n; k++) {
                sum -= m[row][k] * solution[k];
            }
            solution[row] = sum / m[row][row];
        }
        return solution;
    }

    /**
     * Verifies a candidate solution vector against the original augmented matrix.
     * Returns {@code true} when every residual |Ax − b|ᵢ < 1e-9.
     */
    @CodeFunction(
            description = "Verify an n-variable solution against the original n×n system. "
                    + "Returns true when every equation holds to within 1e-9.",
            tags = {"verify", "check", "residual", "solution", "linear", "system", "validate"})
    public static boolean verifySolution(double[] augmented, double[] solution) {
        int n = dimensionOf(augmented);
        if (solution.length != n) {
            return false;
        }
        int cols = n + 1;
        for (int row = 0; row < n; row++) {
            double lhs = 0.0;
            for (int col = 0; col < n; col++) {
                lhs += augmented[row * cols + col] * solution[col];
            }
            double rhs = augmented[row * cols + n];
            if (Math.abs(lhs - rhs) > 1e-9) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes and returns the residual vector r = b − Ax as a {@code double[n]}.
     * Useful for audit logging even when verification passes.
     */
    @CodeFunction(
            description = "Compute the residual vector r = b − Ax for the given n×n augmented matrix "
                    + "and solution.",
            tags = {"residual", "error", "accuracy", "matrix", "solution", "check"})
    public static double[] computeResidual(double[] augmented, double[] solution) {
        int n = dimensionOf(augmented);
        int cols = n + 1;
        double[] residual = new double[n];
        for (int row = 0; row < n; row++) {
            double lhs = 0.0;
            for (int col = 0; col < n; col++) {
                lhs += augmented[row * cols + col] * solution[col];
            }
            residual[row] = augmented[row * cols + n] - lhs;
        }
        return residual;
    }

    /**
     * Converts a decimal value to its simplest exact fraction string (e.g., −26.2 → "−131/5").
     * Works by multiplying by successive denominators up to 1,000 to find an integer numerator.
     * Falls back to the decimal string if no simple fraction is found within tolerance.
     */
    @CodeFunction(
            description = "Format a decimal as its simplest fraction string, e.g. -26.2 → \"-131/5\".",
            tags = {"fraction", "format", "simplify", "rational", "decimal", "convert", "display"})
    public static String formatAsFraction(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        for (int denom = 2; denom <= 1_000; denom++) {
            double numeratorDouble = value * denom;
            long numerator = Math.round(numeratorDouble);
            if (Math.abs(numeratorDouble - numerator) < 1e-9) {
                long g = gcd(Math.abs(numerator), denom);
                long nn = numerator / g;
                long d = denom / g;
                return d == 1 ? String.valueOf(nn) : nn + "/" + d;
            }
        }
        return String.valueOf(value);
    }

    /**
     * Maps {@link #formatAsFraction(double)} over an entire solution vector, returning a parallel
     * {@code String[]} of fraction strings.
     */
    @CodeFunction(
            description = "Format every value in a solution vector as its simplest fraction string. "
                    + "Returns a string array parallel to the input.",
            tags = {"fraction", "format", "vector", "simplify", "rational", "convert", "display", "batch"})
    public static String[] formatVectorAsFractions(double[] solution) {
        String[] fractions = new String[solution.length];
        for (int i = 0; i < solution.length; i++) {
            fractions[i] = formatAsFraction(solution[i]);
        }
        return fractions;
    }

    /**
     * Builds a one-line "variable = fraction = decimal" summary of a solution vector, suitable for
     * embedding into a prompt. Pairs the solution with its parallel fraction strings.
     */
    @CodeFunction(
            description = "Build a one-line 'var = fraction = decimal' summary of a solution vector "
                    + "and its parallel fraction strings, for embedding into a prompt.",
            tags = {"summarize", "solution", "describe", "format", "vector", "prompt", "text"})
    public static String summarizeSolution(double[] solution, String[] fractions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < solution.length; i++) {
            if (i > 0) sb.append(", ");
            String frac = (fractions != null && i < fractions.length) ? fractions[i] : "?";
            sb.append(String.format("%s = %s = %.4f", varName(i), frac, solution[i]));
        }
        return sb.toString();
    }

    /**
     * Builds a human-readable matrix representation of the n×(n+1) augmented system for audit logging.
     * Variables beyond the first three are labelled x3, x4, … .
     */
    @CodeFunction(
            description = "Render an n×(n+1) augmented matrix as a multi-line ASCII table for logging.",
            tags = {"format", "matrix", "print", "display", "augmented", "table", "debug"})
    public static String formatAugmentedMatrix(double[] augmented) {
        int n = dimensionOf(augmented);
        int cols = n + 1;
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < n; row++) {
            sb.append("  [ ");
            for (int col = 0; col < n; col++) {
                sb.append(String.format("%6.1f·%-3s ", augmented[row * cols + col], varName(col)));
            }
            sb.append(String.format("| %6.1f ]\n", augmented[row * cols + n]));
        }
        return sb.toString();
    }

    /**
     * Packages a verified numeric solution into the structured {@link LinearSystemResult} POJO,
     * pre-filling fraction strings and setting {@code verified} from the caller's check.
     */
    @CodeFunction(
            description = "Package a numeric solution vector into a LinearSystemResult with fraction "
                    + "strings, dimension, and verification flag.",
            tags = {"package", "result", "wrap", "format", "solution", "struct", "assemble"})
    public static LinearSystemResult packageResult(double[] solution, boolean verified, String interpretation) {
        return new LinearSystemResult(
                solution.length,
                solution,
                formatVectorAsFractions(solution),
                interpretation,
                verified);
    }

    // ================================================================
    // @PromptFunction zone — LOCAL model, instance methods, return null
    // ================================================================

    /**
     * Produces a concise natural-language interpretation of what the solution values mean in context.
     * All arithmetic is already done; the model only provides prose framing.
     */
    @PromptFunction(
            model = ModelTier.LOCAL,
            prompt = "You are a mathematics tutor. An n×n linear system has been solved numerically.\n"
                    + "Solution (variable = fraction = decimal): {solutionSummary}.\n"
                    + "Verified: {verified}.\n"
                    + "Provide a one-paragraph plain-English interpretation of what this means for the "
                    + "system of equations — note any variable that is negative and why that is "
                    + "significant. Do NOT perform any arithmetic yourself.",
            description = "Interpret the verified solution in plain English.")
    public String interpretSolution(String solutionSummary, boolean verified) {
        return null;
    }

    /**
     * Explains the Gaussian elimination method and why partial pivoting was used, at a level
     * suitable for a student who has seen basic linear algebra.
     */
    @PromptFunction(
            model = ModelTier.LOCAL,
            prompt = "You are a mathematics educator. Explain in two to three paragraphs:\n"
                    + "1. What Gaussian elimination is and how it solves an n×n linear system.\n"
                    + "2. Why partial pivoting is important for numerical stability.\n"
                    + "Use the following concrete system as your example:\n"
                    + "{systemDescription}\n"
                    + "Do NOT compute any numbers — the solver has already done that.",
            description = "Explain Gaussian elimination with partial pivoting using this system as an example.")
    public String explainMethod(String systemDescription) {
        return null;
    }

    // ================================================================
    // Package-private / private helpers
    // ================================================================

    /**
     * Infers the dimension {@code n} from a flattened augmented matrix of length {@code n*(n+1)}.
     * Throws if the length does not correspond to a valid square system.
     */
    static int dimensionOf(double[] augmented) {
        if (augmented == null || augmented.length == 0) {
            throw new IllegalArgumentException("Augmented matrix must be non-empty.");
        }
        // Solve n*(n+1) = length for positive integer n.
        int n = (int) Math.round((-1.0 + Math.sqrt(1.0 + 4.0 * augmented.length)) / 2.0);
        if (n < 1 || n * (n + 1) != augmented.length) {
            throw new IllegalArgumentException(
                    "Augmented matrix length " + augmented.length + " is not of the form n*(n+1); "
                            + "expected a square system [A|b].");
        }
        return n;
    }

    /** Human-friendly variable name: x, y, z for the first three, then x3, x4, … . */
    static String varName(int index) {
        return switch (index) {
            case 0 -> "x";
            case 1 -> "y";
            case 2 -> "z";
            default -> "x" + index;
        };
    }

    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
