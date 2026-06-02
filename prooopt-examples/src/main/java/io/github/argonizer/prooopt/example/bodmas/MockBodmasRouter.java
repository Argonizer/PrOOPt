/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.example.bodmas;

import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.ModelRouter;

/**
 * Deterministic mock router for the BODMAS solver demo — no API key or network required.
 *
 * <p>Returns canned discovery + DAG-plan JSON (in the {@code streams[]} schema the {@code DagExecutor}
 * expects) for each bundled test case, plus canned prose for the two {@code @PromptFunction} steps.
 * The plans the real {@code CLOUD_ADVANCED} orchestrator would generate are reproduced here so the
 * full pipeline runs offline. Swap for a {@code CloudModelRouter} to plan live.
 *
 * <p>Every number in every plan is a literal handed to a {@code @CodeFunction}; the router never
 * computes anything.
 */
public final class MockBodmasRouter implements ModelRouter {

    /** Capability list returned for the phase-1 discovery prompt. */
    static final String DISCOVERY =
            "[\"add two numbers\",\"subtract numbers\",\"multiply numbers\",\"divide numbers\","
                    + "\"raise to a power\",\"square root\",\"factorial\",\"assert answer\","
                    + "\"format result\",\"build verification steps\",\"analyze bodmas problem\","
                    + "\"interpret solution\",\"assemble result\"]";

    @Override
    public String route(String prompt, ModelTier tier) {
        // Phase 1 — capability discovery.
        if (prompt.contains("planning assistant")) {
            return DISCOVERY;
        }

        // Phase 2 — execution plan. Branch on the embedded expression (most specific first).
        if (prompt.contains("Produce an execution plan")) {
            if (prompt.contains("((8 + 4)")) {
                return primaryPlan(13.5);
            }
            if (prompt.contains("(3 + 4) × 2")) {
                return bracketsPlan();
            }
            if (prompt.contains("2³")) {
                return ordersPlan();
            }
            if (prompt.contains("3 + 4 × 2")) {
                return baselinePlan();
            }
            // Unknown expression: fall back to the baseline plan (custom expressions need a real router).
            return baselinePlan();
        }

        // PROMPT-step executions.
        if (prompt.contains("Analyze this BODMAS")) {
            return "Brackets: (8+4), (2+4), (16+9) — independent, parallel. "
                    + "Orders: 3², 5!, 4², √(16+9) — independent, parallel. "
                    + "Then DM left-to-right, then AS left-to-right. ~14 arithmetic operations.";
        }
        if (prompt.contains("plain-English")) {
            return "BODMAS precedence was applied strictly: brackets and orders resolved first and in "
                    + "parallel (they share no inputs), then division/multiplication, then "
                    + "addition/subtraction left to right. Independent subexpressions ran concurrently "
                    + "because the DAG had no edges between them. The result is correct because every "
                    + "step was an exact @CodeFunction. It demonstrates deterministic, auditable math.";
        }

        return "OK";
    }

    // ================================================================
    // Plan builders (streams[] schema) — exposed package-private for tests
    // ================================================================

    /** {@code 3 + 4 × 2 = 11} — multiplication before addition. */
    static String baselinePlan() {
        String steps =
                code("S1.1", "multiply", "{\"a\":4,\"b\":2}", "[]", "$m1")
                        + "," + code("S1.2", "add", "{\"a\":3,\"b\":\"$m1\"}", "[\"S1.1\"]", "$result")
                        + "," + tail("S1.2", "3 + 4 × 2", 11.0, 2, 1);
        return plan(steps);
    }

    /** {@code (3 + 4) × 2 = 14} — brackets override precedence. */
    static String bracketsPlan() {
        String steps =
                code("S1.1", "add", "{\"a\":3,\"b\":4}", "[]", "$b1")
                        + "," + code("S1.2", "multiply", "{\"a\":\"$b1\",\"b\":2}", "[\"S1.1\"]", "$result")
                        + "," + tail("S1.2", "(3 + 4) × 2", 14.0, 2, 1);
        return plan(steps);
    }

    /** {@code 2³ + 3! - √9 = 11} — orders before add/subtract. */
    static String ordersPlan() {
        String steps =
                code("S1.1", "power", "{\"base\":2,\"exponent\":3}", "[]", "$p1")
                        + "," + code("S1.2", "factorial", "{\"n\":3}", "[]", "$f1")
                        + "," + code("S1.3", "sqrt", "{\"radicand\":9}", "[]", "$r1")
                        + "," + code("S1.4", "add", "{\"a\":\"$p1\",\"b\":\"$f1\"}", "[\"S1.1\",\"S1.2\"]", "$s1")
                        + "," + code("S1.5", "subtract", "{\"a\":\"$s1\",\"b\":\"$r1\"}", "[\"S1.4\",\"S1.3\"]", "$result")
                        + "," + tail("S1.5", "2³ + 3! - √9", 11.0, 5, 2);
        return plan(steps);
    }

