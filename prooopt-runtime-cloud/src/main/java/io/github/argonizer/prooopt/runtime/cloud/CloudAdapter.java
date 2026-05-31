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

/**
 * Adapter for one cloud provider's text-completion endpoint.
 */
public interface CloudAdapter {

    /** Sends a single-turn prompt and returns the model's text response. */
    String generate(ModelConfig config, String prompt);
}
