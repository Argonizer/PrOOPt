/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.runtime;

import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.ModelRouter;

/**
 * The local runtime's {@link ModelRouter}: serves the {@link ModelTier#LOCAL} tier via on-device
 * ONNX inference, entirely inside the JVM. Cloud tiers are intentionally unsupported here — add
 * {@code prooopt-runtime-cloud} (and compose the routers) to serve {@code CLOUD_FAST} /
 * {@code CLOUD_ADVANCED}.
 */
public class OnnxModelRouter implements ModelRouter {

    private final OnnxModelLoader loader;

    public OnnxModelRouter(OnnxModelLoader loader) {
        this.loader = loader;
    }

    @Override
    public boolean supports(ModelTier tier) {
        return tier == ModelTier.LOCAL;
    }

    @Override
    public String route(String prompt, ModelTier tier) {
        if (!supports(tier)) {
            throw new PrOOPtConfigException(
                    "OnnxModelRouter only handles ModelTier.LOCAL. "
                            + "Add prooopt-runtime-cloud for CLOUD_FAST / CLOUD_ADVANCED.");
        }
        return loader.infer(prompt);
    }
}
