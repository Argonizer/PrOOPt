/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.llm;

import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.ModelRouter;

/**
 * Default {@link LlmGateway} implementation that delegates to the prooopt-core
 * {@link ModelRouter}.
 *
 * <p>All LLM calls made by prooopt-states route through the existing gateway —
 * no direct HTTP, no hardcoded model names.
 */
public final class PersonaLlmClient implements LlmGateway {

    private final ModelRouter router;

    /**
     * @param router the prooopt-core model router to delegate to.
     */
    public PersonaLlmClient(ModelRouter router) {
        this.router = router;
    }

    @Override
    public String call(String prompt, ModelTier tier) {
        return router.route(prompt, tier);
    }
}
