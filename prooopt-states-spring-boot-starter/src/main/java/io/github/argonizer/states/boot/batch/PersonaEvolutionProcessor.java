/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.batch;

import io.github.argonizer.states.engine.PersonaStateEngine;
import io.github.argonizer.states.meta.PersonaMetadata;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

/**
 * Spring Batch item processor that calls {@code evolveState} on each persona.
 */
public class PersonaEvolutionProcessor<T> implements ItemProcessor<T, T> {

    private final PersonaStateEngine engine;
    private final PersonaMetadata meta;
    private final List<String> evolutionRules;

    public PersonaEvolutionProcessor(PersonaStateEngine engine,
                                     PersonaMetadata meta,
                                     List<String> evolutionRules) {
        this.engine = engine;
        this.meta = meta;
        this.evolutionRules = evolutionRules;
    }

    @Override
    public T process(T persona) {
        engine.evolve(persona, meta);
        return persona;
    }
}
