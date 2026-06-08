/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import java.time.Instant;

public record InflectionPoint(
        Instant occurredAt,
        String traitName,
        double valueBefore,
        double valueAfter,
        String description
) {}
