/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.event;

import io.github.argonizer.states.event.PersonaEvent;
import io.github.argonizer.states.event.PersonaEventType;
import io.github.argonizer.states.subscriber.PersonaEventBus;
import org.springframework.context.ApplicationEventPublisher;

import java.util.function.Consumer;

/**
 * Spring-aware event bus that publishes persona events via
 * {@link ApplicationEventPublisher} as well as the in-memory bus.
 */
public class SpringPersonaEventBus extends PersonaEventBus {

    private final ApplicationEventPublisher publisher;

    public SpringPersonaEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(PersonaEvent event) {
        super.publish(event);
        publisher.publishEvent(event);
    }
}
