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
import io.github.argonizer.prooopt.context.PrOOPtThreadPropagator;
import io.github.argonizer.prooopt.dynamic.DynamicFunctionCache;
import io.github.argonizer.prooopt.dynamic.DynamicPromptFunction;
import io.github.argonizer.prooopt.exception.PrOOPtExecutionException;
import io.github.argonizer.prooopt.invoke.PromptCallEngine;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;
import io.github.argonizer.prooopt.model.FunctionCall;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.LogLevel;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.ToolDescriptor;
import io.github.argonizer.prooopt.registry.FunctionRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * DAG execution engine for {@link ExecutionPlan}s. Replaces the legacy wave-based {@code PlanExecutor}
 * as the sole, default execution model.
 *
 * <p>Sequential execution is a degenerate case of DAG — a linear chain where each step has at most one
 * dependency. When {@code parallel=false} and the plan is a linear chain, steps run synchronously on
 * the calling thread (zero extra threads spawned).
 *
 * <p>Design principles:
 * <ul>
 *   <li>DAG is the only executor — no conditional branching between "sequential" and "DAG".</li>
 *   <li>Every step is a {@link CompletableFuture}. Blocked steps consume zero threads while waiting.
 *   <li>Cross-stream dependencies are first-class: {@code dependsOn} may reference step IDs in any
 *       stream, and the result is injected into args automatically.</li>
 *   <li>Cycle detection happens at plan load time via {@link DagValidator}, never at execution time.</li>
 *   <li>Virtual threads for CLOUD calls; bounded platform threads for LOCAL calls.</li>
 *   <li>{@link PrOOPtContext#clear()} is called in every worker thread's {@code finally}.</li>
 * </ul>
 */
public class DagExecutor {

    private static final Logger audit = LogManager.getLogger(AuditLogger.AUDIT_LOGGER_NAME);

    private static final Pattern WHOLE_REF  = Pattern.compile("^\\$\\{?([A-Za-z0-9_.]+)}?$");
    private static final Pattern INLINE_REF = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)}");

    private final FunctionRegistry registry;
    private final PromptCallEngine promptEngine;
    private final AuditLogger auditLogger;
    private final ExecutorService cloudExecutor;
    private final ExecutorService localExecutor;
    private final boolean parallel;
    private final DagValidator validator = new DagValidator();

    public DagExecutor(FunctionRegistry registry,
                       PromptCallEngine promptEngine,
                       AuditLogger auditLogger,
                       ExecutorService cloudExecutor,
                       ExecutorService localExecutor,
                       boolean parallel) {
        this.registry = registry;
        this.promptEngine = promptEngine;
        this.auditLogger = auditLogger;
        this.cloudExecutor = cloudExecutor;
        this.localExecutor = localExecutor;
        this.parallel = parallel;
    }

    /**
     * Executes an {@link ExecutionPlan} and returns the value produced by the output step.
     *
     * @param plan          the validated plan to execute
     * @param input         the user input, seeded as {@code ${userInput}} / {@code ${input}}
     * @param hooks         optional orchestrator whose lifecycle hooks fire around each step
     * @param dagTimeoutMs  global wall-clock timeout in milliseconds; {@code -1} = no timeout
     */
    public Object execute(ExecutionPlan plan, Object input, BaseOrchestrator hooks, long dagTimeoutMs) {
        validator.validate(plan);

        String traceId = PrOOPtContext.getTraceId();
        Map<String, ExecutionStep> allSteps = plan.allSteps();
        int streamCount = plan.streams().size();

        audit.info("[PROOOPT][DAG][START] trace={} streams={} totalSteps={} parallel={}",
                traceId, streamCount, allSteps.size(), parallel);

        // Seed the result store with user input so ${userInput} / ${input} resolve in args.
        Map<String, Object> resultStore = new ConcurrentHashMap<>();
        if (input != null) {
            resultStore.put("userInput", input);
            resultStore.put("input", input);
        }

        long startMs = System.currentTimeMillis();

        Object result;
        if (!parallel && isLinearPlan(allSteps.values())) {
            result = executeLinear(plan, allSteps, resultStore, hooks, traceId);
        } else {
            result = executeAsync(plan, allSteps, resultStore, hooks, traceId, dagTimeoutMs, startMs);
        }

        long totalMs = System.currentTimeMillis() - startMs;
        DagSummary summary = computeSummary(traceId, plan, totalMs, false);
        emitComplete(summary);
        return result;
    }

    // ------------------------------------------------------------------ synchronous (linear) path

    /**
     * Runs a linear chain of steps synchronously on the calling thread — no thread spawning.
     * Topological order guarantees all deps are satisfied before each step executes.
     */
    private Object executeLinear(ExecutionPlan plan, Map<String, ExecutionStep> allSteps,
                                  Map<String, Object> resultStore, BaseOrchestrator hooks,
                                  String traceId) {
        List<ExecutionStep> ordered = topologicalOrder(allSteps);
        for (ExecutionStep step : ordered) {
            executeStep(step, resultStore, hooks, traceId);
        }
        return resolveOutput(plan.output(), allSteps, resultStore);
    }

    // ------------------------------------------------------------------ async (DAG) path

    private Object executeAsync(ExecutionPlan plan, Map<String, ExecutionStep> allSteps,
                                 Map<String, Object> resultStore, BaseOrchestrator hooks,
                                 String traceId, long dagTimeoutMs, long startMs) {
        // Pre-create one CompletableFuture per step.
        Map<String, CompletableFuture<Object>> futureStore = new ConcurrentHashMap<>();
        for (String stepId : allSteps.keySet()) {
            futureStore.put(stepId, new CompletableFuture<>());
        }

        // Schedule every step.
        for (ExecutionStep step : allSteps.values()) {
            scheduleStep(step, plan, allSteps, futureStore, resultStore, hooks, traceId);
        }

        // Wait for the output step's future.
        String outputStepId = resolveOutputStepId(plan.output(), allSteps, resultStore);
        CompletableFuture<Object> outputFuture = outputStepId != null
                ? futureStore.get(outputStepId)
                : CompletableFuture.completedFuture(resolveOutput(plan.output(), allSteps, resultStore));

        if (outputFuture == null) {
            outputFuture = CompletableFuture.completedFuture(
                    resolveOutput(plan.output(), allSteps, resultStore));
        }

        try {
            Object result;
            if (dagTimeoutMs > 0) {
                result = outputFuture.get(dagTimeoutMs, TimeUnit.MILLISECONDS);
            } else {
                result = outputFuture.get();
            }

            // After output future completes, also retrieve from resultStore if still null.
            if (result == null) {
                result = resolveOutput(plan.output(), allSteps, resultStore);
            }
            return result;

        } catch (TimeoutException e) {
            List<String> pending = futureStore.entrySet().stream()
                    .filter(en -> !en.getValue().isDone())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            futureStore.values().forEach(f -> f.cancel(true));
            audit.error("[PROOOPT][DAG][TIMEOUT] trace={} dagTimeoutMs={} completedSteps={} pendingSteps={}",
                    traceId, dagTimeoutMs, allSteps.size() - pending.size(), pending);
            throw new PrOOPtExecutionException("dag", "DagExecutor",
                    "DAG execution timed out after " + dagTimeoutMs + "ms; pending=" + pending, e);

        } catch (CancellationException e) {
            throw new PrOOPtExecutionException("dag", "DagExecutor", "DAG was cancelled", e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PrOOPtExecutionException pee) throw pee;
            throw new PrOOPtExecutionException("dag", "DagExecutor", cause.getMessage(), cause);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrOOPtExecutionException("dag", "DagExecutor", "Interrupted", e);
        }
    }

    private void scheduleStep(ExecutionStep step, ExecutionPlan plan,
                               Map<String, ExecutionStep> allSteps,
                               Map<String, CompletableFuture<Object>> futureStore,
                               Map<String, Object> resultStore, BaseOrchestrator hooks,
                               String traceId) {
        CompletableFuture<Object> stepFuture = futureStore.get(step.stepId());

        if (step.dependsOn().isEmpty()) {
            // No dependencies — execute immediately.
            ExecutorService executor = resolveExecutor(step);
            CompletableFuture.runAsync(PrOOPtThreadPropagator.propagate(() -> {
                try {
                    executeStep(step, resultStore, hooks, traceId);
                    stepFuture.complete(resultStore.get(canonical(step.assignTo())));
                } catch (Throwable t) {
                    stepFuture.completeExceptionally(t);
                }
            }), executor);
        } else {
            // Has dependencies — collect their futures.
            List<CompletableFuture<Object>> deps = step.dependsOn().stream()
                    .map(futureStore::get)
                    .collect(Collectors.toList());

            List<String> blocking = new ArrayList<>(step.dependsOn());
            audit.info("[PROOOPT][DAG][STEP_BLOCKED] trace={} step={} waiting={}",
                    traceId, step.stepId(), blocking);

            ExecutorService executor = resolveExecutor(step);
            CompletableFuture.allOf(deps.toArray(new CompletableFuture[0]))
                    .thenRunAsync(PrOOPtThreadPropagator.propagate(() -> {
                        audit.info("[PROOOPT][DAG][STEP_UNBLOCKED] trace={} step={} released_by={}",
                                traceId, step.stepId(), blocking.get(blocking.size() - 1));
                        try {
                            executeStep(step, resultStore, hooks, traceId);
                            stepFuture.complete(resultStore.get(canonical(step.assignTo())));
                        } catch (Throwable t) {
                            stepFuture.completeExceptionally(t);
                        }
                    }), executor)
                    .exceptionally(t -> {
                        stepFuture.completeExceptionally(t);
                        return null;
                    });
        }
    }

    // ------------------------------------------------------------------ step execution

    private void executeStep(ExecutionStep step, Map<String, Object> resultStore,
                              BaseOrchestrator hooks, String traceId) {
        long startMs = System.currentTimeMillis();
        String stream = step.streamId() != null ? step.streamId() : "?";

        audit.info("[PROOOPT][DAG][STEP_START] trace={} step={} stream={} type={} model={} blocking={}",
                traceId, step.stepId(), stream, step.type(), step.model(), step.dependsOn());

        // Resolve $-references in args from the result store.
        Map<String, Object> resolvedArgs = resolveArgs(step.args(), resultStore);

        // Set the trace ID in this worker thread.
        PrOOPtContext.setTraceId(traceId);

        try {
            Object result = invokeStep(step, resolvedArgs, hooks, traceId, startMs);

            // Store result under both the canonical assignTo key and the stepId.
            String assignKey = canonical(step.assignTo());
            if (assignKey != null && result != null) {
                resultStore.put(assignKey, result);
            }
            resultStore.put(step.stepId(), result != null ? result : "");

            PrOOPtContext.incrementFunctionCount();

            long duration = System.currentTimeMillis() - startMs;
            String outType = result != null ? result.getClass().getSimpleName() : "null";
            audit.info("[PROOOPT][DAG][STEP_COMPLETE] trace={} step={} stream={} duration={}ms output_type={}",
                    traceId, step.stepId(), stream, duration, outType);

        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startMs;
            audit.error("[PROOOPT][DAG][STEP_FAILED] trace={} step={} stream={} error='{}' duration={}ms",
                    traceId, step.stepId(), stream, t.getMessage(), duration);
            throw (t instanceof PrOOPtExecutionException pee) ? pee
                    : new PrOOPtExecutionException(step.stepId(), step.function(), t.getMessage(), t);
        } finally {
            PrOOPtContext.clear();
        }
    }

    private Object invokeStep(ExecutionStep step, Map<String, Object> resolvedArgs,
                               BaseOrchestrator hooks, String traceId, long startMs) {
        ToolDescriptor descriptor = registry.get(step.function());
        if (descriptor == null) {
            // Check for session-scoped dynamic prompt function.
            Optional<DynamicPromptFunction> dynamic = DynamicFunctionCache.find(step.function());
            if (dynamic.isPresent()) {
                return invokeDynamic(step, dynamic.get(), resolvedArgs, hooks, traceId, startMs);
            }
            throw new PrOOPtExecutionException(step.stepId(), step.function(),
                    "no such registered function", null);
        }

        Method method = descriptor.method();
        FunctionType type = descriptor.type();
        Object[] positionalArgs = orderArgs(descriptor, resolvedArgs);
        Map<String, Object> redactedInputs = Redaction.redactedInputs(method, positionalArgs);

        FunctionCall call = new FunctionCall(step.function(), descriptor.description(), type,
                descriptor.modelTier(), method, positionalArgs, resolvedArgs, traceId, startMs);
        if (hooks != null) hooks.beforeFunction(call);

        try {
            Object result;
            if (type == FunctionType.PROMPT) {
                PromptFunction annotation = method.getAnnotation(PromptFunction.class);
                LogLevel level = annotation.logLevel();
                auditLogger.promptStart(call, annotation.thinking(), redactedInputs, level);
                result = promptEngine.call(annotation, method.getReturnType(), resolvedArgs);
                auditLogger.promptEnd(call, Redaction.redactOutput(method, result), level);
            } else {
                CodeFunction annotation = method.getAnnotation(CodeFunction.class);
                LogLevel level = annotation != null ? annotation.logLevel() : LogLevel.FULL;
                auditLogger.codeStart(call, redactedInputs, level);
                result = registry.invokeNamed(step.function(), resolvedArgs);
                auditLogger.codeEnd(call, Redaction.redactOutput(method, result), level);
            }
            if (hooks != null) hooks.afterFunction(call, result);
            return result;

        } catch (Throwable t) {
            if (type == FunctionType.PROMPT) {
                auditLogger.promptError(call, t, LogLevel.FULL);
            } else {
                auditLogger.codeError(call, t, LogLevel.FULL);
            }
            if (hooks != null) hooks.onError(call, t);
            throw (t instanceof PrOOPtExecutionException pee) ? pee
                    : new PrOOPtExecutionException(step.stepId(), step.function(), t.getMessage(), t);
        }
    }

    private Object invokeDynamic(ExecutionStep step, DynamicPromptFunction fn,
                                  Map<String, Object> resolvedArgs, BaseOrchestrator hooks,
                                  String traceId, long startMs) {
        FunctionCall call = new FunctionCall(fn.name(), fn.description(), FunctionType.PROMPT,
                fn.model(), null, resolvedArgs.values().toArray(), resolvedArgs, traceId, startMs);
        if (hooks != null) hooks.beforeFunction(call);
        try {
            auditLogger.dynamicPromptStart(traceId, fn.name(), fn.model());
            Object result = promptEngine.call(fn.prompt(), fn.model(), 0, 0L,
                    fn.returnType(), resolvedArgs);
            auditLogger.dynamicPromptEnd(traceId, fn.name(), fn.model(),
                    System.currentTimeMillis() - startMs, result);
            if (hooks != null) hooks.afterFunction(call, result);
            return result;
        } catch (Throwable t) {
            auditLogger.promptError(call, t, LogLevel.FULL);
            if (hooks != null) hooks.onError(call, t);
            throw (t instanceof PrOOPtExecutionException pee) ? pee
                    : new PrOOPtExecutionException(step.stepId(), step.function(), t.getMessage(), t);
        }
    }

    // ------------------------------------------------------------------ executor selection

    private ExecutorService resolveExecutor(ExecutionStep step) {
        if (step.type() == FunctionType.CODE) {
            return localExecutor;
        }
        if (step.model() == ModelTier.LOCAL) {
            return localExecutor;
        }
        return cloudExecutor;
    }

    // ------------------------------------------------------------------ linear chain detection

    /**
     * Returns {@code true} if every step has at most one dependency and at most one successor —
     * i.e., the plan is a linear chain and can safely run synchronously with no branching.
     */
    private static boolean isLinearPlan(Collection<ExecutionStep> steps) {
        // A linear chain: each step has ≤1 dep, and each step is the successor of at most one other.
        Set<String> multiDepSuccessors = new HashSet<>();
        Map<String, Integer> successorCount = new HashMap<>();
        for (ExecutionStep step : steps) {
            if (step.dependsOn().size() > 1) return false;
            for (String dep : step.dependsOn()) {
                successorCount.merge(dep, 1, Integer::sum);
            }
        }
        for (int count : successorCount.values()) {
            if (count > 1) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ topological sort

    private static List<ExecutionStep> topologicalOrder(Map<String, ExecutionStep> allSteps) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (String id : allSteps.keySet()) {
            inDegree.put(id, 0);
            adj.put(id, new ArrayList<>());
        }
        for (ExecutionStep step : allSteps.values()) {
            for (String dep : step.dependsOn()) {
                adj.get(dep).add(step.stepId());
                inDegree.merge(step.stepId(), 1, Integer::sum);
            }
        }
        java.util.Queue<String> queue = new java.util.LinkedList<>();
        inDegree.forEach((id, deg) -> { if (deg == 0) queue.add(id); });
        List<ExecutionStep> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            String curr = queue.poll();
            ordered.add(allSteps.get(curr));
            for (String next : adj.get(curr)) {
                if (inDegree.merge(next, -1, Integer::sum) == 0) queue.add(next);
            }
        }
        return ordered;
    }

    // ------------------------------------------------------------------ critical path

    /**
     * Computes the longest path (by step count) in the DAG using DFS with memoization.
     */
    List<String> computeCriticalPath(Map<String, ExecutionStep> allSteps) {
        // Build adjacency: dep → successors.
        Map<String, List<String>> adj = new HashMap<>();
        for (String id : allSteps.keySet()) adj.put(id, new ArrayList<>());
        for (ExecutionStep step : allSteps.values()) {
            for (String dep : step.dependsOn()) {
                adj.get(dep).add(step.stepId());
            }
        }

        Map<String, List<String>> memo = new HashMap<>();
        List<String> longest = List.of();

        for (String id : allSteps.keySet()) {
            List<String> path = longestFrom(id, adj, memo);
            if (path.size() > longest.size()) longest = path;
        }
        return longest;
    }

    private List<String> longestFrom(String id, Map<String, List<String>> adj,
                                      Map<String, List<String>> memo) {
        if (memo.containsKey(id)) return memo.get(id);
        List<String> best = List.of(id);
        for (String next : adj.getOrDefault(id, List.of())) {
            List<String> sub = longestFrom(next, adj, memo);
            if (sub.size() + 1 > best.size()) {
                List<String> candidate = new ArrayList<>();
                candidate.add(id);
                candidate.addAll(sub);
                best = candidate;
            }
        }
        memo.put(id, best);
        return best;
    }

    // ------------------------------------------------------------------ output resolution

    private Object resolveOutput(String outputRef, Map<String, ExecutionStep> allSteps,
                                  Map<String, Object> resultStore) {
        if (outputRef == null) return null;
        // Direct stepId reference.
        if (resultStore.containsKey(outputRef)) return resultStore.get(outputRef);
        // $-prefixed variable or stepId.
        String key = canonical(outputRef);
        if (resultStore.containsKey(key)) return resultStore.get(key);
        // Find the step whose assignTo matches.
        for (ExecutionStep step : allSteps.values()) {
            if (outputRef.equals(step.stepId()) || key.equals(canonical(step.assignTo()))) {
                Object r = resultStore.get(canonical(step.assignTo()));
                if (r != null) return r;
                r = resultStore.get(step.stepId());
                if (r != null) return r;
            }
        }
        return null;
    }

    /** Returns the stepId of the step whose result is designated as output, or null if unresolvable. */
    private String resolveOutputStepId(String outputRef, Map<String, ExecutionStep> allSteps,
                                        Map<String, Object> resultStore) {
        if (outputRef == null) return null;
        if (allSteps.containsKey(outputRef)) return outputRef;
        String key = canonical(outputRef);
        // Find step whose stepId or assignTo matches.
        for (ExecutionStep step : allSteps.values()) {
            if (key.equals(step.stepId()) || key.equals(canonical(step.assignTo()))) {
                return step.stepId();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ arg resolution

    private Map<String, Object> resolveArgs(Map<String, Object> args, Map<String, Object> resultStore) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : args.entrySet()) {
            resolved.put(e.getKey(), resolveValue(e.getValue(), resultStore));
        }
        return resolved;
    }

    Object resolveValue(Object value, Map<String, Object> store) {
        if (!(value instanceof String s)) return value;
        Matcher whole = WHOLE_REF.matcher(s.trim());
        if (whole.matches()) {
            String name = whole.group(1);
            // Try canonical name first, then full stepId (e.g. "S2.3").
            if (store.containsKey(name)) return store.get(name);
            if (store.containsKey(s.trim().substring(s.trim().startsWith("${") ? 2 : 1,
                    s.trim().endsWith("}") ? s.trim().length() - 1 : s.trim().length()))) {
                return store.get(name);
            }
            return s; // unknown ref → treat as literal
        }
        if (s.contains("${")) {
            Matcher m = INLINE_REF.matcher(s);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                Object replacement = store.get(m.group(1));
                m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(replacement)));
            }
            m.appendTail(sb);
            return sb.toString();
        }
        return s;
    }

    private static Object[] orderArgs(ToolDescriptor descriptor, Map<String, Object> resolvedArgs) {
        List<Object> ordered = new ArrayList<>();
        for (String name : descriptor.paramSchema().keySet()) {
            ordered.add(resolvedArgs.get(name));
        }
        return ordered.toArray();
    }

    /** Strips {@code $} / {@code ${...}} decoration from a variable name. */
    static String canonical(String ref) {
        if (ref == null) return null;
        String r = ref.trim();
        if (r.startsWith("${") && r.endsWith("}")) return r.substring(2, r.length() - 1);
        if (r.startsWith("$")) return r.substring(1);
        return r;
    }

    // ------------------------------------------------------------------ summary

    private record DagSummary(
            String traceId,
            long totalDurationMs,
            int totalSteps,
            int codeSteps,
            int localSteps,
            int cloudSteps,
            List<String> criticalPath,
            boolean timedOut
    ) {}

    private DagSummary computeSummary(String traceId, ExecutionPlan plan, long totalMs, boolean timedOut) {
        Map<String, ExecutionStep> allSteps = plan.allSteps();
        int total = allSteps.size();
        int code = 0, local = 0, cloud = 0;
        for (ExecutionStep step : allSteps.values()) {
            ToolDescriptor desc = registry.get(step.function());
            FunctionType type = desc != null ? desc.type() : step.type();
            ModelTier tier = desc != null ? desc.modelTier() : step.model();
            if (type == FunctionType.CODE) code++;
            else if (tier == ModelTier.LOCAL) local++;
            else cloud++;
        }
        List<String> criticalPath = computeCriticalPath(allSteps);
        return new DagSummary(traceId, totalMs, total, code, local, cloud, criticalPath, timedOut);
    }

    private static void emitComplete(DagSummary s) {
        audit.info("[PROOOPT][DAG][COMPLETE] trace={} total={}ms streams=N/A steps={} " +
                        "code_steps={} local_steps={} cloud_steps={} critical_path={}",
                s.traceId(), s.totalDurationMs(), s.totalSteps(), s.codeSteps(), s.localSteps(),
                s.cloudSteps(), String.join("→", s.criticalPath()));
    }
}
