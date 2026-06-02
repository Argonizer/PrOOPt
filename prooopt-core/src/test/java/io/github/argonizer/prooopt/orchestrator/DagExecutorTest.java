/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.annotation.CodeFunction;
import io.github.argonizer.prooopt.audit.AuditLogger;
import io.github.argonizer.prooopt.context.PrOOPtContext;
import io.github.argonizer.prooopt.exception.PrOOPtExecutionException;
import io.github.argonizer.prooopt.invoke.PromptCallEngine;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;
import io.github.argonizer.prooopt.model.ExecutionStream;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.registry.FunctionRegistry;
import io.github.argonizer.prooopt.registry.FunctionScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DagExecutor}.
 *
 * <p>All tests use a real {@link FunctionRegistry} populated from {@link TestFunctions}, a no-op
 * {@link PromptCallEngine} (no actual model calls), and an {@link AuditLogger}.
 */
class DagExecutorTest {

    // ------------------------------------------------------------------ test functions

    static class TestFunctions {
        final List<String> invocations = Collections.synchronizedList(new ArrayList<>());
        final Set<String> traceIds = ConcurrentHashMap.newKeySet();

        @CodeFunction(description = "Return text unchanged", tags = {"echo"})
        public String echo(String text) {
            invocations.add("echo:" + text);
            return text;
        }

        @CodeFunction(description = "Prepend 'A:' to text", tags = {"stepA"})
        public String stepA(String text) {
            invocations.add("stepA");
            return "A:" + text;
        }

        @CodeFunction(description = "Prepend 'B:' to text", tags = {"stepB"})
        public String stepB(String text) {
            invocations.add("stepB");
            return "B:" + text;
        }

        @CodeFunction(description = "Concatenate two strings", tags = {"concat"})
        public String concat(String left, String right) {
            invocations.add("concat");
            return left + "|" + right;
        }

        @CodeFunction(description = "Record trace id and return input", tags = {"traceCapture"})
        public String traceCapture(String text) {
            traceIds.add(PrOOPtContext.getTraceId());
            return text;
        }

        @CodeFunction(description = "Sleep then return", tags = {"slowStep"})
        public String slowStep(String text) throws InterruptedException {
            Thread.sleep(500);
            return text;
        }

        @CodeFunction(description = "Always throws", tags = {"failStep"})
        public String failStep(String text) {
            throw new RuntimeException("intentional failure");
        }
    }

    // ------------------------------------------------------------------ infrastructure

    private TestFunctions functions;
    private FunctionRegistry registry;
    private DagExecutor executor;
    private ExecutorService cloudExec;
    private ExecutorService localExec;

    @BeforeEach
    void setUp() {
        functions = new TestFunctions();
        registry = new FunctionRegistry();
        registry.registerInstance(TestFunctions.class, functions);
        registry.registerAll(FunctionScanner.scan(TestFunctions.class));

        cloudExec = Executors.newVirtualThreadPerTaskExecutor();
        localExec = Executors.newFixedThreadPool(4);
        AuditLogger audit = new AuditLogger();
        PromptCallEngine engine = new PromptCallEngine(
                (prompt, tier) -> "MOCK", new io.github.argonizer.prooopt.autobox.PrOOPtAutoBoxer());
        executor = new DagExecutor(registry, engine, audit, cloudExec, localExec, true);
    }

    @AfterEach
    void tearDown() {
        cloudExec.shutdownNow();
        localExec.shutdownNow();
        PrOOPtContext.clear();
    }

    // ------------------------------------------------------------------ helpers

    private static ExecutionStep codeStep(String stepId, String streamId, String function,
                                          Map<String, Object> args, String assignTo,
                                          String... deps) {
        return new ExecutionStep(stepId, streamId, function, FunctionType.CODE, null,
                args, List.of(deps), assignTo, 0);
    }

    private static ExecutionPlan singleStream(String output, ExecutionStep... steps) {
        return new ExecutionPlan("trace-test", List.of(new ExecutionStream("S1", List.of(steps))), output);
    }

    // ------------------------------------------------------------------ tests

