/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.lifecycle;

import io.github.argonizer.states.engine.PersonaStateDiff;

/**
 * Optional lifecycle callback interface for {@link io.github.argonizer.states.annotation.Persona}
 * classes that need to observe or veto state transitions.
 *
 * <p>All methods are {@code default} (no-op), so a persona class only needs to implement
 * the hooks it cares about. The engine calls these hooks only when the persona instance
 * implements this interface — no extra registration required.
 *
 * <p>There are no lifecycle <em>annotations</em>; this interface is the only hook mechanism.
 *
 * <p>Example:
 * <pre>{@code
 * @Persona("A city guard NPC")
 * public class GuardNpc implements PersonaLifecycle {
 *
 *     @Override
 *     public void onStateUpdated(PersonaStateDiff diff) {
 *         if (diff.crossed("suspicion_level", Direction.ABOVE, 90)) {
 *             log.warn("Guard {} is extremely suspicious!", npcId);
 *         }
 *     }
 * }
 * }</pre>
 */
public interface PersonaLifecycle {

    /**
     * Called before the LLM is invoked for a state update.
     * Throw {@link PersonaUpdateVetoException} to cancel the update cleanly.
     *
     * @param prompt the prompt that will be sent to the LLM.
     * @throws PersonaUpdateVetoException to cancel the update.
     */
    default void beforeStateUpdate(String prompt) throws PersonaUpdateVetoException {}

    /**
     * Called after the diff has been applied to the in-memory persona object,
     * but before the state has been committed to the store.
     *
     * @param diff the computed state diff.
     */
    default void onStateUpdated(PersonaStateDiff diff) {}

    /**
     * Called after the state write has been committed atomically
     * (state blob + index + history).
     *
     * @param diff the committed diff.
     */
    default void onStatePersisted(PersonaStateDiff diff) {}

    /**
     * Called after a persona has been loaded from the store and its
     * fields have been populated.
     */
    default void onStateLoaded() {}

    /**
     * Called after a persona has been retired (soft-deleted).
     *
     * @param reason the retirement reason supplied to the manager.
     */
    default void onStateRetired(String reason) {}

    /**
     * Called after a retired persona has been restored to active status.
     *
     * @param reason the restoration reason supplied to the manager.
     */
    default void onStateRestored(String reason) {}
}
