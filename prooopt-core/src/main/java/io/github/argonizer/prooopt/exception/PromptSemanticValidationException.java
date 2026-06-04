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
 * Thrown by the {@code prooopt:validate} Maven goal when the semantic validator classifies a
 * {@code @PromptFunction} method as {@code INVALID}, or as {@code UNCERTAIN} under a {@code FAIL}
 * uncertainty policy. Carries the canonical {@code BUILD FAILURE} message format.
 */
public final class PromptSemanticValidationException extends PrOOPtException {

    public PromptSemanticValidationException(String message) {
        super(message);
        AuditErrorLog.record("PromptSemanticValidationException", message);
    }

    public PromptSemanticValidationException(String message, Throwable cause) {
        super(message, cause);
        AuditErrorLog.record("PromptSemanticValidationException", message);
    }

    /** An {@code INVALID} classification: the prompt's core intent is not bindable to the type. */
    public static PromptSemanticValidationException invalid(
            String method, String prompt, String returns, String reason, String fix) {
        return new PromptSemanticValidationException("""
                [PrOOPt] PromptSemanticValidationException — BUILD FAILURE
                  Method  : %s
                  Prompt  : "%s"
                  Returns : %s
                  Result  : INVALID
                  Reason  : %s
                  Fix     : %s\
                """.formatted(method, AuditErrorLog.truncatePrompt(prompt), returns, reason, fix));
    }

    /** An {@code UNCERTAIN} classification under {@code uncertaintyPolicy=FAIL}. */
    public static PromptSemanticValidationException uncertain(
            String method, String prompt, String returns, String reason, String fix) {
        return new PromptSemanticValidationException("""
                [PrOOPt] PromptSemanticValidationException — BUILD FAILURE
                  Method  : %s
                  Prompt  : "%s"
                  Returns : %s
                  Result  : UNCERTAIN
                  Reason  : %s
                  Fix     : %s
                            Or set <uncertaintyPolicy>WARN</uncertaintyPolicy> in the plugin config
                            to downgrade this to a warning if the ambiguity is intentional.\
                """.formatted(method, AuditErrorLog.truncatePrompt(prompt), returns, reason, fix));
    }
}
