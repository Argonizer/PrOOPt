/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.example;

import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.ModelRouter;

/**
 * A deterministic, offline stand-in for a real model, used so the example (and tests) run end-to-end
 * with no API key and no network. It inspects the enriched prompt and returns the appropriate canned
 * response: a capability list for discovery, a fixed {@code ExecutionPlan} for planning, and concrete
 * values for each leaf prompt function.
 */
public class MockModelRouter implements ModelRouter {

    private static final String CAPABILITIES = """
            ["validate and normalize the contract text",
             "extract the contract signing date",
             "assess the contract risk level",
             "count the number of words",
             "summarize the contract"]
            """;

    private static final String PLAN = """
            {
              "traceId": "legal-demo",
              "steps": [
                { "stepId": 1, "function": "normalizeWhitespace", "type": "CODE",
                  "args": {"text": "${userInput}"}, "dependsOn": [], "assignTo": "$clean" },
                { "stepId": 2, "function": "extractSigningDate", "type": "PROMPT", "model": "LOCAL",
                  "args": {"text": "$clean"}, "dependsOn": [1], "assignTo": "$date" },
                { "stepId": 3, "function": "detectRiskLevel", "type": "PROMPT", "model": "LOCAL",
                  "args": {"text": "$clean"}, "dependsOn": [1], "assignTo": "$risk" },
                { "stepId": 4, "function": "countWords", "type": "CODE",
                  "args": {"text": "$clean"}, "dependsOn": [1], "assignTo": "$words" },
                { "stepId": 5, "function": "generateSummary", "type": "PROMPT", "model": "CLOUD_ADVANCED",
                  "args": {"text": "$clean"}, "dependsOn": [1], "assignTo": "$summary" }
              ],
              "output": "$summary"
            }
            """;

    private static final String SUMMARY = "This mutual non-disclosure agreement between Acme Corp and "
            + "Beta LLC takes effect on 2026-01-15, binds both parties to confidentiality for three "
            + "years, and is assessed as HIGH risk due to broad indemnification obligations.";

    @Override
    public String route(String prompt, ModelTier tier) {
        if (prompt.contains("Produce an execution plan")) {
            return PLAN;
        }
        if (prompt.contains("planning assistant")) {
            return CAPABILITIES;
        }
        if (prompt.contains("signing date")) {
            return "2026-01-15";
        }
        if (prompt.contains("risk level")) {
            return "HIGH";
        }
        if (prompt.contains("Summarize")) {
            return SUMMARY;
        }
        return "OK";
    }
}
