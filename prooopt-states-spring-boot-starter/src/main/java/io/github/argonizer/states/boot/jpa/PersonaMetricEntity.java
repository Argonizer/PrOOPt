/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.jpa;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "prooopt_persona_metrics")
@IdClass(PersonaMetricEntity.MetricPK.class)
public class PersonaMetricEntity {

    @Id
    @Column(name = "persona_id")
    private String personaId;

    @Id
    @Column(name = "persona_type")
    private String personaType;

    @Id
    @Column(name = "metric_name")
    private String metricName;

    @Column(name = "metric_value", nullable = false)
    private double metricValue;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    public PersonaMetricEntity() {}

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

    public static class MetricPK implements Serializable {
        private String personaId;
        private String personaType;
        private String metricName;
        public MetricPK() {}
        @Override public boolean equals(Object o) {
            if (!(o instanceof MetricPK pk)) return false;
            return Objects.equals(personaId, pk.personaId)
                    && Objects.equals(personaType, pk.personaType)
                    && Objects.equals(metricName, pk.metricName);
        }
        @Override public int hashCode() { return Objects.hash(personaId, personaType, metricName); }
    }
}
