/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.stdlib;

import io.github.argonizer.prooopt.annotation.CodeFunction;

/**
 * Deterministic arithmetic and elementary math, exposed as zero-token {@code @CodeFunction}s. When a
 * plan needs an exact sum or square root, it should reach for these rather than ask a model to compute
 * — always correct, always free.
 */
public final class ArithmeticFunctions {

    private ArithmeticFunctions() {
    }

    @CodeFunction(description = "Add two numbers and return their sum.",
            tags = {"arithmetic", "math", "add", "sum", "plus", "addition"})
    public static double add(double a, double b) {
        return a + b;
    }

    @CodeFunction(description = "Subtract the second number from the first.",
            tags = {"arithmetic", "math", "subtract", "minus", "difference", "subtraction"})
    public static double subtract(double a, double b) {
        return a - b;
    }

    @CodeFunction(description = "Multiply two numbers and return their product.",
            tags = {"arithmetic", "math", "multiply", "product", "times", "multiplication"})
    public static double multiply(double a, double b) {
        return a * b;
    }

    @CodeFunction(description = "Divide the first number by the second; errors on division by zero.",
            tags = {"arithmetic", "math", "divide", "quotient", "division", "ratio"})
    public static double divide(double a, double b) {
        if (b == 0.0) {
            throw new ArithmeticException("division by zero");
        }
        return a / b;
    }

    @CodeFunction(description = "Raise a base to an exponent (base^exponent).",
            tags = {"math", "power", "exponent", "raise", "pow"})
    public static double power(double base, double exponent) {
        return Math.pow(base, exponent);
    }

    @CodeFunction(description = "Square root of a non-negative number.",
            tags = {"math", "square root", "sqrt", "root"})
    public static double squareRoot(double value) {
        if (value < 0) {
            throw new ArithmeticException("square root of a negative number");
        }
        return Math.sqrt(value);
    }

    @CodeFunction(description = "Natural logarithm (base e) of a positive number.",
            tags = {"math", "logarithm", "log", "ln", "natural log"})
    public static double logarithm(double value) {
        if (value <= 0) {
            throw new ArithmeticException("logarithm of a non-positive number");
        }
        return Math.log(value);
    }

    @CodeFunction(description = "Absolute value of a number.",
            tags = {"math", "absolute", "abs", "magnitude"})
    public static double absolute(double value) {
        return Math.abs(value);
    }

    @CodeFunction(description = "Round a number to the nearest whole number.",
            tags = {"math", "round", "nearest", "rounding"})
    public static long roundValue(double value) {
        return Math.round(value);
    }

    @CodeFunction(description = "Largest whole number less than or equal to the value (floor).",
            tags = {"math", "floor", "round down"})
    public static double floorValue(double value) {
        return Math.floor(value);
    }

    @CodeFunction(description = "Smallest whole number greater than or equal to the value (ceiling).",
            tags = {"math", "ceiling", "ceil", "round up"})
    public static double ceilValue(double value) {
        return Math.ceil(value);
    }
}
