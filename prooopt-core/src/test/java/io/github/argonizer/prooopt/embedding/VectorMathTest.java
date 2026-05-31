/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class VectorMathTest {

    @Test
    void identicalVectorsHaveSimilarityOne() {
        float[] v = {1, 2, 3};
        assertEquals(1.0, VectorMath.cosineSimilarity(v, v), 1e-9);
    }

    @Test
    void orthogonalVectorsHaveSimilarityZero() {
        float[] a = {1, 0};
        float[] b = {0, 1};
        assertEquals(0.0, VectorMath.cosineSimilarity(a, b), 1e-9);
    }

    @Test
    void oppositeVectorsHaveSimilarityMinusOne() {
        float[] a = {1, 0};
        float[] b = {-1, 0};
        assertEquals(-1.0, VectorMath.cosineSimilarity(a, b), 1e-9);
    }

    @Test
    void nullInputReturnsZero() {
        assertEquals(0.0, VectorMath.cosineSimilarity(null, new float[]{1}), 1e-9);
    }

    @Test
    void zeroVectorReturnsZeroSimilarity() {
        float[] zero = {0, 0};
        float[] v = {1, 1};
        assertEquals(0.0, VectorMath.cosineSimilarity(zero, v), 1e-9);
    }

    @Test
    void normComputedCorrectly() {
        float[] v = {3, 4};
        assertEquals(5.0, VectorMath.norm(v), 1e-9);
    }

    @Test
    void normalizeInPlaceProducesUnitVector() {
        float[] v = {3, 4};
        VectorMath.normalizeInPlace(v);
        assertEquals(1.0, VectorMath.norm(v), 1e-6);
    }

    @Test
    void normalizeZeroVectorIsNoOp() {
        float[] zero = {0, 0};
        VectorMath.normalizeInPlace(zero);
        assertArrayEquals(new float[]{0, 0}, zero);
    }
}
