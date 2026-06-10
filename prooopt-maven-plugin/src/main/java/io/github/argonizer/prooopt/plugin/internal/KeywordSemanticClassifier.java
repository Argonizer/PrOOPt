/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, model-free {@link SemanticClassifier} used when no local model is available (CI,
 * offline builds) and as the default backing for {@code prooopt:validate}. Applies the same intent
 * keyword families as the APT Layer-1 heuristics, plus a small ambiguity rule that yields
 * {@code UNCERTAIN} for scoring/sentiment prompts paired with numeric types.
 */
public final class KeywordSemanticClassifier implements SemanticClassifier {

    private static final List<String> TEXTUAL =
            List.of("name", "word", "sentence", "text", "title", "label");
    private static final List<String> COUNTING =
            List.of("count", "number", "how many", "total", "sum");
    // Short verbs use word-boundary matching so "this" does not match "is", etc.
    private static final java.util.regex.Pattern BOOLEAN_VERB =
            java.util.regex.Pattern.compile(".*\\b(is|are|does|has)\\b.*");
    private static final List<String> LISTY =
            List.of("list of", "enumerate", "all the", "each of");
    private static final List<String> AMBIGUOUS =
            List.of("sentiment", "evaluate", "rate", "score", "assess");

    @Override
    public Map<Integer, ClassificationResult> classify(List<PromptMethod> batch) {
        Map<Integer, ClassificationResult> out = new LinkedHashMap<>();
        for (int i = 0; i < batch.size(); i++) {
            out.put(i + 1, classifyOne(batch.get(i)));
        }
        return out;
    }

    private ClassificationResult classifyOne(PromptMethod m) {
        String p = m.prompt() == null ? "" : m.prompt().toLowerCase();
        boolean list = isList(m.returnType());
        boolean map = isMap(m.returnType());
        boolean numeric = !list && !map && isNumeric(m.returnType());
        boolean bool = !list && !map && isBoolean(m.returnType());
        boolean text = !list && !map && isText(m.returnType());

        if (containsAny(p, TEXTUAL) && (numeric || bool)) {
            return ClassificationResult.INVALID;
        }
        if (containsAny(p, COUNTING) && (text || list || map || bool)) {
            return ClassificationResult.INVALID;
        }
        boolean booleanish = p.contains("true or false") || p.contains("yes or no")
                || BOOLEAN_VERB.matcher(p).matches();
        if (booleanish && (numeric || text)) {
            return ClassificationResult.INVALID;
        }
        if (containsAny(p, LISTY) && (text || numeric || bool)) {
            return ClassificationResult.INVALID;
        }
        if (containsAny(p, AMBIGUOUS) && numeric) {
            return ClassificationResult.UNCERTAIN;
        }
        return ClassificationResult.VALID;
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        return needles.stream().anyMatch(haystack::contains);
    }

    private static boolean isList(String rt) {
        return rt.contains("List") || rt.contains("Set") || rt.contains("Collection")
                || rt.contains("Queue") || rt.contains("Deque") || rt.contains("Stack")
                || rt.endsWith("[]");
    }

    private static boolean isMap(String rt) {
        return rt.contains("Map") || rt.contains("Hashtable");
    }

    private static boolean isNumeric(String rt) {
        return rt.contains("Integer") || rt.contains("Long") || rt.contains("Double")
                || rt.contains("Float") || rt.contains("Short") || rt.contains("Byte")
                || rt.contains("BigInteger") || rt.contains("BigDecimal")
                || rt.equals("int") || rt.equals("long") || rt.equals("double")
                || rt.equals("float") || rt.equals("short") || rt.equals("byte");
    }

    private static boolean isBoolean(String rt) {
        return rt.contains("Boolean") || rt.equals("boolean");
    }

    private static boolean isText(String rt) {
        return rt.equals("String") || rt.equals("java.lang.String") || rt.contains("CharSequence");
    }
}
