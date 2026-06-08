/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.batch;

import io.github.argonizer.states.engine.PersonaStateEngine;
import io.github.argonizer.states.event.LoopDepth;
import io.github.argonizer.states.meta.PersonaMetadata;
import org.springframework.batch.item.ItemProcessor;

/**
 * Spring Batch item processor that runs the internal loop on each persona.
 */
public class PersonaLoopProcessor<T> implements ItemProcessor<T, T> {

    private final PersonaStateEngine engine;
    private final PersonaMetadata meta;
    private final LoopDepth depth;

    public PersonaLoopProcessor(PersonaStateEngine engine, PersonaMetadata meta, LoopDepth depth) {
        this.engine = engine;
        this.meta = meta;
        this.depth = depth;
    }

    @Override
    public T process(T persona) {
        engine.runLoop(persona, meta, depth);
        return persona;
    }
}
