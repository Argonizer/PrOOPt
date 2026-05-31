/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A directed acyclic plan produced by the orchestrator's execution model: an ordered set of
 * {@link ExecutionStep}s plus the name of the variable holding the final {@code output}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionPlan(
        String traceId,
        List<ExecutionStep> steps,
        String output) {

    public ExecutionPlan {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
