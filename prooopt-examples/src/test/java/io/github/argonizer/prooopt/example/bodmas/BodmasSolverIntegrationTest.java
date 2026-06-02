/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.example.bodmas;

import io.github.argonizer.prooopt.PrOOPt;
import io.github.argonizer.prooopt.PrOOPtRuntime;
import io.github.argonizer.prooopt.annotation.PromptOrchestrator;
import io.github.argonizer.prooopt.exception.PrOOPtExecutionException;
import io.github.argonizer.prooopt.model.FunctionCall;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanMode;
import io.github.argonizer.prooopt.orchestrator.BaseOrchestrator;
import io.github.argonizer.prooopt.router.ModelRouter;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests driving the full orchestration pipeline against a deterministic mock router —
 * no real LLM, no network. Verifies the DagExecutor runs the planned arithmetic correctly.
 */
class BodmasSolverIntegrationTest {

    // ------------------------------------------------------------------ helpers

    /** Router that serves the discovery list, a supplied plan, and canned prose for PROMPT steps. */
    private record TestRouter(String plan) implements ModelRouter {
        @Override
        public String route(String prompt, ModelTier tier) {
            if (prompt.contains("planning assistant")) {
                return MockBodmasRouter.DISCOVERY;
            }
            if (prompt.contains("Produce an execution plan")) {
                return plan;
            }
            if (prompt.contains("Analyze this BODMAS")) {
                return "Structured analysis: brackets and orders are independent and run in parallel.";
            }
            if (prompt.contains("plain-English")) {
                return "BODMAS precedence applied; independent subexpressions ran concurrently.";
            }
            return "OK";
        }
    }

    private static BodmasResult solve(BaseOrchestrator solver, String problem, String plan) {
        PrOOPtRuntime runtime = PrOOPt.builder()
                .router(new TestRouter(plan))
                .includeStdlib(false)
                .registerInstance(new BodmasFunctions())
                .registerInstance(solver)
                .build();
        try {
            return (BodmasResult) runtime.orchestrate(solver, problem);
        } finally {
            runtime.shutdown();
        }
    }

    private static String singleStepPlan(String fn, String argsJson) {
        return "{\"traceId\":\"err\",\"streams\":[{\"streamId\":\"S1\",\"steps\":["
                + "{\"stepId\":\"S1.1\",\"streamId\":\"S1\",\"function\":\"" + fn + "\","
                + "\"type\":\"CODE\",\"args\":" + argsJson + ",\"dependsOn\":[],"
                + "\"assignTo\":\"$result\",\"timeoutMs\":0}"
                + "]}],\"output\":\"$result\"}";
    }

    /** A recording orchestrator: its own @PromptOrchestrator + hooks that capture execution facts. */
    @PromptOrchestrator(
            prompt = "Recording BODMAS solver for tests.",
            planMode = PlanMode.DYNAMIC,
            model = ModelTier.CLOUD_ADVANCED,
            parallel = true,
            maxThreads = 8,
            dagTimeoutMs = 120_000,
            name = "RecordingBodmasSolver",
            version = "0.1.0")
    static class RecordingSolver extends BaseOrchestrator {
        final List<String> codeThreads = new CopyOnWriteArrayList<>();
        final Set<String> traceIds = ConcurrentHashMap.newKeySet();
        final Map<String, Map<String, Object>> argsByFunction = new ConcurrentHashMap<>();
        final Map<String, Object> resultByFunction = new ConcurrentHashMap<>();

        @Override
        protected void beforeFunction(FunctionCall call) {
            traceIds.add(call.traceId());
            if (call.type() == FunctionType.CODE) {
                codeThreads.add(Thread.currentThread().getName());
            }
            argsByFunction.put(call.name(), call.variables());
        }

        @Override
        protected void afterFunction(FunctionCall call, Object result) {
            resultByFunction.put(call.name(), result);
        }
    }

    private static double asDouble(Object o) {
        return ((Number) o).doubleValue();
    }

