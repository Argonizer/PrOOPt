/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.invoke;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A prompt template compiled once at registration time so resolution at call time is a pure
 * {@code O(n)} string join — no regex, no repeated {@code String.replace} scans.
 *
 * <p>A template like {@code "Extract the date from {text} for {client}"} compiles into interleaved
 * static {@code segments} ({@code ["Extract the date from ", " for ", ""]}) and ordered
 * {@code placeholders} ({@code ["text", "client"]}). There is always exactly one more segment than
 * placeholder.
 */
public record PromptTemplate(String raw, List<String> segments, List<String> placeholders) {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)}");

    public PromptTemplate {
        segments = List.copyOf(segments);
        placeholders = List.copyOf(placeholders);
    }

    /** Parses {@code {paramName}} placeholders once into interleaved segments and names. */
    public static PromptTemplate compile(String template) {
        String safe = template == null ? "" : template;
        List<String> segments = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(safe);
        int last = 0;
        while (m.find()) {
            segments.add(safe.substring(last, m.start()));
            placeholders.add(m.group(1));
            last = m.end();
        }
        segments.add(safe.substring(last));
        return new PromptTemplate(safe, segments, placeholders);
    }

    /** Joins the static segments with the resolved variable values — no search, no replace. */
    public String resolve(Map<String, Object> variables) {
        StringBuilder sb = new StringBuilder(raw.length() + 32);
        for (int i = 0; i < placeholders.size(); i++) {
            sb.append(segments.get(i));
            Object value = variables == null ? null : variables.get(placeholders.get(i));
            sb.append(value == null ? "" : String.valueOf(value));
        }
        sb.append(segments.get(segments.size() - 1));
        return sb.toString();
    }

    /** Whether this template contains any {@code {placeholder}} at all. */
    public boolean hasPlaceholders() {
        return !placeholders.isEmpty();
    }
}
