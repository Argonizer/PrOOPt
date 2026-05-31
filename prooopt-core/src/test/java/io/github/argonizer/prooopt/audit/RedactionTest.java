/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.audit;

import io.github.argonizer.prooopt.annotation.SensitiveData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedactionTest {

    @SuppressWarnings("unused")
    static class Subject {
        public String process(@SensitiveData(label = "***SECRET***") String secret, String plain) {
            return "ok";
        }

        public String normalMethod(String a, String b) { return a + b; }
    }

    private Method method(String name, Class<?>... params) throws Exception {
        return Subject.class.getDeclaredMethod(name, params);
    }

    @Test
    void sensitivParameterIsRedacted() throws Exception {
        Method m = method("process", String.class, String.class);
        Map<String, Object> inputs = Redaction.redactedInputs(m, new Object[]{"my-secret", "visible"});
        assertEquals("***SECRET***", inputs.get("secret"));
        assertEquals("visible", inputs.get("plain"));
    }

    @Test
    void nonSensitiveParametersPassThrough() throws Exception {
        Method m = method("normalMethod", String.class, String.class);
        Map<String, Object> inputs = Redaction.redactedInputs(m, new Object[]{"hello", "world"});
        assertEquals("hello", inputs.get("a"));
        assertEquals("world", inputs.get("b"));
    }

    @Test
    void isSensitiveReturnFalseForNonAnnotatedType() throws Exception {
        assertFalse(Redaction.isSensitiveReturn(method("process", String.class, String.class)));
    }

    @Test
    void nullArgsArrayHandledGracefully() throws Exception {
        Method m = method("process", String.class, String.class);
        Map<String, Object> inputs = Redaction.redactedInputs(m, null);
        assertEquals("***SECRET***", inputs.get("secret")); // sensitive annotation wins even with null arg
        assertEquals(null, inputs.get("plain"));
    }
}
