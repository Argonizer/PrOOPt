/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.annotation.CodeFunction;
import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.audit.AuditLogger;
import io.github.argonizer.prooopt.audit.Redaction;
import io.github.argonizer.prooopt.context.PrOOPtContext;
import io.github.argonizer.prooopt.exception.PrOOPtExecutionException;
import io.github.argonizer.prooopt.invoke.PromptCallEngine;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;
import io.github.argonizer.prooopt.model.FunctionCall;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.LogLevel;
import io.github.argonizer.prooopt.model.ToolDescriptor;
import io.github.argonizer.prooopt.registry.FunctionRegistry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs an {@link ExecutionPlan} as a wave-scheduled dependency graph. Each step's wave is
 * {@code max(wave of its dependencies) + 1} (no-dependency steps are wave 0); waves run in order, and
 * the independent steps within a wave run in parallel on a bounded pool. Step results land in a shared
 * execution context keyed by {@code assignTo}; {@code $var} / {@code ${var}} references in later steps
 * resolve from it.
 *
 * <p>CODE steps execute through the {@link FunctionRegistry} (a cached {@code MethodHandle}); PROMPT
 * steps execute through the shared {@link PromptCallEngine}, so a planner can never escalate a
 * function beyond the tier its {@code @PromptFunction} annotation grants — the annotation, not the
 * plan, is the governance decision. The parent trace id is propagated into worker threads, and every
 * step contributes to the audit trail.
 */
public class PlanExecutor {

    private static final Pattern WHOLE_REF = Pattern.compile("^\\$\\{?([A-Za-z0-9_]+)}?$");
    private static final Pattern INLINE_REF = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    private final FunctionRegistry registry;
    private final PromptCallEngine promptEngine;
    private final AuditLogger audit;

    public PlanExecutor(FunctionRegistry registry, PromptCallEngine promptEngine, AuditLogger audit) {
        this.registry = registry;
        this.promptEngine = promptEngine;
        this.audit = audit;
    }

    /** Executes a plan sequentially (single-threaded). */
    public Object execute(ExecutionPlan plan, Object input) {
        return execute(plan, input, false, 1, null);
    }

    /**
     * Executes a plan.
     *
     * @param plan       the plan to run
     * @param input      the run input, seeded as {@code ${userInput}} / {@code ${input}}
     * @param parallel   whether independent steps within a wave run concurrently
     * @param maxThreads worker pool size for parallel waves ({@code <= 0} means available processors)
     * @param hooks      optional orchestrator whose lifecycle hooks fire around each step
     * @return the value bound to {@link ExecutionPlan#output()}
     */
    public Object execute(ExecutionPlan plan, Object input, boolean parallel, int maxThreads,
                          BaseOrchestrator hooks) {
        Map<String, Object> context = new ConcurrentHashMap<>();
        if (input != null) {
            context.put("userInput", input);
            context.put("input", input);
        }

        Map<Integer, ExecutionStep> byId = new HashMap<>();
        for (ExecutionStep step : plan.steps()) {
            byId.put(step.stepId(), step);
        }
        TreeMap<Integer, List<ExecutionStep>> waves = assignWaves(plan.steps(), byId);

        String parentTrace = PrOOPtContext.getTraceId();
        int threads = maxThreads <= 0 ? Runtime.getRuntime().availableProcessors() : maxThreads;
        ExecutorService pool = null;
        try {
            for (List<ExecutionStep> wave : waves.values()) {
                if (!parallel || wave.size() == 1) {
                    for (ExecutionStep step : wave) {
                        runStep(step, context, hooks);
                    }
                } else {
                    if (pool == null) {
                        pool = Executors.newFixedThreadPool(Math.min(threads, wave.size()), worker(parentTrace));
                    }
                    List<CompletableFuture<Void>> futures = new ArrayList<>(wave.size());
                    for (ExecutionStep step : wave) {
                        futures.add(CompletableFuture.runAsync(() -> {
                            PrOOPtContext.setTraceId(parentTrace);
                            try {
                                runStep(step, context, hooks);
                            } finally {
                                PrOOPtContext.clear();
                            }
                        }, pool));
                    }
                    joinAll(futures);
                }
            }
        } finally {
            if (pool != null) {
                pool.shutdown();
            }
        }
        String outputKey = canonical(plan.output());
        return outputKey == null ? null : context.get(outputKey);
    }

    // ------------------------------------------------------------------ wave scheduling

    /** Groups steps by wave; wave(step) = max(wave(dep)) + 1, with cycle detection. */
    static TreeMap<Integer, List<ExecutionStep>> assignWaves(List<ExecutionStep> steps,
                                                             Map<Integer, ExecutionStep> byId) {
        Map<Integer, Integer> waveOf = new HashMap<>();
        for (ExecutionStep step : steps) {
            waveOf.put(step.stepId(), waveFor(step.stepId(), byId, waveOf, new HashSet<>()));
        }
        TreeMap<Integer, List<ExecutionStep>> waves = new TreeMap<>();
        for (ExecutionStep step : steps) {
            waves.computeIfAbsent(waveOf.get(step.stepId()), k -> new ArrayList<>()).add(step);
        }
        return waves;
    }

