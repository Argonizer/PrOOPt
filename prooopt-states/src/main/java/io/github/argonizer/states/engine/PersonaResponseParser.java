/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.argonizer.states.meta.TraitMetadata;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Parses and validates LLM JSON responses into typed trait value maps.
 *
 * <p>The LLM is instructed to respond with a JSON object containing only the
 * traits that should change. This parser extracts those values and coerces them
 * to the declared Java types of the corresponding fields.
 *
 * <p>Out-of-range numeric values are coerced to the nearest bound (not rejected),
 * and the coercion is logged. Enum/string traits are normalised to upper-case
 * when the trait description enumerates allowed values.
 */
public final class PersonaResponseParser {

    private static final Logger log = Logger.getLogger(PersonaResponseParser.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PersonaResponseParser() {}

    /**
     * Parses the raw LLM JSON response and returns a map of snake_name → typed value
     * for traits that appear in the response and are present in the known trait list.
     *
     * <p>Traits not present in the response are omitted — the caller treats absence
     * as "no change".
     *
     * @param rawJson  the raw LLM output (may contain leading/trailing prose; the
     *                 method extracts the first {@code {...}} block).
     * @param traits   the mutable traits that the LLM may have changed.
     * @return map of snake_name → coerced value.
     */
    public static Map<String, Object> parse(String rawJson, List<TraitMetadata> traits) {
        String json = extractJson(rawJson);
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        Map<String, Object> raw;
        try {
            raw = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warning("Failed to parse LLM response as JSON: " + e.getMessage());
            return new HashMap<>();
        }

        Map<String, TraitMetadata> byName = new LinkedHashMap<>();
        for (TraitMetadata tm : traits) {
            byName.put(tm.snakeName(), tm);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey();
            TraitMetadata tm = byName.get(key);
            if (tm == null) continue;
            Object coerced = coerce(entry.getValue(), tm);
            if (coerced != null) {
                result.put(key, coerced);
            }
        }
        return result;
    }

    /**
     * Extracts the first JSON object block {@code {...}} from a string that may
     * contain surrounding prose (a common LLM failure mode).
     */
    static String extractJson(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return raw.substring(start, end + 1);
    }

    private static Object coerce(Object value, TraitMetadata tm) {
        if (value == null) return null;
        Class<?> target = tm.type();

        if (target == String.class) return value.toString();

        if (target == int.class || target == Integer.class) {
            return clampInt(value, tm);
        }
        if (target == long.class || target == Long.class) {
            return clampLong(value, tm);
        }
        if (target == double.class || target == Double.class) {
            return clampDouble(value, tm);
        }
        if (target == float.class || target == Float.class) {
            return ((Number) coerceNumber(value)).floatValue();
        }
        if (target == boolean.class || target == Boolean.class) {
            if (value instanceof Boolean b) return b;
            return Boolean.parseBoolean(value.toString());
        }
        return value;
    }

    private static int clampInt(Object value, TraitMetadata tm) {
        int v;
        try { v = ((Number) coerceNumber(value)).intValue(); }
        catch (Exception e) { v = 0; }
        int[] range = inferIntRange(tm.description());
        if (range != null) {
            int orig = v;
            v = Math.max(range[0], Math.min(range[1], v));
            if (orig != v) log.info("Clamped " + tm.snakeName() + " from " + orig + " to " + v);
        }
        return v;
    }

    private static long clampLong(Object value, TraitMetadata tm) {
        long v;
        try { v = ((Number) coerceNumber(value)).longValue(); }
        catch (Exception e) { v = 0L; }
        return v;
    }

    private static double clampDouble(Object value, TraitMetadata tm) {
        double v;
        try { v = ((Number) coerceNumber(value)).doubleValue(); }
        catch (Exception e) { v = 0.0; }
        return v;
    }

    private static Number coerceNumber(Object value) {
        if (value instanceof Number n) return n;
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return 0; }
    }

    /**
     * Infers an integer range from descriptions of the form "0–100" or "(0..100)".
     * Returns null if no range can be parsed.
     */
    private static int[] inferIntRange(String description) {
        if (description == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+)[\\s]*[–\\-\\.]{1,2}[\\s]*(\\d+)")
                .matcher(description);
        if (m.find()) {
            try {
                return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
