/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for persona state.
 *
 * <p>The core module defines this interface; the starter provides the JPA
 * implementation ({@code JpaPersonaStore}). Tests use an in-memory
 * implementation. This separation keeps the core module free of Spring/JPA.
 *
 * <p>Callers must not manage transaction boundaries — the implementation owns
 * the transaction so that all three writes ({@link PersonaWriteUnit}) commit
 * or roll back together.
 */
public interface PersonaStore {

    /**
     * Atomically writes the state blob, index rows (delete-then-insert), and
     * history row for a single persona update.
     *
     * @param unit the write unit; all three components are written in one transaction.
     */
    void persist(PersonaWriteUnit unit);

    /**
     * Loads the state record for the given persona id and type.
     *
     * @param personaId   the id value as a string.
     * @param personaType the simple class name.
     * @return the record, or empty if not found.
     */
    Optional<PersonaStateRecord> findById(String personaId, String personaType);

    /**
     * Returns all non-retired state records for the given persona type.
     */
    List<PersonaStateRecord> findAllActive(String personaType);

    /**
     * Returns all retired state records for the given persona type.
     */
    List<PersonaStateRecord> findAllRetired(String personaType);

    /**
     * Returns all state records (active and retired) for the given persona type.
     */
    List<PersonaStateRecord> findAll(String personaType);

    /**
     * Executes a query translated by {@link PersonaQueryTranslator} and returns
     * the matching state records.
     *
     * @param personaType     the simple class name.
     * @param indexConditions list of parameterised {@link IndexCondition} clauses.
     * @param includeRetired  whether retired personas are included.
     */
    List<PersonaStateRecord> findWhere(String personaType,
                                       List<IndexCondition> indexConditions,
                                       boolean includeRetired);

    /**
     * Updates the retirement status of a single persona without changing its state.
     *
     * @param personaId       the id value as a string.
     * @param personaType     the simple class name.
     * @param retired         the new retired flag.
     * @param retiredAt       the retirement timestamp (null when restoring).
     * @param retirementReason the reason text (null when restoring).
     */
    void updateRetirement(String personaId, String personaType, boolean retired,
                          java.time.Instant retiredAt, String retirementReason);

    /**
     * Persists a single metric value (upsert + history row).
     */
    void persistMetric(PersonaMetricRecord metric, PersonaMetricHistoryRecord history);

    /**
     * Loads the current metric value for a persona and metric name.
     */
    Optional<PersonaMetricRecord> findMetric(String personaId, String personaType, String metricName);

    /**
     * Returns history records for a persona and type within the given time window,
     * ordered by {@code changedAt} ascending.
     */
    List<PersonaHistoryRecord> findHistory(String personaId, String personaType,
                                           java.time.Instant from, java.time.Instant to);
}
