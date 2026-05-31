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
import io.github.argonizer.prooopt.model.ModelTier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an LLM-backed method. PrOOPt intercepts the call via AOP, enriches the {@link #prompt()}
 * template with the method's arguments, routes it to the configured {@link #model()} tier, and
 * autoboxes the raw model response into the method's declared return type.
 *
 * <p>The method body is optional pre/post-processing and should {@code return null} — the framework
 * supplies the real return value. Because interception relies on a runtime proxy, an annotated method
 * <strong>must be an instance method</strong>; declaring it {@code static} fails fast at startup with
 * a {@code PrOOPtConfigException} directing you to {@link CodeFunction} instead.
 *
 * <p>Placeholders in the prompt template use {@code {paramName}} syntax and are resolved from the
 * method's parameter names via reflection (compile with {@code -parameters}).
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PromptFunction {

    /**
     * The prompt template. {@code {paramName}} placeholders are replaced with the corresponding
     * argument values at call time.
     */
    String prompt();

    /** The model tier granted to this function. Defaults to {@link ModelTier#AUTO}. */
    ModelTier model() default ModelTier.AUTO;

    /** Human-readable capability description, used for semantic tool selection. */
    String description() default "";

    /** Enables the local model's thinking mode. Only meaningful for {@link ModelTier#LOCAL}. */
    boolean thinking() default false;

    /** How much this function contributes to the audit log. */
    LogLevel logLevel() default LogLevel.FULL;

    /**
     * Maximum number of times to retry autoboxing with a progressively stricter format instruction
     * when the model's response cannot be parsed into the return type. {@code 1} means one retry.
     */
    int maxRetries() default 1;

    /** Per-call timeout in milliseconds. */
    long timeoutMs() default 30_000L;

    /** Tags that sharpen semantic matching during tool selection. */
    String[] tags() default {};
}