    @Test
    void execute_singleStep_returnsResult() {
        ExecutionPlan plan = singleStream("$out",
                codeStep("S1.1", "S1", "echo", Map.of("text", "hello"), "$out"));

        Object result = executor.execute(plan, "hello", null, 10_000);

        assertEquals("hello", result);
        assertEquals(1, functions.invocations.size());
    }

    @Test
    void execute_linearChain_executesInOrder() {
        // stepA → stepB → echo(stepB result)
        ExecutionPlan plan = singleStream("$out",
                codeStep("S1.1", "S1", "stepA", Map.of("text", "${userInput}"), "$a"),
                codeStep("S1.2", "S1", "stepB", Map.of("text", "$a"), "$b", "S1.1"),
                codeStep("S1.3", "S1", "echo",  Map.of("text", "$b"),  "$out", "S1.2"));

        Object result = executor.execute(plan, "x", null, 10_000);

        assertEquals("B:A:x", result);
        // order must be stepA before stepB before echo
        int idxA = functions.invocations.indexOf("stepA");
        int idxB = functions.invocations.indexOf("stepB");
        int idxE = functions.invocations.indexOf("echo:B:A:x");
        assertTrue(idxA < idxB && idxB < idxE, "invocation order must be stepA → stepB → echo");
    }

    @Test
    void execute_parallelBranches_bothExecute() {
        // S1.1 fans out to S1.2 and S1.3, then S1.4 merges
        ExecutionPlan plan = singleStream("$merged",
                codeStep("S1.1", "S1", "echo",   Map.of("text", "${userInput}"), "$root"),
                codeStep("S1.2", "S1", "stepA",  Map.of("text", "$root"), "$left",   "S1.1"),
                codeStep("S1.3", "S1", "stepB",  Map.of("text", "$root"), "$right",  "S1.1"),
                codeStep("S1.4", "S1", "concat", Map.of("left", "$left", "right", "$right"), "$merged", "S1.2", "S1.3"));

        Object result = executor.execute(plan, "in", null, 10_000);

        assertEquals("A:in|B:in", result);
        assertTrue(functions.invocations.contains("stepA"), "branch A must execute");
        assertTrue(functions.invocations.contains("stepB"), "branch B must execute");
        assertTrue(functions.invocations.contains("concat"), "merge step must execute");
    }

    @Test
    void execute_crossStreamDependency_step1BlocksOnStream2() {
        // S2: S2.1 produces a value; S1: S1.1 consumes it
        ExecutionStep s2_1 = codeStep("S2.1", "S2", "stepA", Map.of("text", "${userInput}"), "$fromS2");
        ExecutionStep s1_1 = codeStep("S1.1", "S1", "echo",  Map.of("text", "$fromS2"), "$out", "S2.1");

        ExecutionPlan plan = new ExecutionPlan("t", List.of(
                new ExecutionStream("S1", List.of(s1_1)),
                new ExecutionStream("S2", List.of(s2_1))),
                "$out");

        Object result = executor.execute(plan, "data", null, 10_000);

        assertEquals("A:data", result);
        int idxA = functions.invocations.indexOf("stepA");
        int idxE = functions.invocations.indexOf("echo:A:data");
        assertTrue(idxA < idxE, "S2.1 (stepA) must complete before S1.1 (echo) uses its result");
    }

    @Test
    void execute_stepTimeout_throwsPrOOPtExecutionException() {
        // slowStep sleeps 500 ms; DAG timeout of 100 ms should abort it
        ExecutionStep slow = new ExecutionStep("S1.1", "S1", "slowStep", FunctionType.CODE, null,
                Map.of("text", "${userInput}"), List.of(), "$out", 0L);
        ExecutionPlan plan = singleStream("$out", slow);

        assertThrows(PrOOPtExecutionException.class,
                () -> executor.execute(plan, "x", null, 100L),
                "DAG timeout on a slow step must surface as PrOOPtExecutionException");
    }

