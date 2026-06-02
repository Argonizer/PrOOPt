/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.model;

import java.util.List;

/**
 * A named stream of {@link ExecutionStep}s within an {@link ExecutionPlan}.
 *
 * <p>Streams are logical groupings — the DAG executor does not treat them as isolated units.
 * Cross-stream {@code dependsOn} references are resolved globally across all streams in the plan.</p>
 */
public record ExecutionStream(
        String streamId,
        List<ExecutionStep> steps
) {
    public ExecutionStream {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
