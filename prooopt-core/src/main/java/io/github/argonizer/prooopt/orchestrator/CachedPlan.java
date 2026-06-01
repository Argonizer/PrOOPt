/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.model.ExecutionPlan;

/**
 * A cached execution-plan template plus the metadata needed for lookup and expiry.
 *
 * @param plan          the plan template (input placeholders retained)
 * @param embedding     the input embedding for {@code SEMANTIC} lookup; {@code null} otherwise
 * @param originalInput the input that produced the plan, kept for debug/logging
 * @param cachedAtMs    epoch millis at insertion, for TTL checks
 */
public record CachedPlan(
        ExecutionPlan plan,
        float[] embedding,
        String originalInput,
        long cachedAtMs) {
}
