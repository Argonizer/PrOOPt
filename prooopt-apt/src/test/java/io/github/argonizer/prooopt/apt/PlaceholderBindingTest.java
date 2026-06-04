/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.apt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderBindingTest {

    @Test
    void extractsSingleBracePlaceholdersInOrder() {
        assertEquals(List.of("text", "language"),
                List.copyOf(PlaceholderBinding.placeholders("Translate '{text}' to {language}")));
    }

    @Test
    void ignoresPromptsWithoutPlaceholders() {
        assertTrue(PlaceholderBinding.placeholders("no placeholders here").isEmpty());
    }

    @Test
    void ignoresEmptyOrDottedBraces() {
        assertTrue(PlaceholderBinding.placeholders("braces {} and {a.b} are not params").isEmpty());
    }

    @Test
    void suggestsClosestParameterName() {
        assertEquals("language",
                PlaceholderBinding.suggest("lang", List.of("text", "language")).orElseThrow());
    }

    @Test
    void noSuggestionWhenNothingClose() {
        assertTrue(PlaceholderBinding.suggest("zzzzzzzz", List.of("text", "language")).isEmpty());
    }
}
