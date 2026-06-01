/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.audit;

import io.github.argonizer.prooopt.model.PlanMode;

/**
 * The performance-diagnostic payload of the enhanced {@code [ORCHESTRATOR][SUMMARY]} line. The audit
 * log doubles as the profiler: {@code cloudCalls == 0} on a STATIC warm hit is visible proof of the
 * plan-caching optimisation.
 */
public record RunMetrics(
        String traceId,
        PlanMode mode,
        boolean cached,
        long totalMs,
        long planGenerationMs,
        long localInferenceMs,
        long cloudInferenceMs,
        long codeFunctionsMs,
        long overheadMs,
        int functionsCalled,
        int codeCalls,
        int localCalls,
        int cloudCalls,
        int dynamicGenerated,
        long tokensUsed,
        double estCostUsd) {
}
