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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A deterministic, dependency-free TF-IDF embedding engine. The vocabulary and inverse-document
 * frequencies are built from the tool corpus at startup; each text becomes an L2-normalised TF-IDF
 * vector over that vocabulary, so cosine similarity reduces to a dot product. Good enough to route
 * plain-English capability requests to the right tool without loading a model.
 */
public class TfIdfEmbeddingEngine implements EmbeddingEngine {

    private static final Pattern TOKEN = Pattern.compile("[^a-z0-9]+");

    private static final int CACHE_SIZE = 2000;

    private final Map<String, Integer> vocabulary = new HashMap<>();
    private final Map<String, Double> idf = new HashMap<>();
    private int dimension;

    /** Bounded LRU cache of computed embeddings — repeated capability/input strings hit it often. */
    private final Map<String, float[]> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    @Override
    public void fit(Collection<String> corpus) {
        vocabulary.clear();
        idf.clear();
        cache.clear();  // vocabulary changed — prior vectors are no longer valid

        Map<String, Integer> documentFrequency = new HashMap<>();
        int documentCount = 0;
        for (String document : corpus) {
            documentCount++;
            Set<String> seen = new HashSet<>(tokenize(document));
            for (String term : seen) {
                vocabulary.computeIfAbsent(term, t -> vocabulary.size());
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        this.dimension = vocabulary.size();

        // Smoothed IDF: ln((1 + N) / (1 + df)) + 1 — never zero, never undefined.
        for (Map.Entry<String, Integer> e : documentFrequency.entrySet()) {
            double value = Math.log((1.0 + documentCount) / (1.0 + e.getValue())) + 1.0;
            idf.put(e.getKey(), value);
        }
    }

    @Override
    public float[] embed(String text) {
        String key = text == null ? "" : text;
        float[] cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        float[] computed = computeEmbedding(text);
        cache.put(key, computed);
        return computed;
    }

    private float[] computeEmbedding(String text) {
        float[] vector = new float[Math.max(dimension, 1)];
        if (dimension == 0) {
            return vector;
        }
        Map<String, Integer> termCounts = new HashMap<>();
        var tokens = tokenize(text);
        for (String term : tokens) {
            termCounts.merge(term, 1, Integer::sum);
        }
        int total = tokens.size();
        if (total == 0) {
            return vector;
        }
        for (Map.Entry<String, Integer> e : termCounts.entrySet()) {
            Integer index = vocabulary.get(e.getKey());
            if (index == null) {
                continue; // out-of-vocabulary term
            }
            double tf = (double) e.getValue() / total;
            double weight = tf * idf.getOrDefault(e.getKey(), 1.0);
            vector[index] = (float) weight;
        }
        return VectorMath.normalizeInPlace(vector);
    }

    /** The vocabulary size (vector dimension) after fitting. */
    public int dimension() {
        return dimension;
    }

    private static java.util.List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return java.util.List.of();
        }
        String[] parts = TOKEN.split(text.toLowerCase().trim());
        var tokens = new java.util.ArrayList<String>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }
        return tokens;
    }
}
