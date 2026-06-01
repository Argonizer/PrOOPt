/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.context;

import java.util.concurrent.Callable;

/**
 * Wraps {@link Runnable}/{@link Callable} with explicit trace-id propagation across thread
 * boundaries. Required for {@code ExecutorService}, {@code CompletableFuture}, and
 * {@code ForkJoinPool} usage, where the worker thread does not inherit the submitting thread's
 * {@link PrOOPtContext}.
 *
 * <p>The parent thread's trace id is captured at wrap time and restored on the worker thread before
 * execution; the worker's thread-local state is always cleared afterwards to avoid leaks in pooled
 * threads.
 */
public final class PrOOPtThreadPropagator {

    private PrOOPtThreadPropagator() {
    }

    /** Wraps a {@link Runnable}, capturing the current trace id and restoring it on the worker. */
    public static Runnable propagate(Runnable task) {
        String traceId = PrOOPtContext.getTraceId();
        return () -> {
            PrOOPtContext.setTraceId(traceId);
            try {
                task.run();
            } finally {
                PrOOPtContext.clear();
            }
        };
    }

    /** Wraps a {@link Callable}, capturing the current trace id and restoring it on the worker. */
    public static <T> Callable<T> propagate(Callable<T> task) {
        String traceId = PrOOPtContext.getTraceId();
        return () -> {
            PrOOPtContext.setTraceId(traceId);
            try {
                return task.call();
            } finally {
                PrOOPtContext.clear();
            }
        };
    }
}