    /**
     * The full 14-step plan for {@code ((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)}.
     * Expected is parameterised so a test can force an assertion failure.
     */
    static String primaryPlan(double expected) {
        String expr = "((8 + 4) × 3² - 6) ÷ (2 + 4) + 5! ÷ (4² × 5) - √(16 + 9)";
        String steps =
                  code("S1.1", "add", "{\"a\":8,\"b\":4}", "[]", "$b1")
                + "," + code("S1.2", "add", "{\"a\":2,\"b\":4}", "[]", "$b2")
                + "," + code("S1.3", "add", "{\"a\":16,\"b\":9}", "[]", "$b3")
                + "," + code("S1.4", "power", "{\"base\":3,\"exponent\":2}", "[]", "$o1")
                + "," + code("S1.5", "factorial", "{\"n\":5}", "[]", "$o2")
                + "," + code("S1.6", "power", "{\"base\":4,\"exponent\":2}", "[]", "$o3")
                + "," + code("S1.7", "sqrt", "{\"radicand\":\"$b3\"}", "[\"S1.3\"]", "$o4")
                + "," + code("S1.8", "multiply", "{\"a\":\"$b1\",\"b\":\"$o1\"}", "[\"S1.1\",\"S1.4\"]", "$dm1")
                + "," + code("S1.9", "multiply", "{\"a\":\"$o3\",\"b\":5}", "[\"S1.6\"]", "$dm2")
                + "," + code("S1.10", "subtract", "{\"a\":\"$dm1\",\"b\":6}", "[\"S1.8\"]", "$dm3")
                + "," + code("S1.11", "divide", "{\"a\":\"$o2\",\"b\":\"$dm2\"}", "[\"S1.5\",\"S1.9\"]", "$dm5")
                + "," + code("S1.12", "divide", "{\"a\":\"$dm3\",\"b\":\"$b2\"}", "[\"S1.10\",\"S1.2\"]", "$dm4")
                + "," + code("S1.13", "add", "{\"a\":\"$dm4\",\"b\":\"$dm5\"}", "[\"S1.12\",\"S1.11\"]", "$as1")
                + "," + code("S1.14", "subtract", "{\"a\":\"$as1\",\"b\":\"$o4\"}", "[\"S1.13\",\"S1.7\"]", "$result")
                + "," + tail("S1.14", expr, expected, 14, 6);
        return plan(steps);
    }

    // ---- shared tail: assert, format, verify, analyze, interpret, assemble ----

    /**
     * Builds the result-assembly tail. {@code finalStepId} is the step that produced {@code $result};
     * the assert/format/verify steps depend on it so they never run before the answer exists.
     */
    private static String tail(String finalStepId, String expr, double expected, int dagSteps, int waves) {
        String dep = "[\"" + finalStepId + "\"]";
        String exprJson = jsonString(expr);
        String summary = jsonString("DAG with " + dagSteps + " arithmetic steps across " + waves + " waves");
        return
                  code("S1.90", "assertAnswer",
                        "{\"computed\":\"$result\",\"expected\":" + expected + "}", dep, "$asserted")
                + "," + code("S1.91", "formatResult", "{\"value\":\"$result\"}", dep, "$fmt")
                + "," + code("S1.92", "buildVerificationSteps",
                        "{\"problem\":" + exprJson + ",\"result\":\"$result\"}", dep, "$verif")
                + "," + promptStep("S1.93", "analyzeBodmasProblem", "CLOUD_ADVANCED",
                        "{\"problem\":" + exprJson + "}", "[]", "$analysis")
                + "," + promptStep("S1.94", "interpretSolution", "CLOUD_FAST",
                        "{\"problem\":" + exprJson + ",\"dagPlan\":" + summary + ",\"result\":\"$fmt\"}",
                        "[\"S1.91\"]", "$interp")
                + "," + code("S1.95", "assembleResult",
                        "{\"problem\":" + exprJson + ",\"bodmasAnalysis\":\"$analysis\","
                                + "\"dagPlanSummary\":" + summary + ",\"computedResult\":\"$result\","
                                + "\"resultFormatted\":\"$fmt\",\"expectedResult\":" + expected + ","
                                + "\"assertionPassed\":\"$asserted\",\"verificationSteps\":\"$verif\","
                                + "\"interpretation\":\"$interp\",\"totalDagSteps\":" + dagSteps + ","
                                + "\"parallelWaves\":" + waves + "}",
                        "[\"S1.90\",\"S1.91\",\"S1.92\",\"S1.93\",\"S1.94\"]", "$out");
    }

    // ---- low-level JSON builders ----

    private static String plan(String stepsCsv) {
        return "{\"traceId\":\"bodmas\",\"streams\":[{\"streamId\":\"S1\",\"steps\":["
                + stepsCsv + "]}],\"output\":\"$out\"}";
    }

    private static String code(String id, String fn, String argsJson, String dependsOn, String assignTo) {
        return "{\"stepId\":\"" + id + "\",\"streamId\":\"S1\",\"function\":\"" + fn + "\","
                + "\"type\":\"CODE\",\"args\":" + argsJson + ",\"dependsOn\":" + dependsOn + ","
                + "\"assignTo\":\"" + assignTo + "\",\"timeoutMs\":0}";
    }

    private static String promptStep(String id, String fn, String model, String argsJson,
                                     String dependsOn, String assignTo) {
        return "{\"stepId\":\"" + id + "\",\"streamId\":\"S1\",\"function\":\"" + fn + "\","
                + "\"type\":\"PROMPT\",\"model\":\"" + model + "\",\"args\":" + argsJson + ","
                + "\"dependsOn\":" + dependsOn + ",\"assignTo\":\"" + assignTo + "\",\"timeoutMs\":0}";
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
