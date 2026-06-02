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
import io.github.argonizer.prooopt.model.ExecutionStream;
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
        ExecutionStep step = new ExecutionStep("S1.1", "S1", "normalize", null, null,
                Map.of("text", "${userInput}"), List.of(), "$clean", 0);
        ExecutionStream stream = new ExecutionStream("S1", List.of(step));
        ExecutionPlan template = new ExecutionPlan("template-trace", List.of(stream), "$clean");

        ExecutionPlan live = instantiator.instantiate(template, "the live contract");

        assertEquals("the live contract",
                live.streams().get(0).steps().get(0).args().get("text"));
    }

    @Test
    void leavesInternalVariableReferencesUntouched() {
        ExecutionStep step = new ExecutionStep("S1.2", "S1", "summarize", null, null,
                Map.of("text", "$clean"), List.of("S1.1"), "$summary", 0);
        ExecutionStream stream = new ExecutionStream("S1", List.of(step));
        ExecutionPlan template = new ExecutionPlan("t", List.of(stream), "$summary");

        ExecutionPlan live = instantiator.instantiate(template, "live");

        assertEquals("$clean", live.streams().get(0).steps().get(0).args().get("text"),
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
        ExecutionStep a = new ExecutionStep("S1.1", "S1", "f", null, null,
                Map.of("x", "${input}"), List.of(), "$a", 0);
        ExecutionStep b = new ExecutionStep("S1.2", "S1", "g", null, null,
                Map.of("y", "$a"), List.of("S1.1"), "$b", 0);
        ExecutionStream stream = new ExecutionStream("S1", List.of(a, b));
        ExecutionPlan template = new ExecutionPlan("t", List.of(stream), "$b");

        ExecutionPlan live = instantiator.instantiate(template, "in");

        assertEquals(1, live.streams().size());
        assertEquals(2, live.streams().get(0).steps().size());
        assertEquals("$b", live.output());
        assertEquals(List.of("S1.1"), live.streams().get(0).steps().get(1).dependsOn());
    }
}
