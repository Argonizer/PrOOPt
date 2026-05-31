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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic descriptive statistics over a list of numbers.
 */
public final class StatisticalFunctions {

    private StatisticalFunctions() {
    }

    @CodeFunction(description = "Arithmetic mean (average) of a list of numbers.",
            tags = {"statistics", "statistical", "mean", "average", "avg"})
    public static double mean(List<? extends Number> values) {
        requireNonEmpty(values);
        double sum = 0.0;
        for (Number n : values) {
            sum += n.doubleValue();
        }
        return sum / values.size();
    }

    @CodeFunction(description = "Median (middle value) of a list of numbers.",
            tags = {"statistics", "statistical", "median", "middle", "percentile"})
    public static double median(List<? extends Number> values) {
        requireNonEmpty(values);
        List<Double> sorted = sortedDoubles(values);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    @CodeFunction(description = "Sample standard deviation of a list of numbers (n-1 denominator).",
            tags = {"statistics", "statistical", "standard deviation", "stddev", "spread", "variance"})
    public static double standardDeviation(List<? extends Number> values) {
        requireNonEmpty(values);
        if (values.size() < 2) {
            return 0.0;
        }
        double mu = mean(values);
        double sumSquares = 0.0;
        for (Number n : values) {
            double d = n.doubleValue() - mu;
            sumSquares += d * d;
        }
        return Math.sqrt(sumSquares / (values.size() - 1));
    }

    @CodeFunction(description = "The p-th percentile (0–100) of a list of numbers, linearly interpolated.",
            tags = {"statistics", "statistical", "percentile", "quantile", "rank"})
    public static double percentile(List<? extends Number> values, double p) {
        requireNonEmpty(values);
        if (p < 0 || p > 100) {
            throw new IllegalArgumentException("percentile must be in [0, 100]");
        }
        List<Double> sorted = sortedDoubles(values);
        if (sorted.size() == 1) {
            return sorted.get(0);
        }
        double rank = p / 100.0 * (sorted.size() - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        double weight = rank - lower;
        return sorted.get(lower) * (1.0 - weight) + sorted.get(upper) * weight;
    }

    private static List<Double> sortedDoubles(List<? extends Number> values) {
        List<Double> doubles = new ArrayList<>(values.size());
        for (Number n : values) {
            doubles.add(n.doubleValue());
        }
        Collections.sort(doubles);
        return doubles;
    }

    private static void requireNonEmpty(List<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
    }
}
