/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

import java.time.Instant;

/**
 * Represents a row in {@code prooopt_persona_metrics}.
 */
public class PersonaMetricRecord {

    private String personaId;
    private String personaType;
    private String metricName;
    private double metricValue;
    private Instant computedAt;

    public PersonaMetricRecord() {}

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public String getPersonaType() { return personaType; }
    public void setPersonaType(String personaType) { this.personaType = personaType; }

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }

    public double getMetricValue() { return metricValue; }
    public void setMetricValue(double metricValue) { this.metricValue = metricValue; }

    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
}
