/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.router;

import io.github.argonizer.prooopt.config.PrOOPtProperties;
import io.github.argonizer.prooopt.model.ModelTier;

import java.util.List;

/**
 * Combines several routers so a single application can mix on-device and cloud inference. AUTO is
 * resolved once here, then the resolved tier is dispatched to the first child that supports it — so
 * children never re-resolve AUTO.
 */
public final class CompositeModelRouter implements ModelRouter {

    private final List<ModelRouter> delegates;
    private final PrOOPtProperties properties;

    public CompositeModelRouter(PrOOPtProperties properties, List<ModelRouter> delegates) {
        this.properties = properties;
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public String route(String prompt, ModelTier tier) {
        ModelTier concrete = AutoRouting.resolve(prompt, tier, properties);
        for (ModelRouter delegate : delegates) {
            if (delegate.supports(concrete)) {
                return delegate.route(prompt, concrete);
            }
        }
        throw new UnsupportedOperationException(
                "No registered runtime supports tier " + concrete + ". Registered routers: " + delegates);
    }

    @Override
    public boolean supports(ModelTier tier) {
        return delegates.stream().anyMatch(d -> d.supports(tier));
    }
}
