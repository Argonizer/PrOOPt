/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.argonizer.states.annotation.Persona;
import io.github.argonizer.states.boot.PersonaProperties;
import io.github.argonizer.states.engine.PersonaStateEngine;
import io.github.argonizer.states.metric.PersonaMetricEngine;
import io.github.argonizer.states.store.PersonaStore;
import io.github.argonizer.states.subscriber.PersonaEventBus;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Dynamically registers a {@link DefaultPersonaManager} bean for each
 * {@code @Persona}-annotated class found on the classpath.
 *
 * <p>This is a best-effort scan. Applications can also define their own
 * {@code PersonaManager} beans explicitly.
 */
public class PersonaManagerRegistrar implements ApplicationContextAware {

    private ApplicationContext context;
    private final PersonaStateEngine engine;
    private final PersonaStore store;
    private final PersonaMetricEngine metricEngine;
    private final PersonaEventBus eventBus;
    private final ObjectMapper objectMapper;
    private final PersonaProperties properties;

    public PersonaManagerRegistrar(PersonaStateEngine engine,
                                   PersonaStore store,
                                   PersonaMetricEngine metricEngine,
                                   PersonaEventBus eventBus,
                                   ObjectMapper objectMapper,
                                   PersonaProperties properties) {
        this.engine = engine;
        this.store = store;
        this.metricEngine = metricEngine;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public <T, ID> DefaultPersonaManager<T, ID> createManager(Class<T> personaClass) {
        return new DefaultPersonaManager<>(
                personaClass, engine, store, metricEngine, eventBus, objectMapper,
                properties.getPhaseManager().isEnabled());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }
}
