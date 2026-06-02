/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.spring;

import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.aop.PrOOPtLoggingInterceptor;
import io.github.argonizer.prooopt.audit.AuditLogger;
import io.github.argonizer.prooopt.autobox.PrOOPtAutoBoxer;
import io.github.argonizer.prooopt.config.PrOOPtProperties;
import io.github.argonizer.prooopt.embedding.EmbeddingEngine;
import io.github.argonizer.prooopt.embedding.TfIdfEmbeddingEngine;
import io.github.argonizer.prooopt.embedding.ToolIndexer;
import io.github.argonizer.prooopt.invoke.PromptCallEngine;
import io.github.argonizer.prooopt.model.ToolDescriptor;
import io.github.argonizer.prooopt.orchestrator.DagExecutor;
import io.github.argonizer.prooopt.orchestrator.TwoPhaseOrchestrator;
import io.github.argonizer.prooopt.registry.FunctionRegistry;
import io.github.argonizer.prooopt.registry.FunctionScanner;
import io.github.argonizer.prooopt.router.ModelRouter;
import io.github.argonizer.prooopt.router.NoRuntimeModelRouter;
import io.github.argonizer.prooopt.stdlib.StandardLibrary;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional Spring Boot auto-configuration. Conditional on the PrOOPt annotations being on the
 * classpath, it wires the {@link ModelRouter} (a fail-fast {@link NoRuntimeModelRouter} unless a
 * runtime module provides one), the {@link FunctionRegistry}, the {@link ToolIndexer}, the AOP
 * interceptor, and the orchestrator — then, once the context is ready, registers and indexes every
 * {@code @PromptFunction}/{@code @CodeFunction} bean method alongside the standard library.
 *
 * <p>{@code @EnableAspectJAutoProxy} weaves the interceptor so direct calls to annotated bean methods
 * are governed; orchestrated runs are driven by the {@link PlanExecutor}.
 */
@AutoConfiguration
@ConditionalOnClass(PromptFunction.class)
@EnableAspectJAutoProxy
public class PrOOPtAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "prooopt")
    public PrOOPtProperties prooptProperties() {
        return new PrOOPtProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelRouter modelRouter() {
        // No runtime module contributed a router: fail fast with guidance when a model call is made.
        return new NoRuntimeModelRouter();
    }

    @Bean
    @ConditionalOnMissingBean
    public PrOOPtAutoBoxer prooptAutoBoxer() {
        return new PrOOPtAutoBoxer();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogger auditLogger() {
        return new AuditLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmbeddingEngine embeddingEngine() {
        return new TfIdfEmbeddingEngine();
    }

    @Bean
    public PromptCallEngine promptCallEngine(ModelRouter router, PrOOPtAutoBoxer autoBoxer) {
        return new PromptCallEngine(router, autoBoxer);
    }

    @Bean
    public PrOOPtLoggingInterceptor prooptLoggingInterceptor(PromptCallEngine engine, AuditLogger audit) {
        return new PrOOPtLoggingInterceptor(engine, audit);
    }

    @Bean
    public FunctionRegistry functionRegistry(ApplicationContext context) {
        // Instance functions bind to managed beans; PrOOPt never re-creates what Spring owns.
        return new FunctionRegistry(type -> context.getBeanProvider(type).getIfAvailable());
    }

    @Bean
    public ToolIndexer toolIndexer(EmbeddingEngine embeddingEngine, PrOOPtProperties properties) {
        return new ToolIndexer(embeddingEngine,
                properties.getToolSelection().getMinSimilarity(),
                properties.getToolSelection().getTopK());
    }

    @Bean
    public DagExecutor dagExecutor(FunctionRegistry registry, PromptCallEngine engine, AuditLogger audit) {
        java.util.concurrent.ExecutorService cloudExec =
                io.github.argonizer.prooopt.context.PrOOPtExecutors.newCloudExecutor();
        java.util.concurrent.ExecutorService localExec =
                io.github.argonizer.prooopt.context.PrOOPtExecutors.newLocalExecutor();
        return new DagExecutor(registry, engine, audit, cloudExec, localExec, false);
    }

    @Bean
    public TwoPhaseOrchestrator twoPhaseOrchestrator(ModelRouter router, PrOOPtAutoBoxer autoBoxer,
                                                     ToolIndexer indexer, DagExecutor executor,
                                                     AuditLogger audit, PrOOPtProperties properties,
                                                     EmbeddingEngine embeddingEngine) {
        return new TwoPhaseOrchestrator(router, autoBoxer, indexer, executor, audit,
                properties.getOrchestration(), embeddingEngine);
    }

    /**
     * After the context is built, discover every bean method annotated with {@code @PromptFunction} /
     * {@code @CodeFunction}, register it, and (with the standard library) build the semantic index.
     */
    @Bean
    public SmartInitializingSingleton prooptFunctionRegistrar(ApplicationContext context,
                                                              FunctionRegistry registry,
                                                              ToolIndexer indexer) {
        return () -> {
            Map<String, ToolDescriptor> descriptors = new LinkedHashMap<>();
            for (ToolDescriptor descriptor : StandardLibrary.descriptors()) {
                descriptors.putIfAbsent(descriptor.name(), descriptor);
            }
            for (String beanName : context.getBeanDefinitionNames()) {
                Class<?> type = context.getType(beanName);
                if (type == null) {
                    continue;
                }
                for (ToolDescriptor descriptor : FunctionScanner.scan(ClassUtils.getUserClass(type))) {
                    descriptors.putIfAbsent(descriptor.name(), descriptor);
                }
            }
            List<ToolDescriptor> all = new ArrayList<>(descriptors.values());
            registry.registerAll(all);
            indexer.index(all);
        };
    }
}
