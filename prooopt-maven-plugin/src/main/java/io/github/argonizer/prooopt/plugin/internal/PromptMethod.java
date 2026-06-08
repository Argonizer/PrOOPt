/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin.internal;

/**
 * A discovered {@code @PromptFunction} method awaiting validation.
 *
 * @param className    fully qualified declaring class
 * @param methodName   simple method name
 * @param prompt       the prompt text
 * @param returnType   the resolved return type signature (generic where known, e.g. {@code List<String>})
 * @param modelTier    the declared model tier name (e.g. {@code LOCAL}, {@code CLOUD_ADVANCED})
 */
public record PromptMethod(String className, String methodName, String prompt,
                           String returnType, String modelTier) {

    /** {@code Class.method()} label used in messages and reports. */
    public String label() {
        return className + "." + methodName + "()";
    }
}
