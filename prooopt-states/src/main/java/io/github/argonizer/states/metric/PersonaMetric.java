/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.metric;

import io.github.argonizer.states.engine.UpdateSource;
import io.github.argonizer.states.event.Direction;

/**
 * Definition of a computed metric for a persona.
 *
 * <p>Use the fluent builder: {@code PersonaMetric.define(prompt).named(...).build()}.
 */
public final class PersonaMetric {

    private final String prompt;
    private final String name;
    private final double rangeMin;
    private final double rangeMax;
    private final UpdateSource refreshOn;
    private final boolean emitEvents;
    private final double threshold;
    private final Direction thresholdDirection;

    private PersonaMetric(Builder b) {
        this.prompt = b.prompt;
        this.name = b.name;
        this.rangeMin = b.rangeMin;
        this.rangeMax = b.rangeMax;
        this.refreshOn = b.refreshOn;
        this.emitEvents = b.emitEvents;
        this.threshold = b.threshold;
        this.thresholdDirection = b.thresholdDirection;
    }

    public String prompt()           { return prompt; }
    public String name()             { return name; }
    public double rangeMin()         { return rangeMin; }
    public double rangeMax()         { return rangeMax; }
    public UpdateSource refreshOn()  { return refreshOn; }
    public boolean emitEvents()      { return emitEvents; }
    public double threshold()        { return threshold; }
    public Direction thresholdDirection() { return thresholdDirection; }

    public static Builder define(String prompt) { return new Builder(prompt); }

    public static final class Builder {
        private final String prompt;
        private String name = "metric";
        private double rangeMin = 0;
        private double rangeMax = 100;
        private UpdateSource refreshOn = UpdateSource.EXTERNAL;
        private boolean emitEvents = false;
        private double threshold = Double.NaN;
        private Direction thresholdDirection = Direction.ABOVE;

        private Builder(String prompt) { this.prompt = prompt; }

        public Builder named(String n)               { this.name = n; return this; }
        public Builder range(double min, double max) { this.rangeMin = min; this.rangeMax = max; return this; }
        public Builder refreshOn(UpdateSource s)     { this.refreshOn = s; return this; }
        public Builder emitEvents(boolean e)         { this.emitEvents = e; return this; }
        public Builder threshold(double t)           { this.threshold = t; return this; }
        public Builder thresholdDirection(Direction d){ this.thresholdDirection = d; return this; }

        public PersonaMetric build() { return new PersonaMetric(this); }
    }
}
