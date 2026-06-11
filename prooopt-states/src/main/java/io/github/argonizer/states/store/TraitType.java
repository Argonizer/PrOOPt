/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

/**
 * The storage type recorded in {@code prooopt_persona_index.trait_type}.
 * Used by {@link PersonaQueryTranslator} to cast {@code trait_value} correctly
 * when building parameterised queries.
 */
public enum TraitType {
    INT,
    LONG,
    DOUBLE,
    BOOLEAN,
    STRING,
    ENUM;

    /**
     * Resolves the {@code TraitType} from a Java field type.
     */
    public static TraitType from(Class<?> javaType) {
        if (javaType == int.class || javaType == Integer.class) return INT;
        if (javaType == long.class || javaType == Long.class) return LONG;
        if (javaType == double.class || javaType == Double.class
                || javaType == float.class || javaType == Float.class) return DOUBLE;
        if (javaType == boolean.class || javaType == Boolean.class) return BOOLEAN;
        if (javaType.isEnum()) return ENUM;
        return STRING;
    }
}
