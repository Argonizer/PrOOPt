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
 * All functions for the linear-system solver.
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
     * Solves a 3×3 system Ax = b using Gaussian elimination with partial pivoting.
     *
     * <p>Input is a flattened augmented matrix [A|b] of length 12 (3 rows × 4 columns, row-major).
     * Returns a double[3] with {x, y, z} or throws if the system is singular.
     */
    @CodeFunction(
            description = "Solve a 3×3 linear system via Gaussian elimination with partial pivoting. "
                    + "Input: flattened augmented matrix [A|b] of 12 doubles (row-major). "
                    + "Output: solution vector [x, y, z] as double array.",
            tags = {"solve", "linear", "system", "gaussian", "elimination", "matrix", "algebra", "numeric"})
    public static double[] gaussianElimination(double[] augmented) {
        if (augmented.length != 12) {
            throw new IllegalArgumentException("Expected 12 elements (3×4 augmented matrix), got " + augmented.length);
        }
        // Copy to avoid mutating caller's array
        double[][] m = new double[3][4];
        for (int r = 0; r < 3; r++) {
            System.arraycopy(augmented, r * 4, m[r], 0, 4);
        }

        // Forward elimination with partial pivoting
        for (int col = 0; col < 3; col++) {
            // Find pivot row
            int pivot = col;
            for (int row = col + 1; row < 3; row++) {
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
            for (int row = col + 1; row < 3; row++) {
                double factor = m[row][col] / pivotVal;
                for (int k = col; k < 4; k++) {
                    m[row][k] -= factor * m[col][k];
                }
            }
        }

        // Back-substitution
        double[] solution = new double[3];
        for (int row = 2; row >= 0; row--) {
            double sum = m[row][3];
            for (int k = row + 1; k < 3; k++) {
                sum -= m[row][k] * solution[k];
            }
            solution[row] = sum / m[row][row];
        }
        return solution;
    }

    /**
     * Verifies a candidate solution [x, y, z] against the original augmented matrix.
     * Returns {@code true} when every residual |Ax − b|_i < 1e-9.
     */
    @CodeFunction(
            description = "Verify a 3-variable solution [x,y,z] against the original 3×3 system. "
                    + "Returns true when every equation holds to within 1e-9.",
            tags = {"verify", "check", "residual", "solution", "linear", "system", "validate"})
    public static boolean verifySolution(double[] augmented, double[] solution) {
        if (augmented.length != 12 || solution.length != 3) {
            return false;
        }
        for (int row = 0; row < 3; row++) {
            double lhs = 0.0;
            for (int col = 0; col < 3; col++) {
                lhs += augmented[row * 4 + col] * solution[col];
            }
            double rhs = augmented[row * 4 + 3];
            if (Math.abs(lhs - rhs) > 1e-9) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes and returns the residual vector r = b − Ax as a double[3].
     * Useful for audit logging even when verification passes.
     */
    @CodeFunction(
            description = "Compute the residual vector r = b − Ax for the given augmented matrix and solution.",
            tags = {"residual", "error", "accuracy", "matrix", "solution", "check"})
    public static double[] computeResidual(double[] augmented, double[] solution) {
        double[] residual = new double[3];
        for (int row = 0; row < 3; row++) {
            double lhs = 0.0;
            for (int col = 0; col < 3; col++) {
                lhs += augmented[row * 4 + col] * solution[col];
            }
            residual[row] = augmented[row * 4 + 3] - lhs;
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
                long n = numerator / g;
                long d = denom / g;
                return d == 1 ? String.valueOf(n) : n + "/" + d;
            }
        }
        return String.valueOf(value);
    }

    /**
     * Builds a human-readable matrix representation of the augmented system for audit logging.
     */
    @CodeFunction(
            description = "Render the 3×4 augmented matrix as a multi-line ASCII table for logging.",
            tags = {"format", "matrix", "print", "display", "augmented", "table", "debug"})
    public static String formatAugmentedMatrix(double[] augmented) {
        StringBuilder sb = new StringBuilder();
        String[] vars = {"x", "y", "z"};
        for (int row = 0; row < 3; row++) {
            sb.append("  [ ");
            for (int col = 0; col < 3; col++) {
                sb.append(String.format("%6.1f%s ", augmented[row * 4 + col], vars[col]));
            }
            sb.append(String.format("| %6.1f ]\n", augmented[row * 4 + 3]));
        }
        return sb.toString();
    }

    /**
     * Packages a verified numeric solution into the structured {@link LinearSystemResult} POJO,
     * pre-filling fraction strings and setting {@code verified} from the caller's check.
     */
    @CodeFunction(
            description = "Package numeric solution [x,y,z] into a LinearSystemResult with fraction strings.",
            tags = {"package", "result", "wrap", "format", "solution", "struct", "assemble"})
    public static LinearSystemResult packageResult(double[] solution, boolean verified, String interpretation) {
        return new LinearSystemResult(
                solution[0],
                solution[1],
                solution[2],
                formatAsFraction(solution[0]),
                formatAsFraction(solution[1]),
                formatAsFraction(solution[2]),
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
            prompt = "You are a mathematics tutor. The 3×3 linear system has been solved numerically.\n"
                    + "Solution: x = {xFraction} ({xDecimal}), y = {yFraction} ({yDecimal}), "
                    + "z = {zFraction} ({zDecimal}).\n"
                    + "Verified: {verified}.\n"
                    + "Provide a one-paragraph plain-English interpretation of what this means for the "
                    + "system of equations — note which variable is negative and why that is significant. "
                    + "Do NOT perform any arithmetic yourself.",
            description = "Interpret the verified solution in plain English.")
    public String interpretSolution(
            String xFraction, String xDecimal,
            String yFraction, String yDecimal,
            String zFraction, String zDecimal,
            boolean verified) {
        return null;
    }

    /**
     * Explains the Gaussian elimination method and why partial pivoting was used, at a level
     * suitable for a student who has seen basic linear algebra.
     */
    @PromptFunction(
            model = ModelTier.LOCAL,
            prompt = "You are a mathematics educator. Explain in two to three paragraphs:\n"
                    + "1. What Gaussian elimination is and how it solves linear systems.\n"
                    + "2. Why partial pivoting is important for numerical stability.\n"
                    + "Use the following concrete system as your example:\n"
                    + "{systemDescription}\n"
                    + "Do NOT compute any numbers — the solver has already done that.",
            description = "Explain Gaussian elimination with partial pivoting using this system as an example.")
    public String explainMethod(String systemDescription) {
        return null;
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
