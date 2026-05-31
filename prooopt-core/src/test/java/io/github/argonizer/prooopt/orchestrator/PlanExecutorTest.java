/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.exception.PrOOPtExecutionException;
import io.github.argonizer.prooopt.model.ExecutionStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanExecutorTest {

    // ---- wave assignment ----

    private static ExecutionStep step(int id, List<Integer> deps) {
        return new ExecutionStep(id, "fn" + id, null, null, Map.of(), deps, "$v" + id);
    }

    private static Map<Integer, ExecutionStep> byId(List<ExecutionStep> steps) {
        var map = new java.util.HashMap<Integer, ExecutionStep>();
        for (var s : steps) map.put(s.stepId(), s);
        return map;
    }

    @Test
    void noDepStepsAreWaveZero() {
        var steps = List.of(step(1, List.of()), step(2, List.of()));
        TreeMap<Integer, List<ExecutionStep>> waves = PlanExecutor.assignWaves(steps, byId(steps));
        assertEquals(1, waves.size());
        assertEquals(2, waves.get(0).size());
    }

    @Test
    void chainProducesIncrementalWaves() {
        // 1 → 2 → 3
        var s1 = step(1, List.of());
        var s2 = step(2, List.of(1));
        var s3 = step(3, List.of(2));
        var steps = List.of(s1, s2, s3);
        TreeMap<Integer, List<ExecutionStep>> waves = PlanExecutor.assignWaves(steps, byId(steps));
        assertEquals(3, waves.size());
        assertEquals(s1, waves.get(0).get(0));
        assertEquals(s2, waves.get(1).get(0));
        assertEquals(s3, waves.get(2).get(0));
    }

    @Test
    void diamondDependencyCollapsesCorrectly() {
        // 1 → 2, 1 → 3, 2 → 4, 3 → 4
        var s1 = step(1, List.of());
        var s2 = step(2, List.of(1));
        var s3 = step(3, List.of(1));
        var s4 = step(4, List.of(2, 3));
        var steps = List.of(s1, s2, s3, s4);
        TreeMap<Integer, List<ExecutionStep>> waves = PlanExecutor.assignWaves(steps, byId(steps));
        // wave 0: step 1; wave 1: steps 2,3; wave 2: step 4
        assertEquals(3, waves.size());
        assertEquals(2, waves.get(1).size());
        assertEquals(s4, waves.get(2).get(0));
    }

    @Test
    void cycleDetectionThrows() {
        var s1 = step(1, List.of(2));
        var s2 = step(2, List.of(1));
        var steps = List.of(s1, s2);
        assertThrows(PrOOPtExecutionException.class,
                () -> PlanExecutor.assignWaves(steps, byId(steps)));
    }

    // ---- canonical ----

    @Test
    void canonicalStripsLeadingDollar() {
        assertEquals("summary", PlanExecutor.canonical("$summary"));
    }

    @Test
    void canonicalStripsBracketForm() {
        assertEquals("date", PlanExecutor.canonical("${date}"));
    }

    @Test
    void canonicalReturnsNullForNull() {
        assertNull(PlanExecutor.canonical(null));
    }

    @Test
    void canonicalReturnsPlainStringUnchanged() {
        assertEquals("output", PlanExecutor.canonical("output"));
    }
}
