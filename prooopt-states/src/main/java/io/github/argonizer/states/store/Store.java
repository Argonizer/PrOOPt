/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

/**
 * Persistence backend strategy for {@code @Persona} state.
 *
 * <p>Currently only {@code JPA} is supported. Future backends (e.g. Redis,
 * DocumentDB) may be added without modifying the annotation API.
 */
public enum Store {
    /** Persist via Spring Data JPA to any JDBC-compatible database. */
    JPA
}
