/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.embedding;

import java.util.Collection;

/**
 * Turns text into a vector for semantic matching. Two implementations ship: a zero-dependency,
 * deterministic {@link TfIdfEmbeddingEngine} (vocabulary built from tool descriptions at startup) and
 * a dense {@code LocalModelEmbeddingEngine} in the runtime-local module that reuses the bundled model.
 */
public interface EmbeddingEngine {

    /**
     * Optionally builds internal state from the corpus of tool texts before any {@link #embed}. Sparse
     * engines build a vocabulary here; dense engines may ignore it.
     */
    default void fit(Collection<String> corpus) {
    }

    /** Embeds a single piece of text into a fixed-length vector. */
    float[] embed(String text);
}
