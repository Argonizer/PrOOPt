/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.annotation.PromptOrchestrator;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanCacheStrategy;
import io.github.argonizer.prooopt.model.PlanMode;

/**
 * The runtime view of a {@code @PromptOrchestrator}: its system prompt, execution preferences, dynamic
 * function policy, and plan-cache settings, plus the optional {@link BaseOrchestrator} whose lifecycle
 * hooks should fire. Decouples the orchestration engine from the annotation so it can also be driven
 * programmatically or in tests.
 */
public record OrchestratorSpec(
        String systemPrompt,
        boolean parallel,
        int maxThreads,
        BaseOrchestrator hooks,
        boolean allowDynamic,
        int maxDynamicFunctions,
        ModelTier dynamicFunctionModel,
        PlanMode planMode,
        PlanCacheStrategy planCacheStrategy,
        long planCacheTtl,
        int planCacheSize,
        double planCacheSimilarityThreshold) {

    /** Builds a spec from an annotation and (optionally) the orchestrator bean providing hooks. */
    public static OrchestratorSpec from(PromptOrchestrator annotation, BaseOrchestrator hooks) {
        return new OrchestratorSpec(annotation.prompt(), annotation.parallel(), annotation.maxThreads(),
                hooks, annotation.allowDynamic(), annotation.maxDynamicFunctions(),
                annotation.dynamicFunctionModel(), annotation.planMode(), annotation.planCacheStrategy(),
                annotation.planCacheTtl(), annotation.planCacheSize(),
                annotation.planCacheSimilarityThreshold());
    }

    /** A minimal spec with just a system prompt: sequential, STATIC plan mode, no dynamic functions. */
    public static OrchestratorSpec of(String systemPrompt) {
        return new OrchestratorSpec(systemPrompt, false, -1, null, false, 3, ModelTier.CLOUD_FAST,
                PlanMode.STATIC, PlanCacheStrategy.SEMANTIC, 3600, 500, 0.85);
    }
}
