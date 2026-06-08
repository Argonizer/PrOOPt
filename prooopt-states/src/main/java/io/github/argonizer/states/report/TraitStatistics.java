/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import io.github.argonizer.states.event.Trend;
import io.github.argonizer.states.engine.UpdateSource;

public record TraitStatistics(
        String traitName,
        double min,
        double max,
        double mean,
        double stdDev,
        double first,
        double last,
        Trend trend,
        UpdateSource dominantSource,
        int sampleCount
) {}
