/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.lifecycle;

/**
 * Checked exception thrown by {@link PersonaLifecycle#beforeStateUpdate(String)}
 * to cancel a pending state update.
 *
 * <p>When this exception is thrown, the engine performs no LLM call, writes no
 * state or history rows, and emits no events. The persona is left unchanged.
 */
public final class PersonaUpdateVetoException extends Exception {

    /** @param message human-readable reason for the veto. */
    public PersonaUpdateVetoException(String message) {
        super(message);
    }

    /** @param message human-readable reason for the veto.
     *  @param cause   underlying cause. */
    public PersonaUpdateVetoException(String message, Throwable cause) {
        super(message, cause);
    }
}
