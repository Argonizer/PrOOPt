/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.meta;

import io.github.argonizer.states.annotation.Persona;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Resolved metadata for a {@link Persona}-annotated class, produced by
 * {@link PersonaMetaReader} and cached for the lifetime of the application.
 *
 * @param personaClass  the annotated class.
 * @param annotation    the {@code @Persona} annotation instance.
 * @param idField       the single {@code @PersonaId} field.
 * @param traits        ordered list of trait descriptors (mutable + fixed-identity).
 * @param mutableTraits traits the LLM may change (excludes fixed-identity and sensitive).
 * @param fixedTraits   read-only identity traits shown as context but not modifiable.
 */
public record PersonaMetadata(
        Class<?> personaClass,
        Persona annotation,
        Field idField,
        List<TraitMetadata> traits,
        List<TraitMetadata> mutableTraits,
        List<TraitMetadata> fixedTraits
) {

    /**
     * Computes the age of a persona in the requested unit from its origination date.
     * Age is never stored as a trait; it is always derived.
     *
     * @param originationDate when the persona was created.
     * @param unit            the desired unit (DAYS, MONTHS, etc.).
     * @return age in the given unit.
     */
    public static long age(Instant originationDate, ChronoUnit unit) {
        return unit.between(originationDate, Instant.now());
    }

    /**
     * Derives the lifecycle phase from the age in days.
     *
     * <ul>
     *   <li>FORMATION: age &lt; 30 days</li>
     *   <li>MATURITY: 30–180 days</li>
     *   <li>ENTRENCHMENT: &gt; 180 days</li>
     * </ul>
     *
     * @param originationDate when the persona was created.
     * @return phase label suitable for storage in {@code current_phase}.
     */
    public static String phase(Instant originationDate) {
        long days = ChronoUnit.DAYS.between(originationDate, Instant.now());
        if (days < 30) return "FORMATION";
        if (days <= 180) return "MATURITY";
        return "ENTRENCHMENT";
    }
}
