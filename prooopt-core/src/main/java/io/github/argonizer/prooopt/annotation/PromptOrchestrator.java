/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.annotation;

import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanCacheStrategy;
import io.github.argonizer.prooopt.model.PlanMode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the autonomous agent brain. The annotated type is always an instance bean; its
 * {@link #prompt()} becomes the system prompt handed to the execution model when building an
 * {@code ExecutionPlan} from the tools selected for a request.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PromptOrchestrator {

    /** The orchestrator's system prompt. */
    String prompt();

    /** The model tier used to build execution plans. Defaults to {@link ModelTier#LOCAL}. */
    ModelTier model() default ModelTier.LOCAL;

    /** Whether independent steps within a wave run in parallel. */
    boolean parallel() default false;

    /** Worker pool size when {@link #parallel()} is enabled; {@code -1} means available processors. */
    int maxThreads() default -1;

    /** Optional name for this orchestrator (audit/identification). */
    String name() default "";

    /** Orchestrator version string (audit/identification). */
    String version() default "0.1.0";

    // ------------------------------------------------------------------ dynamic prompt functions

    /**
     * When true, PrOOPt generates ephemeral prompt functions at runtime when no registered tool
     * matches a requested capability above the configured similarity threshold.
     *
     * <p>Generated functions are session-scoped — they are discarded at run end and never persist to
     * the static registry. Default: {@code false} (pure compile-time governance).
     */
    boolean allowDynamic() default false;

    /**
     * Maximum number of LLM-generated functions allowed per orchestration run. Once the budget is
     * exhausted, unmatched capabilities are skipped and logged as warnings rather than errors.
     * Default: {@code 3}.
     */
    int maxDynamicFunctions() default 3;

    /**
     * The model tier used to generate dynamic prompt function definitions. {@code CLOUD_FAST} is
     * recommended — generation is simple structured output, not deep reasoning.
     */
    ModelTier dynamicFunctionModel() default ModelTier.CLOUD_FAST;

    // ------------------------------------------------------------------ plan caching

    /**
     * Controls whether the execution plan is cached and reused ({@link PlanMode#STATIC}) or generated
     * fresh on every invocation ({@link PlanMode#DYNAMIC}). Default: {@code STATIC}.
     */
    PlanMode planMode() default PlanMode.STATIC;

    /** Cache key strategy, applied only when {@link #planMode()} is {@link PlanMode#STATIC}. */
    PlanCacheStrategy planCacheStrategy() default PlanCacheStrategy.SEMANTIC;

    /** Cached plan time-to-live in seconds; {@code -1} means never expire. */
    long planCacheTtl() default 3600;

    /** LRU eviction ceiling for the plan cache. */
    int planCacheSize() default 500;

    /** Minimum cosine similarity for a {@link PlanCacheStrategy#SEMANTIC} cache hit. */
    double planCacheSimilarityThreshold() default 0.85;

    // ------------------------------------------------------------------ DAG execution

    /**
     * Global wall-clock timeout in milliseconds for a complete DAG execution run.
     * If the DAG does not complete within this window, all pending futures are cancelled and a
     * {@link io.github.argonizer.prooopt.exception.PrOOPtExecutionException} is thrown.
     *
     * <p>Default: 120000 (2 minutes).
     * Set to {@code -1} to disable the global timeout (not recommended for production).
     */
    long dagTimeoutMs() default 120_000L;
}
