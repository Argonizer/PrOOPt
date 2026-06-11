/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

/**
 * Discriminator for {@link io.github.argonizer.states.annotation.OnPersonaEvent} handlers.
 *
 * <p>One handler annotation covers all event categories via this enum, avoiding
 * an explosion of per-event annotation types.
 */
public enum PersonaEventType {

    /** One or more trait values changed as a result of a state update. */
    TRAIT_CHANGED,

    /** A trait value crossed a declared numeric threshold. */
    THRESHOLD_CROSSED,

    /** A custom metric value crossed its configured threshold. */
    METRIC_CROSSED,

    /**
     * A lifecycle event occurred: creation, retirement, restoration,
     * or a phase transition (FORMATION → MATURITY → ENTRENCHMENT).
     */
    LIFECYCLE,

    /** A population-level trend was detected across many personas. */
    POPULATION_TREND,

    /** The internal loop auto-escalated to a deeper {@link LoopDepth}. */
    LOOP_ESCALATION
}
