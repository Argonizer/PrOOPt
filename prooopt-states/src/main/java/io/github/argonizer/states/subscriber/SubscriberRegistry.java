/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.subscriber;

import io.github.argonizer.states.annotation.OnPersonaEvent;
import io.github.argonizer.states.annotation.PersonaSubscriber;
import io.github.argonizer.states.event.PersonaEvent;
import io.github.argonizer.states.event.PersonaEventType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans objects annotated with {@link PersonaSubscriber} and registers their
 * {@link OnPersonaEvent} methods with a {@link PersonaEventBus}.
 */
public final class SubscriberRegistry {

    private SubscriberRegistry() {}

    public static void register(Object subscriber, PersonaEventBus bus) {
        Class<?> cls = subscriber.getClass();
        if (cls.getAnnotation(PersonaSubscriber.class) == null) return;

        for (Method m : cls.getDeclaredMethods()) {
            OnPersonaEvent ann = m.getAnnotation(OnPersonaEvent.class);
            if (ann == null) continue;
            m.setAccessible(true);

            PersonaEventType type = ann.type();
            Class<?> personaClass = ann.persona();

            bus.subscribe(type, event -> {
                if (!personaClass.getSimpleName().equals(event.personaType())) return;
                if (!matchesTrait(ann, event)) return;
                try {
                    if (m.getParameterCount() == 0) {
                        m.invoke(subscriber);
                    } else {
                        m.invoke(subscriber, event);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error dispatching event to " + m, e);
                }
            });
        }
    }

    private static boolean matchesTrait(OnPersonaEvent ann, PersonaEvent event) {
        if (!ann.trait().isEmpty()) {
            return event.affectedTraits().contains(ann.trait());
        }
        if (ann.traits().length > 0) {
            for (String t : ann.traits()) {
                if (event.affectedTraits().contains(t)) return true;
            }
            return false;
        }
        return true;
    }
}
