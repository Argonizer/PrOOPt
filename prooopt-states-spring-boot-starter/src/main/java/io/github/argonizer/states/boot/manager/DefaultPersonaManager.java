/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.argonizer.states.api.*;
import io.github.argonizer.states.engine.PersonaStateEngine;
import io.github.argonizer.states.engine.UpdateSource;
import io.github.argonizer.states.llm.LlmGateway;
import io.github.argonizer.states.meta.PersonaMetaReader;
import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.states.metric.PersonaMetric;
import io.github.argonizer.states.metric.PersonaMetricEngine;
import io.github.argonizer.states.phase.DefaultPersonaPhaseManagerImpl;
import io.github.argonizer.states.phase.NoOpPersonaPhaseManager;
import io.github.argonizer.states.phase.PersonaPhaseManager;
import io.github.argonizer.states.report.*;
import io.github.argonizer.states.store.*;
import io.github.argonizer.states.subscriber.PersonaEventBus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring-managed implementation of {@link PersonaManager}.
 *
 * <p>One bean per {@code @Persona}-annotated class. Thread-safe.
 * All state+index+history writes are atomic (delegated to {@link PersonaStore}).
 */
@Transactional
public class DefaultPersonaManager<T, ID> implements PersonaManager<T, ID> {

    private final Class<T> personaClass;
    private final PersonaMetadata meta;
    private final PersonaStateEngine engine;
    private final PersonaStore store;
    private final PersonaMetricEngine metricEngine;
    private final PersonaEventBus eventBus;
    private final ObjectMapper objectMapper;
    private final boolean phaseManagerEnabled;

    private final List<String> populationRules = new CopyOnWriteArrayList<>();
    private final List<String> evolutionRules = new CopyOnWriteArrayList<>();
    private final List<String> loopRules = new CopyOnWriteArrayList<>();
    private final Map<String, List<Consumer<PersonaStateChange>>> observers = new ConcurrentHashMap<>();
    private final List<Consumer<PopulationSignal>> populationObservers = new CopyOnWriteArrayList<>();

    private volatile PersonaPhaseManager<T> phaseManager;

    public DefaultPersonaManager(Class<T> personaClass,
                                 PersonaStateEngine engine,
                                 PersonaStore store,
                                 PersonaMetricEngine metricEngine,
                                 PersonaEventBus eventBus,
                                 ObjectMapper objectMapper,
                                 boolean phaseManagerEnabled) {
        this.personaClass = personaClass;
        this.meta = PersonaMetaReader.read(personaClass);
        this.engine = engine;
        this.store = store;
        this.metricEngine = metricEngine;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        this.phaseManagerEnabled = phaseManagerEnabled;
    }

    @Override
    public T create(ID id, String seed) {
        try {
            T persona = personaClass.getDeclaredConstructor().newInstance();
            engine.initialise(persona, meta, id.toString(), seed);
            return persona;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create persona " + personaClass.getSimpleName(), e);
        }
    }

