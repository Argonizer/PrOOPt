/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

import java.time.Duration;
import java.time.Instant;

/**
 * Controls when a fired event is delivered to its target personas.
 *
 * <p>Use the static factory methods rather than the constants where delay or
 * scheduling is needed. {@link #NEXT_LOOP_CYCLE} buffers the event and merges
 * it with the next internal-loop LLM call in a single round-trip.
 */
public final class EventTiming {

    /** Deliver immediately when the event is fired. */
    public static final EventTiming IMMEDIATE = new EventTiming(Kind.IMMEDIATE, null, null);

    /**
     * Buffer the event and resolve it together with the next internal-loop
     * cycle for each target persona, combining both in a single LLM call.
     */
    public static final EventTiming NEXT_LOOP_CYCLE = new EventTiming(Kind.NEXT_LOOP_CYCLE, null, null);

    /** Deliver after the specified delay. */
    public static EventTiming delayed(Duration delay) {
        return new EventTiming(Kind.DELAYED, delay, null);
    }

    /** Deliver at an absolute instant. */
    public static EventTiming scheduled(Instant at) {
        return new EventTiming(Kind.SCHEDULED, null, at);
    }

    public enum Kind { IMMEDIATE, DELAYED, SCHEDULED, NEXT_LOOP_CYCLE }

    private final Kind kind;
    private final Duration delay;
    private final Instant scheduledAt;

    private EventTiming(Kind kind, Duration delay, Instant scheduledAt) {
        this.kind = kind;
        this.delay = delay;
        this.scheduledAt = scheduledAt;
    }

    /** @return the timing kind. */
    public Kind kind() { return kind; }

    /** @return the delay, non-null only when {@code kind == DELAYED}. */
    public Duration delay() { return delay; }

    /** @return the delivery instant, non-null only when {@code kind == SCHEDULED}. */
    public Instant scheduledAt() { return scheduledAt; }
}
