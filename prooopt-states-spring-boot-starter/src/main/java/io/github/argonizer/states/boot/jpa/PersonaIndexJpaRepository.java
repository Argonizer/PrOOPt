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

import java.util.List;

public interface PersonaIndexJpaRepository extends JpaRepository<PersonaIndexEntity, Long> {

    List<PersonaIndexEntity> findByPersonaIdAndPersonaType(String personaId, String personaType);

    @Modifying
    @Query("DELETE FROM PersonaIndexEntity e WHERE e.personaId = :id AND e.personaType = :type")
    void deleteByPersonaIdAndPersonaType(@Param("id") String personaId, @Param("type") String personaType);
}
