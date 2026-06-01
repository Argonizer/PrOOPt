/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.audit.AuditLogger;
import io.github.argonizer.prooopt.embedding.EmbeddingEngine;
import io.github.argonizer.prooopt.embedding.TfIdfEmbeddingEngine;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanCacheStrategy;
import io.github.argonizer.prooopt.router.ModelRouter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanCacheTest {

    private final AuditLogger audit = new AuditLogger();

    private static ExecutionPlan planWith(String function) {
        ExecutionStep step = new ExecutionStep(1, function, null, null,
                Map.of("text", "${userInput}"), List.of(), "$out");
        return new ExecutionPlan("t", List.of(step), "$out");
    }

    private EmbeddingEngine fittedEngine() {
        TfIdfEmbeddingEngine engine = new TfIdfEmbeddingEngine();
        engine.fit(List.of(
                "analyze this loan application for approval",
                "review the contract for risk and liability",
                "summarize the meeting notes into bullet points"));
        return engine;
    }

    // ---- EXACT ----

    @Test
    void exactHitsOnIdenticalInput() {
        PlanCache cache = new PlanCache(PlanCacheStrategy.EXACT, 3600, 500, 0.85, null, null, audit);
        cache.put("Analyze loan 123", planWith("analyze"));
        assertTrue(cache.get("Analyze loan 123").isPresent());
        assertTrue(cache.get("  Analyze loan 123  ").isPresent(), "input is trimmed before hashing");
    }

    @Test
    void exactMissesOnDifferentInput() {
        PlanCache cache = new PlanCache(PlanCacheStrategy.EXACT, 3600, 500, 0.85, null, null, audit);
        cache.put("Analyze loan 123", planWith("analyze"));
        assertFalse(cache.get("Analyze loan 999").isPresent());
    }

    // ---- SEMANTIC ----

    @Test
    void semanticHitsOnSimilarInput() {
        PlanCache cache = new PlanCache(PlanCacheStrategy.SEMANTIC, 3600, 500, 0.50,
                fittedEngine(), null, audit);
        cache.put("analyze this loan application for approval", planWith("analyze"));
        assertTrue(cache.get("analyze this loan application").isPresent(),
                "structurally similar input should reuse the cached plan");
    }

    @Test
    void semanticMissesOnDissimilarInput() {
        PlanCache cache = new PlanCache(PlanCacheStrategy.SEMANTIC, 3600, 500, 0.50,
                fittedEngine(), null, audit);
        cache.put("analyze this loan application for approval", planWith("analyze"));
        assertFalse(cache.get("summarize the meeting notes into bullet points").isPresent());
    }

    // ---- INTENT ----

    @Test
    void intentHitsWhenClassifierReturnsSameCategory() {
        ModelRouter classifier = (prompt, tier) -> "loan_application_analysis";
        PlanCache cache = new PlanCache(PlanCacheStrategy.INTENT, 3600, 500, 0.85,
                null, classifier, audit);
        cache.put("Analyze loan 1", planWith("analyze"));
        assertTrue(cache.get("Analyze a totally different loan 2").isPresent(),
                "same classified intent reuses the plan regardless of literal input");
    }

    // ---- TTL ----

    @Test
    void expiredEntryIsAMiss() throws InterruptedException {
        PlanCache cache = new PlanCache(PlanCacheStrategy.EXACT, 0, 500, 0.85, null, null, audit);
        cache.put("x", planWith("f"));
        Thread.sleep(5);
        assertFalse(cache.get("x").isPresent(), "a zero-second TTL expires immediately");
    }

    // ---- invalidation ----

    @Test
    void globalInvalidationClearsEverything() {
        PlanCache cache = new PlanCache(PlanCacheStrategy.EXACT, 3600, 500, 0.85, null, null, audit);
        cache.put("a", planWith("f"));
        cache.put("b", planWith("g"));
        cache.invalidate();
        assertEquals(0, cache.size());
    }

    @Test
    void selectiveInvalidationRemovesOnlyReferencingPlans() {
        PlanCache cache = new PlanCache(PlanCacheStrategy.EXACT, 3600, 500, 0.85, null, null, audit);
        cache.put("uses-extract", planWith("extractSigningDate"));
        cache.put("uses-count", planWith("countWords"));

        cache.invalidateFor("extractSigningDate");

        assertEquals(1, cache.size());
        assertFalse(cache.get("uses-extract").isPresent());
        assertTrue(cache.get("uses-count").isPresent());
    }
}
