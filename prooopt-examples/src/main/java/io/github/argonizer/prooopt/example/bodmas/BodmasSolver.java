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
import io.github.argonizer.prooopt.model.FunctionCall;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanMode;
import io.github.argonizer.prooopt.orchestrator.BaseOrchestrator;
import io.github.argonizer.prooopt.router.ModelRouter;

/**
 * Autonomous orchestrator that solves BODMAS/PEMDAS expressions by generating a DAG execution plan.
 *
 * <p>The {@code CLOUD_ADVANCED} planner reasons about operator precedence and parallelism, then emits
 * a multi-step DAG. The {@code DagExecutor} runs it — every arithmetic operation is a deterministic
 * {@code @CodeFunction}. <b>No LLM ever touches a number.</b>
 *
 * <p>The bundled demo runs against {@link MockBodmasRouter} so it needs no API key. Swap in a real
 * {@code CloudModelRouter} for live planning.
 */
@PromptOrchestrator(
        prompt = """
                You are a mathematical expression solver specialising in BODMAS/PEMDAS.
                You have access to deterministic arithmetic @CodeFunction tools.
                Your job is to generate a DAG execution plan — you do NOT compute.

                CRITICAL RULE: You must NEVER compute any arithmetic yourself.
                Every number produced by an operation must come from a @CodeFunction.
                You are the planner. The @CodeFunction tools are the calculators.

                BODMAS precedence rules you must enforce in the DAG:
                1. B — Brackets: innermost brackets first. Independent brackets run in parallel.
                2. O — Orders: powers, roots, factorials. Run in parallel where independent.
                3. DM — Division and Multiplication: left to right. Chain sequentially.
                4. AS — Addition and Subtraction: left to right. Chain sequentially.

                DAG plan rules:
                - Each step has a globally unique stepId (e.g. "S1.1", "S2.1")
                - Each step declares which prior steps it dependsOn (may be cross-stream)
                - Independent steps (no shared dependencies) MUST be placed in separate
                  streams so the DagExecutor can run them concurrently
                - Args reference prior step results as "$stepId" string values
                - Args reference literal numbers as numeric JSON values

                For EVERY problem, your plan must:
                1. Call analyzeBodmasProblem first to understand structure
                2. Generate @CodeFunction steps for every arithmetic operation
                3. Call assertAnswer to verify the final result against the expected value
                4. Call buildVerificationSteps to produce an audit trail
                5. Call interpretSolution to explain the result
                6. Assemble a complete BodmasResult

                Available @CodeFunction tools:
                add, subtract, multiply, divide, power, sqrt, factorial,
                modulo, negate, absolute, assertAnswer, formatResult, buildVerificationSteps

                Available @PromptFunction tools:
                analyzeBodmasProblem, interpretSolution
                """,
        planMode = PlanMode.DYNAMIC,
        model = ModelTier.CLOUD_ADVANCED,
        parallel = true,
        maxThreads = 8,
        dagTimeoutMs = 120_000,
        name = "BodmasSolver",
        version = "0.1.0")
public class BodmasSolver extends BaseOrchestrator {

    // ---- BaseOrchestrator lifecycle hooks ----

    @Override
    protected void onRunStart(String traceId, Object input) {
        audit.info("[BODMAS][START] trace={} problem='{}'", traceId, input);
    }

    @Override
    protected void beforeFunction(FunctionCall call) {
        if (call.type() == FunctionType.CODE) {
            audit.info("[BODMAS][CODE] {} args={}", call.name(), call.variables());
        } else {
            audit.info("[BODMAS][LLM]  {} model={}", call.name(), call.modelTier());
        }
    }

    @Override
    protected void afterFunction(FunctionCall call, Object result) {
        audit.info("[BODMAS][DONE]  {} → {} ({}ms)",
                call.name(), sanitize(result), call.elapsedMs());
    }

    @Override
    protected void onError(FunctionCall call, Throwable error) {
        audit.error("[BODMAS][ERROR] {} failed: {}", call.name(), error.getMessage());
    }

    @Override
    protected void onRunComplete(String traceId, long durationMs, int count) {
        audit.info("[BODMAS][END]   trace={} functions={} duration={}ms", traceId, count, durationMs);
    }

    private static String sanitize(Object result) {
        String s = result == null ? "null" : result.toString();
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }

    // ---- Entry point ----

    public static void main(String[] args) {
        System.out.println("PrOOPt BODMAS/PEMDAS Solver");
        System.out.println("Demonstrates DAG execution with CLOUD_ADVANCED planning");
        System.out.println();

        // Test Case 1 — operator precedence baseline (must equal 11, not 14).
        runCase("3 + 4 × 2", 11.0, "Baseline: multiplication before addition");

        // Test Case 2 — brackets override precedence.
        runCase("(3 + 4) × 2", 14.0, "Brackets override default multiplication-first precedence");

        // Test Case 3 — orders (powers, factorial, root).
        runCase("2³ + 3! - √9", 11.0, "Powers, factorials, and roots evaluated before add/subtract");

        // Test Case 4 — primary assertion (complex multi-stream DAG).
        runCase("((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)", 13.5,
                "Complex expression — primary assertion");

        // Test Case 5 — user-provided expression (from CLI args).
        if (args.length >= 2) {
            try {
                double expected = Double.parseDouble(args[1]);
                runCase(args[0], expected, "User-provided expression");
            } catch (NumberFormatException e) {
                System.err.println("Usage: BodmasSolver \"<expression>\" <expected_answer>");
            }
        }
    }

    private static void runCase(String problem, double expected, String description) {
        System.out.println("─────────────────────────────────────────────────────");
        System.out.println("Case: " + description);
        System.out.println("Problem: " + problem);
        System.out.println("Expected: " + expected);

        BodmasFunctions functions = new BodmasFunctions();
        BodmasSolver solver = new BodmasSolver();
        ModelRouter router = new MockBodmasRouter();

        PrOOPtRuntime runtime = PrOOPt.builder()
                .router(router)
                .includeStdlib(false)
                .registerInstance(functions)
                .registerInstance(solver)
                .build();
        try {
            Object raw = runtime.orchestrate(solver, problem);
            if (!(raw instanceof BodmasResult result)) {
                throw new AssertionError("Orchestrator did not return a BodmasResult for: " + problem);
            }
            System.out.println(result);
            if (!result.isAssertionPassed()) {
                throw new AssertionError(
                        "BODMAS assertion FAILED for: " + problem
                                + " — expected=" + expected
                                + " computed=" + result.getComputedResult());
            }
        } finally {
            runtime.shutdown();
        }
        System.out.println();
    }
}
