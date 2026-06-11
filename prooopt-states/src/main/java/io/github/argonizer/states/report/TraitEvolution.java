/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import io.github.argonizer.states.engine.UpdateSource;
import io.github.argonizer.states.event.Trend;

import java.time.Instant;
import java.util.List;

public record TraitEvolution(
        String traitName,
        List<TraitDataPoint> dataPoints,
        TraitStatistics statistics,
        UpdateSource dominantSource
) {
    public record TraitDataPoint(Instant changedAt, Object value, UpdateSource source) {}
}
