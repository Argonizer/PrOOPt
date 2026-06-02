/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.router;

import io.github.argonizer.prooopt.model.ModelTier;

/**
 * The router PrOOPt falls back to when only {@code prooopt-core} is on the classpath. Every call
 * fails fast with an instructive message — core deliberately ships no model engine of its own.
 */
public final class NoRuntimeModelRouter implements ModelRouter {

    @Override
    public String route(String prompt, ModelTier tier) {
        throw new UnsupportedOperationException(
                "No PrOOPt runtime is on the classpath, so tier " + tier + " cannot be served. "
                        + "Add 'prooopt-runtime-local-java17' for on-device LOCAL inference, and/or "
                        + "'prooopt-runtime-cloud' for the CLOUD_FAST / CLOUD_ADVANCED tiers.");
    }

    @Override
    public boolean supports(ModelTier tier) {
        return false;
    }
}
