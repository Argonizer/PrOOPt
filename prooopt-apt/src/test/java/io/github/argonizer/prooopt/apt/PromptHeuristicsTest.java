/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.apt;

import io.github.argonizer.prooopt.apt.PromptHeuristics.Kind;
import io.github.argonizer.prooopt.apt.PromptHeuristics.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptHeuristicsTest {

    @Test
    void textualPromptWithNumericTypeIsError() {
        var f = PromptHeuristics.evaluate("Generate a random name", Kind.INTEGRAL, "Integer");
        assertTrue(f.isPresent());
        assertEquals(Severity.ERROR, f.get().severity());
        assertTrue(f.get().detail().contains("Integer"));
    }

    @Test
    void countingPromptWithTextTypeIsError() {
        var f = PromptHeuristics.evaluate("Count the words", Kind.TEXT, "String");
        assertTrue(f.isPresent());
        assertEquals(Severity.ERROR, f.get().severity());
    }

    @Test
    void booleanQuestionWithNumericTypeIsError() {
        var f = PromptHeuristics.evaluate("Is the account active?", Kind.INTEGRAL, "Integer");
        assertTrue(f.isPresent());
        assertEquals(Severity.ERROR, f.get().severity());
    }

    @Test
    void listPromptWithScalarTypeIsError() {
        var f = PromptHeuristics.evaluate("List of colors", Kind.TEXT, "String");
        assertTrue(f.isPresent());
        assertEquals(Severity.ERROR, f.get().severity());
    }

    @Test
    void datePromptWithIntegerIsWarning() {
        var f = PromptHeuristics.evaluate("Provide the date of the event", Kind.INTEGRAL, "Integer");
        assertTrue(f.isPresent());
        assertEquals(Severity.WARNING, f.get().severity());
    }

    @Test
    void neutralPromptHasNoFinding() {
        assertTrue(PromptHeuristics.evaluate("Return a value for the field",
                Kind.OTHER, "Foo").isEmpty());
    }

    @Test
    void proseKeywordsAreDetected() {
        assertTrue(PromptHeuristics.isProse("Summarize this document"));
        assertTrue(PromptHeuristics.isProse("Explain in detail how it works"));
        assertTrue(PromptHeuristics.isProse("WRITE a short essay"));
    }

    @Test
    void nonProseIsNotFlagged() {
        assertTrue(!PromptHeuristics.isProse("Return the age as a number"));
    }
}
