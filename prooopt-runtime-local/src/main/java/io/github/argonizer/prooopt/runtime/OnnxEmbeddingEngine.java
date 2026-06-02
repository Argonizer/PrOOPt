/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.runtime;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.github.argonizer.prooopt.embedding.EmbeddingEngine;
import io.github.argonizer.prooopt.exception.PrOOPtExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.LongBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Produces dense embeddings from the same ONNX model session held by {@link OnnxModelLoader} — it
 * never opens a second session. Embeddings are mean-pooled over the sequence dimension and
 * L2-normalised, then cached in a bounded LRU (max 2000 entries) keyed by input text.
 *
 * <p>Used by {@code ToolIndexer} for semantic tool selection.
 */
public class OnnxEmbeddingEngine implements EmbeddingEngine {

    private static final Logger log = LogManager.getLogger("io.github.argonizer.prooopt.audit");

    private final OnnxModelLoader loader;
    private final Map<String, float[]> cache;

    public OnnxEmbeddingEngine(OnnxModelLoader loader) {
        this.loader = loader;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                return size() > 2000;
            }
        });
    }

    @Override
    public float[] embed(String text) {
        String key = text == null ? "" : text;
        long start = System.currentTimeMillis();
        log.info("[PROOOPT][ONNX][EMBED_START] textLength={}", key.length());

        float[] cached = cache.get(key);
        if (cached != null) {
            log.info("[PROOOPT][ONNX][EMBED_END] dimensions={} cached=true duration={}ms",
                    cached.length, System.currentTimeMillis() - start);
            return cached;
        }

        loader.awaitReady();
        Encoding encoding = loader.getTokenizer().encode(key, true, false);
        long[] inputIds = OnnxModelLoader.toLongArray(encoding.getIds());

        float[][][] hiddenState = forward(inputIds);
        float[] embedding = l2Normalise(meanPool(hiddenState[0]));

        cache.put(key, embedding);
        log.info("[PROOOPT][ONNX][EMBED_END] dimensions={} cached=false duration={}ms",
                embedding.length, System.currentTimeMillis() - start);
        return embedding;
    }

    /**
     * Runs the model's forward pass for {@code inputIds} and returns the {@code last_hidden_state}
     * tensor of shape {@code [batch=1, seqLen, hiddenDim]}. Package-private so tests can supply a
     * canned tensor without an actual ONNX model.
     */
    float[][][] forward(long[] inputIds) {
        long[] shape = {1L, inputIds.length};
        OrtSession.Result result = null;
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                     loader.getEnv(), LongBuffer.wrap(inputIds), shape);
             OnnxTensor maskTensor = OnnxTensor.createTensor(
                     loader.getEnv(), LongBuffer.wrap(OnnxModelLoader.onesLong(inputIds.length)), shape)) {

            Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
            inputs.put("input_ids", inputTensor);
            inputs.put("attention_mask", maskTensor);

            result = loader.getSession().run(inputs);
            return (float[][][]) result.get(0).getValue();
        } catch (OrtException e) {
            throw new PrOOPtExecutionException("ONNX", "embed", "ONNX embedding forward pass failed", e);
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    /** Averages a {@code [seqLen][hiddenDim]} matrix across the sequence dimension. */
    static float[] meanPool(float[][] hidden) {
        int seqLen = hidden.length;
        int hiddenDim = hidden[0].length;
        float[] pooled = new float[hiddenDim];
        for (float[] token : hidden) {
            for (int j = 0; j < hiddenDim; j++) {
                pooled[j] += token[j];
            }
        }
        for (int j = 0; j < hiddenDim; j++) {
            pooled[j] /= seqLen;
        }
        return pooled;
    }

    /** L2-normalises a vector. Returns the vector unchanged when its norm is effectively zero. */
    static float[] l2Normalise(float[] vector) {
        double sumSquares = 0.0;
        for (float v : vector) {
            sumSquares += (double) v * v;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm < 1e-12) {
            return vector;
        }
        float[] out = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            out[i] = (float) (vector[i] / norm);
        }
        return out;
    }
}
