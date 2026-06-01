/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.model;

/**
 * Controls whether an orchestrator caches and reuses its execution plan or rebuilds one per call.
 */
public enum PlanMode {

    /**
     * Generate the execution plan once on the first call and cache it. Subsequent executions with
     * semantically similar input reuse the cached plan — the Cloud LLM plan-generation call is
     * skipped entirely. The cached artifact is a PLAN TEMPLATE (variable placeholders retained);
     * {@code PlanInstantiator} binds live input values at each execution.
     *
     * <p>Best for: batch document processing, repetitive enterprise workflows, high-throughput
     * pipelines where input structure is predictable. Cloud LLM plan-generation calls: 1 total.
     */
    STATIC,

    /**
     * Generate a fresh execution plan on every invocation. The plan cache is never consulted or
     * written.
     *
     * <p>Best for: conversational agents, multi-turn workflows, inputs whose structure genuinely
     * varies. Cloud LLM plan-generation calls: 1 per execution.
     */
    DYNAMIC
}