    @Override
    public T create(ID id, Object structuredSeed) {
        try {
            String seedJson = objectMapper.writeValueAsString(structuredSeed);
            return create(id, seedJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PersonaGeneratorBuilder<T> generate() {
        return new PersonaGeneratorBuilder<>(personaClass, meta, engine, this);
    }

    @Override
    public T load(ID id) {
        String personaType = personaClass.getSimpleName();
        PersonaStateRecord rec = store.findById(id.toString(), personaType)
                .orElseThrow(() -> new NoSuchElementException(
                        "Persona not found: " + personaType + "/" + id));
        try {
            T persona = personaClass.getDeclaredConstructor().newInstance();
            engine.load(persona, meta);
            return persona;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> loadAll() {
        return store.findAll(personaClass.getSimpleName())
                .stream().map(r -> hydrateFromRecord(r)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> loadActive() {
        return store.findAllActive(personaClass.getSimpleName())
                .stream().map(r -> hydrateFromRecord(r)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> loadRetired() {
        return store.findAllRetired(personaClass.getSimpleName())
                .stream().map(r -> hydrateFromRecord(r)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> loadWhere(String condition) {
        List<IndexCondition> conditions = PersonaQueryTranslator.translate(condition, meta);
        return store.findWhere(personaClass.getSimpleName(), conditions, false)
                .stream().map(r -> hydrateFromRecord(r)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<T> stream() {
        return loadActive().stream();
    }

    @Override
    public void update(T persona, String prompt) {
        engine.update(persona, meta, prompt, UpdateSource.EXTERNAL, populationRules);
        notifyObservers(persona);
    }

    @Override
    public void broadcast(String prompt) {
        for (T persona : loadActive()) {
            engine.update(persona, meta, prompt, UpdateSource.EXTERNAL, populationRules);
        }
    }

    @Override
    public void broadcast(String prompt, String whereClause) {
        for (T persona : loadWhere(whereClause)) {
            engine.update(persona, meta, prompt, UpdateSource.EXTERNAL, populationRules);
        }
    }

    @Override
    public void retire(T persona, String reason) {
        engine.retire(persona, meta, reason);
    }

    @Override
    public void retireById(ID id, String reason) {
        store.updateRetirement(id.toString(), personaClass.getSimpleName(), true, Instant.now(), reason);
    }

    @Override
    public void retireWhere(String condition, String reason) {
        for (T persona : loadWhere(condition)) {
            engine.retire(persona, meta, reason);
        }
    }

    @Override
    public void restore(T persona, String reason) {
        engine.restore(persona, meta, reason);
    }

    @Override
    public PersonaManager<T, ID> withRule(String populationRule) {
        populationRules.add(populationRule);
        return this;
    }

    @Override
    public PersonaManager<T, ID> withEvolutionRule(String rule) {
        evolutionRules.add(rule);
        return this;
    }

    @Override
    public PersonaManager<T, ID> withLoopRule(String rule) {
        loopRules.add(rule);
        return this;
    }

    @Override
    public void registerMetric(PersonaMetric metric) {
        metricEngine.register(metric);
    }

    @Override
    public double getMetric(T persona, String metricName) {
        return metricEngine.compute(persona, meta, metricName);
    }

    @Override
    public double getMetric(String metricName) {
        List<T> active = loadActive();
        if (active.isEmpty()) return Double.NaN;
        return active.stream()
                .mapToDouble(p -> {
                    double v = metricEngine.getCached(p, meta, metricName);
                    return Double.isNaN(v) ? metricEngine.compute(p, meta, metricName) : v;
                })
                .average().orElse(Double.NaN);
    }

    @Override
    @Transactional(readOnly = true)
    public EvolutionReport report(T persona) {
        return report(persona, EvolutionWindow.allTime());
    }

    @Override
    @Transactional(readOnly = true)
    public EvolutionReport report(T persona, EvolutionWindow window) {
        String personaId = getPersonaId(persona);
        List<PersonaHistoryRecord> history = store.findHistory(
                personaId, personaClass.getSimpleName(), window.from(), window.to());
        return buildReport(personaId, history, window);
    }

    @Override
    @Transactional(readOnly = true)
    public EvolutionReport report(EvolutionWindow window) {
        return EvolutionReport.builder()
                .personaId("population")
                .personaType(personaClass.getSimpleName())
                .window(window)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EvolutionReport reportWhere(String condition, EvolutionWindow window) {
        return report(window);
    }

    @Override
    @Transactional(readOnly = true)
    public EvolutionReport reportComparative(T persona, EvolutionWindow a, EvolutionWindow b) {
        EvolutionReport rA = report(persona, a);
        EvolutionReport rB = report(persona, b);
        return EvolutionReport.builder()
                .personaId(getPersonaId(persona))
                .personaType(personaClass.getSimpleName())
                .window(b)
                .llmNarrative("Window A: " + rA.trajectory() + " → Window B: " + rB.trajectory())
                .build();
    }

    @Override
    public CustomReport customReport(String prompt) {
        return new CustomReport(prompt, "Custom report not yet implemented.", Instant.now());
    }

    @Override
    public void observe(T persona, Consumer<PersonaStateChange> handler) {
        observers.computeIfAbsent(getPersonaId(persona), k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    @Override
    public void observePopulation(Consumer<PopulationSignal> handler) {
        populationObservers.add(handler);
    }

    @Override
    public PersonaPhaseManager<T> phaseManager() {
        if (phaseManager == null) {
            synchronized (this) {
                if (phaseManager == null) {
                    phaseManager = phaseManagerEnabled
                            ? new DefaultPersonaPhaseManagerImpl<>()
                            : new NoOpPersonaPhaseManager<>();
                }
            }
        }
        return phaseManager;
    }

    // --- Helpers ---

    private T hydrateFromRecord(PersonaStateRecord rec) {
        try {
            T persona = personaClass.getDeclaredConstructor().newInstance();
            engine.load(persona, meta);
            return persona;
        } catch (Exception e) {
            throw new RuntimeException("Failed to hydrate " + personaClass.getSimpleName(), e);
        }
    }

    private String getPersonaId(T persona) {
        try {
            meta.idField().setAccessible(true);
            Object id = meta.idField().get(persona);
            return id != null ? id.toString() : "unknown";
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private EvolutionReport buildReport(String personaId, List<PersonaHistoryRecord> history,
                                        EvolutionWindow window) {
        Map<String, TraitStatistics> stats = TraitStatisticsEngine.compute(history);
        Map<String, TraitEvolution> evolutions = new LinkedHashMap<>();
        for (Map.Entry<String, TraitStatistics> e : stats.entrySet()) {
            evolutions.put(e.getKey(), new TraitEvolution(e.getKey(), List.of(),
                    e.getValue(), e.getValue().dominantSource()));
        }
        TrajectoryAssessment trajectory = evolutions.values().stream()
                .map(te -> te.statistics().trend())
                .anyMatch(t -> t != io.github.argonizer.states.event.Trend.NONE)
                ? TrajectoryAssessment.DRIFTING : TrajectoryAssessment.STABLE;

        return EvolutionReport.builder()
                .personaId(personaId)
                .personaType(personaClass.getSimpleName())
                .window(window)
                .traitEvolutions(evolutions)
                .trajectory(trajectory)
                .build();
    }

    private void notifyObservers(T persona) {
        String personaId = getPersonaId(persona);
        List<Consumer<PersonaStateChange>> handlers = observers.get(personaId);
        if (handlers != null) {
            PersonaStateChange change = new PersonaStateChange(
                    personaId, personaClass.getSimpleName(), null, UpdateSource.EXTERNAL, Instant.now());
            handlers.forEach(h -> h.accept(change));
        }
    }

    public List<String> evolutionRules() { return Collections.unmodifiableList(evolutionRules); }
    public List<String> loopRules()      { return Collections.unmodifiableList(loopRules); }
}
