/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.context.PrOOPtContext;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PlanInstantiatorTest {

    private final PlanInstantiator instantiator = new PlanInstantiator();

    @AfterEach
    void cleanup() {
        PrOOPtContext.clear();
    }

    @Test
    void bindsTopLevelInputPlaceholders() {
        ExecutionStep step = new ExecutionStep(1, "normalize", null, null,
                Map.of("text", "${userInput}"), List.of(), "$clean");
        ExecutionPlan template = new ExecutionPlan("template-trace", List.of(step), "$clean");

        ExecutionPlan live = instantiator.instantiate(template, "the live contract");

        assertEquals("the live contract", live.steps().get(0).args().get("text"));
    }

    @Test
    void leavesInternalVariableReferencesUntouched() {
        ExecutionStep step = new ExecutionStep(2, "summarize", null, null,
                Map.of("text", "$clean"), List.of(1), "$summary");
        ExecutionPlan template = new ExecutionPlan("t", List.of(step), "$summary");

        ExecutionPlan live = instantiator.instantiate(template, "live");

        assertEquals("$clean", live.steps().get(0).args().get("text"),
                "step-to-step references resolve at execution time, not instantiation");
    }

    @Test
    void assignsFreshTraceId() {
        ExecutionPlan template = new ExecutionPlan("old-trace", List.of(), "$out");
        String runTrace = PrOOPtContext.getTraceId();
        ExecutionPlan live = instantiator.instantiate(template, "x");
        assertEquals(runTrace, live.traceId());
        assertNotEquals("old-trace", live.traceId());
    }

    @Test
    void preservesStructureAndOutput() {
        ExecutionStep a = new ExecutionStep(1, "f", null, null, Map.of("x", "${input}"), List.of(), "$a");
        ExecutionStep b = new ExecutionStep(2, "g", null, null, Map.of("y", "$a"), List.of(1), "$b");
        ExecutionPlan template = new ExecutionPlan("t", List.of(a, b), "$b");

        ExecutionPlan live = instantiator.instantiate(template, "in");

        assertEquals(2, live.steps().size());
        assertEquals("$b", live.output());
        assertEquals(List.of(1), live.steps().get(1).dependsOn());
    }
}
