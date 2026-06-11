/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.annotation;

import io.github.argonizer.states.meta.PersonaMetaReader;
import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.states.meta.TraitMetadata;
import io.github.argonizer.states.personas.npc.GuardNpc;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonaAnnotationProcessorTest {

    @Test
    void guardNpcMetadataIsReadCorrectly() {
        PersonaMetadata meta = PersonaMetaReader.read(GuardNpc.class);

        assertNotNull(meta.annotation(), "Should have @Persona annotation");
        assertNotNull(meta.idField(), "Should have @PersonaId field");
        assertEquals("id", meta.idField().getName());

        List<TraitMetadata> traits = meta.traits();
        assertFalse(traits.isEmpty(), "Should have traits");

        // Check that snake_case conversion works
        TraitMetadata trustTrait = traits.stream()
                .filter(t -> t.snakeName().equals("trust_in_player"))
                .findFirst()
                .orElse(null);
        assertNotNull(trustTrait, "trust_in_player trait should exist");
        assertTrue(trustTrait.index(), "trust_in_player should be indexed");
    }

    @Test
    void fixedTraitsAreIdentifiedByPrefix() {
        PersonaMetadata meta = PersonaMetaReader.read(GuardNpc.class);

        List<TraitMetadata> fixed = meta.fixedTraits();
        assertFalse(fixed.isEmpty(), "Should have fixed traits");

        for (TraitMetadata t : fixed) {
            assertTrue(t.fixedIdentity(), "Fixed trait should be marked fixedIdentity");
            assertTrue(t.description().startsWith("[FIXED]"),
                    "Fixed trait description should start with [FIXED]");
        }
    }

    @Test
    void mutableTraitsExcludeFixedOnes() {
        PersonaMetadata meta = PersonaMetaReader.read(GuardNpc.class);

        meta.mutableTraits().forEach(t ->
                assertFalse(t.fixedIdentity(), "Mutable trait should not be fixedIdentity"));
    }

    @Test
    void snakeCaseConversionIsCorrect() {
        assertEquals("trust_in_player", PersonaMetaReader.toSnakeCase("trustInPlayer"));
        assertEquals("emotional_valence", PersonaMetaReader.toSnakeCase("emotionalValence"));
        assertEquals("cortisol_reactivity", PersonaMetaReader.toSnakeCase("cortisolReactivity"));
        assertEquals("id", PersonaMetaReader.toSnakeCase("id"));
    }
}
