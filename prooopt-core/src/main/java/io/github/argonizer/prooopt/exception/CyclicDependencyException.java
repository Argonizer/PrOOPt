/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.exception;

import java.util.List;

/**
 * Thrown when an {@link io.github.argonizer.prooopt.model.ExecutionPlan} contains a circular
 * dependency between steps.
 *
 * <p>Detected at plan load time via topological sort (Kahn's algorithm), never at execution time.</p>
 */
public class CyclicDependencyException extends RuntimeException {

    private final List<String> cycle;

    public CyclicDependencyException(List<String> cycle) {
        super("Circular dependency detected in execution plan: " + String.join(" → ", cycle));
        this.cycle = List.copyOf(cycle);
    }

    /** Returns the step IDs forming the detected cycle. */
    public List<String> getCycle() {
        return cycle;
    }
}
