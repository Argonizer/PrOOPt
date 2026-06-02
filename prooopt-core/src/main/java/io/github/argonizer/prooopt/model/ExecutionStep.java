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
 * Represents a single node in a PrOOPt DAG execution plan.
 *
 * <p>Step IDs are globally unique across all streams in a plan. The convention is
 * "{streamId}.{stepNumber}" (e.g. "S1.2", "S4.5") but any unique string is valid.</p>
 *
 * <p>{@code dependsOn} may reference step IDs in ANY stream — this is the mechanism for cross-stream
 * data dependencies.</p>
 *
 * <p>The canonical constructor normalises {@code null} collections to empties so a planner model may
 * omit {@code args}/{@code dependsOn} for steps that take none.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionStep(
        String stepId,                  // globally unique, e.g. "S1.2"
        String streamId,                // parent stream, e.g. "S1"
        String function,                // registered function name
        FunctionType type,              // PROMPT or CODE
        ModelTier model,                // only relevant for PROMPT steps
        Map<String, Object> args,       // {paramName: value or "$stepId" reference}
        List<String> dependsOn,         // step IDs this step waits for (any stream)
        String assignTo,                // key under which to store this step's result
        long timeoutMs                  // per-step timeout; 0 = inherit global DAG timeout
) {
    public ExecutionStep {
        args = args == null ? Map.of() : args;
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    }

    /** Returns a copy of this step with its {@code args} replaced (used by {@code PlanInstantiator}). */
    public ExecutionStep withArgs(Map<String, Object> newArgs) {
        return new ExecutionStep(stepId, streamId, function, type, model, newArgs, dependsOn,
                assignTo, timeoutMs);
    }
}
