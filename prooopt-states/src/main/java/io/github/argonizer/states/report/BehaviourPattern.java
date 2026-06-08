/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import java.util.List;

public record BehaviourPattern(
        String name,
        String description,
        List<String> relatedTraits,
        double confidence
) {}
