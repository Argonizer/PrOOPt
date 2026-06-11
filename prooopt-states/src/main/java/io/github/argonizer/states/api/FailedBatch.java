/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.api;

import java.util.List;

public record FailedBatch<T>(
        int batchIndex,
        List<String> attemptedIds,
        Throwable cause
) {}
