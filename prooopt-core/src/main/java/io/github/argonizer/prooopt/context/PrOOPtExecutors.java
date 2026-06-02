/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Factory methods for the executors PrOOPt uses internally. PrOOPt targets Java 17, so cloud I/O runs
 * on an elastic pool of daemon <em>platform</em> threads rather than virtual threads: cloud calls are
 * blocking and bursty, so a cached pool grows on demand and lets idle threads die back.
 */
public final class PrOOPtExecutors {

    private PrOOPtExecutors() {
    }

    /**
     * An elastic, cached pool of daemon platform threads for blocking cloud I/O. Threads are daemons
     * so a lingering pool never blocks JVM shutdown.
     */
    public static ExecutorService newCloudExecutor() {
        return Executors.newCachedThreadPool(daemonThreadFactory("prooopt-cloud-"));
    }

    /** A bounded pool of daemon platform threads sized to the host, for CPU-bound LOCAL inference. */
    public static ExecutorService newLocalExecutor() {
        return Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(), daemonThreadFactory("prooopt-local-"));
    }

    /** A daemon thread factory with sequential, prefixed names for readable audit/thread dumps. */
    public static ThreadFactory daemonThreadFactory(String prefix) {
        AtomicLong counter = new AtomicLong();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