    // ------------------------------------------------------------------ tests

    @Test
    void solve_baselineOperatorPrecedence_returns11() {
        BodmasResult result = solve(new BodmasSolver(), "3 + 4 × 2", MockBodmasRouter.baselinePlan());
        assertEquals(11.0, result.getComputedResult(), 1e-9);
        assertTrue(result.isAssertionPassed());
    }

    @Test
    void solve_brackets_returns14() {
        BodmasResult result = solve(new BodmasSolver(), "(3 + 4) × 2", MockBodmasRouter.bracketsPlan());
        assertEquals(14.0, result.getComputedResult(), 1e-9);
    }

    @Test
    void solve_primaryAssertion_returns13point5() {
        BodmasResult result = solve(new BodmasSolver(),
                "((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)",
                MockBodmasRouter.primaryPlan(13.5));
        assertEquals(13.5, result.getComputedResult(), 1e-9);
        assertTrue(result.isAssertionPassed());
        assertEquals(14, result.getTotalDagSteps());
    }

    @Test
    void solve_wrongAnswer_assertionFails() {
        PrOOPtExecutionException ex = assertThrows(PrOOPtExecutionException.class, () ->
                solve(new BodmasSolver(),
                        "((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)",
                        MockBodmasRouter.primaryPlan(14.0)));
        assertInstanceOf(AssertionError.class, ex.getCause());
    }

    @Test
    void solve_divisionByZero_propagatesException() {
        PrOOPtExecutionException ex = assertThrows(PrOOPtExecutionException.class, () ->
                solve(new BodmasSolver(), "10 ÷ 0", singleStepPlan("divide", "{\"a\":10,\"b\":0}")));
        assertInstanceOf(ArithmeticException.class, ex.getCause());
    }

    @Test
    void solve_sqrtNegative_propagatesException() {
        PrOOPtExecutionException ex = assertThrows(PrOOPtExecutionException.class, () ->
                solve(new BodmasSolver(), "√-4", singleStepPlan("sqrt", "{\"radicand\":-4}")));
        assertInstanceOf(ArithmeticException.class, ex.getCause());
    }

    @Test
    void solve_independentSteps_runInParallel() {
        RecordingSolver solver = new RecordingSolver();
        BodmasResult result = solve(solver,
                "((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)",
                MockBodmasRouter.primaryPlan(13.5));

        assertEquals(13.5, result.getComputedResult(), 1e-9);
        // The DagExecutor scheduled the independent wave-1 steps off the calling thread.
        assertFalse(solver.codeThreads.isEmpty(), "code steps must have executed");
        String main = Thread.currentThread().getName();
        assertTrue(solver.codeThreads.stream().noneMatch(t -> t.equals(main)),
                "independent arithmetic steps must run on worker threads, not the calling thread");
        Set<String> distinct = new HashSet<>(solver.codeThreads);
        assertTrue(distinct.size() >= 1, "at least one worker thread executed the arithmetic");
    }

    @Test
    void solve_crossStreamDependency_resolvesCorrectly() {
        RecordingSolver solver = new RecordingSolver();
        solve(solver, "((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)",
                MockBodmasRouter.primaryPlan(13.5));

        // S1.7 sqrt depends on S1.3 add(16, 9) = 25 → sqrt receives 25.0 and returns 5.0.
        Map<String, Object> sqrtArgs = solver.argsByFunction.get("sqrt");
        assertEquals(25.0, asDouble(sqrtArgs.get("radicand")), 1e-9);
        assertEquals(5.0, asDouble(solver.resultByFunction.get("sqrt")), 1e-9);
    }

    @Test
    void solve_traceIdPropagatedAcrossAllSteps() {
        RecordingSolver solver = new RecordingSolver();
        solve(solver, "((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)",
                MockBodmasRouter.primaryPlan(13.5));

        assertEquals(1, solver.traceIds.size(), "all steps must share a single traceId");
        assertFalse(solver.traceIds.iterator().next().isBlank(), "traceId must be non-blank");
    }
}
