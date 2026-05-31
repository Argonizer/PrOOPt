/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.model;

/**
 * Controls how much detail a function contributes to the PrOOPt audit log.
 */
public enum LogLevel {

    /** Emit START and END lines including resolved inputs and outputs (sensitive values redacted). */
    FULL,

    /** Emit a single, compact line per call (name, model, duration) with no inputs or outputs. */
    SUMMARY,

    /** Emit nothing for this function. */
    OFF
}
