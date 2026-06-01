/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.audit;

import io.github.argonizer.prooopt.dynamic.DynamicPromptFunction;
import io.github.argonizer.prooopt.model.FunctionCall;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.LogLevel;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanCacheStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Emits the structured PrOOPt audit trail to the dedicated {@code io.github.argonizer.prooopt.audit}
 * logger. Lines are greppable by {@code trace=} so an entire run reads as one story; per-function
 * {@link LogLevel} controls verbosity, and callers pass already-redacted values for sensitive data.
 */
public class AuditLogger {

    /** The dedicated audit logger name; configured with its own appender in {@code log4j2.xml}. */
    public static final String AUDIT_LOGGER_NAME = "io.github.argonizer.prooopt.audit";

    private static final int MAX_VALUE_LEN = 300;

    private final Logger log;

    public AuditLogger() {
        this(LogManager.getLogger(AUDIT_LOGGER_NAME));
    }

    public AuditLogger(Logger log) {
        this.log = log;
    }

    // ------------------------------------------------------------------ prompt functions

    public void promptStart(FunctionCall call, boolean thinking, Map<String, Object> redactedInputs, LogLevel level) {
        if (level == LogLevel.FULL) {
            log.info("[PROOOPT][PROMPT_FUNCTION][START] trace={} class={} function={} model={} thinking={} "
                            + "description='{}' inputs={}",
                    call.traceId(), simpleClass(call), call.name(), call.modelTier(), thinking,
                    nullSafe(call.description()), formatMap(redactedInputs));
        }
    }

    public void promptEnd(FunctionCall call, Object redactedOutput, LogLevel level) {
        if (level == LogLevel.FULL) {
            log.info("[PROOOPT][PROMPT_FUNCTION][END]   trace={} function={} model={} duration={}ms output={}",
                    call.traceId(), call.name(), call.modelTier(), call.elapsedMs(), truncate(redactedOutput));
        } else if (level == LogLevel.SUMMARY) {
            log.info("[PROOOPT][PROMPT_FUNCTION] trace={} function={} model={} duration={}ms",
                    call.traceId(), call.name(), call.modelTier(), call.elapsedMs());
        }
    }

    public void promptError(FunctionCall call, Throwable error, LogLevel level) {
        if (level != LogLevel.OFF) {
            log.error("[PROOOPT][PROMPT_FUNCTION][ERROR] trace={} function={} model={} duration={}ms error={}",
                    call.traceId(), call.name(), call.modelTier(), call.elapsedMs(), describe(error));
        }
    }

    // ------------------------------------------------------------------ code functions

    public void codeStart(FunctionCall call, Map<String, Object> redactedInputs, LogLevel level) {
        if (level == LogLevel.FULL) {
            log.info("[PROOOPT][CODE_FUNCTION][START] trace={} class={} function={} description='{}' inputs={}",
                    call.traceId(), simpleClass(call), call.name(), nullSafe(call.description()),
                    formatMap(redactedInputs));
        }
    }

    public void codeEnd(FunctionCall call, Object redactedOutput, LogLevel level) {
        if (level == LogLevel.FULL) {
            log.info("[PROOOPT][CODE_FUNCTION][END]   trace={} function={} duration={}ms output={}",
                    call.traceId(), call.name(), call.elapsedMs(), truncate(redactedOutput));
        } else if (level == LogLevel.SUMMARY) {
            log.info("[PROOOPT][CODE_FUNCTION] trace={} function={} duration={}ms",
                    call.traceId(), call.name(), call.elapsedMs());
        }
    }

    public void codeError(FunctionCall call, Throwable error, LogLevel level) {
        if (level != LogLevel.OFF) {
            log.error("[PROOOPT][CODE_FUNCTION][ERROR] trace={} function={} duration={}ms error={}",
                    call.traceId(), call.name(), call.elapsedMs(), describe(error));
        }
    }

    // ------------------------------------------------------------------ orchestration

    public void discovery(String input, List<String> requestedCapabilities) {
        log.info("[PROOOPT][DISCOVERY] input='{}' requestedCapabilities={}",
                truncate(input), requestedCapabilities);
    }

    public void toolMatch(String function, FunctionType type, ModelTier tier, double score) {
        log.info("[PROOOPT][TOOL_MATCH] {} → {} {} (score={})",
                function, type, tier == null ? "-" : tier, String.format("%.3f", score));
    }

    public void orchestratorSummary(String traceId, long totalMs, int functions, int code,
                                    int local, int cloud, long tokens, double estCost) {
        log.info("[PROOOPT][ORCHESTRATOR][SUMMARY] trace={} total={}ms functions={} code={} local={} "
                        + "cloud={} tokens={} est_cost=${}",
                traceId, totalMs, functions, code, local, cloud, tokens, String.format("%.4f", estCost));
    }

