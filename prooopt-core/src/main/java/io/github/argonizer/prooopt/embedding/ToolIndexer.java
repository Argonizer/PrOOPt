/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.embedding;

import io.github.argonizer.prooopt.model.ToolDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Indexes tools for semantic selection. At startup it embeds each tool's {@code description + tags}
 * and caches the vector on the {@link ToolDescriptor}; at query time it ranks tools by cosine
 * similarity. The orchestrator never sees the full tool list — only the relevant slice this produces.
 */
public class ToolIndexer {

    private final EmbeddingEngine engine;
    private final double defaultMinSimilarity;
    private final int defaultTopK;

    private List<ToolDescriptor> tools = List.of();

    public ToolIndexer(EmbeddingEngine engine) {
        this(engine, 0.30, 10);
    }

    public ToolIndexer(EmbeddingEngine engine, double defaultMinSimilarity, int defaultTopK) {
        this.engine = engine;
        this.defaultMinSimilarity = defaultMinSimilarity;
        this.defaultTopK = defaultTopK;
    }

    /** Fits the engine on the tool corpus and caches an embedding vector on each descriptor. */
    public void index(Collection<ToolDescriptor> toolDescriptors) {
        List<ToolDescriptor> copy = List.copyOf(toolDescriptors);
        engine.fit(copy.stream().map(ToolDescriptor::embeddableText).toList());
        for (ToolDescriptor tool : copy) {
            tool.setEmbedding(engine.embed(tool.embeddableText()));
        }
        this.tools = copy;
    }

    /** Top-{@code topK} tools whose similarity to {@code input} meets {@code minSimilarity}. */
    public List<ScoredTool> selectRelevant(String input, int topK, double minSimilarity) {
        float[] query = engine.embed(input);
        List<ScoredTool> scored = new ArrayList<>(tools.size());
        for (ToolDescriptor tool : tools) {
            double score = VectorMath.cosineSimilarity(query, tool.embedding());
            if (score >= minSimilarity) {
                scored.add(new ScoredTool(tool, score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredTool::score).reversed());
        return scored.size() > topK ? new ArrayList<>(scored.subList(0, topK)) : scored;
    }

    /** Relevance selection using the configured defaults. */
    public List<ScoredTool> selectRelevant(String input) {
        return selectRelevant(input, defaultTopK, defaultMinSimilarity);
    }

    /** The single best tool for a capability description, if one clears {@code minScore}. */
    public Optional<ScoredTool> findBestMatch(String capability, double minScore) {
        float[] query = engine.embed(capability);
        ScoredTool best = null;
        for (ToolDescriptor tool : tools) {
            double score = VectorMath.cosineSimilarity(query, tool.embedding());
            if (score >= minScore && (best == null || score > best.score())) {
                best = new ScoredTool(tool, score);
            }
        }
        return Optional.ofNullable(best);
    }

    /** Best-match using the configured default threshold. */
    public Optional<ScoredTool> findBestMatch(String capability) {
        return findBestMatch(capability, defaultMinSimilarity);
    }

    public List<ToolDescriptor> tools() {
        return tools;
    }

    public int size() {
        return tools.size();
    }
}
