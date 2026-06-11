/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Aggregate report for a persona's trait history over a given window.
 */
public final class EvolutionReport {

    private final String personaId;
    private final String personaType;
    private final EvolutionWindow window;
    private final Map<String, TraitEvolution> traitEvolutions;
    private final List<MetricEvolution> metricEvolutions;
    private final List<SignificantEvent> significantEvents;
    private final List<InflectionPoint> inflectionPoints;
    private final List<TraitCorrelation> correlations;
    private final List<BehaviourPattern> behaviourPatterns;
    private final ForwardProjection projection;
    private final TrajectoryAssessment trajectory;
    private final String llmNarrative;
    private final Instant generatedAt;

    private EvolutionReport(Builder b) {
        this.personaId = b.personaId;
        this.personaType = b.personaType;
        this.window = b.window;
        this.traitEvolutions = b.traitEvolutions != null ? b.traitEvolutions : Map.of();
        this.metricEvolutions = b.metricEvolutions != null ? b.metricEvolutions : List.of();
        this.significantEvents = b.significantEvents != null ? b.significantEvents : List.of();
        this.inflectionPoints = b.inflectionPoints != null ? b.inflectionPoints : List.of();
        this.correlations = b.correlations != null ? b.correlations : List.of();
        this.behaviourPatterns = b.behaviourPatterns != null ? b.behaviourPatterns : List.of();
        this.projection = b.projection;
        this.trajectory = b.trajectory != null ? b.trajectory : TrajectoryAssessment.STABLE;
        this.llmNarrative = b.llmNarrative;
        this.generatedAt = Instant.now();
    }

    public String personaId()      { return personaId; }
    public String personaType()    { return personaType; }
    public EvolutionWindow window(){ return window; }
    public Map<String, TraitEvolution> traitEvolutions() { return traitEvolutions; }
    public List<MetricEvolution> metricEvolutions()      { return metricEvolutions; }
    public List<SignificantEvent> significantEvents()     { return significantEvents; }
    public List<InflectionPoint> inflectionPoints()      { return inflectionPoints; }
    public List<TraitCorrelation> correlations()         { return correlations; }
    public List<BehaviourPattern> behaviourPatterns()    { return behaviourPatterns; }
    public ForwardProjection projection()                { return projection; }
    public TrajectoryAssessment trajectory()             { return trajectory; }
    public String llmNarrative()                         { return llmNarrative; }
    public Instant generatedAt()                         { return generatedAt; }

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Evolution Report — ").append(personaType).append(" [").append(personaId).append("]\n\n");
        if (window != null) {
            sb.append("**Window**: ").append(window.from()).append(" → ").append(window.to()).append("\n\n");
        }
        sb.append("**Trajectory**: ").append(trajectory).append("\n\n");
        if (llmNarrative != null && !llmNarrative.isBlank()) {
            sb.append("## Narrative\n").append(llmNarrative).append("\n\n");
        }
        if (!traitEvolutions.isEmpty()) {
            sb.append("## Trait Evolutions\n");
            traitEvolutions.forEach((name, te) -> {
                TraitStatistics s = te.statistics();
                sb.append("- **").append(name).append("**: mean=").append(String.format("%.2f", s.mean()))
                  .append(", trend=").append(s.trend()).append(", dominant=").append(te.dominantSource()).append("\n");
            });
            sb.append("\n");
        }
        return sb.toString();
    }

    public String toJson() {
        return "{\"personaId\":\"" + personaId + "\",\"personaType\":\"" + personaType
                + "\",\"trajectory\":\"" + trajectory + "\"}";
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String personaId;
        private String personaType;
        private EvolutionWindow window;
        private Map<String, TraitEvolution> traitEvolutions;
        private List<MetricEvolution> metricEvolutions;
        private List<SignificantEvent> significantEvents;
        private List<InflectionPoint> inflectionPoints;
        private List<TraitCorrelation> correlations;
        private List<BehaviourPattern> behaviourPatterns;
        private ForwardProjection projection;
        private TrajectoryAssessment trajectory;
        private String llmNarrative;

        public Builder personaId(String v)          { this.personaId = v; return this; }
        public Builder personaType(String v)        { this.personaType = v; return this; }
        public Builder window(EvolutionWindow v)    { this.window = v; return this; }
        public Builder traitEvolutions(Map<String, TraitEvolution> v) { this.traitEvolutions = v; return this; }
        public Builder metricEvolutions(List<MetricEvolution> v)      { this.metricEvolutions = v; return this; }
        public Builder significantEvents(List<SignificantEvent> v)    { this.significantEvents = v; return this; }
        public Builder inflectionPoints(List<InflectionPoint> v)      { this.inflectionPoints = v; return this; }
        public Builder correlations(List<TraitCorrelation> v)         { this.correlations = v; return this; }
        public Builder behaviourPatterns(List<BehaviourPattern> v)    { this.behaviourPatterns = v; return this; }
        public Builder projection(ForwardProjection v)                { this.projection = v; return this; }
        public Builder trajectory(TrajectoryAssessment v)             { this.trajectory = v; return this; }
        public Builder llmNarrative(String v)                         { this.llmNarrative = v; return this; }

        public EvolutionReport build() { return new EvolutionReport(this); }
    }
}