    /** Rich summary with plan-mode, caching, timing breakdown, and the headline {@code cloud_calls}. */
    public void orchestratorSummary(RunMetrics m) {
        log.info("[PROOOPT][ORCHESTRATOR][SUMMARY] trace={} mode={} cached={} total={}ms "
                        + "plan_generation={}ms local_inference={}ms cloud_inference={}ms code_functions={}ms "
                        + "overhead={}ms functions_called={} code_calls={} local_calls={} cloud_calls={} "
                        + "dynamic_generated={} tokens_used={} est_cost_usd=${}",
                m.traceId(), m.mode(), m.cached(), m.totalMs(), m.planGenerationMs(), m.localInferenceMs(),
                m.cloudInferenceMs(), m.codeFunctionsMs(), m.overheadMs(), m.functionsCalled(),
                m.codeCalls(), m.localCalls(), m.cloudCalls(), m.dynamicGenerated(), m.tokensUsed(),
                String.format("%.4f", m.estCostUsd()));
    }

    // ------------------------------------------------------------------ plan cache

    public void planCacheMiss(PlanCacheStrategy strategy) {
        log.info("[PROOOPT][PLAN_CACHE][MISS] strategy={}", strategy);
    }

    public void planCacheStored(int steps, long ttlSeconds) {
        log.info("[PROOOPT][PLAN_CACHE][STORED] steps={} ttl={}s", steps, ttlSeconds);
    }

    public void planCacheHit(String key, double similarity) {
        log.info("[PROOOPT][PLAN_CACHE][HIT] key='{}' similarity={} → skipping Cloud LLM",
                key, String.format("%.2f", similarity));
    }

    public void planCacheInvalidated(int cleared, String reason) {
        log.info("[PROOOPT][PLAN_CACHE][INVALIDATED] cleared={} entries reason={}", cleared, reason);
    }

    // ------------------------------------------------------------------ dynamic prompt functions

    public void dynamicGapDetected(String traceId, String capability, double bestScore, double threshold) {
        log.warn("[PROOOPT][DYNAMIC][GAP_DETECTED] trace={} capability='{}' bestScore={} threshold={}",
                traceId, truncate(capability), String.format("%.2f", bestScore),
                String.format("%.2f", threshold));
    }

    public void dynamicGenerating(String traceId, ModelTier model, int remainingBudget) {
        log.info("[PROOOPT][DYNAMIC][GENERATING] trace={} model={} remainingBudget={}",
                traceId, model, remainingBudget);
    }

    public void dynamicRegistered(DynamicPromptFunction fn) {
        log.warn("[PROOOPT][DYNAMIC][REGISTERED]  ⚠  trace={} name={} prompt='{}' model={} scope=SESSION",
                fn.traceId(), fn.name(), truncate(fn.prompt()), fn.model());
    }

    public void dynamicBudgetExceeded(String traceId, int max, String capability) {
        log.warn("[PROOOPT][DYNAMIC][BUDGET_EXCEEDED] trace={} max={} reached capability='{}' skipped",
                traceId, max, truncate(capability));
    }

    public void dynamicGapSkipped(String traceId, String capability) {
        log.warn("[PROOOPT][DYNAMIC][GAP_SKIPPED] trace={} capability='{}' (allowDynamic=false)",
                traceId, truncate(capability));
    }

    public void dynamicPromptStart(String traceId, String function, ModelTier model) {
        log.info("[PROOOPT][PROMPT_FUNCTION][START] trace={} function={} [DYNAMIC] model={}",
                traceId, function, model);
    }

    public void dynamicPromptEnd(String traceId, String function, ModelTier model, long durationMs,
                                 Object output) {
        log.info("[PROOOPT][PROMPT_FUNCTION][END]   trace={} function={} [DYNAMIC] model={} duration={}ms "
                + "output={}", traceId, function, model, durationMs, truncate(output));
    }

    // ------------------------------------------------------------------ helpers

    private static String simpleClass(FunctionCall call) {
        return call.method() != null ? call.method().getDeclaringClass().getSimpleName() : "?";
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String formatMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append('=').append(truncate(e.getValue()));
            first = false;
        }
        return sb.append('}').toString();
    }

    private static String truncate(Object value) {
        if (value == null) {
            return "null";
        }
        String s = String.valueOf(value).replaceAll("\\s+", " ").trim();
        return s.length() > MAX_VALUE_LEN ? s.substring(0, MAX_VALUE_LEN) + "…" : s;
    }

    private static String describe(Throwable t) {
        return t.getClass().getSimpleName() + ": " + nullSafe(t.getMessage());
    }
}
