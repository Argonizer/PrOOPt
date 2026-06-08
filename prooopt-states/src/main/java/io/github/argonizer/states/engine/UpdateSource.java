/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.engine;

/**
 * Records the origin of each state write in {@code prooopt_persona_history.update_source}.
 * Every history row must carry an accurate {@code UpdateSource}.
 */
public enum UpdateSource {
    /** Initial seed — the persona was just created. */
    SEED,
    /** A developer-supplied prompt via {@code PersonaManager.update()}. */
    EXTERNAL,
    /** An event fired through {@code PersonaPhaseManager.fireEvent()}. */
    EVENT,
    /** A delayed or scheduled event that was buffered before delivery. */
    EVENT_DELAYED,
    /** The internal thought loop at SHALLOW or MODERATE depth. */
    INTERNAL_LOOP,
    /** The internal thought loop at DEEP depth (belief revision territory). */
    INTERNAL_LOOP_DEEP,
    /** An explicit belief-revision call. */
    BELIEF_REVISION,
    /** The Spring Batch evolution job (time-based natural drift). */
    EVOLUTION,
    /** Restore from retired status. */
    RESTORE
}
