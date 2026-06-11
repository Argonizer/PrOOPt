/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as an LLM-managed trait of a {@link Persona}.
 *
 * <p>The {@link #value()} is the natural-language description presented to the LLM
 * as the contract for this field. Rich, specific descriptions produce better
 * state transitions — the description <em>is</em> the rule.
 *
 * <p>All traits are indexed by default in {@code prooopt_persona_index} to support
 * {@code loadWhere(String)} queries. Set {@link #index()} to {@code false} only
 * for large text blobs (e.g. narrative memory) that should never be queried.
 *
 * <p>Fields annotated {@code @SensitiveData} (from prooopt-core) are excluded
 * from all LLM prompts regardless of this annotation.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Trait {

    /** Natural-language description used as the LLM contract for this field. */
    String value();

    /**
     * Whether this trait is written to {@code prooopt_persona_index}.
     * Defaults to {@code true}. Set to {@code false} for large text blobs
     * that degrade query performance without adding filter utility.
     */
    boolean index() default true;
}
