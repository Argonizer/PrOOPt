/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.config;

/**
 * How tools are narrowed before reaching the orchestrator. The orchestrator never sees the full tool
 * list; it sees only the semantically relevant slice.
 */
public class ToolSelectionConfig {

    /** {@code semantic} (embeddings + cosine), {@code category} (by tag), or {@code all}. */
    private String strategy = "semantic";

    /** Maximum number of tools returned by relevance selection. */
    private int topK = 10;

    /** Minimum cosine similarity for a tool to be considered relevant. */
    private double minSimilarity = 0.30;

    /** {@code tfidf} (zero-dependency) or {@code local-model} (dense vectors). */
    private String embeddingEngine = "tfidf";

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinSimilarity() {
        return minSimilarity;
    }

    public void setMinSimilarity(double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }

    public String getEmbeddingEngine() {
        return embeddingEngine;
    }

    public void setEmbeddingEngine(String embeddingEngine) {
        this.embeddingEngine = embeddingEngine;
    }
}
