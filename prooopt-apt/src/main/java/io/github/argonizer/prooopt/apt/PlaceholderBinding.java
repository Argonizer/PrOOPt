/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.apt;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and validates single-brace {@code {param}} placeholders in a prompt against a method's
 * parameter names. PrOOPt resolves placeholders from parameter names via reflection (the build is
 * compiled with {@code -parameters}), so every placeholder must match a parameter exactly.
 */
public final class PlaceholderBinding {

    // A {name} where name is a valid Java identifier. Does not match empty {} or {a.b}.
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)}");

    private PlaceholderBinding() {
    }

    /** All distinct placeholder names referenced in the prompt, in first-seen order. */
    public static Set<String> placeholders(String prompt) {
        Set<String> names = new LinkedHashSet<>();
        if (prompt == null) {
            return names;
        }
        Matcher m = PLACEHOLDER.matcher(prompt);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    /**
     * Returns the closest parameter name to {@code placeholder} for a "did you mean" hint, when one is
     * within a small edit distance; otherwise empty.
     */
    public static Optional<String> suggest(String placeholder, List<String> parameters) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        String lower = placeholder.toLowerCase();
        for (String p : parameters) {
            int d = levenshtein(lower, p.toLowerCase());
            // On a tie, prefer a candidate that shares a prefix with the placeholder
            // (e.g. "lang" → "language" beats an equidistant "text").
            boolean tieBreak = d == bestDist && best != null
                    && p.toLowerCase().startsWith(lower) && !best.toLowerCase().startsWith(lower);
            if (d < bestDist || tieBreak) {
                bestDist = d;
                best = p;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        int threshold = Math.max(2, Math.max(placeholder.length(), best.length()) / 2);
        return bestDist <= threshold ? Optional.of(best) : Optional.empty();
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length()];
    }
}
