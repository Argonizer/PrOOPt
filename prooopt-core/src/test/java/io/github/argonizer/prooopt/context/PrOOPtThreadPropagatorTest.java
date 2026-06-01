/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrOOPtThreadPropagatorTest {

    @AfterEach
    void cleanup() {
        PrOOPtContext.clear();
    }

    @Test
    void concurrentTasksShareParentTraceId() throws Exception {
        String parentTrace = PrOOPtContext.getTraceId();
        Set<String> observed = ConcurrentHashMap.newKeySet();

        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            Future<?>[] futures = new Future<?>[50];
            for (int i = 0; i < futures.length; i++) {
                futures[i] = pool.submit(PrOOPtThreadPropagator.propagate(
                        () -> observed.add(PrOOPtContext.getTraceId())));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertEquals(1, observed.size(), "all 50 tasks should observe exactly one trace id");
        assertTrue(observed.contains(parentTrace), "tasks should inherit the parent trace id");
    }

    @Test
    void callablePropagatesTraceIdAndReturnsValue() throws Exception {
        String parentTrace = PrOOPtContext.getTraceId();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> future = pool.submit(
                    PrOOPtThreadPropagator.propagate(PrOOPtContext::getTraceId));
            assertEquals(parentTrace, future.get());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void workerThreadStateIsClearedAfterTask() throws Exception {
        PrOOPtContext.getTraceId();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // First task sets and clears its trace; reuse the same (pooled) thread for a second probe.
            pool.submit(PrOOPtThreadPropagator.propagate(() -> { })).get();
            Boolean clearedBetweenTasks = pool.submit(() -> PrOOPtContext.getFunctionCount() == 0).get();
            assertTrue(clearedBetweenTasks, "pooled worker must not leak thread-local state between tasks");
        } finally {
            pool.shutdown();
        }
    }
}
