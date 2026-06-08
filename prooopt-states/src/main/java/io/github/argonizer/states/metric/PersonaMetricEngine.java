/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.metric;

import io.github.argonizer.states.event.Direction;
import io.github.argonizer.states.event.MetricThresholdEvent;
import io.github.argonizer.states.llm.LlmGateway;
import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.states.store.PersonaMetricHistoryRecord;
import io.github.argonizer.states.store.PersonaMetricRecord;
import io.github.argonizer.states.store.PersonaStore;
import io.github.argonizer.states.subscriber.PersonaEventBus;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.github.argonizer.prooopt.model.ModelTier;

/**
 * Computes and persists persona metrics via LLM calls.
 */
public final class PersonaMetricEngine {

    private final LlmGateway gateway;
    private final PersonaStore store;
    private final PersonaEventBus eventBus;
    private final Map<String, PersonaMetric> registry = new ConcurrentHashMap<>();

    public PersonaMetricEngine(LlmGateway gateway, PersonaStore store, PersonaEventBus eventBus) {
        this.gateway = gateway;
        this.store = store;
        this.eventBus = eventBus;
    }

    public void register(PersonaMetric metric) {
        registry.put(metric.name(), metric);
    }

    public double compute(Object persona, PersonaMetadata meta, String metricName) {
        PersonaMetric metric = registry.get(metricName);
        if (metric == null) throw new IllegalArgumentException("Unknown metric: " + metricName);

        String personaId = getPersonaId(persona, meta);
        String prompt = buildMetricPrompt(metric, persona, meta);
        String response = gateway.call(prompt, ModelTier.CLOUD_ADVANCED);
        double value = parseDouble(response, metric.rangeMin(), metric.rangeMax());

        Instant now = Instant.now();
        PersonaMetricRecord record = new PersonaMetricRecord();
        record.setPersonaId(personaId);
        record.setPersonaType(meta.personaClass().getSimpleName());
        record.setMetricName(metricName);
        record.setMetricValue(value);
        record.setComputedAt(now);
        PersonaMetricHistoryRecord history = new PersonaMetricHistoryRecord();
        history.setPersonaId(personaId);
        history.setPersonaType(meta.personaClass().getSimpleName());
        history.setMetricName(metricName);
        history.setMetricValue(value);
        history.setComputedAt(now);
        store.persistMetric(record, history);

        if (metric.emitEvents() && !Double.isNaN(metric.threshold())) {
            boolean crossed = metric.thresholdDirection() == Direction.ABOVE
                    ? value >= metric.threshold()
                    : value <= metric.threshold();
            if (crossed) {
                eventBus.publish(MetricThresholdEvent.builder()
                        .personaId(personaId)
                        .personaType(meta.personaClass().getSimpleName())
                        .metricName(metricName)
                        .value(value)
                        .threshold(metric.threshold())
                        .direction(metric.thresholdDirection())
                        .build());
            }
        }
        return value;
    }

    public double getCached(Object persona, PersonaMetadata meta, String metricName) {
        String personaId = getPersonaId(persona, meta);
        Optional<PersonaMetricRecord> rec = store.findMetric(personaId,
                meta.personaClass().getSimpleName(), metricName);
        return rec.map(PersonaMetricRecord::getMetricValue).orElse(Double.NaN);
    }

    private String buildMetricPrompt(PersonaMetric metric, Object persona, PersonaMetadata meta) {
        return "Evaluate the following metric for persona " + meta.personaClass().getSimpleName()
                + ".\n\nMetric: " + metric.name()
                + "\nDefinition: " + metric.prompt()
                + "\nRange: " + metric.rangeMin() + " to " + metric.rangeMax()
                + "\n\nReturn ONLY a single number within the range.";
    }

    private double parseDouble(String response, double min, double max) {
        if (response == null) return min;
        try {
            double v = Double.parseDouble(response.trim().replaceAll("[^0-9.\\-]", ""));
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return min;
        }
    }

    private String getPersonaId(Object persona, PersonaMetadata meta) {
        try {
            meta.idField().setAccessible(true);
            Object id = meta.idField().get(persona);
            return id != null ? id.toString() : "unknown";
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
