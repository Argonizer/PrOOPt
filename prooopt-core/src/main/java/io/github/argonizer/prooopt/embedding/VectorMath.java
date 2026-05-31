/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.embedding;

/**
 * Small vector helpers for semantic matching. At standard-library scale (a few hundred tools) a
 * brute-force cosine sweep is sub-millisecond, so no approximate-nearest-neighbour index is needed.
 */
public final class VectorMath {

    private VectorMath() {
    }

    /** Cosine similarity in {@code [-1, 1]}; returns 0 for a zero-length or mismatched vector. */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** Euclidean (L2) norm of a vector. */
    public static double norm(float[] v) {
        double sum = 0.0;
        for (float x : v) {
            sum += (double) x * x;
        }
        return Math.sqrt(sum);
    }

    /** Normalises a vector to unit length in place and returns it; a zero vector is left unchanged. */
    public static float[] normalizeInPlace(float[] v) {
        double n = norm(v);
        if (n == 0.0) {
            return v;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (v[i] / n);
        }
        return v;
    }
}
