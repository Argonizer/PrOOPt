/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.api;

import java.util.List;

public record BuildResult<T>(
        List<T> succeeded,
        List<FailedBatch<T>> failed
) {
    public double successRate() {
        int total = succeeded.size() + failed.stream().mapToInt(b -> b.attemptedIds().size()).sum();
        return total == 0 ? 1.0 : (double) succeeded.size() / total;
    }
}
