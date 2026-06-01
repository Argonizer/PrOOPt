/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.model;

/**
 * How cached plan templates are keyed and looked up under {@link PlanMode#STATIC}.
 */
public enum PlanCacheStrategy {

    /**
     * SHA-256 of the trimmed input string. Cache hit only on byte-for-byte identical input. Zero
     * false positives, narrow reuse.
     */
    EXACT,

    /**
     * Cosine similarity between the input embedding and cached plan embeddings. Similar inputs (above
     * the configured threshold) reuse the same plan. Ideal for batch workloads with varied but
     * structurally similar inputs. Uses the configured {@code EmbeddingEngine}.
     */
    SEMANTIC,

    /**
     * A LOCAL model classifies the input into an intent category (for example
     * {@code "loan_application_analysis"}). The plan is cached and retrieved by intent key. Highest
     * reuse rate, lowest granularity.
     */
    INTENT
}
