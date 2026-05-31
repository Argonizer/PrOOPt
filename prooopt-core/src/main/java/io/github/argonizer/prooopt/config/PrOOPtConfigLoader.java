/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.config;

import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.ModelTier;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Plain-Java configuration loader for environments without Spring Boot. Parses {@code application.yml}
 * or {@code application.properties} from the classpath into a {@link PrOOPtProperties}, tolerating
 * kebab-, camel-, and snake-case keys, and resolving {@code ${ENV_VAR}} credentials through
 * {@link CredentialResolver}.
 */
public class PrOOPtConfigLoader {

    private static final String ROOT = "prooopt";

    private final CredentialResolver credentials;

    public PrOOPtConfigLoader() {
        this(new CredentialResolver());
    }

    public PrOOPtConfigLoader(CredentialResolver credentials) {
        this.credentials = credentials;
    }

    /**
     * Loads configuration from the classpath, trying {@code application.yml}, {@code application.yaml},
     * then {@code application.properties}. Returns defaults when none is found.
     */
    public PrOOPtProperties load() {
        for (String name : new String[] {"application.yml", "application.yaml"}) {
            InputStream in = classpath(name);
            if (in != null) {
                return loadYaml(in);
            }
        }
        InputStream props = classpath("application.properties");
        if (props != null) {
            return loadProperties(props);
        }
        return new PrOOPtProperties();
    }

    /** Loads from a specific classpath resource, choosing the parser by extension. */
    public PrOOPtProperties load(String classpathResource) {
        InputStream in = classpath(classpathResource);
        if (in == null) {
            throw new PrOOPtConfigException("config resource not found on classpath: " + classpathResource);
        }
        return classpathResource.endsWith(".properties") ? loadProperties(in) : loadYaml(in);
    }

    @SuppressWarnings("unchecked")
    public PrOOPtProperties loadYaml(InputStream in) {
        try (in) {
            Object root = new Yaml().load(in);
            if (root == null) {
                return new PrOOPtProperties();
            }
            if (!(root instanceof Map)) {
                throw new PrOOPtConfigException("top-level YAML must be a mapping, was: " + root.getClass());
            }
            return fromRoot((Map<String, Object>) root);
        } catch (IOException e) {
            throw new PrOOPtConfigException("failed to read YAML configuration", e);
        }
    }

    public PrOOPtProperties loadProperties(InputStream in) {
        try (in) {
            Properties p = new Properties();
            p.load(in);
            Map<String, Object> nested = new HashMap<>();
            for (String key : p.stringPropertyNames()) {
                put(nested, key.split("\\."), p.getProperty(key));
            }
            return fromRoot(nested);
        } catch (IOException e) {
            throw new PrOOPtConfigException("failed to read properties configuration", e);
        }
    }

    // ------------------------------------------------------------------ mapping

    @SuppressWarnings("unchecked")
    private PrOOPtProperties fromRoot(Map<String, Object> root) {
        PrOOPtProperties props = new PrOOPtProperties();
        Object prooopt = root.get(ROOT);
        if (!(prooopt instanceof Map<?, ?> tree)) {
            return props;
        }
        Map<String, Object> cfg = (Map<String, Object>) tree;

        Object models = pick(cfg, "models");
        if (models instanceof Map) {
            props.setModels(toModels((Map<String, Object>) models));
        }
        Object toolSel = pick(cfg, "toolSelection");
        if (toolSel instanceof Map) {
            props.setToolSelection(toToolSelection((Map<String, Object>) toolSel));
        }
        Object orch = pick(cfg, "orchestration");
        if (orch instanceof Map) {
            props.setOrchestration(toOrchestration((Map<String, Object>) orch));
        }
        return props;
    }

    @SuppressWarnings("unchecked")
    private PrOOPtProperties.Models toModels(Map<String, Object> m) {
        PrOOPtProperties.Models models = new PrOOPtProperties.Models();
        Object local = pick(m, "local");
        if (local instanceof Map) {
            models.setLocal(toModelConfig((Map<String, Object>) local));
        }
        Object cloudFast = pick(m, "cloudFast");
        if (cloudFast instanceof Map) {
            models.setCloudFast(toModelConfig((Map<String, Object>) cloudFast));
        }
        Object cloudAdvanced = pick(m, "cloudAdvanced");
        if (cloudAdvanced instanceof Map) {
            models.setCloudAdvanced(toModelConfig((Map<String, Object>) cloudAdvanced));
        }
        Object auto = pick(m, "auto");
        if (auto instanceof Map) {
            models.setAuto(toModelConfig((Map<String, Object>) auto));
        }
        return models;
    }

