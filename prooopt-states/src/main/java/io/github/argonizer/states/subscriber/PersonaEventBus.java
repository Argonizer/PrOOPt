/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.subscriber;

import io.github.argonizer.states.event.PersonaEvent;
import io.github.argonizer.states.event.PersonaEventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * In-memory event bus for persona events.
 *
 * <p>Thread-safe publish/subscribe. The Spring starter replaces this with
 * {@code SpringPersonaEventBus} which can dispatch on a task executor.
 */
public class PersonaEventBus {

    private final Map<PersonaEventType, List<Consumer<PersonaEvent>>> handlers =
            new ConcurrentHashMap<>();

    public void subscribe(PersonaEventType type, Consumer<PersonaEvent> handler) {
        handlers.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
    }

    public void publish(PersonaEvent event) {
        List<Consumer<PersonaEvent>> list = handlers.get(event.type());
        if (list != null) {
            for (Consumer<PersonaEvent> h : list) {
                try { h.accept(event); } catch (Exception ignored) {}
            }
        }
    }

    public void clear() { handlers.clear(); }
}
