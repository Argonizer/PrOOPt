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
 * Thrown at proxy invocation time when a {@code {placeholder}} in a prompt has no corresponding
 * non-null method argument. This is the runtime guard that complements the compile-time placeholder
 * check performed by the {@code prooopt-apt} annotation processor.
 *
 * <p>PrOOPt uses single-brace {@code {param}} placeholders, resolved from method parameter names.
 */
public final class PromptPlaceholderException extends PrOOPtException {

    public PromptPlaceholderException(String message) {
        super(message);
        AuditErrorLog.record("PromptPlaceholderException", message);
    }

    public PromptPlaceholderException(String message, Throwable cause) {
        super(message, cause);
        AuditErrorLog.record("PromptPlaceholderException", message);
    }

    /** Builds the canonical message for a placeholder that received no bound (or a null) argument. */
    public static PromptPlaceholderException missing(
            String method, String prompt, String missingPlaceholder,
            String provided, String methodSignature, String received) {
        return new PromptPlaceholderException("""
                [PrOOPt] PromptPlaceholderException in %s
                  Prompt      : "%s"
                  Missing     : {%s}
                  Provided    : %s
                  Cause       : No method argument was bound to placeholder '{%s}'.
                                Method signature: %s
                                Received: %s
                  Fix         : Ensure all arguments are non-null when invoking this method.
                                PrOOPt does not substitute null arguments into prompt placeholders.\
                """.formatted(method, AuditErrorLog.truncatePrompt(prompt), missingPlaceholder,
                provided, missingPlaceholder, methodSignature, received));
    }
}