    private static int waveFor(int stepId, Map<Integer, ExecutionStep> byId,
                               Map<Integer, Integer> memo, Set<Integer> visiting) {
        if (memo.containsKey(stepId)) {
            return memo.get(stepId);
        }
        if (!visiting.add(stepId)) {
            throw new PrOOPtExecutionException(stepId, byId.containsKey(stepId)
                    ? byId.get(stepId).function() : "?", "dependency cycle detected at step " + stepId, null);
        }
        ExecutionStep step = byId.get(stepId);
        int wave = 0;
        if (step != null) {
            for (Integer dep : step.dependsOn()) {
                wave = Math.max(wave, waveFor(dep, byId, memo, visiting) + 1);
            }
        }
        visiting.remove(stepId);
        memo.put(stepId, wave);
        return wave;
    }

    // ------------------------------------------------------------------ step execution

    private void runStep(ExecutionStep step, Map<String, Object> context, BaseOrchestrator hooks) {
        Map<String, Object> resolvedArgs = new LinkedHashMap<>();
        for (Map.Entry<String, Object> arg : step.args().entrySet()) {
            resolvedArgs.put(arg.getKey(), resolveValue(arg.getValue(), context));
        }

        ToolDescriptor descriptor = registry.get(step.function());
        if (descriptor == null) {
            throw new PrOOPtExecutionException(step.stepId(), step.function(),
                    "no such registered function", null);
        }
        Method method = descriptor.method();
        FunctionType type = descriptor.type();
        Object[] positionalArgs = orderArgs(descriptor, resolvedArgs);
        Map<String, Object> redactedInputs = Redaction.redactedInputs(method, positionalArgs);

        FunctionCall call = new FunctionCall(step.function(), descriptor.description(), type,
                descriptor.modelTier(), method, positionalArgs, resolvedArgs,
                PrOOPtContext.getTraceId(), System.currentTimeMillis());
        if (hooks != null) {
            hooks.beforeFunction(call);
        }

        try {
            Object result;
            if (type == FunctionType.PROMPT) {
                PromptFunction annotation = method.getAnnotation(PromptFunction.class);
                LogLevel level = annotation.logLevel();
                audit.promptStart(call, annotation.thinking(), redactedInputs, level);
                result = promptEngine.call(annotation, method.getReturnType(), resolvedArgs);
                PrOOPtContext.incrementFunctionCount();
                audit.promptEnd(call, Redaction.redactOutput(method, result), level);
            } else {
                CodeFunction annotation = method.getAnnotation(CodeFunction.class);
                LogLevel level = annotation != null ? annotation.logLevel() : LogLevel.FULL;
                audit.codeStart(call, redactedInputs, level);
                result = registry.invokeNamed(step.function(), resolvedArgs);
                PrOOPtContext.incrementFunctionCount();
                audit.codeEnd(call, Redaction.redactOutput(method, result), level);
            }

            String key = canonical(step.assignTo());
            // ConcurrentHashMap forbids null values; a null result simply leaves the key unbound.
            if (key != null && result != null) {
                context.put(key, result);
            }
            if (hooks != null) {
                hooks.afterFunction(call, result);
            }
        } catch (Throwable t) {
            if (type == FunctionType.PROMPT) {
                audit.promptError(call, t, LogLevel.FULL);
            } else {
                audit.codeError(call, t, LogLevel.FULL);
            }
            if (hooks != null) {
                hooks.onError(call, t);
            }
            throw (t instanceof PrOOPtExecutionException pee)
                    ? pee
                    : new PrOOPtExecutionException(step.stepId(), step.function(), t.getMessage(), t);
        }
    }

    private static Object[] orderArgs(ToolDescriptor descriptor, Map<String, Object> resolvedArgs) {
        List<Object> ordered = new ArrayList<>();
        for (String name : descriptor.paramSchema().keySet()) {
            ordered.add(resolvedArgs.get(name));
        }
        return ordered.toArray();
    }

    // ------------------------------------------------------------------ variable resolution

    /** Resolves a single argument value against the execution context. */
    Object resolveValue(Object value, Map<String, Object> context) {
        if (!(value instanceof String s)) {
            return value; // already a typed literal (number, boolean, list, map)
        }
        Matcher whole = WHOLE_REF.matcher(s.trim());
        if (whole.matches()) {
            String name = whole.group(1);
            return context.containsKey(name) ? context.get(name) : s; // unknown ref → treat as literal
        }
        if (s.contains("${")) {
            Matcher m = INLINE_REF.matcher(s);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                Object replacement = context.get(m.group(1));
                m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(replacement)));
            }
            m.appendTail(sb);
            return sb.toString();
        }
        return s;
    }

    /** Strips {@code $} / {@code ${...}} decoration from a variable name. */
    static String canonical(String ref) {
        if (ref == null) {
            return null;
        }
        String r = ref.trim();
        if (r.startsWith("${") && r.endsWith("}")) {
            return r.substring(2, r.length() - 1);
        }
        if (r.startsWith("$")) {
            return r.substring(1);
        }
        return r;
    }

    private static java.util.concurrent.ThreadFactory worker(String parentTrace) {
        return runnable -> {
            Thread t = new Thread(runnable, "prooopt-wave-" + parentTrace);
            t.setDaemon(true);
            return t;
        };
    }

    private static void joinAll(List<CompletableFuture<Void>> futures) {
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (java.util.concurrent.CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }
}
