/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

import java.time.Instant;

public record PendingEvent(
        PersonaEvent event,
        EventTiming timing,
        BlastRadius blastRadius,
        AttenuationModel attenuation,
        Instant scheduledFor
) {}
