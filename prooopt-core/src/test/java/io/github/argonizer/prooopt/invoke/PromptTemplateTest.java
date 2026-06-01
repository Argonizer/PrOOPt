/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.invoke;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptTemplateTest {

    @Test
    void resolvesMultiplePlaceholders() {
        PromptTemplate t = PromptTemplate.compile("Extract the date from {text} for client {client}.");
        String out = t.resolve(Map.of("text", "a contract", "client", "Acme"));
        assertEquals("Extract the date from a contract for client Acme.", out);
    }

    @Test
    void missingVariableResolvesToEmpty() {
        PromptTemplate t = PromptTemplate.compile("Hello {name}!");
        assertEquals("Hello !", t.resolve(Map.of()));
    }

    @Test
    void templateWithNoPlaceholdersIsUnchanged() {
        PromptTemplate t = PromptTemplate.compile("static prompt");
        assertFalse(t.hasPlaceholders());
        assertEquals("static prompt", t.resolve(Map.of()));
    }

    @Test
    void segmentsAreOneMoreThanPlaceholders() {
        PromptTemplate t = PromptTemplate.compile("{a}{b}{c}");
        assertEquals(3, t.placeholders().size());
        assertEquals(4, t.segments().size());
    }

    @Test
    void matchesStringReplaceSemantics() {
        String raw = "Summarize {doc} in {n} sentences for {audience}.";
        Map<String, Object> vars = Map.of("doc", "the NDA", "n", 3, "audience", "executives");
        String expected = raw.replace("{doc}", "the NDA")
                .replace("{n}", "3")
                .replace("{audience}", "executives");
        assertEquals(expected, PromptTemplate.compile(raw).resolve(vars));
    }

    @Test
    void compiledResolveIsFasterThanRepeatedStringReplace() {
        String raw = "Analyze {a} with {b} and {c} producing {d}.";
        Map<String, Object> vars = Map.of("a", "x", "b", "y", "c", "z", "d", "w");
        PromptTemplate compiled = PromptTemplate.compile(raw);

        int iterations = 10_000;
        // Warm up both paths.
        for (int i = 0; i < 1_000; i++) {
            compiled.resolve(vars);
            naiveReplace(raw, vars);
        }

        long compiledStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            compiled.resolve(vars);
        }
        long compiledNs = System.nanoTime() - compiledStart;

        long replaceStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            naiveReplace(raw, vars);
        }
        long replaceNs = System.nanoTime() - replaceStart;

        assertTrue(compiledNs < replaceNs,
                "compiled resolve (" + compiledNs + "ns) should beat String.replace (" + replaceNs + "ns)");
    }

    private static String naiveReplace(String raw, Map<String, Object> vars) {
        String result = raw;
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return result;
    }
}
