/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.model;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Immutable metadata for one function invocation, passed to {@code BaseOrchestrator} lifecycle hooks
 * and used to build audit lines. Captures who was called, with what, under which model authority, and
 * when — the raw material of an auditable governance trail.
 */
public final class FunctionCall {

    private final String name;
    private final String description;
    private final FunctionType type;
    private final ModelTier modelTier;
    private final Method method;
    private final Object[] args;
    private final Map<String, Object> variables;
    private final String traceId;
    private final long startTime;

    public FunctionCall(String name,
                        String description,
                        FunctionType type,
                        ModelTier modelTier,
                        Method method,
                        Object[] args,
                        Map<String, Object> variables,
                        String traceId,
                        long startTime) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.modelTier = modelTier;
        this.method = method;
        this.args = args;
        this.variables = variables;
        this.traceId = traceId;
        this.startTime = startTime;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public FunctionType type() {
        return type;
    }

    public ModelTier modelTier() {
        return modelTier;
    }

    public Method method() {
        return method;
    }

    public Object[] args() {
        return args;
    }

    /** Resolved {@code paramName -> value} map for prompt enrichment and logging. */
    public Map<String, Object> variables() {
        return variables;
    }

    public String traceId() {
        return traceId;
    }

    public long startTime() {
        return startTime;
    }

    /** Milliseconds elapsed since {@link #startTime()}. */
    public long elapsedMs() {
        return System.currentTimeMillis() - startTime;
    }
}
