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
 * The tier of model authority granted to a function — the core governance dial of PrOOPt.
 *
 * <p>Each {@code @PromptFunction} declares exactly how much AI authority it is granted and where its
 * data may travel. This is an explicit, auditable decision made at the function level rather than a
 * blanket delegation to a single hyperscaled model.
 */
public enum ModelTier {

    /**
     * Bounded AI zone: on-device inference via the bundled local model (JLama). No data leaves the
     * JVM. Cheapest and most private tier; ideal for extraction, classification, and routing.
     */
    LOCAL,

    /**
     * Elevated AI zone (fast): a small, inexpensive cloud model (for example, a Haiku-class model).
     * Data leaves the JVM for the configured provider. Suited to high-volume, latency-sensitive work.
     */
    CLOUD_FAST,

    /**
     * Elevated AI zone (advanced): the most capable cloud model (for example, a Sonnet- or Opus-class
     * model). Highest cost and capability; reserve for genuinely hard reasoning.
     */
    CLOUD_ADVANCED,

    /**
     * Let the {@code ModelRouter} choose a concrete tier at call time using a configured strategy
     * (for example, a complexity heuristic over prompt size), falling back as configured. AUTO is a
     * routing intent, never a destination: it always resolves to one of the concrete tiers above.
     */
    AUTO;

    /**
     * Lenient parse for configuration values such as {@code cloud-fast} or {@code "LOCAL"}: trims,
     * upper-cases, and normalises {@code -}/space separators to {@code _}.
     *
     * @return the matching tier, or {@code null} when {@code value} is blank
     * @throws IllegalArgumentException when {@code value} is non-blank but unrecognised
     */
    public static ModelTier fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ModelTier.valueOf(value.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
    }
}
