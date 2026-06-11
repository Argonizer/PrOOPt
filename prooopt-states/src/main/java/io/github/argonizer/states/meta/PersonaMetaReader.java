/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.meta;

import io.github.argonizer.prooopt.annotation.SensitiveData;
import io.github.argonizer.states.annotation.Persona;
import io.github.argonizer.states.annotation.PersonaId;
import io.github.argonizer.states.annotation.Trait;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads {@link Persona}-annotated classes via reflection and produces
 * {@link PersonaMetadata} records.
 *
 * <p>The reader is stateless; callers are expected to cache the result.
 * Validation is strict at read time — configuration errors surface immediately
 * rather than at the first state operation.
 *
 * <h3>Fixed-identity convention</h3>
 * A trait whose {@link Trait#value()} description starts with {@code "[FIXED]"}
 * is treated as a read-only identity trait. The LLM may see it as context
 * (under the "[Fixed identity]" prompt block) but may never modify it.
 * This convention is documented here and enforced consistently by
 * {@link io.github.argonizer.states.engine.PersonaPromptBuilder}.
 */
public final class PersonaMetaReader {

    private static final String FIXED_MARKER = "[FIXED]";

    private PersonaMetaReader() {}

    /**
     * Reads metadata for the given {@link Persona}-annotated class.
     *
     * @param personaClass the class to read.
     * @return fully resolved metadata.
     * @throws IllegalArgumentException if the class lacks {@code @Persona}, has
     *                                  zero or more than one {@code @PersonaId} field,
     *                                  or is otherwise misconfigured.
     */
    public static PersonaMetadata read(Class<?> personaClass) {
        Persona annotation = personaClass.getAnnotation(Persona.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    personaClass.getName() + " is not annotated with @Persona");
        }

        Field idField = null;
        List<TraitMetadata> allTraits = new ArrayList<>();
        List<TraitMetadata> mutableTraits = new ArrayList<>();
        List<TraitMetadata> fixedTraits = new ArrayList<>();

        Class<?> cursor = personaClass;
        while (cursor != null && cursor != Object.class) {
            for (Field f : cursor.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;

                if (f.isAnnotationPresent(PersonaId.class)) {
                    if (idField != null) {
                        throw new IllegalArgumentException(
                                personaClass.getName()
                                        + " declares more than one @PersonaId field: "
                                        + idField.getName() + " and " + f.getName());
                    }
                    f.setAccessible(true);
                    idField = f;
                    continue;
                }

                Trait trait = f.getAnnotation(Trait.class);
                if (trait == null) continue;

                f.setAccessible(true);
                boolean sensitive = f.isAnnotationPresent(SensitiveData.class);
                boolean fixed = trait.value().startsWith(FIXED_MARKER);
                TraitMetadata tm = new TraitMetadata(
                        f,
                        toSnakeCase(f.getName()),
                        trait.value(),
                        trait.index(),
                        sensitive,
                        fixed
                );
                allTraits.add(tm);
                if (fixed) {
                    fixedTraits.add(tm);
                } else if (!sensitive) {
                    mutableTraits.add(tm);
                }
            }
            cursor = cursor.getSuperclass();
        }

        if (idField == null) {
            throw new IllegalArgumentException(
                    personaClass.getName()
                            + " must declare exactly one field annotated with @PersonaId");
        }

        return new PersonaMetadata(
                personaClass,
                annotation,
                idField,
                Collections.unmodifiableList(allTraits),
                Collections.unmodifiableList(mutableTraits),
                Collections.unmodifiableList(fixedTraits)
        );
    }

    /**
     * Converts a camelCase field name to snake_case for use as a trait key
     * in prompts and the index table.
     */
    public static String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return camelCase;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
