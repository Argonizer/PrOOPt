/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import io.github.argonizer.states.engine.UpdateSource;

import java.time.Instant;

public record SignificantEvent(
        Instant occurredAt,
        String description,
        UpdateSource source,
        double magnitude
) {}
