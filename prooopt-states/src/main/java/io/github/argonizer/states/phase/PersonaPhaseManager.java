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
 * Manages phased persona events and multi-persona interactions.
 *
 * <p>Accessible only through {@code PersonaManager.phaseManager()} — never
 * wired as a top-level bean.
 *
 * @param <T> the persona type
 */
public interface PersonaPhaseManager<T> {

    /**
     * Broadcasts an event to a subset of personas defined by a blast radius
     * and pipeline configuration.
     */
    void broadcast(PersonaEvent event, EventPipeline pipeline);

    /**
     * Registers a reaction that fires when one persona's state change
     * affects another.
     *
     * @param trigger  the trait snake_name that triggers the reaction
     * @param reaction receives the persona whose trait changed
     */
    void onTraitChange(String trigger, Consumer<T> reaction);

    /**
     * Schedules a deferred broadcast for the next loop cycle.
     */
    void scheduleNextCycle(PersonaEvent event, EventPipeline pipeline);

    /**
     * Returns all personas currently registered with this phase manager.
     */
    List<T> registeredPersonas();

    /**
     * Registers a persona so it can receive phase-managed events.
     */
    void register(T persona);

    /**
     * Removes a persona from phase-managed delivery.
     */
    void deregister(T persona);
}
