/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.dynamic;

import io.github.argonizer.prooopt.model.ModelTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicFunctionCacheTest {

    @AfterEach
    void cleanup() {
        DynamicFunctionCache.clear();
    }

    private static DynamicPromptFunction fn(String name) {
        return new DynamicPromptFunction(name, "{input}", ModelTier.LOCAL, "desc", "trace", 0L);
    }

    @Test
    void registerAndFind() {
        DynamicFunctionCache.register(fn("validateSwift"));
        assertTrue(DynamicFunctionCache.contains("validateSwift"));
        assertEquals("validateSwift", DynamicFunctionCache.find("validateSwift").orElseThrow().name());
        assertEquals(1, DynamicFunctionCache.count());
    }

    @Test
    void clearRemovesAll() {
        DynamicFunctionCache.register(fn("a"));
        DynamicFunctionCache.clear();
        assertEquals(0, DynamicFunctionCache.count());
        assertFalse(DynamicFunctionCache.contains("a"));
    }

    @Test
    void cacheIsThreadIsolated() throws Exception {
        // Register in the main thread; a worker thread must NOT see it (ThreadLocal isolation).
        DynamicFunctionCache.register(fn("mainOnly"));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger leaked = new AtomicInteger();
        AtomicInteger isolatedOk = new AtomicInteger();
        try {
            Future<?>[] futures = new Future<?>[50];
            for (int i = 0; i < futures.length; i++) {
                final int id = i;
                futures[i] = pool.submit(() -> {
                    if (DynamicFunctionCache.contains("mainOnly")) {
                        leaked.incrementAndGet();
                    }
                    DynamicFunctionCache.register(fn("worker" + id));
                    // Each worker sees exactly its own one entry, never others'.
                    if (DynamicFunctionCache.count() == 1
                            && DynamicFunctionCache.contains("worker" + id)) {
                        isolatedOk.incrementAndGet();
                    }
                    DynamicFunctionCache.clear();
                });
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertEquals(0, leaked.get(), "worker threads must not see the main thread's registration");
        assertEquals(50, isolatedOk.get(), "each worker must see only its own registration");
        assertTrue(DynamicFunctionCache.contains("mainOnly"), "main thread's cache survives workers");
    }
}
