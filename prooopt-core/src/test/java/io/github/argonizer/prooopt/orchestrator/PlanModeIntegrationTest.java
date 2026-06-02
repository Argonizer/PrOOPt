/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.PrOOPt;
import io.github.argonizer.prooopt.PrOOPtRuntime;
import io.github.argonizer.prooopt.annotation.CodeFunction;
import io.github.argonizer.prooopt.annotation.PromptOrchestrator;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanCacheStrategy;
import io.github.argonizer.prooopt.model.PlanMode;
import io.github.argonizer.prooopt.router.ModelRouter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end coverage of STATIC vs DYNAMIC plan modes (verified by counting the Cloud LLM
 * plan-generation calls) and dynamic prompt-function generation with a {@code maxDynamic} budget.
 */
class PlanModeIntegrationTest {

    // ---- Test orchestrators ----

    @PromptOrchestrator(prompt = "echo orchestrator", planMode = PlanMode.STATIC,
            planCacheStrategy = PlanCacheStrategy.EXACT)
    static class StaticEcho {
        @CodeFunction(description = "Echo the text back unchanged",
                tags = {"echo", "text", "repeat", "return"})
        public String echo(String text) {
            return text;
        }
    }

    @PromptOrchestrator(prompt = "echo orchestrator", planMode = PlanMode.DYNAMIC)
    static class DynamicEcho {
        @CodeFunction(description = "Echo the text back unchanged",
                tags = {"echo", "text", "repeat", "return"})
        public String echo(String text) {
            return text;
        }
    }

    @PromptOrchestrator(prompt = "gap orchestrator", planMode = PlanMode.DYNAMIC,
            allowDynamic = true, maxDynamicFunctions = 1, dynamicFunctionModel = ModelTier.CLOUD_FAST)
    static class GapOrchestrator {
        @CodeFunction(description = "Echo the text back unchanged", tags = {"echo", "text"})
        public String echo(String text) {
            return text;
        }
    }

    // ---- Routers ----

    /** Returns canned discovery + DAG plan for the echo orchestrator and counts plan-gen calls. */
    static class EchoRouter implements ModelRouter {
        final AtomicInteger planGenerations = new AtomicInteger();

        @Override
        public String route(String prompt, ModelTier tier) {
            if (prompt.contains("Produce an execution plan")) {
                planGenerations.incrementAndGet();
                return """
                        {"traceId":"t","streams":[{"streamId":"S1","steps":[
                          {"stepId":"S1.1","streamId":"S1","function":"echo","type":"CODE",
                           "args":{"text":"${userInput}"},"dependsOn":[],"assignTo":"$out","timeoutMs":0}
                        ]}],"output":"$out"}
                        """;
            }
            if (prompt.contains("planning assistant")) {
                return "[\"echo the text back\"]";
            }
            return "OK";
        }
    }

    /** Drives the dynamic-gap path: generation JSON, DAG plan referencing it, and the leaf result. */
    static class GapRouter implements ModelRouter {
        final AtomicInteger generations = new AtomicInteger();

        @Override
        public String route(String prompt, ModelTier tier) {
            if (prompt.contains("no registered match")) {
                generations.incrementAndGet();
                return """
                        {"name":"computeThing","prompt":"compute {input}",
                         "model":"LOCAL","description":"compute a value"}
                        """;
            }
            if (prompt.contains("Produce an execution plan")) {
                return """
                        {"traceId":"t","streams":[{"streamId":"S1","steps":[
                          {"stepId":"S1.1","streamId":"S1","function":"computeThing","type":"PROMPT",
                           "model":"LOCAL","args":{"input":"${userInput}"},"dependsOn":[],
                           "assignTo":"$out","timeoutMs":0}
                        ]}],"output":"$out"}
                        """;
            }
            if (prompt.contains("planning assistant")) {
                return "[\"compute the foo metric\",\"compute the bar metric\"]";
            }
            return "COMPUTED";
        }
    }

    // ---- Tests ----

    @Test
    void staticModeGeneratesPlanExactlyOnceAcrossManyRuns() {
        EchoRouter router = new EchoRouter();
        StaticEcho bean = new StaticEcho();
        PrOOPtRuntime runtime = PrOOPt.builder()
                .router(router)
                .includeStdlib(false)
                .registerInstance(bean)
                .build();
        try {
            for (int i = 0; i < 100; i++) {
                Object result = runtime.orchestrate(bean, "hello world");
                assertEquals("hello world", result);
            }
            assertEquals(1, router.planGenerations.get(),
                    "STATIC mode must call the planning model exactly once across 100 runs");
        } finally {
            runtime.shutdown();
        }
    }

    @Test
    void dynamicModeGeneratesPlanEveryRun() {
        EchoRouter router = new EchoRouter();
        DynamicEcho bean = new DynamicEcho();
        PrOOPtRuntime runtime = PrOOPt.builder()
                .router(router)
                .includeStdlib(false)
                .registerInstance(bean)
                .build();
        try {
            for (int i = 0; i < 5; i++) {
                assertEquals("hello world", runtime.orchestrate(bean, "hello world"));
            }
            assertEquals(5, router.planGenerations.get(),
                    "DYNAMIC mode must call the planning model on every run");
        } finally {
            runtime.shutdown();
        }
    }

    @Test
    void allowDynamicGeneratesFunctionAndEnforcesBudget() {
        GapRouter router = new GapRouter();
        GapOrchestrator bean = new GapOrchestrator();
        PrOOPtRuntime runtime = PrOOPt.builder()
                .router(router)
                .includeStdlib(false)
                .registerInstance(bean)
                .build();
        try {
            Object result = runtime.orchestrate(bean, "input data");
            assertEquals("COMPUTED", result, "the generated dynamic function should execute");
            assertEquals(1, router.generations.get(),
                    "two gaps with a budget of 1 must generate exactly one function");
        } finally {
            runtime.shutdown();
        }
    }
}
