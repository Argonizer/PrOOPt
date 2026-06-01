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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts a cached {@link ExecutionPlan} template into a live plan instance by binding the current
 * invocation's input to top-level input placeholders.
 *
 * <p>Internal step-to-step variable references ({@code $variableName}) are left as-is — they are
 * resolved at execution time by {@link PlanExecutor} from the execution context. A fresh trace id is
 * always assigned so each run has an independent audit trail even when reusing a cached template.
 */
public class PlanInstantiator {

    private static final Set<String> INPUT_PLACEHOLDERS =
            Set.of("${input}", "${userInput}", "${request}");

    public ExecutionPlan instantiate(ExecutionPlan template, String liveInput) {
        List<ExecutionStep> boundSteps = template.steps().stream()
                .map(step -> bindStep(step, liveInput))
                .toList();
        return new ExecutionPlan(PrOOPtContext.getTraceId(), boundSteps, template.output());
    }

    private ExecutionStep bindStep(ExecutionStep step, String liveInput) {
        Map<String, Object> boundArgs = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : step.args().entrySet()) {
            boundArgs.put(e.getKey(),
                    INPUT_PLACEHOLDERS.contains(e.getValue()) ? liveInput : e.getValue());
        }
        return step.withArgs(boundArgs);
    }
}
