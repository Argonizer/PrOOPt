/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.ModelRouter;
import io.github.argonizer.states.boot.event.SpringPersonaEventBus;
import io.github.argonizer.states.boot.event.SubscriberBeanPostProcessor;
import io.github.argonizer.states.boot.jpa.*;
import io.github.argonizer.states.boot.manager.DefaultPersonaManager;
import io.github.argonizer.states.engine.PersonaStateEngine;
import io.github.argonizer.states.llm.LlmGateway;
import io.github.argonizer.states.llm.PersonaLlmClient;
import io.github.argonizer.states.metric.PersonaMetricEngine;
import io.github.argonizer.states.store.PersonaStore;
import io.github.argonizer.states.subscriber.PersonaEventBus;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for the PrOOPt persona states module.
 *
 * <p>Registers the core infrastructure beans: store, engine, event bus.
 * Individual {@link DefaultPersonaManager} beans are registered by
 * {@link PersonaManagerRegistrar} which scans for {@code @Persona} classes.
 */
@AutoConfiguration
@EnableConfigurationProperties(PersonaProperties.class)
@EnableJpaRepositories(basePackages = "io.github.argonizer.states.boot.jpa")
public class PersonaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PersonaEventBus personaEventBus(ApplicationEventPublisher publisher) {
        return new SpringPersonaEventBus(publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmGateway personaLlmGateway(ObjectProvider<ModelRouter> routerProvider) {
        ModelRouter router = routerProvider.getIfAvailable();
        if (router == null) {
            return (prompt, tier) -> "{\"error\":\"No ModelRouter available\"}";
        }
        return new PersonaLlmClient(router);
    }

    @Bean
    @ConditionalOnMissingBean
    public PersonaStateEngine personaStateEngine(LlmGateway gateway, PersonaStore store,
                                                  PersonaProperties properties) {
        ModelTier tier = ModelTier.fromString(properties.getModelTier());
        return new PersonaStateEngine(gateway, store, tier);
    }

    @Bean
    @ConditionalOnMissingBean
    public PersonaStore personaStore(PersonaStateJpaRepository stateRepo,
                                     PersonaIndexJpaRepository indexRepo,
                                     PersonaHistoryJpaRepository historyRepo,
                                     PersonaMetricJpaRepository metricRepo,
                                     PersonaMetricHistoryJpaRepository metricHistoryRepo,
                                     EntityManager em) {
        return new JpaPersonaStore(stateRepo, indexRepo, historyRepo, metricRepo, metricHistoryRepo, em);
    }

    @Bean
    @ConditionalOnMissingBean
    public PersonaMetricEngine personaMetricEngine(LlmGateway gateway,
                                                   PersonaStore store,
                                                   PersonaEventBus eventBus) {
        return new PersonaMetricEngine(gateway, store, eventBus);
    }

    @Bean
    public SubscriberBeanPostProcessor subscriberBeanPostProcessor(PersonaEventBus eventBus) {
        return new SubscriberBeanPostProcessor(eventBus);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper personaObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @ConditionalOnMissingBean
    public io.github.argonizer.states.boot.manager.PersonaManagerRegistrar personaManagerRegistrar(
            PersonaStateEngine engine, PersonaStore store, PersonaMetricEngine metricEngine,
            PersonaEventBus eventBus, ObjectMapper objectMapper, PersonaProperties properties) {
        return new io.github.argonizer.states.boot.manager.PersonaManagerRegistrar(
                engine, store, metricEngine, eventBus, objectMapper, properties);
    }
}
