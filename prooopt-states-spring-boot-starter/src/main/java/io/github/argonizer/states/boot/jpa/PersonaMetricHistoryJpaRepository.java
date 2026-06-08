/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaMetricHistoryJpaRepository extends JpaRepository<PersonaMetricHistoryEntity, Long> {
}
