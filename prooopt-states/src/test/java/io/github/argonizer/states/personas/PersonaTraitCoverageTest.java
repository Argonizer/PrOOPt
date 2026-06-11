/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.personas;

import io.github.argonizer.states.annotation.Trait;
import io.github.argonizer.states.meta.PersonaMetaReader;
import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.states.meta.TraitMetadata;
import io.github.argonizer.states.personas.human.Consumer;
import io.github.argonizer.states.personas.human.Person;
import io.github.argonizer.states.personas.npc.GuardNpc;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonaTraitCoverageTest {

    @Test
    void personHasExpectedTraitGroups() {
        PersonaMetadata meta = PersonaMetaReader.read(Person.class);
        List<String> traitNames = meta.traits().stream().map(TraitMetadata::snakeName).toList();

        assertTrue(traitNames.contains("stress_sensitivity"), "Should have stress_sensitivity");
        assertTrue(traitNames.contains("emotional_valence"), "Should have emotional_valence");
        assertTrue(traitNames.contains("openness"), "Should have openness");
        assertTrue(traitNames.contains("resilience"), "Should have resilience");
    }

    @Test
    void consumerInheritsPersonTraits() {
        PersonaMetadata meta = PersonaMetaReader.read(Consumer.class);
        List<String> traitNames = meta.traits().stream().map(TraitMetadata::snakeName).toList();

        // Inherited from Person
        assertTrue(traitNames.contains("stress_sensitivity"));
        // Consumer-specific
        assertTrue(traitNames.contains("price_sensitivity"));
        assertTrue(traitNames.contains("churn_risk"));
        assertTrue(traitNames.contains("brand_loyalty_propensity"));
    }

    @Test
    void guardNpcHasTrustAndDutyTraits() {
        PersonaMetadata meta = PersonaMetaReader.read(GuardNpc.class);
        List<String> traitNames = meta.traits().stream().map(TraitMetadata::snakeName).toList();

        assertTrue(traitNames.contains("trust_in_player"));
        assertTrue(traitNames.contains("duty_orientation"));
        assertTrue(traitNames.contains("alert_state"));
    }

    @Test
    void allTraitsHaveNonBlankDescriptions() {
        for (Class<?> cls : List.of(Person.class, Consumer.class, GuardNpc.class)) {
            PersonaMetadata meta = PersonaMetaReader.read(cls);
            for (TraitMetadata t : meta.traits()) {
                assertFalse(t.description().isBlank(),
                        cls.getSimpleName() + "." + t.snakeName() + " has blank description");
            }
        }
    }
}
