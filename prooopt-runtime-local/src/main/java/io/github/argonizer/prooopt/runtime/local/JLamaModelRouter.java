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
import com.github.tjake.jlama.safetensors.prompt.PromptContext;
import io.github.argonizer.prooopt.config.ModelConfig;
import io.github.argonizer.prooopt.config.PrOOPtProperties;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.AbstractModelRouter;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The local runtime's {@link io.github.argonizer.prooopt.router.ModelRouter}: serves the
 * {@link ModelTier#LOCAL} tier with on-device JLama inference, entirely inside the JVM. Models are
 * loaded lazily on first use and cached by model path. Cloud tiers are intentionally unsupported here
 * — combine with the cloud runtime via {@code CompositeModelRouter} to serve all tiers.
 */
public class JLamaModelRouter extends AbstractModelRouter {

    /** Async, de-duplicated model loads keyed by model path; each loads on a virtual thread. */
    private final ConcurrentHashMap<String, CompletableFuture<AbstractModel>> loads =
            new ConcurrentHashMap<>();

    public JLamaModelRouter(PrOOPtProperties properties) {
        super(properties);
    }

    /**
     * Begins loading the configured LOCAL model on a virtual thread (with a trivial warm-up
     * inference) so the first real request does not pay the full cold-start cost. Safe to call at
     * runtime build time; returns immediately. Idempotent per model path.
     */
    public void warmUpAsync() {
        ModelConfig local = properties.forTier(ModelTier.LOCAL);
        if (local != null && local.getModelPath() != null && !local.getModelPath().isBlank()) {
            loadFuture(local.getModelPath());
        }
    }

    private CompletableFuture<AbstractModel> loadFuture(String path) {
        return loads.computeIfAbsent(path, p -> CompletableFuture.supplyAsync(() -> {
            AbstractModel model = JLamaModels.loadGenerative(p);
            try {
                // Trivial warm-up call so weights and buffers are resident before real traffic.
                model.generate(UUID.randomUUID(), PromptContext.of("."), 0.0f, 1, (t, time) -> { });
            } catch (RuntimeException ignored) {
                // A failed warm-up is non-fatal; the next real call will surface any genuine error.
            }
            return model;
        }, runnable -> Thread.ofVirtual().name("prooopt-jlama-load").unstarted(runnable)));
    }

    @Override
    protected Set<ModelTier> supportedTiers() {
        return Set.of(ModelTier.LOCAL);
    }

    @Override
    protected String generate(ModelTier tier, ModelConfig config, String prompt) {
        if (config.getModelPath() == null || config.getModelPath().isBlank()) {
            throw new PrOOPtConfigException("LOCAL tier requires prooopt.models.local.model-path");
        }
        AbstractModel model = loadFuture(config.getModelPath()).join();

        PromptContext context = PromptContext.of(applyThinking(config, prompt));
        Generator.Response response = model.generate(
                UUID.randomUUID(),
                context,
                (float) config.getTemperature(),
                config.getMaxTokens(),
                (token, time) -> { /* token stream ignored; PrOOPt consumes the full response */ });
        return response.responseText;
    }

    /** Tier-level thinking mode nudges the model toward explicit reasoning before its answer. */
    private static String applyThinking(ModelConfig config, String prompt) {
        if (config.isThinking()) {
            return "Think step by step, then give your final answer.\n\n" + prompt;
        }
        return prompt;
    }
}
