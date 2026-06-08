/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Defines a population segment for {@link PersonaGeneratorBuilder}.
 */
public final class Segment {

    private final String description;
    private final double proportion;
    private final List<PinnedTrait> pinnedTraits;
    private final Map<String, double[]> distributions;

    private Segment(Builder b) {
        this.description = b.description;
        this.proportion = b.proportion;
        this.pinnedTraits = List.copyOf(b.pinnedTraits);
        this.distributions = Map.copyOf(b.distributions);
    }

    public String description()             { return description; }
    public double proportion()              { return proportion; }
    public List<PinnedTrait> pinnedTraits() { return pinnedTraits; }
    public Map<String, double[]> distributions() { return distributions; }

    public static Builder of(String description, double proportion) {
        return new Builder(description, proportion);
    }

    public record PinnedTrait(String traitName, Object value) {}

    public static final class Builder {
        private final String description;
        private final double proportion;
        private final List<PinnedTrait> pinnedTraits = new ArrayList<>();
        private final Map<String, double[]> distributions = new java.util.LinkedHashMap<>();

        private Builder(String description, double proportion) {
            this.description = description;
            this.proportion = proportion;
        }

        public Builder pinTrait(String traitName, Object value) {
            this.pinnedTraits.add(new PinnedTrait(traitName, value));
            return this;
        }

        public Builder normalDistribution(String traitName, double mean, double stdDev) {
            this.distributions.put(traitName, new double[]{mean, stdDev});
            return this;
        }

        public Segment build() { return new Segment(this); }
    }
}
