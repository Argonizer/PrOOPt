/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.event;

import io.github.argonizer.states.annotation.PersonaSubscriber;
import io.github.argonizer.states.subscriber.PersonaEventBus;
import io.github.argonizer.states.subscriber.SubscriberRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Registers any Spring bean annotated with {@link PersonaSubscriber}
 * with the {@link PersonaEventBus} after construction.
 */
public class SubscriberBeanPostProcessor implements BeanPostProcessor {

    private final PersonaEventBus eventBus;

    public SubscriberBeanPostProcessor(PersonaEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(PersonaSubscriber.class)) {
            SubscriberRegistry.register(bean, eventBus);
        }
        return bean;
    }
}
