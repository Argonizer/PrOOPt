/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptExceptionsTest {

    @Test
    void typeBindingScalarMessageHasAllParts() {
        PromptTypeBindingException ex = PromptTypeBindingException.scalar(
                "UserService.getAge()", "What is the user's age?", "Integer",
                "\"The user is thirty-four years old.\"", "LLM returned prose text.");
        String m = ex.getMessage();
        assertTrue(m.contains("UserService.getAge()"));
        assertTrue(m.contains("What is the user's age?"));
        assertTrue(m.contains("Integer"));
        assertTrue(m.contains("thirty-four"));
        assertTrue(m.contains("Fix"));
    }

    @Test
    void typeBindingPojoMessageNamesFieldAndDeclaredFields() {
        PromptTypeBindingException ex = PromptTypeBindingException.pojo(
                "PassengerService.getPassengers()", "List the passengers", "List<Person>",
                "[{\"fullName\":\"Alice\"}]", "fullName", "Person",
                "name (String), age (Integer), email (String)");
        assertTrue(ex.getMessage().contains("fullName"));
        assertTrue(ex.getMessage().contains("name (String), age (Integer), email (String)"));
    }

    @Test
    void placeholderMessageNamesMissingPlaceholder() {
        PromptPlaceholderException ex = PromptPlaceholderException.missing(
                "TranslationService.translate()", "Translate '{text}' to {language}", "language",
                "text=\"Hello world\"", "translate(String text, String language)", "language=null");
        assertTrue(ex.getMessage().contains("{language}"));
        assertTrue(ex.getMessage().contains("Fix"));
    }

    @Test
    void descriptorResolutionMessageNamesDescriptorClassAndProcessorPathFix() {
        GenericTypeResolutionException ex = GenericTypeResolutionException.descriptorNotFound(
                "getItems()", "ReportService", "ReportServicePromptDescriptor");
        assertTrue(ex.getMessage().contains("ReportServicePromptDescriptor"));
        assertTrue(ex.getMessage().contains("annotationProcessorPath"));
    }

    @Test
    void pojoIntrospectionInheritedMessageStatesInheritedNotIntrospected() {
        PojoIntrospectionException ex = PojoIntrospectionException.inheritedFields(
                "PassengerService.getPassengers()", "com.example.Person", "com.example.BaseEntity",
                "id, createdAt");
        assertTrue(ex.getMessage().contains("only fields declared directly"));
        assertTrue(ex.getMessage().contains("not visible"));
    }

    @Test
    void modelInvocationWrapsCauseAsInitCause() {
        RuntimeException ort = new RuntimeException("OrtException: model not found");
        ModelInvocationException ex = ModelInvocationException.local(
                "UserService.getName()", "Generate a random first name",
                "ONNX Runtime session failed to load model.", ort);
        assertSame(ort, ex.getCause());
        assertTrue(ex.getMessage().contains("LOCAL"));
    }

    @Test
    void descriptorNotFoundMessageIncludesExpectedClass() {
        PromptDescriptorNotFoundException ex = PromptDescriptorNotFoundException.forInterface(
                "com.example.UserService", "com.example.UserServicePromptDescriptor");
        assertTrue(ex.getMessage().contains("com.example.UserServicePromptDescriptor"));
        assertTrue(ex.getMessage().contains("annotationProcessorPaths"));
    }

    @Test
    void everyPromptExceptionExtendsPrOOPtExceptionAndIsFinal() {
        List<Class<? extends PrOOPtException>> types = List.of(
                PromptTypeBindingException.class, PromptPlaceholderException.class,
                PromptSemanticValidationException.class, PromptVerbosityWarningException.class,
                GenericTypeResolutionException.class, PojoIntrospectionException.class,
                ModelInvocationException.class, PromptDescriptorNotFoundException.class);
        for (Class<?> t : types) {
            assertTrue(PrOOPtException.class.isAssignableFrom(t), t + " must extend PrOOPtException");
            assertTrue(Modifier.isFinal(t.getModifiers()), t + " must be final");
        }
    }

    @Test
    void promptIsTruncatedToOneHundredTwentyChars() {
        String longPrompt = "x".repeat(200);
        PromptVerbosityWarningException ex = new PromptVerbosityWarningException(
                AuditErrorLogProbe.truncate(longPrompt));
        assertEquals(123, ex.getMessage().length()); // 120 chars + "..."
    }

    /** Exercises the package-private truncation helper used across exception factories. */
    static final class AuditErrorLogProbe {
        static String truncate(String prompt) {
            return AuditErrorLog.truncatePrompt(prompt);
        }
    }
}
