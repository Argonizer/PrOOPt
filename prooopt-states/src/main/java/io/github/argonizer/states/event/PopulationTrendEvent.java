/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

public final class PopulationTrendEvent extends PersonaEvent {
    private final String traitName;
    private final Trend trend;
    private final double populationMean;

    public PopulationTrendEvent(Builder b) {
        super(b);
        this.traitName = b.traitName;
        this.trend = b.trend;
        this.populationMean = b.populationMean;
    }

    public String traitName()     { return traitName; }
    public Trend trend()          { return trend; }
    public double populationMean(){ return populationMean; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder extends PersonaEvent.Builder<Builder> {
        private String traitName = "";
        private Trend trend = Trend.NONE;
        private double populationMean = 0;

        public Builder traitName(String n)     { this.traitName = n; return this; }
        public Builder trend(Trend t)          { this.trend = t; return this; }
        public Builder populationMean(double m){ this.populationMean = m; return this; }

        @Override public PopulationTrendEvent build() {
            type(PersonaEventType.POPULATION_TREND);
            return new PopulationTrendEvent(this);
        }
    }
}
