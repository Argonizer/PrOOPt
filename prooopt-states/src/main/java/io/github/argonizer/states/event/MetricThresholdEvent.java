/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

public final class MetricThresholdEvent extends PersonaEvent {
    private final String metricName;
    private final double value;
    private final double threshold;
    private final Direction direction;

    public MetricThresholdEvent(Builder b) {
        super(b);
        this.metricName = b.metricName;
        this.value = b.value;
        this.threshold = b.threshold;
        this.direction = b.direction;
    }

    public String metricName() { return metricName; }
    public double value()      { return value; }
    public double threshold()  { return threshold; }
    public Direction direction(){ return direction; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder extends PersonaEvent.Builder<Builder> {
        private String metricName;
        private double value;
        private double threshold;
        private Direction direction = Direction.ABOVE;

        public Builder metricName(String n)   { this.metricName = n; return this; }
        public Builder value(double v)        { this.value = v; return this; }
        public Builder threshold(double t)    { this.threshold = t; return this; }
        public Builder direction(Direction d) { this.direction = d; return this; }

        @Override public MetricThresholdEvent build() {
            type(PersonaEventType.METRIC_CROSSED);
            return new MetricThresholdEvent(this);
        }
    }
}
