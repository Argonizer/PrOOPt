/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.api;

import io.github.argonizer.states.engine.PersonaStateDiff;
import io.github.argonizer.states.engine.UpdateSource;

import java.time.Instant;

public record PersonaStateChange(
        String personaId,
        String personaType,
        PersonaStateDiff diff,
        UpdateSource source,
        Instant changedAt
) {}
