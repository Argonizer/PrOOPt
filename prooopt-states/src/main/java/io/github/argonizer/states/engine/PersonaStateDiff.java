/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.engine;

import io.github.argonizer.states.event.Direction;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable record of the traits that changed in a single state resolution.
 *
 * <p>The diff is the payload for {@link io.github.argonizer.states.lifecycle.PersonaLifecycle}
 * hooks, the basis for event emission, and the source of the
 * {@code fields_changed} column written to {@code prooopt_persona_history}.
 */
public final class PersonaStateDiff {

    private final String personaId;
    private final String personaType;
    private final String prompt;
    private final UpdateSource source;
    /** key = snake_name, value[0] = before, value[1] = after (null = not present before). */
    private final Map<String, Object[]> changes;

    /**
     * @param personaId  identifier of the persona.
     * @param personaType simple class name of the persona type.
     * @param prompt     the prompt that triggered this diff (may be null for evolution).
     * @param source     the origin of this write.
     * @param changes    map of snake_name → [before, after] pairs.
     */
    public PersonaStateDiff(String personaId, String personaType,
                            String prompt, UpdateSource source,
                            Map<String, Object[]> changes) {
        this.personaId = personaId;
        this.personaType = personaType;
        this.prompt = prompt;
        this.source = source;
        this.changes = Collections.unmodifiableMap(changes);
    }

    /** @return the persona's id value as a string. */
    public String personaId() { return personaId; }

    /** @return the persona's class name. */
    public String personaType() { return personaType; }

    /** @return the triggering prompt or {@code null} for batch/evolution sources. */
    public String prompt() { return prompt; }

    /** @return the origin of this state change. */
    public UpdateSource source() { return source; }

    /** @return unmodifiable map of changed trait snake_names to [before, after] pairs. */
    public Map<String, Object[]> changes() { return changes; }

    /** @return {@code true} if the named trait appears in this diff. */
    public boolean affected(String traitSnakeName) {
        return changes.containsKey(traitSnakeName);
    }

    /** @return {@code true} if this diff is empty (no traits changed). */
    public boolean isEmpty() { return changes.isEmpty(); }

    /**
     * Returns {@code true} if the named trait crossed {@code threshold} in the
     * specified direction during this update.
     *
     * @param traitSnakeName snake_case trait name.
     * @param direction      the crossing direction to check.
     * @param threshold      the numeric threshold.
     */
    public boolean crossed(String traitSnakeName, Direction direction, double threshold) {
        Object[] pair = changes.get(traitSnakeName);
        if (pair == null) return false;
        double before = pair[0] == null ? 0.0 : toDouble(pair[0]);
        double after = pair[1] == null ? 0.0 : toDouble(pair[1]);
        if (direction == Direction.ABOVE) {
            return before <= threshold && after > threshold;
        } else {
            return before >= threshold && after < threshold;
        }
    }

    private static double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return 0.0; }
    }
}
