/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.stdlib;

import io.github.argonizer.prooopt.annotation.CodeFunction;

import java.util.regex.Pattern;

/**
 * Deterministic validation predicates — exact, model-free guards for an orchestration's inputs.
 */
public final class ValidationFunctions {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");
    private static final Pattern NUMERIC = Pattern.compile("^[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?$");

    private ValidationFunctions() {
    }

    @CodeFunction(description = "Whether a string is a syntactically valid email address.",
            tags = {"validation", "validate", "email", "isEmail", "check"})
    public static boolean isEmail(String text) {
        return text != null && EMAIL.matcher(text.trim()).matches();
    }

    @CodeFunction(description = "Whether a string represents a number (integer or decimal).",
            tags = {"validation", "validate", "numeric", "isNumeric", "number", "check"})
    public static boolean isNumeric(String text) {
        return text != null && NUMERIC.matcher(text.trim()).matches();
    }

    @CodeFunction(description = "Whether a string is null, empty, or only whitespace.",
            tags = {"validation", "validate", "blank", "isBlank", "empty", "check"})
    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    @CodeFunction(description = "Whether a string fully matches a regular expression.",
            tags = {"validation", "validate", "regex", "matchesRegex", "pattern", "check"})
    public static boolean matchesRegex(String text, String regex) {
        return text != null && regex != null && Pattern.compile(regex).matcher(text).matches();
    }
}
