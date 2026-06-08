/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Fluent builder for configuring how a persona event is routed and filtered
 * before reaching subscribers.
 */
public final class EventPipeline {

    private BlastRadius blastRadius = BlastRadius.ALL;
    private AttenuationModel attenuation = AttenuationModel.NONE;
    private BackpressureStrategy backpressure = BackpressureStrategy.BUFFER;
    private EventTiming timing = EventTiming.IMMEDIATE;
    private final List<Predicate<PersonaEvent>> filters = new ArrayList<>();
    private int bufferCapacity = 1000;

    private EventPipeline() {}

    public static EventPipeline create() { return new EventPipeline(); }

    public EventPipeline blastRadius(BlastRadius r) { this.blastRadius = r; return this; }
    public EventPipeline attenuation(AttenuationModel m) { this.attenuation = m; return this; }
    public EventPipeline backpressure(BackpressureStrategy s) { this.backpressure = s; return this; }
    public EventPipeline timing(EventTiming t) { this.timing = t; return this; }
    public EventPipeline filter(Predicate<PersonaEvent> f) { this.filters.add(f); return this; }
    public EventPipeline bufferCapacity(int n) { this.bufferCapacity = n; return this; }
    public EventPipeline minSeverity(EventSeverity min) {
        return filter(e -> e.severity().ordinal() >= min.ordinal());
    }

    public BlastRadius blastRadius()          { return blastRadius; }
    public AttenuationModel attenuation()     { return attenuation; }
    public BackpressureStrategy backpressure(){ return backpressure; }
    public EventTiming timing()               { return timing; }
    public int bufferCapacity()               { return bufferCapacity; }

    public boolean accepts(PersonaEvent event) {
        return filters.stream().allMatch(f -> f.test(event));
    }
}
