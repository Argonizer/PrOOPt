/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record EvolutionWindow(Instant from, Instant to) {

    public static EvolutionWindow last(long n, ChronoUnit unit) {
        Instant now = Instant.now();
        return new EvolutionWindow(now.minus(n, unit), now);
    }

    public static EvolutionWindow between(Instant from, Instant to) {
        return new EvolutionWindow(from, to);
    }

    public static EvolutionWindow allTime() {
        return new EvolutionWindow(Instant.EPOCH, Instant.now());
    }
}
