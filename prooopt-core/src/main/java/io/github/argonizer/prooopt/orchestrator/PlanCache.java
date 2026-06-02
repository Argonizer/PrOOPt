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
import io.github.argonizer.prooopt.embedding.VectorMath;
import io.github.argonizer.prooopt.exception.PrOOPtException;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.PlanCacheStrategy;
import io.github.argonizer.prooopt.router.ModelRouter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * An LRU-bounded in-memory cache of execution-plan templates, supporting three lookup strategies
 * (EXACT, SEMANTIC, INTENT) and a TTL. A cache hit lets the orchestrator skip the Cloud LLM plan
 * generation entirely — the headline plan-mode optimisation.
 *
 * <p>Invalidated wholesale on new function registration (cached plans may become suboptimal) or
 * selectively via {@link #invalidateFor(String)} when a single function changes.
 */
public class PlanCache {

    private final PlanCacheStrategy strategy;
    private final long ttlMs;            // -1 = never expire
    private final double similarityThreshold;
    private final EmbeddingEngine embeddingEngine;  // SEMANTIC only
    private final ModelRouter router;               // INTENT only
    private final AuditLogger audit;

    private final Map<String, CachedPlan> store;

    public PlanCache(PlanCacheStrategy strategy, long ttlSeconds, int maxSize, double similarityThreshold,
                     EmbeddingEngine embeddingEngine, ModelRouter router, AuditLogger audit) {
        this.strategy = strategy;
        this.ttlMs = ttlSeconds < 0 ? -1 : ttlSeconds * 1000L;
        this.similarityThreshold = similarityThreshold;
        this.embeddingEngine = embeddingEngine;
        this.router = router;
        this.audit = audit;
        int cap = Math.max(1, maxSize);
        this.store = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedPlan> eldest) {
                return size() > cap;
            }
        });
    }

    /** Stores a plan template. The input derives the cache key per the configured strategy. */
    public void put(String input, ExecutionPlan plan) {
        String safe = input == null ? "" : input.trim();
        switch (strategy) {
            case EXACT -> store.put(sha256(safe), new CachedPlan(plan, null, safe, now()));
            case INTENT -> store.put(classifyIntent(safe), new CachedPlan(plan, null, safe, now()));
            case SEMANTIC -> {
                float[] embedding = embeddingEngine.embed(safe);
                store.put(UUID.randomUUID().toString(), new CachedPlan(plan, embedding, safe, now()));
            }
        }
        audit.planCacheStored(plan.allSteps().size(), ttlMs < 0 ? -1 : ttlMs / 1000L);
    }

    /** Retrieves a matching template, or {@link Optional#empty()} on miss or TTL expiry. */
    public Optional<ExecutionPlan> get(String input) {
        String safe = input == null ? "" : input.trim();
        Optional<ExecutionPlan> result = switch (strategy) {
            case EXACT -> direct(sha256(safe));
            case INTENT -> direct(classifyIntent(safe));
            case SEMANTIC -> nearest(safe);
        };
        if (result.isEmpty()) {
            audit.planCacheMiss(strategy);
        }
        return result;
    }

    /** Invalidates all cached plans. */
    public void invalidate() {
        int cleared;
        synchronized (store) {
            cleared = store.size();
            store.clear();
        }
        audit.planCacheInvalidated(cleared, "manual_or_new_function");
    }

    /** Removes all plans that reference {@code functionName} in any step. */
    public void invalidateFor(String functionName) {
        int cleared = 0;
        synchronized (store) {
            Iterator<Map.Entry<String, CachedPlan>> it = store.entrySet().iterator();
            while (it.hasNext()) {
                ExecutionPlan plan = it.next().getValue().plan();
                boolean referenced = plan.allSteps().values().stream()
                        .anyMatch(s -> functionName.equals(s.function()));
                if (referenced) {
                    it.remove();
                    cleared++;
                }
            }
        }
        if (cleared > 0) {
            audit.planCacheInvalidated(cleared, "function_changed:" + functionName);
        }
    }

    public int size() {
        return store.size();
    }

    // ------------------------------------------------------------------ strategy internals

    private Optional<ExecutionPlan> direct(String key) {
        CachedPlan cached = store.get(key);
        if (cached == null || expired(cached)) {
            if (cached != null) {
                store.remove(key);
            }
            return Optional.empty();
        }
        audit.planCacheHit(truncateKey(cached.originalInput()), 1.0);
        return Optional.of(cached.plan());
    }

    private Optional<ExecutionPlan> nearest(String input) {
        float[] query = embeddingEngine.embed(input);
        CachedPlan best = null;
        double bestScore = similarityThreshold;
        synchronized (store) {
            Iterator<Map.Entry<String, CachedPlan>> it = store.entrySet().iterator();
            while (it.hasNext()) {
                CachedPlan cached = it.next().getValue();
                if (expired(cached)) {
                    it.remove();
                    continue;
                }
                double score = VectorMath.cosineSimilarity(query, cached.embedding());
                if (score >= bestScore) {
                    bestScore = score;
                    best = cached;
                }
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        audit.planCacheHit(truncateKey(best.originalInput()), bestScore);
        return Optional.of(best.plan());
    }

    private String classifyIntent(String input) {
        String prompt = """
                Classify the following request into a single short snake_case intent category
                (for example loan_application_analysis or contract_risk_review). Respond with the
                category only — no preamble.

                Request:
                """ + input;
        try {
            String intent = router.route(prompt, ModelTier.LOCAL);
            return intent == null ? "unknown" : intent.trim().toLowerCase().replaceAll("[^a-z0-9_]+", "_");
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    private boolean expired(CachedPlan cached) {
        return ttlMs >= 0 && (now() - cached.cachedAtMs()) > ttlMs;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static String truncateKey(String s) {
        if (s == null) {
            return "";
        }
        String trimmed = s.replaceAll("\\s+", " ").trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 40) + "…" : trimmed;
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new PrOOPtException("SHA-256 unavailable for plan cache key derivation", e);
        }
    }
}
