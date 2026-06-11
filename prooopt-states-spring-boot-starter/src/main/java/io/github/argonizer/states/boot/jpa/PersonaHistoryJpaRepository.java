/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PersonaHistoryJpaRepository extends JpaRepository<PersonaHistoryEntity, Long> {

    @Query("SELECT h FROM PersonaHistoryEntity h " +
           "WHERE h.personaId = :id AND h.personaType = :type " +
           "AND h.changedAt >= :from AND h.changedAt <= :to " +
           "ORDER BY h.changedAt ASC")
    List<PersonaHistoryEntity> findHistory(@Param("id") String personaId,
                                           @Param("type") String personaType,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to);
}
