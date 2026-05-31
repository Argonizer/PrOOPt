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

/**
 * The runtime view of a {@code @PromptOrchestrator}: its system prompt and execution preferences,
 * plus the optional {@link BaseOrchestrator} instance whose lifecycle hooks should fire. Decouples the
 * orchestration engine from the annotation so it can also be driven programmatically or in tests.
 */
public record OrchestratorSpec(String systemPrompt, boolean parallel, int maxThreads,
                               BaseOrchestrator hooks) {

    /** Builds a spec from an annotation and (optionally) the orchestrator bean providing hooks. */
    public static OrchestratorSpec from(PromptOrchestrator annotation, BaseOrchestrator hooks) {
        return new OrchestratorSpec(annotation.prompt(), annotation.parallel(),
                annotation.maxThreads(), hooks);
    }

    /** A minimal spec with just a system prompt, sequential execution, and no hooks. */
    public static OrchestratorSpec of(String systemPrompt) {
        return new OrchestratorSpec(systemPrompt, false, -1, null);
    }
}
