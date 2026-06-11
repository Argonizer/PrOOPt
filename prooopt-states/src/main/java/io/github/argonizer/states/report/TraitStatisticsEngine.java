/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import io.github.argonizer.states.engine.UpdateSource;
import io.github.argonizer.states.event.Trend;
import io.github.argonizer.states.store.PersonaHistoryRecord;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes per-trait statistics from persona history records.
 * All computation is local — no LLM calls.
 */
public final class TraitStatisticsEngine {

    private TraitStatisticsEngine() {}

    public static Map<String, TraitStatistics> compute(List<PersonaHistoryRecord> history) {
        Map<String, List<TraitEvolution.TraitDataPoint>> byTrait = new LinkedHashMap<>();

        for (PersonaHistoryRecord record : history) {
            if (record.getFieldsChanged() == null || record.getFullStateAfter() == null) continue;
            UpdateSource src = record.getUpdateSource() != null
                    ? safeSource(record.getUpdateSource()) : UpdateSource.EXTERNAL;

            for (String field : record.getFieldsChanged().split(",")) {
                String trait = field.trim();
                if (trait.isEmpty()) continue;
                byTrait.computeIfAbsent(trait, k -> new ArrayList<>())
                        .add(new TraitEvolution.TraitDataPoint(record.getChangedAt(), extractValue(record.getFullStateAfter(), trait), src));
            }
        }

        Map<String, TraitStatistics> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<TraitEvolution.TraitDataPoint>> e : byTrait.entrySet()) {
            result.put(e.getKey(), computeStats(e.getKey(), e.getValue()));
        }
        return result;
    }

    private static TraitStatistics computeStats(String traitName, List<TraitEvolution.TraitDataPoint> points) {
        List<Double> numeric = points.stream()
                .map(p -> toDouble(p.value()))
                .filter(v -> !Double.isNaN(v))
                .collect(Collectors.toList());

        if (numeric.isEmpty()) {
            return new TraitStatistics(traitName, 0, 0, 0, 0, 0, 0, Trend.NONE, UpdateSource.EXTERNAL, 0);
        }

        double min = numeric.stream().mapToDouble(d -> d).min().orElse(0);
        double max = numeric.stream().mapToDouble(d -> d).max().orElse(0);
        double mean = numeric.stream().mapToDouble(d -> d).average().orElse(0);
        double variance = numeric.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double first = numeric.get(0);
        double last = numeric.get(numeric.size() - 1);

        Trend trend;
        if (last > first + stdDev * 0.5) trend = Trend.RISING;
        else if (last < first - stdDev * 0.5) trend = Trend.FALLING;
        else trend = Trend.NONE;

        UpdateSource dominant = points.stream()
                .collect(Collectors.groupingBy(TraitEvolution.TraitDataPoint::source, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(UpdateSource.EXTERNAL);

        return new TraitStatistics(traitName, min, max, mean, stdDev, first, last, trend, dominant, numeric.size());
    }

    private static double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; }
        }
        if (v instanceof Boolean b) return b ? 1.0 : 0.0;
        return Double.NaN;
    }

    private static Object extractValue(String json, String key) {
        // Simple extraction: find "key": value pattern
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;
        char c = json.charAt(start);
        if (c == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : null;
        }
        int end = start;
        while (end < json.length() && ",}\n".indexOf(json.charAt(end)) < 0) end++;
        String raw = json.substring(start, end).trim();
        if (raw.equalsIgnoreCase("true")) return true;
        if (raw.equalsIgnoreCase("false")) return false;
        try { return Double.parseDouble(raw); } catch (NumberFormatException e) { return raw; }
    }

    private static UpdateSource safeSource(String s) {
        try { return UpdateSource.valueOf(s); } catch (Exception e) { return UpdateSource.EXTERNAL; }
    }
}
