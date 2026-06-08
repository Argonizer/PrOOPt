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
 * Advisory-only condition: prose-level output was detected with {@code ModelTier.LOCAL}. This type is
 * <strong>never thrown</strong> to fail a build — the {@code prooopt:validate} goal logs its message
 * as a structured {@code WARNING} and writes it into the validation report. It exists as a named,
 * formattable condition so the warning text has a single canonical source.
 */
public final class PromptVerbosityWarningException extends PrOOPtException {

    public PromptVerbosityWarningException(String message) {
        super(message);
        AuditErrorLog.record("PromptVerbosityWarningException", message);
    }

    public PromptVerbosityWarningException(String message, Throwable cause) {
        super(message, cause);
        AuditErrorLog.record("PromptVerbosityWarningException", message);
    }

    /** Builds the canonical prose-at-LOCAL advisory message. Caller logs it; it is not thrown. */
    public static String message(String method, String prompt) {
        return """
                [PrOOPt WARN] Prose-level output detected with ModelTier.LOCAL
                  Method  : %s
                  Prompt  : "%s"
                  Tier    : LOCAL (ONNX Runtime + Phi 3.5, ~15-20 tok/s)
                  Concern : Prose-level LLM output at LOCAL tier may cause significant
                             latency or request timeouts under concurrent load.
                  Options :
                    1. Switch to modelTier = ModelTier.CLOUD_ADVANCED for faster prose generation.
                    2. Rewrite the prompt to produce a short scalar output if prose
                       is not actually required.\
                """.formatted(method, AuditErrorLog.truncatePrompt(prompt));
    }
}
