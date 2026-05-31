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
 * Thrown when a raw model response cannot be converted into a function's declared return type.
 * Carries the offending response and target type so retries and audit lines can explain the failure.
 */
public class PrOOPtAutoBoxException extends PrOOPtException {

    private final String rawResponse;
    private final Class<?> targetType;

    public PrOOPtAutoBoxException(String message, String rawResponse, Class<?> targetType) {
        super(message);
        this.rawResponse = rawResponse;
        this.targetType = targetType;
    }

    public PrOOPtAutoBoxException(String message, String rawResponse, Class<?> targetType, Throwable cause) {
        super(message, cause);
        this.rawResponse = rawResponse;
        this.targetType = targetType;
    }

    /** The model output that could not be parsed (may be {@code null}). */
    public String getRawResponse() {
        return rawResponse;
    }

    /** The return type PrOOPt attempted to produce. */
    public Class<?> getTargetType() {
        return targetType;
    }
}
