/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.annotation;

import io.github.argonizer.states.event.LoopDepth;
import io.github.argonizer.states.store.Store;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a POJO as an LLM-managed persona whose {@code @Trait} fields are resolved,
 * persisted, evolved, and queried by PrOOPt States.
 *
 * <p>The developer describes <em>what the entity is</em> via {@link #value()}; the LLM
 * decides <em>what it becomes</em> in response to events. Evolution and internal-loop
 * configuration are absorbed directly into this annotation — no separate
 * {@code @EvolutionPolicy} or {@code @InternalLoop} annotations exist.
 *
 * <p>Exactly one field on the annotated class must carry {@link PersonaId}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Persona {

    /**
     * Natural-language description of what this persona is.
     * Forms the core prompt contract and the default internal-loop description.
     */
    String value();

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    /** Whether state should be persisted at all. Defaults to {@code true}. */
    boolean persist() default true;

    /** Backing store strategy. Defaults to {@link Store#JPA}. */
    Store store() default Store.JPA;

    /**
     * Table name override for any schema-level suffix or prefix conventions.
     * Blank = snake_case of the simple class name is used as a display label;
     * all personas share the same five physical tables regardless.
     */
    String table() default "";

    /** Whether state changes are recorded in {@code prooopt_persona_history}. */
    boolean trackHistory() default true;

    // -------------------------------------------------------------------------
    // Intrinsic time-based evolution (no separate annotation)
    // -------------------------------------------------------------------------

    /**
     * Spring cron expression or scheduler macro ({@code @daily}, {@code @weekly}, …)
     * that controls when the evolution batch job runs.
     * Blank disables evolution entirely.
     */
    String evolutionSchedule() default "";

    /**
     * Natural-language description of how this persona drifts over time.
     * Injected into the evolution LLM prompt as the evolution policy.
     */
    String evolutionDescription() default "";

    // -------------------------------------------------------------------------
    // Internal thought loop (no separate annotation)
    // -------------------------------------------------------------------------

    /** Whether the internal thought feedback loop is enabled. Defaults to {@code false}. */
    boolean internalLoop() default false;

    /**
     * Spring cron or macro for the loop schedule.
     * Defaults to {@code @daily}.
     */
    String loopSchedule() default "@daily";

    /** Depth of the loop applied by default when scheduled. */
    LoopDepth loopDepth() default LoopDepth.MODERATE;

    /**
     * Natural-language description of what drives the internal loop.
     * Blank falls back to {@link #value()}.
     */
    String loopDescription() default "";

    /**
     * Minimum hours since the last <em>external</em> update before the loop
     * is allowed to run. Set to {@code 0} to disable the quiet-period gate.
     */
    int loopQuietPeriodHours() default 0;
}
