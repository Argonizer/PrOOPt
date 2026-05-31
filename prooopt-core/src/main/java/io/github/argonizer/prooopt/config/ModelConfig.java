/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.config;

import io.github.argonizer.prooopt.model.ModelTier;

/**
 * Configuration for a single model tier. A mutable JavaBean so both Spring's relaxed
 * {@code @ConfigurationProperties} binding and the plain-Java {@code PrOOPtConfigLoader} can populate
 * it. Not every field applies to every tier: {@code modelPath}/{@code thinking} are for the local
 * engine, {@code provider}/{@code modelId}/{@code apiKey} for cloud, and {@code strategy}/
 * {@code tokenThreshold}/{@code fallback} for the AUTO router.
 */
public class ModelConfig {

    /** Engine that backs this tier: {@code jlama}, {@code anthropic}, or {@code openai}. */
    private String engine;

    /** Cloud provider name (alias for/companion to {@link #engine} for cloud tiers). */
    private String provider;

    /** Provider-specific model identifier, for example {@code claude-haiku-4-5}. */
    private String modelId;

    /** Filesystem path (or model reference) for the local engine. */
    private String modelPath;

    /** API credential; in YAML this should be a {@code ${ENV_VAR}} placeholder, never a literal. */
    private String apiKey;

    /** Enables local thinking mode. */
    private boolean thinking;

    private int maxTokens = 1024;
    private double temperature = 0.7;
    private long timeoutMs = 30_000L;

    // ---- AUTO-tier routing fields ----

    /** AUTO strategy, for example {@code complexity-heuristic}. */
    private String strategy;

    /** Token count above which AUTO escalates beyond the cheap tier. */
    private int tokenThreshold = 500;

    /** Tier AUTO falls back to when its primary choice is unavailable. */
    private ModelTier fallback;

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public int getTokenThreshold() {
        return tokenThreshold;
    }

    public void setTokenThreshold(int tokenThreshold) {
        this.tokenThreshold = tokenThreshold;
    }

    public ModelTier getFallback() {
        return fallback;
    }

    public void setFallback(ModelTier fallback) {
        this.fallback = fallback;
    }

    /** The effective engine name: {@link #engine} when set, otherwise {@link #provider}. */
    public String resolvedEngine() {
        if (engine != null && !engine.isBlank()) {
            return engine.trim().toLowerCase();
        }
        return provider == null ? null : provider.trim().toLowerCase();
    }
}
