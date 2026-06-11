/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import java.time.Instant;
import java.util.List;

public record MetricEvolution(
        String metricName,
        List<MetricDataPoint> dataPoints,
        double min,
        double max,
        double mean
) {
    public record MetricDataPoint(Instant computedAt, double value) {}
}
