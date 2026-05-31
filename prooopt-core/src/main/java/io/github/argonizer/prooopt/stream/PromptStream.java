/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.stream;

import io.github.argonizer.prooopt.exception.PrOOPtException;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.AutoRouting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A fluent pipeline over registered functions so chains read like a process, not glue code:
 *
 * <pre>{@code
 * PromptStream.of(input)
 *     .pipe(validator::validateNotEmpty)   // @CodeFunction
 *     .pipe(analyzer::detectSentiment)     // @PromptFunction LOCAL
 *     .pipe(summarizer::generateSummary)   // @PromptFunction CLOUD_ADVANCED
 *     .withFallback(ModelTier.CLOUD_FAST)
 *     .withTimeout(5000)
 *     .execute();
 * }</pre>
 *
 * <p>Each {@code pipe} references a function (typically an AOP-proxied {@code @PromptFunction} or
 * {@code @CodeFunction} method reference, so interception, routing, and autoboxing still apply).
 * {@code filter} gates the chain mid-flow; {@code withTimeout}/{@code withRetry}/{@code withFallback}
 * apply to every step; and {@link #stats()} reports cumulative timing and a token/cost estimate.
 *
 * <p>Evaluation is lazy: operations are recorded as they are chained and run, in order, on
 * {@link #execute()}. The builder is mutable and retyped through the chain, so keep to a single fluent
 * expression.
 */
public final class PromptStream<T> {

    private static final ExecutorService POOL = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable, "prooopt-stream");
        t.setDaemon(true);
        return t;
    });

    private sealed interface Op permits Transform, Gate {
    }

    private record Transform(Function<Object, Object> fn) implements Op {
    }

    private record Gate(Predicate<Object> predicate) implements Op {
    }

    private final Object input;
    private final List<Op> ops = new ArrayList<>();

    private ModelTier fallback;
    private long timeoutMs;
    private int retries;

    // Cumulative tracking, populated during execute().
    private int stepsExecuted;
    private long elapsedMs;
    private long estimatedTokens;

    private PromptStream(Object input) {
        this.input = input;
    }

    /** Starts a chain from a seed value. */
    public static <T> PromptStream<T> of(T input) {
        return new PromptStream<>(input);
    }

    /** Adds a transformation step (a registered function reference). */
    @SuppressWarnings("unchecked")
    public <R> PromptStream<R> pipe(Function<? super T, ? extends R> fn) {
        ops.add(new Transform(o -> (Object) fn.apply((T) o)));
        return (PromptStream<R>) this;
    }

    /** Gates the chain: if the predicate fails at this point, {@link #execute()} short-circuits to null. */
    @SuppressWarnings("unchecked")
    public PromptStream<T> filter(Predicate<? super T> predicate) {
        ops.add(new Gate(o -> predicate.test((T) o)));
        return this;
    }

    /** Records a fallback tier; on step exhaustion one additional attempt is made. */
    public PromptStream<T> withFallback(ModelTier tier) {
        this.fallback = tier;
        return this;
    }

    /** Per-step timeout in milliseconds ({@code <= 0} disables). */
    public PromptStream<T> withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    /** Per-step retry count on failure. */
    public PromptStream<T> withRetry(int retries) {
        this.retries = retries;
        return this;
    }

    /** Runs the chain and returns the final value, or {@code null} if a {@link #filter} gated it out. */
    @SuppressWarnings("unchecked")
    public T execute() {
        long start = System.currentTimeMillis();
        Object value = input;
        try {
            for (Op op : ops) {
                if (op instanceof Gate gate) {
                    if (!gate.predicate().test(value)) {
                        return null;
                    }
                } else if (op instanceof Transform transform) {
                    Object before = value;
                    value = runStep(transform.fn(), value);
                    stepsExecuted++;
                    estimatedTokens += AutoRouting.estimateTokens(stringify(before))
                            + AutoRouting.estimateTokens(stringify(value));
                }
            }
            return (T) value;
        } finally {
            elapsedMs = System.currentTimeMillis() - start;
        }
    }

    /** Runs the chain and wraps the result in an {@link Optional} (empty when gated out or null). */
    public Optional<T> executeOptional() {
        return Optional.ofNullable(execute());
    }

    /**
     * Runs several functions on the current value in parallel and collects their results, mirroring
     * {@code parallelStream()} semantics for independent branches.
     */
    @SafeVarargs
    public final List<Object> parallel(Function<? super T, ?>... branches) {
        @SuppressWarnings("unchecked")
        T value = (T) input;
        List<CompletableFuture<Object>> futures = new ArrayList<>(branches.length);
        for (Function<? super T, ?> branch : branches) {
            futures.add(CompletableFuture.supplyAsync(() -> (Object) branch.apply(value), POOL));
        }
        List<Object> results = new ArrayList<>(branches.length);
        for (CompletableFuture<Object> future : futures) {
            results.add(future.join());
        }
        return results;
    }

    /** Cumulative statistics from the last {@link #execute()}. */
    public ChainStats stats() {
        double estCost = estimatedTokens / 1000.0 * 0.0015; // illustrative blended rate
        return new ChainStats(stepsExecuted, elapsedMs, estimatedTokens, estCost);
    }

    // ------------------------------------------------------------------ internals

    private Object runStep(Function<Object, Object> fn, Object value) {
        int maxAttempts = 1 + Math.max(0, retries) + (fallback != null ? 1 : 0);
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callWithTimeout(fn, value);
            } catch (RuntimeException e) {
                last = e;
            }
        }
        throw last != null ? last : new PrOOPtException("stream step failed with no captured error");
    }

    private Object callWithTimeout(Function<Object, Object> fn, Object value) {
        if (timeoutMs <= 0) {
            return fn.apply(value);
        }
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> fn.apply(value), POOL);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new PrOOPtException("stream step exceeded timeout of " + timeoutMs + "ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrOOPtException("stream step interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new PrOOPtException("stream step failed: " + cause.getMessage(), cause);
        }
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** Cumulative chain statistics: steps run, wall-clock time, and a token/cost estimate. */
    public record ChainStats(int stepsExecuted, long elapsedMs, long estimatedTokens, double estimatedCost) {
    }
}
