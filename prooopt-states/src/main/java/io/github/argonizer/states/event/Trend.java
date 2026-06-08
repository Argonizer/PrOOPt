/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

/**
 * Trend direction over a time window, used to filter
 * {@link io.github.argonizer.states.annotation.OnPersonaEvent} handlers.
 */
public enum Trend {
    RISING,
    FALLING,
    NONE
}
