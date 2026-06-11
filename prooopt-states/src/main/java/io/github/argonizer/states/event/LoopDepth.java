/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

/**
 * Depth of the internal thought feedback loop applied to a persona.
 *
 * <p>SHALLOW processes only surface-level emotional decay and reinforcement.
 * MODERATE reinterprets recent experience — emotions may compound or resolve.
 * DEEP triggers belief revision and identity-level processing; core values may shift.
 */
public enum LoopDepth {
    SHALLOW,
    MODERATE,
    DEEP
}
