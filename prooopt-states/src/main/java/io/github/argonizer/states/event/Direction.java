/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

/**
 * Direction of a threshold crossing or trend movement.
 */
public enum Direction {
    /** Value moved above the threshold. */
    ABOVE,
    /** Value moved below the threshold. */
    BELOW
}
