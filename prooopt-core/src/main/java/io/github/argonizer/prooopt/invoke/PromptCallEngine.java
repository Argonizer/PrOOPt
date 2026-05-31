/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.invoke;

import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.autobox.PrOOPtAutoBoxer;
import io.github.argonizer.prooopt.exception.PrOOPtAutoBoxException;
import io.github.argonizer.prooopt.exception.PrOOPtException;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.ModelRouter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The single place model-backed calls are made. Given a prompt template and its resolved variables,
 * it enriches the prompt with a type-appropriate format instruction, routes it to the chosen
 * {@link ModelTier}, and autoboxes the response into the declared return type — retrying with a
 * stricter prompt on parse failure and enforcing a per-call timeout.
 *
 * <p>Both the AOP interceptor (direct calls) and the {@code PlanExecutor} (orchestrated prompt steps)
 * delegate here, so routing and autoboxing behave identically no matter how a function is reached.
 */
public class PromptCallEngine {

    private static final ExecutorService TIMEOUT_POOL = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable, "prooopt-model-call");
        t.setDaemon(true);
        return t;
    });

    private final ModelRouter router;
    private final PrOOPtAutoBoxer autoBoxer;

    public PromptCallEngine(ModelRouter router, PrOOPtAutoBoxer autoBoxer) {
        this.router = router;
        this.autoBoxer = autoBoxer;
    }

    public PrOOPtAutoBoxer autoBoxer() {
        return autoBoxer;
    }

    /** Convenience overload reading routing parameters from a {@link PromptFunction} annotation. */
    public Object call(PromptFunction annotation, Class<?> returnType, Map<String, Object> variables) {
        return call(annotation.prompt(), annotation.model(), annotation.maxRetries(),
                annotation.timeoutMs(), returnType, variables);
    }

    /** Resolves the template, routes, and autoboxes (with retry and timeout) into {@code returnType}. */
    public Object call(String promptTemplate, ModelTier tier, int maxRetries, long timeoutMs,
                       Class<?> returnType, Map<String, Object> variables) {
        String resolvedPrompt = resolveTemplate(promptTemplate, variables);
        int attempt = 0;
        PrOOPtAutoBoxException lastFailure = null;
        while (attempt <= Math.max(0, maxRetries)) {
            String instruction = attempt == 0
                    ? autoBoxer.buildFormatInstruction(returnType)
                    : autoBoxer.buildStricterFormatInstruction(returnType);
            String enriched = instruction.isEmpty() ? resolvedPrompt : resolvedPrompt + "\n\n" + instruction;

            String response = route(enriched, tier, timeoutMs);
            try {
                return autoBoxer.autobox(response, returnType);
            } catch (PrOOPtAutoBoxException e) {
                lastFailure = e;
                attempt++;
            }
        }
        throw lastFailure;
    }

    private String route(String prompt, ModelTier tier, long timeoutMs) {
        if (timeoutMs <= 0) {
            return router.route(prompt, tier);
        }
        CompletableFuture<String> future =
                CompletableFuture.supplyAsync(() -> router.route(prompt, tier), TIMEOUT_POOL);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new PrOOPtException("model call exceeded timeout of " + timeoutMs + "ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrOOPtException("model call interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new PrOOPtException("model call failed: " + cause.getMessage(), cause);
        }
    }

    /** Replaces each {@code {paramName}} placeholder with the corresponding variable's string form. */
    public static String resolveTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, Object> e : variables.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return result;
    }
}
