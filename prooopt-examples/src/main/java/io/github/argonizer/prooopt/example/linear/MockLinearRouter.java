/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.example.linear;

import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.ModelRouter;

/**
 * Deterministic mock router for the linear-system solver demo. No API key or network required.
 *
 * <p>Returns canned discovery and planning responses so the full orchestration pipeline runs without
 * a live model. The plan is dimension-agnostic: it forwards the orchestrator's input (the augmented
 * matrix) straight into the {@code @CodeFunction} steps, so the same plan works for any n×n system.
 * Swap for a real {@code CloudModelRouter} to use the Anthropic or OpenAI API.
 */
public final class MockLinearRouter implements ModelRouter {

    @Override
    public String route(String prompt, ModelTier tier) {

        // Phase 1 – capability discovery
        if (prompt.contains("planning assistant")) {
            return """
                    ["solve nxn linear system","verify solution","compute residual",
                     "format vector as fractions","interpret solution","explain method","package result"]
                    """;
        }

        // Phase 2 – execution plan (dimension-agnostic: ${userInput} is the augmented matrix)
        if (prompt.contains("Produce an execution plan")) {
            return "{\"traceId\":\"linear-1\",\"streams\":[{\"streamId\":\"S1\",\"steps\":["
                    + "{\"stepId\":\"S1.1\",\"streamId\":\"S1\",\"function\":\"gaussianElimination\",\"type\":\"CODE\","
                    + "\"args\":{\"augmented\":\"${userInput}\"},\"dependsOn\":[],\"assignTo\":\"$sol\",\"timeoutMs\":0},"
                    + "{\"stepId\":\"S1.2\",\"streamId\":\"S1\",\"function\":\"verifySolution\",\"type\":\"CODE\","
                    + "\"args\":{\"augmented\":\"${userInput}\",\"solution\":\"$sol\"},\"dependsOn\":[\"S1.1\"],\"assignTo\":\"$ok\",\"timeoutMs\":0},"
                    + "{\"stepId\":\"S1.3\",\"streamId\":\"S1\",\"function\":\"computeResidual\",\"type\":\"CODE\","
                    + "\"args\":{\"augmented\":\"${userInput}\",\"solution\":\"$sol\"},\"dependsOn\":[\"S1.1\"],\"assignTo\":\"$res\",\"timeoutMs\":0},"
                    + "{\"stepId\":\"S1.4\",\"streamId\":\"S1\",\"function\":\"formatVectorAsFractions\",\"type\":\"CODE\","
                    + "\"args\":{\"solution\":\"$sol\"},\"dependsOn\":[\"S1.1\"],\"assignTo\":\"$fracs\",\"timeoutMs\":0},"
                    + "{\"stepId\":\"S1.5\",\"streamId\":\"S1\",\"function\":\"summarizeSolution\",\"type\":\"CODE\","
                    + "\"args\":{\"solution\":\"$sol\",\"fractions\":\"$fracs\"},\"dependsOn\":[\"S1.1\",\"S1.4\"],\"assignTo\":\"$summary\",\"timeoutMs\":0},"
                    + "{\"stepId\":\"S1.6\",\"streamId\":\"S1\",\"function\":\"interpretSolution\",\"type\":\"PROMPT\",\"model\":\"LOCAL\","
                    + "\"args\":{\"solutionSummary\":\"$summary\",\"verified\":\"$ok\"},"
                    + "\"dependsOn\":[\"S1.2\",\"S1.5\"],\"assignTo\":\"$interp\",\"timeoutMs\":0},"
                    + "{\"stepId\":\"S1.7\",\"streamId\":\"S1\",\"function\":\"packageResult\",\"type\":\"CODE\","
                    + "\"args\":{\"solution\":\"$sol\",\"verified\":\"$ok\",\"interpretation\":\"$interp\"},"
                    + "\"dependsOn\":[\"S1.6\"],\"assignTo\":\"$out\",\"timeoutMs\":0}"
                    + "]}],\"output\":\"$out\"}";
        }

        // LOCAL model — interpretation prose
        if (prompt.contains("mathematics tutor") || prompt.contains("interpretSolution")
                || prompt.contains("Interpret")) {
            return "The system has a unique solution. Where a variable is negative (for this demo "
                    + "x = −131/5 = −26.2), it reflects that the variable must offset a dominant "
                    + "coefficient elsewhere to satisfy all constraints simultaneously. The remaining "
                    + "variables are positive and together satisfy every equation exactly.";
        }

        // LOCAL model — method explanation
        if (prompt.contains("mathematics educator") || prompt.contains("Gaussian elimination")) {
            return "Gaussian elimination reduces the augmented matrix to upper-triangular form. "
                    + "Partial pivoting selects the largest absolute value in each column as the pivot "
                    + "to avoid catastrophic cancellation. Back-substitution then recovers each variable.";
        }

        return "OK";
    }
}
