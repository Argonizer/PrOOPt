/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin.internal;

import java.util.List;
import java.util.Map;

/**
 * Classifies a batch of prompt/return-type pairs as VALID, INVALID, or UNCERTAIN. The production
 * implementation sends a single batched meta-prompt to the local Phi 3.5 model via ONNX Runtime;
 * {@link KeywordSemanticClassifier} provides a deterministic, model-free fallback so the build runs
 * anywhere (and so tests stay hermetic).
 */
public interface SemanticClassifier {

    /** Classifies each item by its position in {@code batch}; the returned map is keyed 1-based. */
    Map<Integer, ClassificationResult> classify(List<PromptMethod> batch);
}
