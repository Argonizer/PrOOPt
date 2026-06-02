/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.context;

import io.github.argonizer.prooopt.dynamic.DynamicFunctionCache;

import java.util.UUID;

/**
 * Per-thread tracing state shared by every function call in a single run. A {@code traceId} ties all
 * audit lines of one orchestration together so they can be grepped as a unit; a function counter and
 * run-start timestamp feed the orchestrator summary line.
 *
 * <p>Always call {@link #clear()} at the end of a run — these values live in {@link ThreadLocal}s and
 * would otherwise leak across tasks in pooled threads.
 */
public final class PrOOPtContext {

    private record State(String traceId, long runStart, int functionCount) {
    }

    private static final ThreadLocal<State> STATE = new ThreadLocal<>();

    private PrOOPtContext() {
    }

    /**
     * Returns the current trace id, lazily creating one (and recording the run start while zeroing the
     * function counter) on first access within a thread.
     */
    public static String getTraceId() {
        State s = STATE.get();
        if (s == null) {
            s = new State(newTraceId(), System.currentTimeMillis(), 0);
            STATE.set(s);
        }
        return s.traceId();
    }

    /**
     * Adopts an explicit trace id — used to propagate a parent run's id into worker threads under
     * parallel execution. Preserves an existing run-start timestamp if one is set.
     */
    public static void setTraceId(String traceId) {
        State s = STATE.get();
        long start = s == null ? System.currentTimeMillis() : s.runStart();
        int count = s == null ? 0 : s.functionCount();
        STATE.set(new State(traceId, start, count));
    }

    /** Increments and returns the number of functions executed in this run. */
    public static int incrementFunctionCount() {
        State s = STATE.get();
        if (s == null) {
            getTraceId();
            s = STATE.get();
        }
        State next = new State(s.traceId(), s.runStart(), s.functionCount() + 1);
        STATE.set(next);
        return next.functionCount();
    }

    /** The number of functions executed so far in this run. */
    public static int getFunctionCount() {
        State s = STATE.get();
        return s == null ? 0 : s.functionCount();
    }

    /** Milliseconds elapsed since the run started. */
    public static long elapsedMs() {
        State s = STATE.get();
        return s == null ? 0L : System.currentTimeMillis() - s.runStart();
    }

    /** Clears all thread-local state. Must be called at the end of every run. */
    public static void clear() {
        STATE.remove();
        DynamicFunctionCache.clear();  // discard all session-generated functions
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