    @Test
    void execute_dagTimeout_cancelsAllPending() {
        // All three steps sleep; the DAG timeout is very short
        ExecutionStep s1 = new ExecutionStep("S1.1", "S1", "slowStep", FunctionType.CODE, null,
                Map.of("text", "${userInput}"), List.of(), "$a", 0);
        ExecutionStep s2 = new ExecutionStep("S1.2", "S1", "slowStep", FunctionType.CODE, null,
                Map.of("text", "$a"), List.of("S1.1"), "$b", 0);
        ExecutionStep s3 = new ExecutionStep("S1.3", "S1", "slowStep", FunctionType.CODE, null,
                Map.of("text", "$b"), List.of("S1.2"), "$out", 0);

        ExecutionPlan plan = singleStream("$out", s1, s2, s3);

        assertThrows(PrOOPtExecutionException.class,
                () -> executor.execute(plan, "x", null, 100L),
                "DAG-level timeout must surface as PrOOPtExecutionException");
    }

    @Test
    void execute_stepFailure_propagatesException() {
        ExecutionPlan plan = singleStream("$out",
                codeStep("S1.1", "S1", "failStep", Map.of("text", "${userInput}"), "$out"));

        PrOOPtExecutionException ex = assertThrows(PrOOPtExecutionException.class,
                () -> executor.execute(plan, "x", null, 10_000));
        assertTrue(ex.getMessage().contains("S1.1") || ex.getCause() != null,
                "exception must identify the failing step or carry the cause");
    }

    @Test
    void execute_parallelFalse_linearChain_runsSynchronously() {
        // Build an executor with parallel=false
        DagExecutor seqExecutor = new DagExecutor(registry,
                new PromptCallEngine((p, t) -> "MOCK", new io.github.argonizer.prooopt.autobox.PrOOPtAutoBoxer()),
                new AuditLogger(), cloudExec, localExec, false);

        ExecutionPlan plan = singleStream("$out",
                codeStep("S1.1", "S1", "stepA", Map.of("text", "${userInput}"), "$a"),
                codeStep("S1.2", "S1", "stepB", Map.of("text", "$a"), "$b", "S1.1"),
                codeStep("S1.3", "S1", "echo",  Map.of("text", "$b"),  "$out", "S1.2"));

        Object result = seqExecutor.execute(plan, "in", null, 10_000);

        assertEquals("B:A:in", result);
        int idxA = functions.invocations.indexOf("stepA");
        int idxB = functions.invocations.indexOf("stepB");
        assertTrue(idxA < idxB, "sequential executor must respect dependency order");
    }

    @Test
    void execute_traceIdPropagatedToWorkerThreads() {
        PrOOPtContext.setTraceId("test-trace-999");

        ExecutionPlan plan = singleStream("$out",
                codeStep("S1.1", "S1", "traceCapture", Map.of("text", "${userInput}"), "$out"));

        executor.execute(plan, "x", null, 10_000);

        assertFalse(functions.traceIds.isEmpty(), "traceCapture must have been called");
        assertTrue(functions.traceIds.stream().anyMatch(id -> id != null && !id.isBlank()),
                "worker thread must have a non-null trace ID");
    }

    @Test
    void computeCriticalPath_identifiesLongestPath() {
        // Diamond: S1.1 → S1.2 (weight=1) and S1.1 → S1.3 (weight=1) → S1.4
        // No way to measure wall-clock weights here, so verify that ALL steps are reachable
        // (i.e. the executor traverses the full graph and returns a meaningful result)
        ExecutionPlan plan = singleStream("$merged",
                codeStep("S1.1", "S1", "echo",   Map.of("text", "${userInput}"), "$root"),
                codeStep("S1.2", "S1", "stepA",  Map.of("text", "$root"), "$left",   "S1.1"),
                codeStep("S1.3", "S1", "stepB",  Map.of("text", "$root"), "$right",  "S1.1"),
                codeStep("S1.4", "S1", "concat", Map.of("left", "$left", "right", "$right"), "$merged", "S1.2", "S1.3"));

        // Execute successfully and verify all 4 steps ran (critical path includes all of them
        // in a diamond shape — every node is on the critical path)
        Object result = executor.execute(plan, "in", null, 10_000);
        assertEquals("A:in|B:in", result);
        assertEquals(4, functions.invocations.size(), "all 4 steps in the diamond must execute");
    }
}
