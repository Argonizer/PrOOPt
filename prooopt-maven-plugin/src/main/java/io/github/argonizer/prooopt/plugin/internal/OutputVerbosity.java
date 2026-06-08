/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin.internal;

import java.util.List;

/**
 * Internal classification of a method's expected output shape, used to drive the LOCAL-tier prose
 * advisory (Step 3). Never exposed in the public API — lives only inside {@code prooopt-maven-plugin}.
 */
public enum OutputVerbosity {
    /** A short scalar value (number, boolean, single word, date). */
    SCALAR,
    /** Long-form natural-language text. */
    PROSE,
    /** A structured collection/map/object bound from JSON. */
    JSON;

    private static final List<String> PROSE_KEYWORDS = List.of(
            "write", "draft", "explain", "describe", "summarize", "elaborate", "essay", "paragraph",
            "in detail", "step by step", "comprehensive", "generate a report", "tell me about",
            "give me a full", "provide an overview");

    /**
     * Classifies a method's output verbosity from its prompt and fully qualified return type. JSON
     * shapes win first (the type is authoritative), then prose keywords, otherwise scalar.
     */
    public static OutputVerbosity classify(String prompt, String returnType) {
        String rt = returnType == null ? "" : returnType;
        if (rt.contains("<") || rt.endsWith("[]")
                || rt.startsWith("java.util.") || rt.contains("List") || rt.contains("Map")
                || rt.contains("Set") || rt.contains("Collection")) {
            return JSON;
        }
        String p = prompt == null ? "" : prompt.toLowerCase();
        boolean prose = PROSE_KEYWORDS.stream().anyMatch(p::contains);
        if (prose && (rt.equals("java.lang.String") || rt.equals("String") || rt.isEmpty())) {
            return PROSE;
        }
        return SCALAR;
    }
}
