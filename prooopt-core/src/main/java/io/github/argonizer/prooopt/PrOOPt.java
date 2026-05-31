/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt;

import io.github.argonizer.prooopt.aop.PrOOPtLoggingInterceptor;
import io.github.argonizer.prooopt.audit.AuditLogger;
import io.github.argonizer.prooopt.autobox.PrOOPtAutoBoxer;
import io.github.argonizer.prooopt.config.PrOOPtProperties;
import io.github.argonizer.prooopt.embedding.EmbeddingEngine;
import io.github.argonizer.prooopt.embedding.TfIdfEmbeddingEngine;
import io.github.argonizer.prooopt.embedding.ToolIndexer;
import io.github.argonizer.prooopt.invoke.PromptCallEngine;
import io.github.argonizer.prooopt.model.ToolDescriptor;
import io.github.argonizer.prooopt.orchestrator.PlanExecutor;
import io.github.argonizer.prooopt.orchestrator.TwoPhaseOrchestrator;
import io.github.argonizer.prooopt.registry.FunctionRegistry;
import io.github.argonizer.prooopt.registry.FunctionScanner;
import io.github.argonizer.prooopt.registry.InstanceResolver;
import io.github.argonizer.prooopt.router.ModelRouter;
import io.github.argonizer.prooopt.router.NoRuntimeModelRouter;
import io.github.argonizer.prooopt.stdlib.StandardLibrary;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The plain-Java entry point. A small fluent builder assembles a {@link PrOOPtRuntime} from a model
 * router, an embedding engine, configuration, and the functions to expose — bundling the standard
 * library by default. Spring Boot users get equivalent wiring from auto-configuration.
 *
 * <pre>{@code
 * PrOOPtRuntime prooopt = PrOOPt.builder()
 *     .router(myRouter)
 *     .registerInstance(new LegalAnalyzer())   // @PromptFunction / @CodeFunction holder
 *     .build();
 * Object verdict = prooopt.orchestrate(new LegalAnalyzer(), contractText);
 * }</pre>
 */
public final class PrOOPt {

    private PrOOPt() {
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent assembler for a {@link PrOOPtRuntime}. */
    public static final class Builder {

        private ModelRouter router = new NoRuntimeModelRouter();
        private EmbeddingEngine embeddingEngine = new TfIdfEmbeddingEngine();
        private PrOOPtProperties properties = new PrOOPtProperties();
        private boolean includeStdlib = true;

        private final List<Class<?>> functionClasses = new ArrayList<>();
        private final List<Object> instances = new ArrayList<>();
        private final List<String> packages = new ArrayList<>();

        public Builder router(ModelRouter router) {
            this.router = router;
            return this;
        }

        public Builder embeddingEngine(EmbeddingEngine embeddingEngine) {
            this.embeddingEngine = embeddingEngine;
            return this;
        }

        public Builder properties(PrOOPtProperties properties) {
            this.properties = properties;
            return this;
        }

        public Builder includeStdlib(boolean includeStdlib) {
            this.includeStdlib = includeStdlib;
            return this;
        }

        /** Registers function-holder classes (their static functions, and instance functions if poolable). */
        public Builder register(Class<?>... classes) {
            for (Class<?> c : classes) {
                functionClasses.add(c);
            }
            return this;
        }

        /** Registers pre-built beans; their instance functions bind to exactly these objects. */
        public Builder registerInstance(Object... beans) {
            for (Object bean : beans) {
                instances.add(bean);
            }
            return this;
        }

        /** Enables classpath scanning of the given packages (used by {@code @PromptFunctionScan}). */
        public Builder scanPackages(String... basePackages) {
            for (String p : basePackages) {
                packages.add(p);
            }
            return this;
        }

        public PrOOPtRuntime build() {
            AuditLogger audit = new AuditLogger();
            PrOOPtAutoBoxer autoBoxer = new PrOOPtAutoBoxer();
            PromptCallEngine promptEngine = new PromptCallEngine(router, autoBoxer);

            Map<Class<?>, Object> instanceMap = new IdentityHashMap<>();
            for (Object bean : instances) {
                instanceMap.put(bean.getClass(), bean);
            }
            InstanceResolver resolver = type -> {
                for (Object bean : instances) {
                    if (type.isInstance(bean)) {
                        return bean;
                    }
                }
                return null;
            };

            FunctionRegistry registry = new FunctionRegistry(resolver);
            for (Map.Entry<Class<?>, Object> e : instanceMap.entrySet()) {
                registry.registerInstance(e.getKey(), e.getValue());
            }

            // Collect descriptors from every source, de-duplicated by function name (first wins).
            Map<String, ToolDescriptor> descriptors = new LinkedHashMap<>();
            if (includeStdlib) {
                addAll(descriptors, StandardLibrary.descriptors());
            }
            if (!functionClasses.isEmpty()) {
                addAll(descriptors, FunctionScanner.scan(functionClasses.toArray(new Class<?>[0])));
            }
            for (Object bean : instances) {
                addAll(descriptors, FunctionScanner.scan(bean.getClass()));
            }
            if (!packages.isEmpty()) {
                addAll(descriptors, FunctionScanner.scanPackages(packages.toArray(new String[0])));
            }

            List<ToolDescriptor> all = new ArrayList<>(descriptors.values());
            registry.registerAll(all);

            ToolIndexer indexer = new ToolIndexer(embeddingEngine,
                    properties.getToolSelection().getMinSimilarity(),
                    properties.getToolSelection().getTopK());
            indexer.index(all);

            PrOOPtLoggingInterceptor interceptor = new PrOOPtLoggingInterceptor(promptEngine, audit);
            PlanExecutor executor = new PlanExecutor(registry, promptEngine, audit);
            TwoPhaseOrchestrator orchestrator = new TwoPhaseOrchestrator(router, autoBoxer, indexer,
                    executor, audit, properties.getOrchestration());

            return new PrOOPtRuntime(registry, indexer, interceptor, promptEngine, orchestrator, router);
        }

        private static void addAll(Map<String, ToolDescriptor> sink, List<ToolDescriptor> descriptors) {
            for (ToolDescriptor descriptor : descriptors) {
                sink.putIfAbsent(descriptor.name(), descriptor);
            }
        }
    }
}
