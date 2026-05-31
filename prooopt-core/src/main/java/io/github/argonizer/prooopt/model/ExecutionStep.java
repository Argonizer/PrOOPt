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
import java.util.Map;

/**
 * A single step of an {@link ExecutionPlan}: invoke {@code function} with {@code args}, after the
 * steps in {@code dependsOn} have completed, and store the result under {@code assignTo}.
 *
 * <p>The canonical constructor normalises {@code null} collections to empties so a planner model may
 * omit {@code args}/{@code dependsOn} for steps that take none.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionStep(
        int stepId,
        String function,
        FunctionType type,
        ModelTier model,
        Map<String, Object> args,
        List<Integer> dependsOn,
        String assignTo) {

    public ExecutionStep {
        args = args == null ? Map.of() : args;
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    }
}
