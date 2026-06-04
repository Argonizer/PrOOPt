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
 * Thrown by {@code prooopt-runtime-local} or {@code prooopt-runtime-cloud} when the underlying model
 * call fails. The root cause (e.g. an ONNX Runtime {@code OrtException} or an HTTP error) is attached
 * via {@code initCause} so the full stack is preserved.
 */
public final class ModelInvocationException extends PrOOPtException {

    public ModelInvocationException(String message) {
        super(message);
        AuditErrorLog.record("ModelInvocationException", message);
    }

    public ModelInvocationException(String message, Throwable cause) {
        super(message, cause);
        AuditErrorLog.record("ModelInvocationException", message);
    }

    /** A LOCAL-tier (ONNX Runtime + Phi 3.5) invocation failure. */
    public static ModelInvocationException local(
            String method, String prompt, String cause, Throwable t) {
        return new ModelInvocationException("""
                [PrOOPt] ModelInvocationException in %s
                  Tier      : LOCAL (ONNX Runtime + Phi 3.5)
                  Prompt    : "%s"
                  Cause     : %s
                  Fix       : Verify the Phi 3.5 ONNX model files are present at the path
                               configured in prooopt-runtime-local. Default expected path:
                               ${user.home}/.prooopt/models/phi-3.5/model.onnx
                               Run: mvn prooopt:download-model to fetch the model automatically.\
                """.formatted(method, AuditErrorLog.truncatePrompt(prompt), cause), t);
    }

    /** A CLOUD-tier invocation failure (e.g. HTTP 429 / 5xx from the provider). */
    public static ModelInvocationException cloud(
            String method, String prompt, String cause, Throwable t) {
        return new ModelInvocationException("""
                [PrOOPt] ModelInvocationException in %s
                  Tier      : CLOUD
                  Prompt    : "%s"
                  Cause     : %s
                  Fix       : Implement retry logic in your @PromptOrchestrator, or reduce
                               the concurrency of @PromptFunction calls. PrOOPt does not
                               retry automatically — this is a deliberate design decision
                               to keep execution behaviour predictable in regulated environments.\
                """.formatted(method, AuditErrorLog.truncatePrompt(prompt), cause), t);
    }
}
