/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.runtime;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnnxEmbeddingEngineTest {

    /** Builds an engine whose forward pass returns a canned hidden state and counts invocations. */
    private static OnnxEmbeddingEngine engineReturning(float[][][] hidden, AtomicInteger forwardCalls) {
        OnnxModelLoader loader = mock(OnnxModelLoader.class);
        HuggingFaceTokenizer tokenizer = mock(HuggingFaceTokenizer.class);
        Encoding encoding = mock(Encoding.class);
        when(encoding.getIds()).thenReturn(new long[]{1L, 2L, 3L});
        when(tokenizer.encode(anyString(), anyBoolean(), anyBoolean())).thenReturn(encoding);
        when(loader.getTokenizer()).thenReturn(tokenizer);

        return new OnnxEmbeddingEngine(loader) {
            @Override
            float[][][] forward(long[] inputIds) {
                forwardCalls.incrementAndGet();
                return hidden;
            }
        };
    }

    @Test
    void embed_returnsNormalisedVector() {
        float[][][] hidden = {{{1f, 2f, 3f, 4f}, {5f, 6f, 7f, 8f}, {9f, 10f, 11f, 12f}}};
        OnnxEmbeddingEngine engine = engineReturning(hidden, new AtomicInteger());

        float[] result = engine.embed("hello");

        // mean pool: [(1+5+9)/3, (2+6+10)/3, (3+7+11)/3, (4+8+12)/3] = [5,6,7,8]
        float[] expected = OnnxEmbeddingEngine.l2Normalise(new float[]{5f, 6f, 7f, 8f});
        assertEquals(expected.length, result.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i], 1e-5);
        }
    }

    @Test
    void embed_cacheHit_doesNotCallSession() {
        AtomicInteger calls = new AtomicInteger();
        OnnxEmbeddingEngine engine = engineReturning(new float[][][]{{{1f, 2f}}}, calls);

        engine.embed("hello");
        engine.embed("hello");

        assertEquals(1, calls.get(), "second call must hit the cache");
    }

    @Test
    void embed_cacheMiss_callsSession() {
        AtomicInteger calls = new AtomicInteger();
        OnnxEmbeddingEngine engine = engineReturning(new float[][][]{{{1f, 2f}}}, calls);

        engine.embed("hello");
        engine.embed("world");

        assertEquals(2, calls.get());
    }

    @Test
    void meanPool_correctlyAverages() {
        float[][] hidden = {{1f, 2f}, {3f, 4f}, {5f, 6f}};
        float[] result = OnnxEmbeddingEngine.meanPool(hidden);
        assertEquals(3.0f, result[0], 1e-6);
        assertEquals(4.0f, result[1], 1e-6);
    }

    @Test
    void l2Normalise_producesUnitVector() {
        float[] result = OnnxEmbeddingEngine.l2Normalise(new float[]{3f, 4f});
        float norm = (float) Math.sqrt(result[0] * result[0] + result[1] * result[1]);
        assertEquals(1.0f, norm, 1e-6);
        assertEquals(0.6f, result[0], 1e-6);
        assertEquals(0.8f, result[1], 1e-6);
    }

    @Test
    void l2Normalise_handlesZeroVector() {
        float[] result = OnnxEmbeddingEngine.l2Normalise(new float[]{0f, 0f, 0f});
        for (float v : result) {
            assertTrue(v == 0f);
        }
    }
}
