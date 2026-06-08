/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.llm;

import io.github.argonizer.prooopt.model.ModelTier;

/**
 * Thin adapter interface that decouples prooopt-states from the concrete
 * {@link io.github.argonizer.prooopt.router.ModelRouter} type.
 *
 * <p>Implementations delegate to the existing PrOOPt core {@code ModelRouter}
 * — no direct HTTP calls and no hardcoded model names.
 *
 * <p>The default implementation is {@link PersonaLlmClient}, which wraps a
 * {@code ModelRouter} bean injected by the starter.
 */
@FunctionalInterface
public interface LlmGateway {

    /**
     * Sends {@code prompt} to a model at the given tier and returns the raw
     * text response.
     *
     * @param prompt the full prompt string.
     * @param tier   the model tier to route to.
     * @return the raw LLM response string.
     */
    String call(String prompt, ModelTier tier);
}
