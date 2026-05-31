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
import java.util.concurrent.ConcurrentHashMap;

/**
 * The local runtime's {@link io.github.argonizer.prooopt.router.ModelRouter}: serves the
 * {@link ModelTier#LOCAL} tier with on-device JLama inference, entirely inside the JVM. Models are
 * loaded lazily on first use and cached by model path. Cloud tiers are intentionally unsupported here
 * — combine with the cloud runtime via {@code CompositeModelRouter} to serve all tiers.
 */
public class JLamaModelRouter extends AbstractModelRouter {

    private final ConcurrentHashMap<String, AbstractModel> models = new ConcurrentHashMap<>();

    public JLamaModelRouter(PrOOPtProperties properties) {
        super(properties);
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
        AbstractModel model = models.computeIfAbsent(config.getModelPath(),
                path -> JLamaModels.loadGenerative(path));

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