    private ModelConfig toModelConfig(Map<String, Object> m) {
        ModelConfig c = new ModelConfig();
        c.setEngine(asString(pick(m, "engine")));
        c.setProvider(asString(pick(m, "provider")));
        c.setModelId(asString(pick(m, "modelId")));
        c.setModelPath(asString(pick(m, "modelPath")));
        c.setApiKey(credentials.resolve(asString(pick(m, "apiKey"))));
        applyIfPresent(m, "thinking", v -> c.setThinking(asBoolean(v)));
        applyIfPresent(m, "maxTokens", v -> c.setMaxTokens(asInt(v)));
        applyIfPresent(m, "temperature", v -> c.setTemperature(asDouble(v)));
        applyIfPresent(m, "timeoutMs", v -> c.setTimeoutMs(asLong(v)));
        c.setStrategy(asString(pick(m, "strategy")));
        applyIfPresent(m, "tokenThreshold", v -> c.setTokenThreshold(asInt(v)));
        String fallback = asString(pick(m, "fallback"));
        if (fallback != null) {
            c.setFallback(ModelTier.fromString(fallback));
        }
        return c;
    }

    private ToolSelectionConfig toToolSelection(Map<String, Object> m) {
        ToolSelectionConfig c = new ToolSelectionConfig();
        applyIfPresent(m, "strategy", v -> c.setStrategy(asString(v)));
        applyIfPresent(m, "topK", v -> c.setTopK(asInt(v)));
        applyIfPresent(m, "minSimilarity", v -> c.setMinSimilarity(asDouble(v)));
        applyIfPresent(m, "embeddingEngine", v -> c.setEmbeddingEngine(asString(v)));
        return c;
    }

    private OrchestrationConfig toOrchestration(Map<String, Object> m) {
        OrchestrationConfig c = new OrchestrationConfig();
        applyIfPresent(m, "strategy", v -> c.setStrategy(asString(v)));
        applyIfPresent(m, "discoveryModel", v -> c.setDiscoveryModel(ModelTier.fromString(asString(v))));
        applyIfPresent(m, "executionModel", v -> c.setExecutionModel(ModelTier.fromString(asString(v))));
        applyIfPresent(m, "minMatchScore", v -> c.setMinMatchScore(asDouble(v)));
        applyIfPresent(m, "maxTools", v -> c.setMaxTools(asInt(v)));
        return c;
    }

    // ------------------------------------------------------------------ key/type helpers

    /** Looks up a camelCase property, also accepting its kebab-case and snake_case spellings. */
    private static Object pick(Map<String, Object> m, String camel) {
        if (m.containsKey(camel)) {
            return m.get(camel);
        }
        String kebab = camelToKebab(camel);
        if (m.containsKey(kebab)) {
            return m.get(kebab);
        }
        String snake = kebab.replace('-', '_');
        return m.get(snake);
    }

    private interface Setter {
        void accept(Object value);
    }

    private static void applyIfPresent(Map<String, Object> m, String camel, Setter setter) {
        Object v = pick(m, camel);
        if (v != null) {
            setter.accept(v);
        }
    }

    private static String camelToKebab(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char ch : camel.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                sb.append('-').append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<String, Object> root, String[] path, String value) {
        Map<String, Object> node = root;
        for (int i = 0; i < path.length - 1; i++) {
            node = (Map<String, Object>) node.computeIfAbsent(path[i], k -> new HashMap<String, Object>());
        }
        node.put(path[path.length - 1], value);
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int asInt(Object o) {
        return o instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(o).trim());
    }

    private static long asLong(Object o) {
        return o instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(o).trim());
    }

    private static double asDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(o).trim());
    }

    private static boolean asBoolean(Object o) {
        return o instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(o).trim());
    }

    private static InputStream classpath(String resource) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }
}
