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
}
