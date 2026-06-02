/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.exception.CyclicDependencyException;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;
import io.github.argonizer.prooopt.model.ExecutionStream;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DagValidatorTest {

    private final DagValidator validator = new DagValidator();

    // ------------------------------------------------------------------ helpers

    private static ExecutionStep step(String id, String streamId, String... deps) {
        return new ExecutionStep(id, streamId, "echo", null, null,
                Map.of(), List.of(deps), "$" + id.replace(".", "_"), 0);
    }

    private static ExecutionPlan singleStream(String output, ExecutionStep... steps) {
        return new ExecutionPlan("t", List.of(new ExecutionStream("S1", List.of(steps))), output);
    }

    // ------------------------------------------------------------------ valid plans

    @Test
    void validate_linearChain_passes() {
        // S1.1 → S1.2 → S1.3
        ExecutionPlan plan = singleStream("$S1_3",
                step("S1.1", "S1"),
                step("S1.2", "S1", "S1.1"),
                step("S1.3", "S1", "S1.2"));
        assertDoesNotThrow(() -> validator.validate(plan));
    }

    @Test
    void validate_parallelBranches_passes() {
        // S1.1 → S1.2 and S1.1 → S1.3, both → S1.4
        ExecutionPlan plan = singleStream("$S1_4",
                step("S1.1", "S1"),
                step("S1.2", "S1", "S1.1"),
                step("S1.3", "S1", "S1.1"),
                step("S1.4", "S1", "S1.2", "S1.3"));
        assertDoesNotThrow(() -> validator.validate(plan));
    }

    @Test
    void validate_crossStreamDependency_passes() {
        // S1: S1.1 → S1.2 (depends on S2.3)
        // S2: S2.1 → S2.2 → S2.3
        ExecutionStep s1_1 = step("S1.1", "S1");
        ExecutionStep s1_2 = step("S1.2", "S1", "S2.3");     // cross-stream dep
        ExecutionStep s2_1 = step("S2.1", "S2");
        ExecutionStep s2_2 = step("S2.2", "S2", "S2.1");
        ExecutionStep s2_3 = step("S2.3", "S2", "S2.2");

        ExecutionPlan plan = new ExecutionPlan("t", List.of(
                new ExecutionStream("S1", List.of(s1_1, s1_2)),
                new ExecutionStream("S2", List.of(s2_1, s2_2, s2_3))),
                "$S1_2");
        assertDoesNotThrow(() -> validator.validate(plan));
    }

    // ------------------------------------------------------------------ invalid plans

    @Test
    void validate_cyclicDependency_throwsCyclicDependencyException() {
        // S1.1 → S1.2 → S1.3 → S1.1 (cycle)
        ExecutionStep s1 = step("S1.1", "S1", "S1.3");
        ExecutionStep s2 = step("S1.2", "S1", "S1.1");
        ExecutionStep s3 = step("S1.3", "S1", "S1.2");

        ExecutionPlan plan = singleStream("$S1_3", s1, s2, s3);

        CyclicDependencyException ex = assertThrows(CyclicDependencyException.class,
                () -> validator.validate(plan));
        // Cycle step IDs must appear in the exception message and cycle list
        String msg = ex.getMessage();
        assertTrue(msg.contains("S1.1") || msg.contains("S1.2") || msg.contains("S1.3"),
                "exception message must reference cyclic step IDs: " + msg);
        assertTrue(ex.getCycle().size() >= 2, "cycle list must contain at least 2 nodes");
    }

    @Test
    void validate_unknownDependency_throwsPrOOPtConfigException() {
        ExecutionStep step = step("S1.1", "S1", "nonexistent");
        ExecutionPlan plan = singleStream("$S1_1", step);

        PrOOPtConfigException ex = assertThrows(PrOOPtConfigException.class,
                () -> validator.validate(plan));
        assertTrue(ex.getMessage().contains("nonexistent"));
    }

    @Test
    void validate_duplicateStepIds_throwsPrOOPtConfigException() {
        ExecutionStep a = step("S1.1", "S1");
        ExecutionStep b = step("S1.1", "S2");   // same stepId in a different stream

        ExecutionPlan plan = new ExecutionPlan("t", List.of(
                new ExecutionStream("S1", List.of(a)),
                new ExecutionStream("S2", List.of(b))),
                "$S1_1");
        assertThrows(PrOOPtConfigException.class, () -> validator.validate(plan));
    }

    @Test
    void validate_missingOutputStep_throwsPrOOPtConfigException() {
        ExecutionStep step = step("S1.1", "S1");
        // Output references a stepId/variable that doesn't exist
        ExecutionPlan plan = new ExecutionPlan("t",
                List.of(new ExecutionStream("S1", List.of(step))), "$doesNotExist");

        assertThrows(PrOOPtConfigException.class, () -> validator.validate(plan));
    }
}
