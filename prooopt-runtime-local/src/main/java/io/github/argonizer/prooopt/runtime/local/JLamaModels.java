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
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.SafeTensorSupport;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Resolves and loads JLama models. A configured {@code model-path} may be either a local model
 * directory (safetensors) or a HuggingFace model id ({@code owner/name}), which is downloaded on first
 * use into a working directory and cached on disk.
 */
final class JLamaModels {

    private static final Pattern HF_ID = Pattern.compile("[\\w.-]+/[\\w.-]+");
    private static final String WORKING_DIR = "models";

    private JLamaModels() {
    }

    /** Loads a generative model (F32 working memory, Q8 weights). */
    static AbstractModel loadGenerative(String modelPath) {
        return ModelSupport.loadModel(resolveDir(modelPath), DType.F32, DType.I8);
    }

    /** Loads a model in embedding mode for dense vector generation. */
    static AbstractModel loadEmbedding(String modelPath) {
        return ModelSupport.loadEmbeddingModel(resolveDir(modelPath), DType.F32, DType.I8);
    }

    /** Resolves a configured model reference to an on-disk directory, downloading from HF if needed. */
    static File resolveDir(String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            throw new PrOOPtConfigException(
                    "no model-path configured for the LOCAL tier. Set prooopt.models.local.model-path to a "
                            + "local safetensors directory or a HuggingFace model id (owner/name).");
        }
        File local = new File(modelPath);
        if (local.isDirectory()) {
            return local;
        }
        if (HF_ID.matcher(modelPath).matches()) {
            try {
                return SafeTensorSupport.maybeDownloadModel(WORKING_DIR, modelPath);
            } catch (IOException e) {
                throw new PrOOPtConfigException("failed to download local model '" + modelPath + "'", e);
            }
        }
        throw new PrOOPtConfigException(
                "model-path '" + modelPath + "' is neither an existing directory nor a HuggingFace model id "
                        + "(owner/name). JLama loads safetensors models, not single .gguf files.");
    }
}
