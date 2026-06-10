/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.apt;

import java.util.List;
import java.util.Optional;

/**
 * Fast, LLM-free keyword heuristics that flag obvious prompt/return-type mismatches at {@code javac}
 * time (APT Layer 1). Pure logic with no dependency on the annotation-processing API so it can be
 * unit-tested directly. The processor maps a method's return {@link Kind} and feeds the prompt here.
 */
public final class PromptHeuristics {

    /** Coarse classification of a method's declared return type, sufficient for the heuristics. */
    public enum Kind { TEXT, INTEGRAL, DECIMAL, BOOLEAN, LIST, MAP, OTHER }

    /** Severity of a heuristic finding, mapped by the processor to a {@code Diagnostic.Kind}. */
    public enum Severity { ERROR, WARNING }

    /** A single heuristic finding: its severity and the indented message body (no location header). */
    public record Finding(Severity severity, String detail) {
    }

    private static final List<String> TEXTUAL =
            List.of("name", "word", "sentence", "text", "title", "label");
    private static final List<String> COUNTING =
            List.of("count", "number", "how many", "total", "sum");
    private static final List<String> BOOLEANISH =
            List.of("true or false", "yes or no", "is ", "are ", "does ", "has ");
    private static final List<String> LISTY =
            List.of("list of", "enumerate", "all the", "each of");
    private static final List<String> TEMPORAL =
            List.of("date", "time", "when");

    private static final List<String> PROSE = List.of(
            "write", "draft", "explain", "describe", "summarize", "elaborate", "essay", "paragraph",
            "in detail", "step by step", "comprehensive", "generate a report", "tell me about",
            "give me a full", "provide an overview");

    private PromptHeuristics() {
    }

    /** True when the prompt's wording suggests prose-level (long-form) output. Case-insensitive. */
    public static boolean isProse(String prompt) {
        String p = prompt == null ? "" : prompt.toLowerCase();
        return PROSE.stream().anyMatch(p::contains);
    }

    /**
     * Evaluates a prompt against a return {@link Kind}. Returns an ERROR finding for a clear
     * incompatibility, a WARNING for an ambiguous one, or empty when the pairing looks fine.
     *
     * @param prompt     the raw prompt text
     * @param kind       the coarse return-type classification
     * @param returnName the simple return-type name, for the message (e.g. {@code Integer})
     */
    public static Optional<Finding> evaluate(String prompt, Kind kind, String returnName) {
        String p = prompt == null ? "" : prompt.toLowerCase();

        if (containsAny(p, TEXTUAL) && (kind == Kind.INTEGRAL || kind == Kind.DECIMAL
                || kind == Kind.BOOLEAN)) {
            return error("""
                      Prompt suggests textual output ("%s") but return type is %s.
                      This binding will always fail at runtime.
                      Fix: change return type to String, or rewrite the prompt to produce a numeric value."""
                    .formatted(snippet(prompt), returnName));
        }
        if (containsAny(p, COUNTING) && (kind == Kind.TEXT || kind == Kind.LIST
                || kind == Kind.MAP || kind == Kind.BOOLEAN)) {
            return error("""
                      Prompt suggests a numeric count ("%s") but return type is %s.
                      This binding will always fail at runtime.
                      Fix: change return type to Integer or Long, or rewrite the prompt to produce text."""
                    .formatted(snippet(prompt), returnName));
        }
        if (containsAny(p, BOOLEANISH) && (kind == Kind.INTEGRAL || kind == Kind.DECIMAL
                || kind == Kind.TEXT)) {
            return error("""
                      Prompt suggests a yes/no (boolean) answer ("%s") but return type is %s.
                      This binding will always fail at runtime.
                      Fix: change return type to boolean/Boolean, or rewrite the prompt to produce %s."""
                    .formatted(snippet(prompt), returnName, returnName.toLowerCase()));
        }
        if (containsAny(p, LISTY) && (kind == Kind.TEXT || kind == Kind.INTEGRAL
                || kind == Kind.DECIMAL || kind == Kind.BOOLEAN)) {
            return error("""
                      Prompt suggests a list/enumeration ("%s") but return type is the scalar %s.
                      This binding will always fail at runtime.
                      Fix: change the return type to a List<...> (or Set<...>), or rewrite the prompt."""
                    .formatted(snippet(prompt), returnName));
        }
        if (containsAny(p, TEMPORAL) && (kind == Kind.INTEGRAL || kind == Kind.BOOLEAN)) {
            return Optional.of(new Finding(Severity.WARNING, """
                      Prompt mentions a date/time ("%s") but return type is %s.
                      If you intend a calendar value, prefer a java.time type (LocalDate, LocalDateTime).
                      Fix: change the return type to a java.time type, or ignore if a %s is intended."""
                    .formatted(snippet(prompt), returnName, returnName)));
        }
        return Optional.empty();
    }

    private static Optional<Finding> error(String detail) {
        return Optional.of(new Finding(Severity.ERROR, detail));
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        return needles.stream().anyMatch(haystack::contains);
    }

    private static String snippet(String prompt) {
        if (prompt == null) {
            return "";
        }
        String s = prompt.strip().replaceAll("\\s+", " ");
        return s.length() <= 60 ? s : s.substring(0, 60) + "...";
    }
}
