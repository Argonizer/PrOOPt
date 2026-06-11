/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.states.event.LoopDepth;
import io.github.argonizer.states.lifecycle.PersonaLifecycle;
import io.github.argonizer.states.lifecycle.PersonaUpdateVetoException;
import io.github.argonizer.states.llm.LlmGateway;
import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.states.meta.TraitMetadata;
import io.github.argonizer.states.store.PersonaHistoryRecord;
import io.github.argonizer.states.store.PersonaIndexRecord;
import io.github.argonizer.states.store.PersonaStateRecord;
import io.github.argonizer.states.store.PersonaStore;
import io.github.argonizer.states.store.PersonaWriteUnit;
import io.github.argonizer.states.store.TraitType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Core engine that orchestrates all persona state transitions.
 *
 * <p>Each operation:
 * <ol>
 *   <li>Builds the appropriate LLM prompt via {@link PersonaPromptBuilder}.</li>
 *   <li>Calls the LLM through {@link LlmGateway} (no direct HTTP).</li>
 *   <li>Parses and validates the response via {@link PersonaResponseParser}.</li>
 *   <li>Computes the diff, applies it to the in-memory object, and builds the
 *       {@link PersonaWriteUnit}.</li>
 *   <li>Hands the unit to {@link PersonaStore#persist(PersonaWriteUnit)} which
 *       commits all three writes atomically.</li>
 *   <li>Invokes {@link PersonaLifecycle} hooks in order.</li>
 * </ol>
 *
 * <p>The engine is stateless and thread-safe. Each {@code PersonaManager} instance
 * holds one engine.
 */
public final class PersonaStateEngine {

    private static final Logger log = Logger.getLogger(PersonaStateEngine.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmGateway llm;
    private final PersonaStore store;
    private final ModelTier defaultTier;

    /**
     * @param llm         LLM gateway (wraps ModelRouter; no direct HTTP).
     * @param store       persona store for persistence.
     * @param defaultTier default model tier for persona state calls.
     */
    public PersonaStateEngine(LlmGateway llm, PersonaStore store, ModelTier defaultTier) {
        this.llm = llm;
        this.store = store;
        this.defaultTier = defaultTier;
    }

    /**
     * Initialises a new persona from a seed description.
     *
     * <p>Calls the LLM with an init prompt, applies all returned trait values,
     * stores the seed and origination date, and persists as {@link UpdateSource#SEED}.
     *
     * @param persona  the newly constructed (empty) persona instance.
     * @param meta     persona metadata.
     * @param id       the persona id value as a string.
     * @param seed     the seed description.
     * @return the populated persona.
     */
    public <T> T initialise(T persona, PersonaMetadata meta, String id, String seed) {
        String prompt = PersonaPromptBuilder.buildInitPrompt(meta, seed);
        String raw = llm.call(prompt, defaultTier);
        Map<String, Object> parsed = PersonaResponseParser.parse(raw, meta.mutableTraits());

        // Also parse fixed-identity traits from init response
        List<TraitMetadata> allNonSensitive = new ArrayList<>(meta.mutableTraits());
        allNonSensitive.addAll(meta.fixedTraits());
        Map<String, Object> allParsed = PersonaResponseParser.parse(raw, allNonSensitive);

        PersonaFieldWriter.apply(persona, meta, allParsed);

        Instant now = Instant.now();
        String phase = PersonaMetadata.phase(now);

        PersonaStateRecord state = buildStateRecord(id, meta.personaClass().getSimpleName(),
                persona, meta, seed, now, now, null, phase, 1L);
        List<PersonaIndexRecord> index = buildIndexRecords(id, meta.personaClass().getSimpleName(), persona, meta);
        PersonaHistoryRecord history = buildHistoryRecord(id, meta.personaClass().getSimpleName(),
                1L, now, prompt, Collections.emptyMap(), persona, meta, UpdateSource.SEED.name());

        store.persist(new PersonaWriteUnit(state, index, history));
        return persona;
    }

    /**
     * Applies an external event or developer-supplied prompt to a persona.
     *
     * <p>Respects the {@link PersonaLifecycle#beforeStateUpdate(String)} veto.
     * If vetoed, no LLM call is made and no state is changed.
     *
     * @param persona         the persona instance.
     * @param meta            persona metadata.
     * @param prompt          the update prompt.
     * @param source          the origin of this update.
     * @param populationRules population rules registered on the manager.
     * @return the computed diff (may be empty if the LLM returned no changes).
     * @throws IllegalStateException if the persona has been retired.
     */
    public <T> PersonaStateDiff update(T persona, PersonaMetadata meta,
                                        String prompt, UpdateSource source,
                                        List<String> populationRules) {
        String id = getPersonaId(persona, meta);
        String typeName = meta.personaClass().getSimpleName();

        PersonaStateRecord stateRec = store.findById(id, typeName)
                .orElseThrow(() -> new IllegalStateException(
                        "No state found for " + typeName + " id=" + id + ". Call create() first."));
        if (stateRec.isRetired()) {
            throw new IllegalStateException(
                    "Cannot update retired persona " + typeName + " id=" + id
                    + ". Call restore() before updating.");
        }

        if (persona instanceof PersonaLifecycle lc) {
            try { lc.beforeStateUpdate(prompt); }
            catch (PersonaUpdateVetoException e) {
                log.info("Update vetoed for " + typeName + " id=" + id + ": " + e.getMessage());
                return new PersonaStateDiff(id, typeName, prompt, source, Collections.emptyMap());
            }
        }

        Instant originationDate = stateRec.getOriginationDate();
        String currentPhase = stateRec.getCurrentPhase();
        String seed = stateRec.getSeed();

        String llmPrompt = PersonaPromptBuilder.buildUpdatePrompt(
                meta, persona, seed, currentPhase, originationDate, prompt, populationRules);
        String raw = llm.call(llmPrompt, defaultTier);
        Map<String, Object> parsed = PersonaResponseParser.parse(raw, meta.mutableTraits());

        Map<String, Object[]> changes = computeChanges(persona, meta, parsed);
        PersonaFieldWriter.apply(persona, meta, parsed);

        long newVersion = stateRec.getStateVersion() + 1;
        Instant now = Instant.now();
        String newPhase = PersonaMetadata.phase(originationDate);

        PersonaStateRecord updatedState = buildStateRecord(id, typeName,
                persona, meta, seed, originationDate, now, stateRec.getLastEvolved(), newPhase, newVersion);
        List<PersonaIndexRecord> index = buildIndexRecords(id, typeName, persona, meta);
        PersonaHistoryRecord history = buildHistoryRecord(id, typeName,
                newVersion, now, prompt, changes, persona, meta, source.name());

        store.persist(new PersonaWriteUnit(updatedState, index, history));

        PersonaStateDiff diff = new PersonaStateDiff(id, typeName, prompt, source, changes);
        if (persona instanceof PersonaLifecycle lc) {
            lc.onStateUpdated(diff);
            lc.onStatePersisted(diff);
        }
        return diff;
    }

    /**
     * Applies time-based natural evolution to a persona.
     */
    public <T> PersonaStateDiff evolve(T persona, PersonaMetadata meta) {
        String id = getPersonaId(persona, meta);
        String typeName = meta.personaClass().getSimpleName();
        PersonaStateRecord stateRec = store.findById(id, typeName).orElseThrow();

        String llmPrompt = PersonaPromptBuilder.buildEvolutionPrompt(
                meta, persona, stateRec.getCurrentPhase(),
                stateRec.getOriginationDate(), stateRec.getLastEvolved());
        String raw = llm.call(llmPrompt, defaultTier);
        Map<String, Object> parsed = PersonaResponseParser.parse(raw, meta.mutableTraits());
        Map<String, Object[]> changes = computeChanges(persona, meta, parsed);
        PersonaFieldWriter.apply(persona, meta, parsed);

        Instant now = Instant.now();
        long newVersion = stateRec.getStateVersion() + 1;
        String newPhase = PersonaMetadata.phase(stateRec.getOriginationDate());
        PersonaStateRecord updatedState = buildStateRecord(id, typeName,
                persona, meta, stateRec.getSeed(), stateRec.getOriginationDate(), now, now, newPhase, newVersion);
        updatedState.setLastEvolved(now);
        List<PersonaIndexRecord> index = buildIndexRecords(id, typeName, persona, meta);
        PersonaHistoryRecord history = buildHistoryRecord(id, typeName,
                newVersion, now, null, changes, persona, meta, UpdateSource.EVOLUTION.name());

        store.persist(new PersonaWriteUnit(updatedState, index, history));
        return new PersonaStateDiff(id, typeName, null, UpdateSource.EVOLUTION, changes);
    }

    /**
     * Runs the internal thought feedback loop at the given depth.
     */
    public <T> PersonaStateDiff runLoop(T persona, PersonaMetadata meta, LoopDepth depth) {
        String id = getPersonaId(persona, meta);
        String typeName = meta.personaClass().getSimpleName();
        PersonaStateRecord stateRec = store.findById(id, typeName).orElseThrow();

        String llmPrompt = PersonaPromptBuilder.buildLoopPrompt(
                meta, persona, stateRec.getSeed(), depth,
                meta.annotation().loopQuietPeriodHours());
        String raw = llm.call(llmPrompt, defaultTier);
        Map<String, Object> parsed = PersonaResponseParser.parse(raw, meta.mutableTraits());
        Map<String, Object[]> changes = computeChanges(persona, meta, parsed);
        PersonaFieldWriter.apply(persona, meta, parsed);

        UpdateSource src = depth == LoopDepth.DEEP
                ? UpdateSource.INTERNAL_LOOP_DEEP : UpdateSource.INTERNAL_LOOP;
        Instant now = Instant.now();
        long newVersion = stateRec.getStateVersion() + 1;
        String newPhase = PersonaMetadata.phase(stateRec.getOriginationDate());
        PersonaStateRecord updatedState = buildStateRecord(id, typeName,
                persona, meta, stateRec.getSeed(), stateRec.getOriginationDate(),
                now, stateRec.getLastEvolved(), newPhase, newVersion);
        List<PersonaIndexRecord> index = buildIndexRecords(id, typeName, persona, meta);
        PersonaHistoryRecord history = buildHistoryRecord(id, typeName,
                newVersion, now, null, changes, persona, meta, src.name());
        store.persist(new PersonaWriteUnit(updatedState, index, history));
        return new PersonaStateDiff(id, typeName, null, src, changes);
    }

    /**
     * Soft-deletes a persona, preventing further updates.
     */
    public <T> void retire(T persona, PersonaMetadata meta, String reason) {
        String id = getPersonaId(persona, meta);
        String typeName = meta.personaClass().getSimpleName();
        Instant now = Instant.now();
        store.updateRetirement(id, typeName, true, now, reason);
        if (persona instanceof PersonaLifecycle lc) lc.onStateRetired(reason);
    }

    /**
     * Restores a retired persona to active status.
     */
    public <T> void restore(T persona, PersonaMetadata meta, String reason) {
        String id = getPersonaId(persona, meta);
        String typeName = meta.personaClass().getSimpleName();
        store.updateRetirement(id, typeName, false, null, null);
        if (persona instanceof PersonaLifecycle lc) lc.onStateRestored(reason);
    }

    /**
     * Loads a persona's state from the store and populates its fields.
     */
    public <T> void load(T persona, PersonaMetadata meta) {
        String id = getPersonaId(persona, meta);
        String typeName = meta.personaClass().getSimpleName();
        Optional<PersonaStateRecord> opt = store.findById(id, typeName);
        if (opt.isEmpty()) return;
        PersonaStateRecord stateRec = opt.get();
        try {
            Map<String, Object> stateMap = MAPPER.readValue(
                    stateRec.getState(), new TypeReference<Map<String, Object>>() {});
            PersonaFieldWriter.apply(persona, meta, stateMap);
        } catch (Exception e) {
            log.warning("Failed to load state for " + typeName + " id=" + id + ": " + e.getMessage());
        }
        if (persona instanceof PersonaLifecycle lc) lc.onStateLoaded();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String getPersonaId(Object persona, PersonaMetadata meta) {
        try {
            Object val = meta.idField().get(persona);
            return val == null ? null : val.toString();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read @PersonaId field", e);
        }
    }

    private PersonaStateRecord buildStateRecord(String id, String typeName, Object persona,
                                                 PersonaMetadata meta, String seed,
                                                 Instant originationDate, Instant lastUpdated,
                                                 Instant lastEvolved, String phase, long version) {
        PersonaStateRecord rec = new PersonaStateRecord();
        rec.setPersonaId(id);
        rec.setPersonaType(typeName);
        rec.setSeed(seed);
        rec.setOriginationDate(originationDate);
        rec.setLastUpdated(lastUpdated);
        rec.setLastEvolved(lastEvolved);
        rec.setCurrentPhase(phase);
        rec.setStateVersion(version);
        rec.setRetired(false);
        rec.setState(toJson(persona, meta));
        return rec;
    }

    private List<PersonaIndexRecord> buildIndexRecords(String id, String typeName,
                                                        Object persona, PersonaMetadata meta) {
        List<PersonaIndexRecord> list = new ArrayList<>();
        for (TraitMetadata tm : meta.traits()) {
            if (!tm.index() || tm.sensitive()) continue;
            try {
                Object val = tm.field().get(persona);
                String strVal = val == null ? null : val.toString();
                String tt = TraitType.from(tm.type()).name();
                list.add(new PersonaIndexRecord(id, typeName, tm.snakeName(), strVal, tt));
            } catch (IllegalAccessException e) {
                log.warning("Cannot read trait " + tm.snakeName() + " for indexing: " + e.getMessage());
            }
        }
        return list;
    }

    private PersonaHistoryRecord buildHistoryRecord(String id, String typeName,
                                                     long version, Instant now, String prompt,
                                                     Map<String, Object[]> changes, Object persona,
                                                     PersonaMetadata meta, String source) {
        PersonaHistoryRecord rec = new PersonaHistoryRecord();
        rec.setPersonaId(id);
        rec.setPersonaType(typeName);
        rec.setChangedAt(now);
        rec.setStateVersion(version);
        rec.setPromptInput(prompt);
        rec.setUpdateSource(source);
        try {
            Map<String, Object[]> safeChanges = new LinkedHashMap<>(changes);
            rec.setFieldsChanged(MAPPER.writeValueAsString(safeChanges));
            rec.setFullStateAfter(toJson(persona, meta));
        } catch (Exception e) {
            rec.setFieldsChanged("{}");
            rec.setFullStateAfter("{}");
        }
        return rec;
    }

    private String toJson(Object persona, PersonaMetadata meta) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (TraitMetadata tm : meta.traits()) {
            if (tm.sensitive()) continue;
            try { map.put(tm.snakeName(), tm.field().get(persona)); }
            catch (IllegalAccessException e) { map.put(tm.snakeName(), null); }
        }
        try { return MAPPER.writeValueAsString(map); } catch (Exception e) { return "{}"; }
    }

    private Map<String, Object[]> computeChanges(Object persona, PersonaMetadata meta,
                                                   Map<String, Object> newValues) {
        Map<String, Object[]> changes = new LinkedHashMap<>();
        Map<String, TraitMetadata> byName = new LinkedHashMap<>();
        for (TraitMetadata tm : meta.mutableTraits()) byName.put(tm.snakeName(), tm);

        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            TraitMetadata tm = byName.get(entry.getKey());
            if (tm == null) continue;
            try {
                Object before = tm.field().get(persona);
                Object after = entry.getValue();
                changes.put(entry.getKey(), new Object[]{before, after});
            } catch (IllegalAccessException e) {
                changes.put(entry.getKey(), new Object[]{null, entry.getValue()});
            }
        }
        return changes;
    }
}
