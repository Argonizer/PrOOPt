/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.audit.AuditLogger;
import io.github.argonizer.prooopt.autobox.PrOOPtAutoBoxer;
import io.github.argonizer.prooopt.config.OrchestrationConfig;
import io.github.argonizer.prooopt.context.PrOOPtContext;
import io.github.argonizer.prooopt.embedding.ScoredTool;
import io.github.argonizer.prooopt.embedding.ToolIndexer;
import io.github.argonizer.prooopt.exception.PrOOPtAutoBoxException;
import io.github.argonizer.prooopt.exception.PrOOPtException;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.ToolDescriptor;
import io.github.argonizer.prooopt.router.AutoRouting;
import io.github.argonizer.prooopt.router.ModelRouter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The headline feature: instead of dumping every tool on a model, PrOOPt narrows the field in two
 * cheap phases before spending on planning.
 *
 * <ol>
 *   <li><b>Discovery</b> — a cheap (LOCAL) model reads the request and lists the plain-English
 *       capabilities needed.</li>
 *   <li><b>Matching</b> — each capability is bound to its single best tool by cosine similarity,
 *       filtered by a score threshold and de-duplicated.</li>
 *   <li><b>Execution</b> — only the matched tools are handed to the execution model, which returns an
 *       {@link ExecutionPlan} that the {@link PlanExecutor} runs with wave-based parallelism.</li>
 * </ol>
 *
 * <p>Plans are memoised by input so identical requests skip discovery and planning entirely.
 */
public class TwoPhaseOrchestrator {

    private final ModelRouter router;
    private final PrOOPtAutoBoxer autoBoxer;
    private final ToolIndexer indexer;
    private final PlanExecutor executor;
    private final AuditLogger audit;
    private final OrchestrationConfig config;
    private final LruCache<String, ExecutionPlan> planCache = new LruCache<>(500);

    public TwoPhaseOrchestrator(ModelRouter router, PrOOPtAutoBoxer autoBoxer, ToolIndexer indexer,
                                PlanExecutor executor, AuditLogger audit, OrchestrationConfig config) {
        this.router = router;
        this.autoBoxer = autoBoxer;
        this.indexer = indexer;
        this.executor = executor;
        this.audit = audit;
        this.config = config;
    }

    /** Runs the full two-phase flow for an input and returns the plan's resolved output. */
    public Object run(Object input, OrchestratorSpec spec) {
        String traceId = PrOOPtContext.getTraceId();
        String inputStr = input == null ? "" : String.valueOf(input);
        long start = System.currentTimeMillis();
        if (spec.hooks() != null) {
            spec.hooks().onRunStart(traceId, input);
        }
        try {
            String cacheKey = spec.systemPrompt().hashCode() + ":" + inputStr;
            ExecutionPlan plan = planCache.get(cacheKey);
            if (plan == null) {
                List<String> capabilities = discover(inputStr);
                audit.discovery(inputStr, capabilities);
                List<ToolDescriptor> tools = match(capabilities, inputStr);
                plan = buildPlan(spec.systemPrompt(), inputStr, tools);
                planCache.put(cacheKey, plan);
            }
            Object result = executor.execute(plan, input, spec.parallel(), spec.maxThreads(), spec.hooks());
            emitSummary(traceId, plan, inputStr, System.currentTimeMillis() - start);
            if (spec.hooks() != null) {
                // Every step is a function execution. We count steps rather than the thread-local
                // counter, which is per-thread and would under-count under parallel waves.
                spec.hooks().onRunComplete(traceId, System.currentTimeMillis() - start, plan.steps().size());
            }
            return result;
        } finally {
            PrOOPtContext.clear();
        }
    }

    // ------------------------------------------------------------------ Phase 1: discovery

    private List<String> discover(String input) {
        String prompt = """
                You are a planning assistant. Read the user request and list the distinct tool
                capabilities required to fulfil it — each as a short, plain-English description of
                one capability (for example, "validate the input is not empty" or "extract a date
                from text").

                User request:
                """ + input + "\n\n" + autoBoxer.buildFormatInstruction(List.class);

        String response = router.route(prompt, config.getDiscoveryModel());
        try {
            Object boxed = autoBoxer.autobox(response, List.class);
            List<String> capabilities = new ArrayList<>();
            for (Object item : (List<?>) boxed) {
                capabilities.add(String.valueOf(item));
            }
            return capabilities;
        } catch (PrOOPtAutoBoxException e) {
            return fallbackSplit(response);
        }
    }

