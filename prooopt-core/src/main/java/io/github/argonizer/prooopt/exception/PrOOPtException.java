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
 * Base type for all PrOOPt runtime failures. Unchecked so it composes cleanly with the fluent and
 * orchestration APIs without forcing checked-exception plumbing through user code.
 */
public class PrOOPtException extends RuntimeException {

    public PrOOPtException(String message) {
        super(message);
    }

    public PrOOPtException(String message, Throwable cause) {
        super(message, cause);
    }
}
