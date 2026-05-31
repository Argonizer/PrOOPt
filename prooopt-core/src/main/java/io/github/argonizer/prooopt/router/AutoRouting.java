/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.router;

import io.github.argonizer.prooopt.config.ModelConfig;
import io.github.argonizer.prooopt.config.PrOOPtProperties;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.ModelTier;

/**
 * Resolves {@link ModelTier#AUTO} to a concrete tier using the configured strategy. The default
 * {@code complexity-heuristic} estimates prompt size in tokens and keeps small prompts on the cheapest
 * available tier, escalating larger ones, with a configured fallback.
 */
public final class AutoRouting {

    private AutoRouting() {
    }

    /** Resolves {@code tier} to a concrete tier; passes concrete tiers through unchanged. */
    public static ModelTier resolve(String prompt, PrOOPtProperties properties) {
        return resolve(prompt, ModelTier.AUTO, properties);
    }

    public static ModelTier resolve(String prompt, ModelTier tier, PrOOPtProperties properties) {
        if (tier != ModelTier.AUTO) {
            return tier;
        }
        ModelConfig auto = properties.forTier(ModelTier.AUTO);
        int threshold = auto != null ? auto.getTokenThreshold() : 500;
        ModelTier fallback = auto != null && auto.getFallback() != null
                ? auto.getFallback() : ModelTier.CLOUD_FAST;

        int estimatedTokens = estimateTokens(prompt);
        if (estimatedTokens <= threshold) {
            // Small/simple prompt: prefer the cheapest available tier.
            if (configured(properties, ModelTier.LOCAL)) {
                return ModelTier.LOCAL;
            }
            return firstAvailable(properties, fallback, ModelTier.CLOUD_FAST, ModelTier.CLOUD_ADVANCED);
        }
        // Larger/complex prompt: prefer the most capable available tier.
        return firstAvailable(properties, ModelTier.CLOUD_ADVANCED, fallback, ModelTier.CLOUD_FAST, ModelTier.LOCAL);
    }

    /** Rough token estimate: ~4 characters per token. */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }

    private static boolean configured(PrOOPtProperties properties, ModelTier tier) {
        return properties.forTier(tier) != null;
    }

    private static ModelTier firstAvailable(PrOOPtProperties properties, ModelTier... preference) {
        for (ModelTier tier : preference) {
            if (tier != null && tier != ModelTier.AUTO && configured(properties, tier)) {
                return tier;
            }
        }
        throw new PrOOPtConfigException(
                "AUTO routing found no configured model tier; configure at least one of "
                        + "prooopt.models.{local,cloud-fast,cloud-advanced}");
    }
}
