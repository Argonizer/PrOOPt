/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import java.time.Instant;
import java.util.Map;

public record ForwardProjection(
        Instant projectedAt,
        Map<String, Double> projectedValues,
        double confidence,
        String methodology
) {}
