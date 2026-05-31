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
 * Distinguishes the two kinds of registered functions PrOOPt orchestrates.
 */
public enum FunctionType {

    /** An LLM-backed {@code @PromptFunction}: intercepted by AOP, prompt-enriched, autoboxed. */
    PROMPT,

    /** A deterministic {@code @CodeFunction}: plain Java executed directly, zero tokens. */
    CODE
}
