/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.runtime.local;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.functions.Generator;
import io.github.argonizer.prooopt.embedding.EmbeddingEngine;

/**
 * A dense {@link EmbeddingEngine} backed by a local JLama model in embedding mode — the bundled model
 * reused for semantic tool selection rather than adding a second model. Produces real sentence
 * embeddings (mean-pooled), a step up in semantic quality from TF-IDF at the cost of loading a model.
 *
 * <p>The model is loaded lazily on first {@link #embed} so constructing the engine is cheap.
 */
public class LocalModelEmbeddingEngine implements EmbeddingEngine {

    private final String modelPath;
    private final Generator.PoolingType poolingType;
    private volatile AbstractModel model;

    public LocalModelEmbeddingEngine(String modelPath) {
        this(modelPath, Generator.PoolingType.AVG);
    }

    public LocalModelEmbeddingEngine(String modelPath, Generator.PoolingType poolingType) {
        this.modelPath = modelPath;
        this.poolingType = poolingType;
    }

    @Override
    public float[] embed(String text) {
        return model().embed(text == null ? "" : text, poolingType);
    }

    private AbstractModel model() {
        AbstractModel local = model;
        if (local == null) {
            synchronized (this) {
                local = model;
                if (local == null) {
                    local = JLamaModels.loadEmbedding(modelPath);
                    model = local;
                }
            }
        }
        return local;
    }
}
