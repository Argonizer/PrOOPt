/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialResolverTest {

    private static final String PROP = "prooopt.test.credential";
    private final CredentialResolver resolver = new CredentialResolver();

    @AfterEach
    void cleanup() {
        System.clearProperty(PROP);
    }

    @Test
    void literalValueReturnedUnchanged() {
        assertEquals("sk-literal-key", resolver.resolve("sk-literal-key"));
    }

    @Test
    void nullInputReturnsNull() {
        assertNull(resolver.resolve(null));
    }

    @Test
    void placeholderResolvesFromSystemProperty() {
        System.setProperty(PROP, "from-sysprop");
        assertEquals("from-sysprop", resolver.resolve("${" + PROP + "}"));
    }

    @Test
    void placeholderWithDefaultFallsBackWhenPropertyAbsent() {
        assertEquals("fallback-value", resolver.resolve("${" + PROP + ":fallback-value}"));
    }

    @Test
    void placeholderWithNoDefaultReturnsNullWhenAbsent() {
        assertNull(resolver.resolve("${" + PROP + "}"));
    }

    @Test
    void isPlaceholderTrueForBracketForm() {
        assertTrue(resolver.isPlaceholder("${MY_VAR}"));
    }

    @Test
    void isPlaceholderFalseForLiteral() {
        assertFalse(resolver.isPlaceholder("sk-literal"));
    }

    @Test
    void isPlaceholderFalseForNull() {
        assertFalse(resolver.isPlaceholder(null));
    }

    @Test
    void systemPropertyTakesPrecedenceOverDefault() {
        System.setProperty(PROP, "sysprop-wins");
        assertEquals("sysprop-wins", resolver.resolve("${" + PROP + ":would-not-use-this}"));
    }
}
