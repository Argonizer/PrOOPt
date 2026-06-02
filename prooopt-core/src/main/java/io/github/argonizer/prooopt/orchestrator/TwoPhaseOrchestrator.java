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
import io.github.argonizer.prooopt.audit.RunMetrics;
import io.github.argonizer.prooopt.autobox.PrOOPtAutoBoxer;
import io.github.argonizer.prooopt.config.OrchestrationConfig;
import io.github.argonizer.prooopt.context.PrOOPtContext;
import io.github.argonizer.prooopt.dynamic.DynamicFunctionCache;
import io.github.argonizer.prooopt.dynamic.DynamicPromptFunction;
import io.github.argonizer.prooopt.embedding.EmbeddingEngine;
import io.github.argonizer.prooopt.embedding.ScoredTool;
import io.github.argonizer.prooopt.embedding.ToolIndexer;
import io.github.argonizer.prooopt.exception.PrOOPtAutoBoxException;
import io.github.argonizer.prooopt.exception.PrOOPtException;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;
import io.github.argonizer.prooopt.model.ExecutionStream;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanMode;
import io.github.argonizer.prooopt.model.ToolDescriptor;
import io.github.argonizer.prooopt.router.AutoRouting;
import io.github.argonizer.prooopt.router.ModelRouter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The headline feature: instead of dumping every tool on a model, PrOOPt narrows the field in two
 * cheap phases before spending on planning, then runs the resulting {@link ExecutionPlan} with
 * wave-based parallelism.
 *
 * <ol>
 *   <li><b>Discovery</b> — a cheap (LOCAL) model lists the plain-English capabilities needed.</li>
 *   <li><b>Matching</b> — each capability binds to its single best tool by cosine similarity. When a
 *       gap is found and {@code allowDynamic} is set, an ephemeral prompt function is generated.</li>
 *   <li><b>Execution</b> — only the matched tools (and any dynamic functions) reach the execution
 *       model, which returns an {@link ExecutionPlan} the {@link PlanExecutor} runs.</li>
 * </ol>
 *
 * <p>Under {@link PlanMode#STATIC} the plan template is memoised in a {@link PlanCache}; warm hits
 * skip discovery and planning entirely (the headline {@code cloud_calls=0}). Under
 * {@link PlanMode#DYNAMIC} a fresh plan is built every call.
 */
public class TwoPhaseOrchestrator {

    private final ModelRouter router;
    private final PrOOPtAutoBoxer autoBoxer;
    private final ToolIndexer indexer;
    private final DagExecutor executor;
    private final AuditLogger audit;
    private final OrchestrationConfig config;
    private final EmbeddingEngine embeddingEngine;
    private final PlanInstantiator instantiator = new PlanInstantiator();

    /** One {@link PlanCache} per orchestrator (keyed by system-prompt identity). */
    private final Map<String, PlanCache> planCaches = new ConcurrentHashMap<>();

    public TwoPhaseOrchestrator(ModelRouter router, PrOOPtAutoBoxer autoBoxer, ToolIndexer indexer,
                                DagExecutor executor, AuditLogger audit, OrchestrationConfig config,
                                EmbeddingEngine embeddingEngine) {
        this.router = router;
        this.autoBoxer = autoBoxer;
        this.indexer = indexer;
        this.executor = executor;
        this.audit = audit;
        this.config = config;
        this.embeddingEngine = embeddingEngine;
    }

    /** Invalidates every orchestrator's plan cache. */
    public void clearPlanCache() {
        planCaches.values().forEach(PlanCache::invalidate);
    }

    /** Removes cached plans referencing {@code functionName} across all orchestrators. */
    public void clearPlanCacheFor(String functionName) {
        planCaches.values().forEach(cache -> cache.invalidateFor(functionName));
    }

    /** Runs the full two-phase flow for an input and returns the plan's resolved output. */
    public Object run(Object input, OrchestratorSpec spec) {
        String traceId = PrOOPtContext.getTraceId();
        String inputStr = input == null ? "" : String.valueOf(input);
        long start = System.currentTimeMillis();
        if (spec.hooks() != null) {
            spec.hooks().onRunStart(traceId, input);
        }
        int dynamicGenerated = 0;
        boolean cached = false;
        long planGenerationMs = 0L;
        boolean planningWasCloud = false;
        try {
            ExecutionPlan plan;
            PlanCache cache = spec.planMode() == PlanMode.STATIC ? cacheFor(spec) : null;

            Optional<ExecutionPlan> hit = cache == null ? Optional.empty() : cache.get(inputStr);
            if (hit.isPresent()) {
                cached = true;
                plan = instantiator.instantiate(hit.get(), inputStr);
            } else {
                long planStart = System.currentTimeMillis();
                List<String> capabilities = discover(inputStr);
                audit.discovery(inputStr, capabilities);
                Matching matching = match(capabilities, inputStr, spec, traceId);
                dynamicGenerated = matching.dynamicGenerated();
                plan = buildPlan(spec.systemPrompt(), inputStr, matching.tools(), matching.dynamic());
                planGenerationMs = System.currentTimeMillis() - planStart;
                planningWasCloud = config.getExecutionModel() != ModelTier.LOCAL;
                // Plans that reference session-scoped dynamic functions must not be cached — those
                // functions are discarded at run end and would not exist on a later warm hit.
                if (cache != null && dynamicGenerated == 0) {
                    cache.put(inputStr, plan);
                }
            }

            long execStart = System.currentTimeMillis();
            Object result = executor.execute(plan, input, spec.hooks(), spec.dagTimeoutMs());
            long execMs = System.currentTimeMillis() - execStart;

            long totalMs = System.currentTimeMillis() - start;
            emitSummary(traceId, plan, inputStr, totalMs, planGenerationMs, execMs, spec.planMode(),
                    cached, planningWasCloud, dynamicGenerated);
            if (spec.hooks() != null) {
                spec.hooks().onRunComplete(traceId, totalMs, plan.allSteps().size());
            }
            return result;
        } finally {
            PrOOPtContext.clear();
        }
    }

    private PlanCache cacheFor(OrchestratorSpec spec) {
        String key = String.valueOf(spec.systemPrompt().hashCode());
        return planCaches.computeIfAbsent(key, k -> new PlanCache(spec.planCacheStrategy(),
                spec.planCacheTtl(), spec.planCacheSize(), spec.planCacheSimilarityThreshold(),
                embeddingEngine, router, audit));
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

    /** Result of the matching phase: the bound static tools and any generated dynamic functions. */
    private record Matching(List<ToolDescriptor> tools, List<DynamicPromptFunction> dynamic,
                            int dynamicGenerated) {
    }

    private Matching match(List<String> capabilities, String input, OrchestratorSpec spec, String traceId) {
        Map<String, ToolDescriptor> matched = new LinkedHashMap<>();
        List<DynamicPromptFunction> dynamic = new ArrayList<>();
        int dynamicGenerated = 0;

        for (String capability : capabilities) {
            Optional<ScoredTool> best = indexer.findBestMatch(capability, config.getMinMatchScore());
            if (best.isPresent()) {
                ScoredTool scored = best.get();
                if (!matched.containsKey(scored.tool().name())) {
                    matched.put(scored.tool().name(), scored.tool());
                    audit.toolMatch(scored.tool().name(), scored.tool().type(),
                            scored.tool().modelTier(), scored.score());
                }
                continue;
            }

            // Gap: no registered tool cleared the threshold.
            double bestScore = indexer.findBestMatch(capability, 0.0).map(ScoredTool::score).orElse(0.0);
            audit.dynamicGapDetected(traceId, capability, bestScore, config.getMinMatchScore());

            if (!spec.allowDynamic()) {
                audit.dynamicGapSkipped(traceId, capability);
                continue;
            }
            if (DynamicFunctionCache.count() >= spec.maxDynamicFunctions()) {
                audit.dynamicBudgetExceeded(traceId, spec.maxDynamicFunctions(), capability);
                continue;
            }
            audit.dynamicGenerating(traceId, spec.dynamicFunctionModel(),
                    spec.maxDynamicFunctions() - DynamicFunctionCache.count());
            DynamicPromptFunction fn = generateDynamic(capability, spec.dynamicFunctionModel(), traceId);
            if (fn != null) {
                DynamicFunctionCache.register(fn);
                audit.dynamicRegistered(fn);
                dynamic.add(fn);
                dynamicGenerated++;
            }
        }

        List<ToolDescriptor> tools = new ArrayList<>(matched.values());
        if (tools.isEmpty() && dynamic.isEmpty()) {
            // Nothing matched and no dynamic functions: fall back to plain relevance over the input.
            for (ScoredTool scored : indexer.selectRelevant(input, config.getMaxTools(), 0.0)) {
                tools.add(scored.tool());
                audit.toolMatch(scored.tool().name(), scored.tool().type(),
                        scored.tool().modelTier(), scored.score());
            }
        }
        if (tools.size() > config.getMaxTools()) {
            tools = new ArrayList<>(tools.subList(0, config.getMaxTools()));
        }
        return new Matching(tools, dynamic, dynamicGenerated);
    }

    private static final String GENERATION_PROMPT = """
            A required tool capability has no registered match: "%s"

            Generate a minimal PrOOPt prompt function definition as a JSON object:
            {
              "name": "<camelCase function name — unique, descriptive>",
              "prompt": "<prompt template using {paramName} placeholders>",
              "model": "<LOCAL | CLOUD_FAST | CLOUD_ADVANCED>",
              "description": "<one sentence, plain English>"
            }

            Rules:
            - Keep the prompt focused on exactly one atomic operation
            - Use LOCAL unless deep reasoning or large context is required
            - Use {input} as the default parameter name if the operation takes one argument
            - Respond with JSON only — no preamble, no markdown fences
            """;

    private DynamicPromptFunction generateDynamic(String capability, ModelTier model, String traceId) {
        String response = router.route(String.format(GENERATION_PROMPT, capability), model);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> def = (Map<String, Object>) autoBoxer.autobox(response, Map.class);
            String name = String.valueOf(def.getOrDefault("name", "dynamicFn"));
            String prompt = String.valueOf(def.getOrDefault("prompt", "{input}"));
            ModelTier tier = ModelTier.fromString(String.valueOf(def.get("model")));
            String description = String.valueOf(def.getOrDefault("description", capability));
            return new DynamicPromptFunction(name, prompt, tier == null ? model : tier, description,
                    traceId, System.currentTimeMillis());
        } catch (PrOOPtAutoBoxException e) {
            return null; // generation failed — capability is skipped
        }
    }

    // ------------------------------------------------------------------ Phase 3: planning

    private ExecutionPlan buildPlan(String systemPrompt, String input, List<ToolDescriptor> tools,
                                    List<DynamicPromptFunction> dynamic) {
        String prompt = buildExecutionPrompt(systemPrompt, input, tools, dynamic);
        String response = router.route(prompt, config.getExecutionModel());
        try {
            return (ExecutionPlan) autoBoxer.autobox(response, ExecutionPlan.class);
        } catch (PrOOPtAutoBoxException e) {
            throw new PrOOPtException(
                    "orchestrator could not produce a valid ExecutionPlan from the execution model. "
                            + e.getMessage(), e);
        }
    }

    private String buildExecutionPrompt(String systemPrompt, String input, List<ToolDescriptor> tools,
                                        List<DynamicPromptFunction> dynamic) {
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt).append("\n\n");
        sb.append("You orchestrate the following tools. Use ONLY these:\n");
        for (ToolDescriptor tool : tools) {
            sb.append(renderTool(tool)).append('\n');
        }
        for (DynamicPromptFunction fn : dynamic) {
            sb.append(renderDynamic(fn)).append('\n');
        }
        sb.append("""

                Produce an execution plan as a single JSON object with this schema:
                {
                  "traceId": "string",
                  "streams": [
                    {
                      "streamId": "S1",
                      "steps": [
                        { "stepId": "S1.1", "streamId": "S1", "function": "<toolName>",
                          "type": "CODE|PROMPT", "model": "LOCAL|CLOUD_FAST|CLOUD_ADVANCED|AUTO",
                          "args": { "<paramName>": "<literal or $S1.1 or ${userInput}>" },
                          "dependsOn": ["S1.0"], "assignTo": "$<variableName>", "timeoutMs": 0 }
                      ]
                    }
                  ],
                  "output": "$<variableName>"
                }

                Rules: stepIds are globally unique strings like "S1.1"; reference the user input as
                ${userInput}; reference a prior step's result by its stepId (e.g. $S1.1) or assignTo
                name (e.g. $date); dependsOn lists stepIds whose results a step needs; "type" must
                match the tool; omit "model" for CODE steps; use a single stream "S1" for simple plans.

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

    private static String renderDynamic(DynamicPromptFunction fn) {
        return "- " + fn.name() + "(input: String) -> String [PROMPT " + fn.model()
                + " DYNAMIC] : " + fn.description();
    }

    // ------------------------------------------------------------------ summary

    private void emitSummary(String traceId, ExecutionPlan plan, String input, long totalMs,
                             long planGenerationMs, long execMs, PlanMode mode, boolean cached,
                             boolean planningWasCloud, int dynamicGenerated) {
        Map<String, ToolDescriptor> byName = new LinkedHashMap<>();
        for (ToolDescriptor tool : indexer.tools()) {
            byName.put(tool.name(), tool);
        }

        int functions = plan.allSteps().size();
        int code = 0;
        int local = 0;
        int cloud = 0;
        for (ExecutionStep step : plan.allSteps().values()) {
            ToolDescriptor descriptor = byName.get(step.function());
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

        // cloud_calls = elevated function executions + (1 plan-generation call, only when not cached
        // and the execution model is a cloud tier). On a STATIC warm hit this is the headline 0.
        int planningCloudCalls = (!cached && planningWasCloud) ? 1 : 0;
        int cloudCalls = cloud + planningCloudCalls;

        // Best-effort timing attribution: lump measured execution time into the dominant tier.
        long localMs = 0;
        long cloudMs = 0;
        long codeMs = 0;
        if (cloud > 0) {
            cloudMs = execMs;
        } else if (local > 0) {
            localMs = execMs;
        } else {
            codeMs = execMs;
        }
        long overheadMs = Math.max(0, totalMs - planGenerationMs - execMs);

        long tokens = AutoRouting.estimateTokens(input) + functions * 50L;
        double estCost = cloudCalls * 0.0020;

        audit.orchestratorSummary(new RunMetrics(traceId, mode, cached, totalMs, planGenerationMs,
                localMs, cloudMs, codeMs, overheadMs, functions, code, local, cloudCalls,
                dynamicGenerated, tokens, estCost));
    }
}
