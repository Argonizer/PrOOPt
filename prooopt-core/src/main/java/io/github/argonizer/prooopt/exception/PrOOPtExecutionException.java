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
 * Thrown when a step of an {@code ExecutionPlan} fails. Pins the failure to a specific step so the
 * audit trail and caller can see exactly which function broke and where.
 */
public class PrOOPtExecutionException extends PrOOPtException {

    private final String stepId;
    private final String function;

    public PrOOPtExecutionException(String stepId, String function, String message, Throwable cause) {
        super("step " + stepId + " (" + function + "): " + message, cause);
        this.stepId = stepId;
        this.function = function;
    }

    /** The {@code stepId} of the failing step. */
    public String getStepId() {
        return stepId;
    }

    /** The name of the function that failed. */
    public String getFunction() {
        return function;
    }
}
