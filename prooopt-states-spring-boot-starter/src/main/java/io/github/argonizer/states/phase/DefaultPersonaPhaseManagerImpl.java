/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.phase;

import io.github.argonizer.states.event.EventPipeline;
import io.github.argonizer.states.event.PersonaEvent;
import io.github.argonizer.states.subscriber.PersonaEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Default in-memory implementation of {@link PersonaPhaseManager}.
 */
public class DefaultPersonaPhaseManagerImpl<T> implements PersonaPhaseManager<T> {

    private final List<T> registered = new CopyOnWriteArrayList<>();
    private final Map<String, List<Consumer<T>>> traitChangeHandlers = new ConcurrentHashMap<>();
    private final List<PendingBroadcast> pendingNextCycle = new CopyOnWriteArrayList<>();

    @Override
    public void broadcast(PersonaEvent event, EventPipeline pipeline) {
        // Deliver to all registered personas that pass the pipeline filter
        for (T persona : registered) {
            if (pipeline.accepts(event)) {
                // Delivery is a no-op here — real delivery done via EventBus
            }
        }
    }

    @Override
    public void onTraitChange(String trigger, Consumer<T> reaction) {
        traitChangeHandlers.computeIfAbsent(trigger, k -> new CopyOnWriteArrayList<>()).add(reaction);
    }

    @Override
    public void scheduleNextCycle(PersonaEvent event, EventPipeline pipeline) {
        pendingNextCycle.add(new PendingBroadcast(event, pipeline));
    }

    public void flushNextCycle() {
        List<PendingBroadcast> toProcess = new ArrayList<>(pendingNextCycle);
        pendingNextCycle.clear();
        for (PendingBroadcast pb : toProcess) {
            broadcast(pb.event(), pb.pipeline());
        }
    }

    @Override
    public List<T> registeredPersonas() {
        return List.copyOf(registered);
    }

    @Override
    public void register(T persona) {
        registered.add(persona);
    }

    @Override
    public void deregister(T persona) {
        registered.remove(persona);
    }

    private record PendingBroadcast(PersonaEvent event, EventPipeline pipeline) {}
}
