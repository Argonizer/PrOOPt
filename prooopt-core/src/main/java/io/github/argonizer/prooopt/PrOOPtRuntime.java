/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt;

import io.github.argonizer.prooopt.annotation.PromptOrchestrator;
import io.github.argonizer.prooopt.aop.PrOOPtLoggingInterceptor;
import io.github.argonizer.prooopt.embedding.ToolIndexer;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.invoke.PromptCallEngine;
import io.github.argonizer.prooopt.orchestrator.BaseOrchestrator;
import io.github.argonizer.prooopt.orchestrator.OrchestratorSpec;
import io.github.argonizer.prooopt.orchestrator.TwoPhaseOrchestrator;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.registry.FunctionRegistry;
import io.github.argonizer.prooopt.router.ModelRouter;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A fully wired PrOOPt instance for plain-Java use: registry, semantic index, AOP interceptor, shared
 * call engine, and the two-phase orchestrator, assembled by {@link PrOOPt#builder()}. Spring Boot
 * users get the same wiring from auto-configuration instead.
 */
public final class PrOOPtRuntime {

    private final FunctionRegistry registry;
    private final ToolIndexer indexer;
    private final PrOOPtLoggingInterceptor interceptor;
    private final PromptCallEngine promptEngine;
    private final TwoPhaseOrchestrator orchestrator;
    private final ModelRouter router;

    /** Cloud calls are blocking I/O — virtual threads eliminate OS thread waste. */
    private final ExecutorService cloudExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /** LOCAL inference is CPU-bound — platform threads only. */
    private final ExecutorService localExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /** Optional user-supplied pool (from {@code builder().threadPool(...)}); closed on shutdown. */
    private final ExecutorService userThreadPool;

    PrOOPtRuntime(FunctionRegistry registry, ToolIndexer indexer, PrOOPtLoggingInterceptor interceptor,
                  PromptCallEngine promptEngine, TwoPhaseOrchestrator orchestrator, ModelRouter router,
                  ExecutorService userThreadPool) {
        this.registry = registry;
        this.indexer = indexer;
        this.interceptor = interceptor;
        this.promptEngine = promptEngine;
        this.orchestrator = orchestrator;
        this.router = router;
        this.userThreadPool = userThreadPool;
    }

    /**
     * Routes a prompt asynchronously on the tier-appropriate pool: a virtual-thread pool for blocking
     * cloud I/O, a bounded platform pool for CPU-bound LOCAL inference.
     */
    public CompletableFuture<String> routeAsync(String prompt, ModelTier tier) {
        ExecutorService pool = tier == ModelTier.LOCAL ? localExecutor : cloudExecutor;
        return CompletableFuture.supplyAsync(() -> router.route(prompt, tier), pool);
    }

    /** The user-supplied executor pool, if one was configured. */
    public ExecutorService threadPool() {
        return userThreadPool;
    }

    public FunctionRegistry registry() {
        return registry;
    }

    public ToolIndexer indexer() {
        return indexer;
    }

    public PrOOPtLoggingInterceptor interceptor() {
        return interceptor;
    }

    public PromptCallEngine promptEngine() {
        return promptEngine;
    }

    public TwoPhaseOrchestrator orchestrator() {
        return orchestrator;
    }

    public ModelRouter router() {
        return router;
    }

    /**
     * Wraps a bean in an AOP proxy so that calling its {@code @PromptFunction}/{@code @CodeFunction}
     * methods directly is intercepted (enriched, routed, autoboxed, audited). Use the returned proxy
     * in {@code PromptStream} chains and direct calls.
     */
    @SuppressWarnings("unchecked")
    public <T> T proxy(T bean) {
        AspectJProxyFactory factory = new AspectJProxyFactory(bean);
        factory.addAspect(interceptor);
        return (T) factory.getProxy();
    }

    /** Runs the two-phase orchestration for an input under an explicit system prompt. */
    public Object orchestrate(String systemPrompt, Object input) {
        return orchestrator.run(input, OrchestratorSpec.of(systemPrompt));
    }

    /**
     * Runs orchestration driven by a {@code @PromptOrchestrator}-annotated bean. If the bean extends
     * {@link BaseOrchestrator}, its lifecycle hooks fire around the run.
     */
    public Object orchestrate(Object orchestratorBean, Object input) {
        PromptOrchestrator annotation = orchestratorBean.getClass().getAnnotation(PromptOrchestrator.class);
        if (annotation == null) {
            throw new PrOOPtConfigException("orchestrator bean " + orchestratorBean.getClass().getName()
                    + " is not annotated with @PromptOrchestrator");
        }
        BaseOrchestrator hooks = orchestratorBean instanceof BaseOrchestrator base ? base : null;
        return orchestrator.run(input, OrchestratorSpec.from(annotation, hooks));
    }

    /** Invalidates every cached execution plan. */
    public void clearPlanCache() {
        orchestrator.clearPlanCache();
    }

    /** Invalidates cached plans referencing a specific function. */
    public void clearPlanCacheFor(String functionName) {
        orchestrator.clearPlanCacheFor(functionName);
    }

    /**
     * Releases runtime resources: the tier-aware executor pools and any user-supplied pool. Always
     * call this when finished with a standalone runtime (for example in a {@code finally} block or a
     * shutdown hook). Thread-local state is cleared internally at the end of every run.
     */
    public void shutdown() {
        cloudExecutor.shutdown();
        localExecutor.shutdown();
        if (userThreadPool != null) {
            userThreadPool.shutdown();
        }
    }
}
