/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the single batched meta-prompt sent to the semantic classifier and parses its JSON-array
 * response back into per-item {@link ClassificationResult}s. The meta-prompt instructs the model to
 * reason only about core semantic intent and to ignore output-format instructions embedded in each
 * prompt. Generic return signatures are passed through fully resolved (e.g. {@code List<String>}).
 */
public final class MetaPrompt {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MetaPrompt() {
    }

    /** Renders the meta-prompt for {@code batch}; items are numbered 1-based. */
    public static String build(List<PromptMethod> batch) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are a type compatibility classifier for a Java prompt engineering library.
                For each item below, classify whether the prompt's semantic intent is compatible
                with the declared Java return type.

                Ignore any output-format instructions within the prompt itself (e.g. "return as integer").
                Reason only about the core semantic intent of what the prompt is asking for.

                Respond ONLY as a JSON array. No explanation. No markdown. Example:
                [{"id":1,"result":"VALID"},{"id":2,"result":"INVALID"},{"id":3,"result":"UNCERTAIN"}]

                Items:
                """);
        for (int i = 0; i < batch.size(); i++) {
            PromptMethod m = batch.get(i);
            sb.append(i + 1).append(". prompt=\"").append(escape(m.prompt()))
                    .append("\", returnType=\"").append(m.returnType()).append("\"\n");
        }
        return sb.toString();
    }

    /** A safe {@code max_tokens} budget: four tokens per item keeps single-word results bounded. */
    public static int maxTokens(int batchSize) {
        return 4 * Math.max(1, batchSize);
    }

    /**
     * Parses the model's JSON-array response into a 1-based id → result map. Tolerates surrounding
     * prose by scanning for the first {@code [...]} array. Unknown/missing entries are omitted.
     */
    public static Map<Integer, ClassificationResult> parse(String response) {
        Map<Integer, ClassificationResult> out = new HashMap<>();
        if (response == null || response.isBlank()) {
            return out;
        }
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return out;
        }
        try {
            JsonNode array = MAPPER.readTree(response.substring(start, end + 1));
            for (JsonNode node : array) {
                if (!node.has("id") || !node.has("result")) {
                    continue;
                }
                int id = node.get("id").asInt();
                String result = node.get("result").asText().trim().toUpperCase();
                try {
                    out.put(id, ClassificationResult.valueOf(result));
                } catch (IllegalArgumentException ignored) {
                    // Unrecognized label — leave unmapped; engine treats absence conservatively.
                }
            }
        } catch (Exception ignored) {
            // Malformed JSON — return whatever parsed so far (possibly empty).
        }
        return out;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }
}
