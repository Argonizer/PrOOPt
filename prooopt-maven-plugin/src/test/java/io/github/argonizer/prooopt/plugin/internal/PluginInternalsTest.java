/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.plugin.internal;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginInternalsTest {

    private static PromptMethod m(String prompt, String returnType, String tier) {
        return new PromptMethod("com.example.Svc", "fn", prompt, returnType, tier);
    }

    // -------------------------------------------------------------- OutputVerbosity

    @Test
    void verbosityClassifiesProseScalarJson() {
        assertEquals(OutputVerbosity.PROSE,
                OutputVerbosity.classify("Summarize this document", "java.lang.String"));
        assertEquals(OutputVerbosity.JSON,
                OutputVerbosity.classify("List the top 5", "java.util.List<java.lang.String>"));
        assertEquals(OutputVerbosity.SCALAR,
                OutputVerbosity.classify("Return the age", "java.lang.Integer"));
    }

    // -------------------------------------------------------------- KeywordSemanticClassifier

    @Test
    void keywordClassifierMatchesIntent() {
        KeywordSemanticClassifier c = new KeywordSemanticClassifier();
        Map<Integer, ClassificationResult> r = c.classify(List.of(
                m("Generate a random name", "java.lang.Integer", "LOCAL"),
                m("List the top 5 programming languages", "java.util.List<java.lang.String>", "LOCAL"),
                m("Count the words in the document", "java.lang.String", "LOCAL"),
                m("Evaluate the sentiment of this review", "java.lang.Integer", "LOCAL")));
        assertEquals(ClassificationResult.INVALID, r.get(1));
        assertEquals(ClassificationResult.VALID, r.get(2));
        assertEquals(ClassificationResult.INVALID, r.get(3));
        assertEquals(ClassificationResult.UNCERTAIN, r.get(4));
    }

    // -------------------------------------------------------------- MetaPrompt

    @Test
    void metaPromptBuildsResolvedSignaturesAndParses() {
        String mp = MetaPrompt.build(List.of(
                m("List the top 5 programming languages", "java.util.List<java.lang.String>", "LOCAL")));
        assertTrue(mp.contains("returnType=\"java.util.List<java.lang.String>\""), mp);
        assertEquals(8, MetaPrompt.maxTokens(2));

        Map<Integer, ClassificationResult> parsed = MetaPrompt.parse(
                "noise [{\"id\":1,\"result\":\"VALID\"},{\"id\":2,\"result\":\"INVALID\"}] trailing");
        assertEquals(ClassificationResult.VALID, parsed.get(1));
        assertEquals(ClassificationResult.INVALID, parsed.get(2));
    }

    // -------------------------------------------------------------- ValidationCache

    @Test
    void cacheKeyIsStableAndTypeSensitive() {
        String k1 = ValidationCache.key("p", "java.lang.String");
        String k2 = ValidationCache.key("p", "java.lang.String");
        String k3 = ValidationCache.key("p", "java.lang.Integer");
        assertEquals(k1, k2);
        assertNotEquals(k1, k3);
    }
}
