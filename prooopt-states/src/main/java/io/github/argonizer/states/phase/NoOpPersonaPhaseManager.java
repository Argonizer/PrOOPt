/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.phase;

import io.github.argonizer.states.event.EventPipeline;
import io.github.argonizer.states.event.PersonaEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * No-op implementation used when {@code phase-manager.enabled=false}.
 * All methods are silent no-ops.
 */
public final class NoOpPersonaPhaseManager<T> implements PersonaPhaseManager<T> {

    @Override public void broadcast(PersonaEvent event, EventPipeline pipeline) {}
    @Override public void onTraitChange(String trigger, Consumer<T> reaction) {}
    @Override public void scheduleNextCycle(PersonaEvent event, EventPipeline pipeline) {}
    @Override public List<T> registeredPersonas() { return List.of(); }
    @Override public void register(T persona) {}
    @Override public void deregister(T persona) {}
}
