/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.runtime.cloud;

import io.github.argonizer.prooopt.config.ModelConfig;
import io.github.argonizer.prooopt.config.PrOOPtProperties;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.AbstractModelRouter;

import java.util.Set;

/**
 * The cloud runtime's {@link io.github.argonizer.prooopt.router.ModelRouter}. Serves the
 * {@link ModelTier#CLOUD_FAST} and {@link ModelTier#CLOUD_ADVANCED} tiers by dispatching to a
 * provider adapter (Anthropic or OpenAI) chosen by each tier's configured engine/provider. Combine it
 * with the local router via {@code CompositeModelRouter} to serve all tiers in one application.
 */
public class CloudModelRouter extends AbstractModelRouter {

    private final AnthropicAdapter anthropic = new AnthropicAdapter();
    private final OpenAIAdapter openai = new OpenAIAdapter();

    public CloudModelRouter(PrOOPtProperties properties) {
        super(properties);
    }

    @Override
    protected Set<ModelTier> supportedTiers() {
        return Set.of(ModelTier.CLOUD_FAST, ModelTier.CLOUD_ADVANCED);
    }

    @Override
    protected String generate(ModelTier tier, ModelConfig config, String prompt) {
        String engine = config.resolvedEngine();
        if (engine == null) {
            throw new PrOOPtConfigException("cloud tier " + tier + " has no engine/provider configured");
        }
        return switch (engine) {
            case "anthropic", "claude" -> anthropic.generate(config, prompt);
            case "openai", "gpt" -> openai.generate(config, prompt);
            default -> throw new PrOOPtConfigException(
                    "unsupported cloud provider '" + engine + "' for tier " + tier
                            + "; expected 'anthropic' or 'openai'");
        };
    }
}
