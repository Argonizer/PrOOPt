/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.runtime;

import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

/**
 * Configuration for the ONNX-backed LOCAL tier, bound from {@code application.yml} under
 * {@code prooopt.models.local}. Consumed by {@link OnnxModelLoader} and {@link OnnxEmbeddingEngine}.
 *
 * <p>Download Phi-3.5 Mini ONNX (recommended default):
 * <pre>
 * huggingface-cli download microsoft/Phi-3.5-mini-instruct-onnx \
 *   --include "cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/*" \
 *   --local-dir models/phi-3.5-mini-onnx
 * </pre>
 *
 * Required files after download:
 * <pre>
 *   models/phi-3.5-mini-onnx/model.onnx
 *   models/phi-3.5-mini-onnx/model.onnx.data   (may be split — required)
 *   models/phi-3.5-mini-onnx/tokenizer.json
 *   models/phi-3.5-mini-onnx/generation_config.json
 * </pre>
 *
 * Lighter alternatives:
 * <pre>
 *   Qwen/Qwen2.5-0.5B-Instruct  (~400MB, fastest)
 *   Qwen/Qwen2.5-1.5B-Instruct  (~900MB, balanced)
 * </pre>
 */
@ConfigurationProperties(prefix = "prooopt.models.local")
public class LocalModelConfig {

    /** Engine selector — {@code "onnx"} activates this module. */
    private String engine = "onnx";

    /** Path to the {@code model.onnx} file. */
    private String modelPath;

    /** Path to the {@code tokenizer.json} file. */
    private String tokenizerPath;

    /** Path to {@code generation_config.json} (optional; defaults used when absent). */
    private String generationConfigPath;

    /** Whether to wrap the prompt in a "think step by step" system template. */
    private boolean thinking = false;

    /** Max new tokens to generate. */
    private int maxTokens = 1024;

    /** Sampling temperature (reserved; v0.1.0 uses greedy decoding). */
    private double temperature = 0.7;

    /** Per-inference timeout in milliseconds. */
    private long timeoutMs = 30_000;

    /** ONNX intra-op thread count. Defaults to the number of available processors. */
    private int intraThroughputThreads = Runtime.getRuntime().availableProcessors();

    /**
     * Validates the configuration. Throws if {@code modelPath} or {@code tokenizerPath} is null,
     * blank, or points to a file that does not exist on disk.
     */
    public void validate() {
        if (modelPath == null || modelPath.isBlank()) {
            throw new PrOOPtConfigException(
                    "prooopt.models.local.model-path must be set to the path of a model.onnx file");
        }
        if (tokenizerPath == null || tokenizerPath.isBlank()) {
            throw new PrOOPtConfigException(
                    "prooopt.models.local.tokenizer-path must be set to the path of a tokenizer.json file");
        }
        if (!new File(modelPath).isFile()) {
            throw new PrOOPtConfigException(
                    "ONNX model file does not exist: " + modelPath
                            + ". Download it from HuggingFace (e.g. microsoft/Phi-3.5-mini-instruct-onnx).");
        }
        if (!new File(tokenizerPath).isFile()) {
            throw new PrOOPtConfigException("tokenizer.json file does not exist: " + tokenizerPath);
        }
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getTokenizerPath() {
        return tokenizerPath;
    }

    public void setTokenizerPath(String tokenizerPath) {
        this.tokenizerPath = tokenizerPath;
    }

    public String getGenerationConfigPath() {
        return generationConfigPath;
    }

    public void setGenerationConfigPath(String generationConfigPath) {
        this.generationConfigPath = generationConfigPath;
    }

    public boolean isThinking() {
        return thinking;
    }

    public void setThinking(boolean thinking) {
        this.thinking = thinking;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getIntraThroughputThreads() {
        return intraThroughputThreads;
    }

    public void setIntraThroughputThreads(int intraThroughputThreads) {
        this.intraThroughputThreads = intraThroughputThreads;
    }
}
