/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.exception;

/**
 * Thrown by {@code PrOOPtAutoBoxer} when an LLM response cannot be coerced into the declared return
 * type — a scalar arrived as prose, a JSON field does not match a POJO, or a map value is the wrong
 * type. Every message names the method, prompt, expected type, actual output, and a {@code Fix:}.
 *
 * <p>Use the {@code scalar}, {@code pojo}, and {@code map} factory methods to build messages in the
 * canonical PrOOPt format rather than hand-assembling strings at the call site.
 */
public final class PromptTypeBindingException extends PrOOPtException {

    public PromptTypeBindingException(String message) {
        super(message);
        AuditErrorLog.record("PromptTypeBindingException", message);
    }

    public PromptTypeBindingException(String message, Throwable cause) {
        super(message, cause);
        AuditErrorLog.record("PromptTypeBindingException", message);
    }

    /** Scalar binding failure: the model returned prose where a typed scalar was required. */
    public static PromptTypeBindingException scalar(
            String method, String prompt, String expectedType, String llmOutput, String cause) {
        return new PromptTypeBindingException("""
                [PrOOPt] PromptTypeBindingException in %s
                  Prompt    : "%s"
                  Expected  : %s
                  LLM output: %s
                  Cause     : %s
                  Fix       : Rewrite the prompt to constrain output. Example:
                              "Return the value as a plain %s only. No text."
                              If the LLM consistently returns prose for this prompt,
                              consider switching to ModelTier.CLOUD_ADVANCED for better instruction-following.\
                """.formatted(method, AuditErrorLog.truncatePrompt(prompt), expectedType,
                llmOutput, cause, expectedType));
    }

    /** POJO deserialization failure: a JSON field does not match any declared field on the POJO. */
    public static PromptTypeBindingException pojo(
            String method, String prompt, String expectedType, String llmOutput,
            String mismatchedField, String pojoSimpleName, String declaredFields) {
        return new PromptTypeBindingException("""
                [PrOOPt] PromptTypeBindingException in %s
                  Prompt    : "%s"
                  Expected  : %s
                  LLM output: %s
                  Cause     : JSON field '%s' does not match any declared field in %s.
                              %s declares: %s
                  Fix       : Ensure %s's field names match the LLM's output keys, or
                              annotate %s fields with @JsonProperty to map alternative names.
                              Example: @JsonProperty("%s") private String name;\
                """.formatted(method, AuditErrorLog.truncatePrompt(prompt), expectedType, llmOutput,
                mismatchedField, pojoSimpleName, pojoSimpleName, declaredFields,
                pojoSimpleName, pojoSimpleName, mismatchedField));
    }

    /** Map deserialization failure: a map key or value cannot be coerced to the declared type. */
    public static PromptTypeBindingException map(
            String method, String prompt, String expectedType, String llmOutput, String cause) {
        return new PromptTypeBindingException("""
                [PrOOPt] PromptTypeBindingException in %s
                  Prompt    : "%s"
                  Expected  : %s
                  LLM output: %s
                  Cause     : %s
                  Fix       : Rewrite the prompt to enforce correctly typed values.
                              Example: "Extract config pairs. Values must be integers only."\
                """.formatted(method, AuditErrorLog.truncatePrompt(prompt), expectedType,
                llmOutput, cause));
    }
}
