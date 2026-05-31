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

import java.util.Set;

/**
 * Base class for runtime routers. Handles the cross-cutting concerns — AUTO resolution, configuration
 * lookup, and tier-support checks — so concrete engines only implement {@link #generate}.
 */
public abstract class AbstractModelRouter implements ModelRouter {

    protected final PrOOPtProperties properties;

    protected AbstractModelRouter(PrOOPtProperties properties) {
        if (properties == null) {
            throw new PrOOPtConfigException("PrOOPtProperties must not be null");
        }
        this.properties = properties;
    }

    @Override
    public final String route(String prompt, ModelTier tier) {
        ModelTier concrete = AutoRouting.resolve(prompt, tier, properties);
        if (!supports(concrete)) {
            throw new UnsupportedOperationException(unsupportedMessage(concrete));
        }
        ModelConfig config = properties.forTier(concrete);
        if (config == null) {
            throw new PrOOPtConfigException("no configuration for model tier " + concrete);
        }
        return generate(concrete, config, prompt);
    }

    @Override
    public boolean supports(ModelTier tier) {
        return tier == ModelTier.AUTO || supportedTiers().contains(tier);
    }

    /** The concrete tiers this router can serve. */
    protected abstract Set<ModelTier> supportedTiers();

    /** Performs the actual model call for an already-resolved, supported tier. */
    protected abstract String generate(ModelTier tier, ModelConfig config, String prompt);

    /** Overridable message used when an unsupported tier is requested. */
    protected String unsupportedMessage(ModelTier tier) {
        return getClass().getSimpleName() + " does not support tier " + tier
                + " (supported: " + supportedTiers() + ")";
    }
}