    private static List<String> fallbackSplit(String response) {
        List<String> out = new ArrayList<>();
        for (String line : response.split("\\r?\\n")) {
            String cleaned = line.replaceAll("^[\\s\\-*0-9.\\)]+", "").trim();
            if (!cleaned.isEmpty()) {
                out.add(cleaned);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ Phase 2: matching

    private List<ToolDescriptor> match(List<String> capabilities, String input) {
        Map<String, ToolDescriptor> matched = new LinkedHashMap<>();
        for (String capability : capabilities) {
            indexer.findBestMatch(capability, config.getMinMatchScore()).ifPresent(scored -> {
                if (!matched.containsKey(scored.tool().name())) {
                    matched.put(scored.tool().name(), scored.tool());
                    audit.toolMatch(scored.tool().name(), scored.tool().type(),
                            scored.tool().modelTier(), scored.score());
                }
            });
        }

        List<ToolDescriptor> tools = new ArrayList<>(matched.values());
        if (tools.isEmpty()) {
            // Nothing cleared the threshold: fall back to plain relevance over the raw input.
            for (ScoredTool scored : indexer.selectRelevant(input, config.getMaxTools(), 0.0)) {
                tools.add(scored.tool());
                audit.toolMatch(scored.tool().name(), scored.tool().type(),
                        scored.tool().modelTier(), scored.score());
            }
        }
        if (tools.size() > config.getMaxTools()) {
            tools = new ArrayList<>(tools.subList(0, config.getMaxTools()));
        }
        return tools;
    }

    // ------------------------------------------------------------------ Phase 3: planning

    private ExecutionPlan buildPlan(String systemPrompt, String input, List<ToolDescriptor> tools) {
        String prompt = buildExecutionPrompt(systemPrompt, input, tools);
        String response = router.route(prompt, config.getExecutionModel());
        try {
            return (ExecutionPlan) autoBoxer.autobox(response, ExecutionPlan.class);
        } catch (PrOOPtAutoBoxException e) {
            throw new PrOOPtException(
                    "orchestrator could not produce a valid ExecutionPlan from the execution model. "
                            + e.getMessage(), e);
        }
    }

    private String buildExecutionPrompt(String systemPrompt, String input, List<ToolDescriptor> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt).append("\n\n");
        sb.append("You orchestrate the following tools. Use ONLY these:\n");
        for (ToolDescriptor tool : tools) {
            sb.append(renderTool(tool)).append('\n');
        }
        sb.append("""

                Produce an execution plan as a single JSON object with this schema:
                {
                  "traceId": "string",
                  "steps": [
                    { "stepId": 1, "function": "<toolName>", "type": "CODE|PROMPT",
                      "model": "LOCAL|CLOUD_FAST|CLOUD_ADVANCED|AUTO",
                      "args": { "<paramName>": "<literal or $variable or ${userInput}>" },
                      "dependsOn": [<stepIds>], "assignTo": "$<variableName>" }
                  ],
                  "output": "$<variableName>"
                }

                Rules: reference the user input as ${userInput}; reference a prior step's result by its
                assignTo name (for example $date); set dependsOn to the stepIds whose results a step
                needs; "type" must match the tool; omit "model" for CODE steps.

                User request:
                """);
        sb.append(input).append("\n\n");
        sb.append(autoBoxer.buildFormatInstruction(ExecutionPlan.class));
        return sb.toString();
    }

    private static String renderTool(ToolDescriptor tool) {
        StringBuilder params = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Class<?>> p : tool.paramSchema().entrySet()) {
            if (!first) {
                params.append(", ");
            }
            params.append(p.getKey()).append(": ").append(p.getValue().getSimpleName());
            first = false;
        }
        String tier = tool.modelTier() == null ? "CODE" : tool.modelTier().name();
        return "- " + tool.name() + "(" + params + ") -> " + tool.returnType().getSimpleName()
                + " [" + tool.type() + " " + tier + "] : " + tool.description();
    }

    // ------------------------------------------------------------------ summary

    private void emitSummary(String traceId, ExecutionPlan plan, String input, long totalMs) {
        Map<String, ToolDescriptor> byName = new LinkedHashMap<>();
        for (ToolDescriptor tool : indexer.tools()) {
            byName.put(tool.name(), tool);
        }

        int functions = plan.steps().size();
        int code = 0;
        int local = 0;
        int cloud = 0;
        for (ExecutionStep step : plan.steps()) {
            ToolDescriptor descriptor = byName.get(step.function());
            // Count by the function's own declared authority — the governance decision — not the
            // planner's advisory step fields.
            FunctionType type = descriptor != null ? descriptor.type() : step.type();
            ModelTier tier = descriptor != null ? descriptor.modelTier() : step.model();
            if (type == FunctionType.CODE) {
                code++;
            } else if (tier == ModelTier.LOCAL) {
                local++;
            } else {
                cloud++;
            }
        }
        // Illustrative cost model: deterministic code and on-device LOCAL inference are free; only the
        // elevated cloud tiers incur an estimated charge.
        long tokens = AutoRouting.estimateTokens(input) + functions * 50L;
        double estCost = cloud * 0.0020;
        audit.orchestratorSummary(traceId, totalMs, functions, code, local, cloud, tokens, estCost);
    }
}
