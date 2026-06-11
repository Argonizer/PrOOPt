/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

/**
 * Specific lifecycle event discriminator used in
 * {@link io.github.argonizer.states.annotation.OnPersonaEvent}.
 */
public enum LifecycleEvent {
    CREATED,
    RETIRED,
    RESTORED,
    PHASE_TRANSITION,
    NONE
}
