/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.meta;

import java.lang.reflect.Field;

/**
 * Immutable description of a single {@link io.github.argonizer.states.annotation.Trait} field
 * derived from class reflection.
 *
 * @param field        the reflected field.
 * @param snakeName    snake_case rendering of the field name (used as the LLM key and index name).
 * @param description  the {@code @Trait.value()} description.
 * @param index        whether the trait is written to {@code prooopt_persona_index}.
 * @param sensitive    whether the field is annotated with {@code @SensitiveData}.
 * @param fixedIdentity whether the trait is treated as read-only (LLM can see but not change).
 */
public record TraitMetadata(
        Field field,
        String snakeName,
        String description,
        boolean index,
        boolean sensitive,
        boolean fixedIdentity
) {

    /**
     * Returns the runtime type of the field.
     */
    public Class<?> type() {
        return field.getType();
    }
}
