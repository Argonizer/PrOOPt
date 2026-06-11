/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.api;

import io.github.argonizer.states.metric.PersonaMetric;
import io.github.argonizer.states.phase.PersonaPhaseManager;
import io.github.argonizer.states.report.CustomReport;
import io.github.argonizer.states.report.EvolutionReport;
import io.github.argonizer.states.report.EvolutionWindow;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Primary facade for managing a typed persona population.
 *
 * <p>One {@code PersonaManager<T, ID>} bean is auto-configured per
 * {@code @Persona}-annotated class. All operations are thread-safe.
 * The implementation owns transaction boundaries.
 *
 * @param <T>  the persona class
 * @param <ID> the id type (matches the {@code @PersonaId} field type)
 */
public interface PersonaManager<T, ID> {

    /** Creates and persists a new persona, seeding traits via LLM. */
    T create(ID id, String seed);

    /** Creates and persists a new persona using a structured seed object. */
    T create(ID id, Object structuredSeed);

    /** Returns a fluent builder for generating a population of personas. */
    PersonaGeneratorBuilder<T> generate();

    /** Loads and hydrates a persona by id; throws if not found. */
    T load(ID id);

    /** Returns all state records (active and retired). */
    List<T> loadAll();

    /** Returns all active (non-retired) personas. */
    List<T> loadActive();

    /** Returns all retired personas. */
    List<T> loadRetired();

    /**
     * Returns all active personas matching the predicate.
     *
     * @param condition a trait predicate such as {@code "trust_in_player < -50 AND mood = 'ANGRY'"}.
     */
    List<T> loadWhere(String condition);

    /** Returns a stream over all active personas. */
    Stream<T> stream();

    /** Updates a persona's state with the given prompt. */
    void update(T persona, String prompt);

    /** Sends the same prompt to all active personas. */
    void broadcast(String prompt);

    /** Sends the same prompt to active personas matching the where clause. */
    void broadcast(String prompt, String whereClause);

    /** Retires a persona instance. */
    void retire(T persona, String reason);

    /** Retires a persona by id. The id is passed as Object to avoid type-erasure clash. */
    void retireById(ID id, String reason);

    /** Retires all active personas matching the condition. */
    void retireWhere(String condition, String reason);

    /** Restores a retired persona. */
    void restore(T persona, String reason);

    /** Adds a population-level rule applied to all subsequent updates. */
    PersonaManager<T, ID> withRule(String populationRule);

    /** Adds a rule applied during scheduled evolution. */
    PersonaManager<T, ID> withEvolutionRule(String rule);

    /** Adds a rule applied during internal loop processing. */
    PersonaManager<T, ID> withLoopRule(String rule);

    /** Registers a computed metric definition. */
    void registerMetric(PersonaMetric metric);

    /** Computes (or returns cached) metric value for a persona instance. */
    double getMetric(T persona, String metricName);

    /** Computes population-level metric mean. */
    double getMetric(String metricName);

    /** Generates an evolution report for a persona over all available history. */
    EvolutionReport report(T persona);

    /** Generates an evolution report for a persona over the given window. */
    EvolutionReport report(T persona, EvolutionWindow window);

    /** Generates a population report over the given window. */
    EvolutionReport report(EvolutionWindow window);

    /** Generates a report for personas matching the where condition. */
    EvolutionReport reportWhere(String condition, EvolutionWindow window);

    /** Generates a comparative report for two windows. */
    EvolutionReport reportComparative(T persona, EvolutionWindow a, EvolutionWindow b);

    /** Generates a custom LLM-interpreted report from the given prompt. */
    CustomReport customReport(String prompt);

    /** Registers a listener for state changes on a single persona. */
    void observe(T persona, Consumer<PersonaStateChange> handler);

    /** Registers a listener for population-level signals. */
    void observePopulation(Consumer<PopulationSignal> handler);

    /**
     * Returns the {@link PersonaPhaseManager} for multi-persona coordination.
     * Lazily initialised on first call.
     */
    PersonaPhaseManager<T> phaseManager();
}
