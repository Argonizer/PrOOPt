/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

import java.util.List;

/**
 * Bundles all three records that must be written atomically in a single
 * state update.
 *
 * <p>The {@link PersonaStore} implementation owns the transaction boundary;
 * the engine prepares this unit and hands it off.
 *
 * @param stateRecord   the updated state blob record.
 * @param indexRecords  one record per {@code @Trait(index=true)} field.
 * @param historyRecord the history row for this update.
 */
public record PersonaWriteUnit(
        PersonaStateRecord stateRecord,
        List<PersonaIndexRecord> indexRecords,
        PersonaHistoryRecord historyRecord
) {}
