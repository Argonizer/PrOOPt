/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import java.util.List;
import java.util.Map;

public record PopulationInsights(
        int totalPersonas,
        int activePersonas,
        int retiredPersonas,
        Map<String, TraitStatistics> populationTraitStats,
        List<TraitCorrelation> correlations,
        List<BehaviourPattern> emergentPatterns,
        String llmSummary
) {}
