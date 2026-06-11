/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "prooopt_persona_metrics_history")
public class PersonaMetricHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "persona_id", nullable = false)
    private String personaId;

    @Column(name = "persona_type", nullable = false)
    private String personaType;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "metric_value", nullable = false)
    private double metricValue;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    public PersonaMetricHistoryEntity() {}

    public Long getId()                { return id; }
    public String getPersonaId()       { return personaId; }
    public void setPersonaId(String v) { this.personaId = v; }
    public String getPersonaType()     { return personaType; }
    public void setPersonaType(String v){ this.personaType = v; }
    public String getMetricName()      { return metricName; }
    public void setMetricName(String v){ this.metricName = v; }
    public double getMetricValue()     { return metricValue; }
    public void setMetricValue(double v){ this.metricValue = v; }
    public Instant getComputedAt()     { return computedAt; }
    public void setComputedAt(Instant v){ this.computedAt = v; }
}
