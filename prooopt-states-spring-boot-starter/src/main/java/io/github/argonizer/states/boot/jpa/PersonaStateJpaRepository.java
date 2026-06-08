/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PersonaStateJpaRepository
        extends JpaRepository<PersonaStateEntity, PersonaStateEntity.PersonaStatePK> {

    Optional<PersonaStateEntity> findByPersonaIdAndPersonaType(String personaId, String personaType);

    List<PersonaStateEntity> findByPersonaTypeAndRetiredFalse(String personaType);

    List<PersonaStateEntity> findByPersonaTypeAndRetiredTrue(String personaType);

    List<PersonaStateEntity> findByPersonaType(String personaType);

    @Modifying
    @Query("UPDATE PersonaStateEntity e SET e.retired = :retired, e.retiredAt = :retiredAt, " +
           "e.retirementReason = :reason WHERE e.personaId = :id AND e.personaType = :type")
    void updateRetirement(@Param("id") String personaId, @Param("type") String personaType,
                          @Param("retired") boolean retired, @Param("retiredAt") Instant retiredAt,
                          @Param("reason") String reason);
}
