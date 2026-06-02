/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.runtime;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the ONNX LOCAL runtime. Active only when
 * {@code prooopt.models.local.engine=onnx} is set and this module is on the classpath. Wires the
 * loader (started asynchronously), the embedding engine, and the model router.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "prooopt.models.local", name = "engine", havingValue = "onnx")
@EnableConfigurationProperties(LocalModelConfig.class)
public class OnnxLocalAutoConfiguration {

    @Bean
    public OnnxModelLoader onnxModelLoader(LocalModelConfig config) {
        config.validate();
        OnnxModelLoader loader = new OnnxModelLoader(config);
        loader.loadAsync();
        return loader;
    }

    @Bean
    public OnnxEmbeddingEngine onnxEmbeddingEngine(OnnxModelLoader loader) {
        return new OnnxEmbeddingEngine(loader);
    }

    @Bean
    public OnnxModelRouter onnxModelRouter(OnnxModelLoader loader) {
        return new OnnxModelRouter(loader);
    }
}
