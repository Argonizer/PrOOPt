/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.api;

import io.github.argonizer.states.event.Trend;

import java.time.Instant;
import java.util.Map;

public record PopulationSignal(
        String personaType,
        String traitName,
        Trend trend,
        double populationMean,
        double populationStdDev,
        int sampleSize,
        Instant observedAt
) {}
