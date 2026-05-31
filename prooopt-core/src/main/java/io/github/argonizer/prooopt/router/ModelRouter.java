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
 * Routes an enriched prompt to a concrete model and returns the raw text response. The interface
 * lives in core; concrete engines live in the runtime modules (JLama for local, Anthropic/OpenAI for
 * cloud). With only core on the classpath, the supplied {@code NoRuntimeModelRouter} throws an
 * instructive error directing the user to add a runtime module.
 */
public interface ModelRouter {

    /**
     * Resolves the configuration for {@code tier} (resolving {@link ModelTier#AUTO} to a concrete
     * tier) and dispatches the prompt to the appropriate engine.
     *
     * @return the model's raw, unparsed text response
     */
    String route(String prompt, ModelTier tier);

    /** Whether this router can serve the given tier. {@link ModelTier#AUTO} is resolved before dispatch. */
    default boolean supports(ModelTier tier) {
        return true;
    }
}
