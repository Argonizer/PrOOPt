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
 * a live model. Swap for a real {@code CloudModelRouter} to use the Anthropic or OpenAI API.
 */
public final class MockLinearRouter implements ModelRouter {

    private static final String AUGMENTED_ARG =
            "[1.0,1.0,1.0,25.0,5.0,3.0,2.0,0.0,0.0,1.0,-1.0,6.0]";

    @Override
    public String route(String prompt, ModelTier tier) {

        // Phase 1 – capability discovery
        if (prompt.contains("planning assistant")) {
            return """
                    ["solve 3x3 linear system","verify solution","compute residual",
                     "format as fraction","interpret solution","explain method","package result"]
                    """;
        }

        // Phase 2 – execution plan
        if (prompt.contains("Produce an execution plan")) {
            String aug = AUGMENTED_ARG;
            return "{\"traceId\":\"linear-1\",\"steps\":["
                    + "{\"stepId\":1,\"function\":\"gaussianElimination\",\"type\":\"CODE\","
                    + "\"args\":{\"augmented\":" + aug + "},\"dependsOn\":[],\"assignTo\":\"$sol\"},"
                    + "{\"stepId\":2,\"function\":\"verifySolution\",\"type\":\"CODE\","
                    + "\"args\":{\"augmented\":" + aug + ",\"solution\":\"$sol\"},\"dependsOn\":[1],\"assignTo\":\"$ok\"},"
                    + "{\"stepId\":3,\"function\":\"computeResidual\",\"type\":\"CODE\","
                    + "\"args\":{\"augmented\":" + aug + ",\"solution\":\"$sol\"},\"dependsOn\":[1],\"assignTo\":\"$res\"},"
                    + "{\"stepId\":4,\"function\":\"formatAsFraction\",\"type\":\"CODE\","
                    + "\"args\":{\"value\":-26.2},\"dependsOn\":[1],\"assignTo\":\"$xf\"},"
                    + "{\"stepId\":5,\"function\":\"interpretSolution\",\"type\":\"PROMPT\",\"model\":\"LOCAL\","
                    + "\"args\":{\"xFraction\":\"$xf\",\"xDecimal\":\"-26.2\",\"yFraction\":\"143/5\","
                    + "\"yDecimal\":\"28.6\",\"zFraction\":\"113/5\",\"zDecimal\":\"22.6\",\"verified\":\"$ok\"},"
                    + "\"dependsOn\":[2,4],\"assignTo\":\"$interp\"},"
                    + "{\"stepId\":6,\"function\":\"packageResult\",\"type\":\"CODE\","
                    + "\"args\":{\"solution\":\"$sol\",\"verified\":\"$ok\",\"interpretation\":\"$interp\"},"
                    + "\"dependsOn\":[5],\"assignTo\":\"$out\"}],"
                    + "\"output\":\"$out\"}";
        }

        // LOCAL model — interpretation prose
        if (prompt.contains("mathematics tutor") || prompt.contains("interpretSolution")
                || prompt.contains("Interpret")) {
            return "The system has a unique solution. Notably, x = −131/5 = −26.2 is negative, "
                    + "which reflects that increasing x simultaneously satisfies the tight sum constraint "
                    + "(x+y+z=25) while offsetting the dominant coefficient 5x in the second equation. "
                    + "Values y = 143/5 and z = 113/5 are both positive, with their difference y−z = 6 "
                    + "confirming the third equation exactly.";
        }

        // LOCAL model — method explanation
        if (prompt.contains("mathematics educator") || prompt.contains("Gaussian elimination")) {
            return "Gaussian elimination reduces the augmented matrix to upper-triangular form. "
                    + "Partial pivoting selects the largest absolute value in each column as the pivot "
                    + "to avoid catastrophic cancellation. Back-substitution then recovers x, y, z.";
        }

        return "OK";
    }
}
