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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic string transforms, searches, formatting, and extraction.
 */
public final class StringFunctions {

    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d*\\.?\\d+");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern URL = Pattern.compile("https?://[\\w./?=&%#+-]+", Pattern.CASE_INSENSITIVE);

    private StringFunctions() {
    }

    @CodeFunction(description = "Convert text to upper case.",
            tags = {"string", "text", "uppercase", "upper", "case"})
    public static String toUpperCase(String text) {
        return text == null ? null : text.toUpperCase();
    }

    @CodeFunction(description = "Convert text to lower case.",
            tags = {"string", "text", "lowercase", "lower", "case"})
    public static String toLowerCase(String text) {
        return text == null ? null : text.toLowerCase();
    }

    @CodeFunction(description = "Trim leading and trailing whitespace from text.",
            tags = {"string", "text", "trim", "strip", "whitespace"})
    public static String trimText(String text) {
        return text == null ? null : text.strip();
    }

    @CodeFunction(description = "Reverse the characters in a string.",
            tags = {"string", "text", "reverse", "invert"})
    public static String reverseText(String text) {
        return text == null ? null : new StringBuilder(text).reverse().toString();
    }

    @CodeFunction(description = "Truncate text to a maximum length, appending an ellipsis if cut.",
            tags = {"string", "text", "truncate", "shorten", "limit"})
    public static String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength)) + "…";
    }

    @CodeFunction(description = "Whether text contains a substring.",
            tags = {"string", "text", "contains", "search", "includes"})
    public static boolean containsText(String text, String substring) {
        return text != null && substring != null && text.contains(substring);
    }

    @CodeFunction(description = "Whether text starts with a given prefix.",
            tags = {"string", "text", "startsWith", "prefix", "begins"})
    public static boolean startsWithText(String text, String prefix) {
        return text != null && prefix != null && text.startsWith(prefix);
    }

    @CodeFunction(description = "Count non-overlapping occurrences of a substring within text.",
            tags = {"string", "text", "count", "occurrences", "frequency"})
    public static int countOccurrences(String text, String substring) {
        if (text == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) >= 0) {
            count++;
            index += substring.length();
        }
        return count;
    }

    @CodeFunction(description = "Convert text to camelCase.",
            tags = {"string", "text", "camelCase", "format", "case"})
    public static String toCamelCase(String text) {
        String[] words = splitWords(text);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i].toLowerCase();
            if (w.isEmpty()) {
                continue;
            }
            sb.append(i == 0 ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1));
        }
        return sb.toString();
    }

    @CodeFunction(description = "Convert text to snake_case.",
            tags = {"string", "text", "snake_case", "snake", "format", "case"})
    public static String toSnakeCase(String text) {
        String[] words = splitWords(text);
        List<String> parts = new ArrayList<>();
        for (String w : words) {
            if (!w.isEmpty()) {
                parts.add(w.toLowerCase());
            }
        }
        return String.join("_", parts);
    }

    @CodeFunction(description = "Convert text to Title Case (capitalize each word).",
            tags = {"string", "text", "title case", "titlecase", "capitalize", "format"})
    public static String toTitleCase(String text) {
        String[] words = splitWords(text);
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    @CodeFunction(description = "Left-pad text with a character to a target length.",
            tags = {"string", "text", "pad", "padLeft", "format", "align"})
    public static String padLeft(String text, int length, char padChar) {
        String s = text == null ? "" : text;
        if (s.length() >= length) {
            return s;
        }
        return String.valueOf(padChar).repeat(length - s.length()) + s;
    }

    @CodeFunction(description = "Extract all numbers found in text, in order.",
            tags = {"string", "text", "extract", "numbers", "digits", "parse"})
    public static List<String> extractNumbers(String text) {
        return findAll(NUMBER, text);
    }

    @CodeFunction(description = "Extract all email addresses found in text.",
            tags = {"string", "text", "extract", "emails", "email", "parse"})
    public static List<String> extractEmails(String text) {
        return findAll(EMAIL, text);
    }

    @CodeFunction(description = "Extract all URLs (http/https) found in text.",
            tags = {"string", "text", "extract", "urls", "url", "links", "parse"})
    public static List<String> extractUrls(String text) {
        return findAll(URL, text);
    }

    private static List<String> findAll(Pattern pattern, String text) {
        List<String> matches = new ArrayList<>();
        if (text == null) {
            return matches;
        }
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            matches.add(m.group());
        }
        return matches;
    }

    private static String[] splitWords(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        // Split on separators and camelCase boundaries alike.
        String normalized = text
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("[_\\-]+", " ");
        return normalized.trim().split("\\s+");
    }
}
