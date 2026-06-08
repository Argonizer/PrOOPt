/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.engine;

import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.states.meta.TraitMetadata;

import java.util.Map;

/**
 * Applies a parsed LLM response (snake_name → value map) to a persona instance
 * by writing the coerced values into the corresponding fields via reflection.
 *
 * <p>Only mutable traits are written. Fixed-identity and sensitive traits are
 * silently ignored even if the LLM emitted values for them.
 */
public final class PersonaFieldWriter {

    private PersonaFieldWriter() {}

    /**
     * Writes the values from {@code parsedResponse} into {@code persona}.
     *
     * @param persona        the persona instance to mutate.
     * @param meta           the metadata describing which fields are mutable.
     * @param parsedResponse snake_name → typed value map from {@link PersonaResponseParser}.
     */
    public static void apply(Object persona, PersonaMetadata meta,
                             Map<String, Object> parsedResponse) {
        for (TraitMetadata tm : meta.mutableTraits()) {
            Object value = parsedResponse.get(tm.snakeName());
            if (value == null) continue;
            try {
                Object coerced = convertTo(value, tm.type());
                tm.field().set(persona, coerced);
            } catch (Exception e) {
                // log and skip — never crash the update on a single field failure
                java.util.logging.Logger.getLogger(PersonaFieldWriter.class.getName())
                        .warning("Could not set " + tm.snakeName() + ": " + e.getMessage());
            }
        }
    }

    private static Object convertTo(Object value, Class<?> target) {
        if (target.isInstance(value)) return value;
        String s = value.toString();
        if (target == int.class || target == Integer.class) return Integer.parseInt(s.replaceAll("\\.0$", ""));
        if (target == long.class || target == Long.class) return Long.parseLong(s.replaceAll("\\.0$", ""));
        if (target == double.class || target == Double.class) return Double.parseDouble(s);
        if (target == float.class || target == Float.class) return Float.parseFloat(s);
        if (target == boolean.class || target == Boolean.class) return Boolean.parseBoolean(s);
        if (target == String.class) return s;
        return value;
    }
}
