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
 * Thrown for misconfiguration discovered at startup or load time — for example, a {@code static}
 * {@code @PromptFunction}, a missing model configuration, or an unreadable config file. The message
 * is intended to be actionable.
 */
public class PrOOPtConfigException extends PrOOPtException {

    public PrOOPtConfigException(String message) {
        super(message);
    }

    public PrOOPtConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
