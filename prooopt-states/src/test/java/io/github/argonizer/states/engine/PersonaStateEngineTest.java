/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.engine;

import io.github.argonizer.states.llm.LlmGateway;
import io.github.argonizer.states.meta.PersonaMetaReader;
import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.states.personas.npc.GuardNpc;
import io.github.argonizer.states.store.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PersonaStateEngineTest {

    private PersonaStateEngine engine;
    private InMemoryPersonaStore store;
    private PersonaMetadata meta;

    @BeforeEach
    void setUp() {
        store = new InMemoryPersonaStore();
        LlmGateway mockGateway = (prompt, tier) ->
                "{\"duty_orientation\":75,\"suspicion\":40,\"trust_in_player\":20," +
                "\"bribability\":10,\"alert_state\":\"NORMAL\",\"morale\":80," +
                "\"fatigue\":20,\"political_sympathy\":50,\"encounter_log\":\"quiet night\"}";
        engine = new PersonaStateEngine(mockGateway, store, ModelTier.CLOUD_ADVANCED);
        meta = PersonaMetaReader.read(GuardNpc.class);
    }

    @Test
    void initialisePopulatesTraits() {
        GuardNpc guard = new GuardNpc("guard-001", "Sergeant Maren");
        engine.initialise(guard, meta, "guard-001", "Veteran city guard, loyal and cautious.");

        assertEquals(75, guard.getDutyOrientation());
        assertEquals(40, guard.getSuspicion());
        assertNotNull(store.findById("guard-001", "GuardNpc"));
    }

    @Test
    void updateAppliesChanges() {
        GuardNpc guard = new GuardNpc("guard-002", "Corporal Hess");
        engine.initialise(guard, meta, "guard-002", "Green recruit");

        int before = guard.getSuspicion();
        engine.update(guard, meta,
                "The player was caught pickpocketing near the gate.", UpdateSource.EXTERNAL, List.of());

        // After update with mock LLM returning suspicion=40, value should be 40
        assertEquals(40, guard.getSuspicion());
    }

    @Test
    void retireMarksClearly() {
        GuardNpc guard = new GuardNpc("guard-003", "Captain Elara");
        engine.initialise(guard, meta, "guard-003", "Veteran captain");
        engine.retire(guard, meta, "Left the city watch");

        Optional<PersonaStateRecord> rec = store.findById("guard-003", "GuardNpc");
        assertTrue(rec.isPresent());
        assertTrue(rec.get().isRetired());
    }

    @Test
    void restoreUndoesRetirement() {
        GuardNpc guard = new GuardNpc("guard-004", "Private Vale");
        engine.initialise(guard, meta, "guard-004", "New recruit");
        engine.retire(guard, meta, "Test retirement");
        engine.restore(guard, meta, "Reinstated");

        Optional<PersonaStateRecord> rec = store.findById("guard-004", "GuardNpc");
        assertTrue(rec.isPresent());
        assertFalse(rec.get().isRetired());
    }

    /** Minimal in-memory store for tests. */
    static class InMemoryPersonaStore implements PersonaStore {
        private final java.util.Map<String, PersonaStateRecord> states = new java.util.LinkedHashMap<>();
        private final java.util.List<PersonaHistoryRecord> history = new java.util.ArrayList<>();

        @Override public void persist(PersonaWriteUnit unit) {
            PersonaStateRecord r = unit.stateRecord();
            states.put(r.getPersonaId() + "/" + r.getPersonaType(), r);
            history.add(unit.historyRecord());
        }

        @Override public Optional<PersonaStateRecord> findById(String id, String type) {
            return Optional.ofNullable(states.get(id + "/" + type));
        }

        @Override public List<PersonaStateRecord> findAllActive(String type) {
            return states.values().stream().filter(r -> r.getPersonaType().equals(type) && !r.isRetired()).toList();
        }
        @Override public List<PersonaStateRecord> findAllRetired(String type) {
            return states.values().stream().filter(r -> r.getPersonaType().equals(type) && r.isRetired()).toList();
        }
        @Override public List<PersonaStateRecord> findAll(String type) {
            return states.values().stream().filter(r -> r.getPersonaType().equals(type)).toList();
        }
        @Override public List<PersonaStateRecord> findWhere(String type, List<IndexCondition> c, boolean incRetired) {
            return findAllActive(type);
        }
        @Override public void updateRetirement(String id, String type, boolean retired, Instant at, String reason) {
            PersonaStateRecord r = states.get(id + "/" + type);
            if (r != null) { r.setRetired(retired); r.setRetiredAt(at); r.setRetirementReason(reason); }
        }
        @Override public void persistMetric(PersonaMetricRecord metric, PersonaMetricHistoryRecord history) {}
        @Override public Optional<PersonaMetricRecord> findMetric(String id, String type, String name) { return Optional.empty(); }
        @Override public List<PersonaHistoryRecord> findHistory(String id, String type, Instant from, Instant to) {
            return history.stream()
                    .filter(h -> h.getPersonaId().equals(id) && h.getPersonaType().equals(type))
                    .filter(h -> !h.getChangedAt().isBefore(from) && !h.getChangedAt().isAfter(to))
                    .toList();
        }
    }
}
