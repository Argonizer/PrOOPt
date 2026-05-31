/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.annotation;

import io.github.argonizer.prooopt.model.LogLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a deterministic Java method — the {@code @CodeFunction} executes directly and consumes zero
 * tokens. It is registered as a tool alongside {@link PromptFunction}s and competes for selection on
 * semantic relevance, so an orchestrator can reach for exact arithmetic or validation instead of
 * asking a model to guess.
 *
 * <p>Unlike {@link PromptFunction}, a {@code @CodeFunction} <strong>may be {@code static}</strong>
 * (preferred for pure functions; the registry invokes statics through a {@code MethodHandle} with no
 * proxy). Declare it as an instance method when it needs injected dependencies.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CodeFunction {

    /** Required human-readable description of what this function does; drives semantic selection. */
    String description();

    /** How much this function contributes to the audit log. */
    LogLevel logLevel() default LogLevel.FULL;

    /** Tags that sharpen semantic matching during tool selection. */
    String[] tags() default {};
}
