/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

/**
 * A single parameterised condition against {@code prooopt_persona_index}.
 *
 * <p>Produced by {@link PersonaQueryTranslator} from a developer-supplied
 * {@code loadWhere(String)} predicate. The store implementation binds
 * {@link #parameter()} safely — it is never string-concatenated into SQL.
 *
 * @param traitName  the snake_case trait name (column: trait_name).
 * @param operator   the comparison operator: {@code =, !=, <, <=, >, >=, IN}.
 * @param parameter  the typed comparison value (safe parameter binding).
 * @param traitType  the declared type for correct CAST in the store.
 * @param logicalOp  {@code AND} or {@code OR} — how this clause joins the next.
 */
public record IndexCondition(
        String traitName,
        String operator,
        Object parameter,
        TraitType traitType,
        String logicalOp
) {}
