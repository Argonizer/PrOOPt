/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.model;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A registered, selectable tool — either a {@code @PromptFunction} or a {@code @CodeFunction}. Holds
 * everything the orchestrator needs to consider, describe, and invoke the function, plus an
 * {@code embedding} slot populated by the {@code ToolIndexer} at startup for semantic matching.
 */
public final class ToolDescriptor {

    private final String name;
    private final String description;
    private final String[] tags;
    private final FunctionType type;
    private final ModelTier modelTier;
    private final Method method;
    private final Class<?> declaringClass;
    private final Map<String, Class<?>> paramSchema;
    private final Class<?> returnType;

    /** Dense or sparse vector for this tool's {@code description + tags}; set at indexing time. */
    private float[] embedding;

    public ToolDescriptor(String name,
                          String description,
                          String[] tags,
                          FunctionType type,
                          ModelTier modelTier,
                          Method method,
                          Class<?> declaringClass,
                          Map<String, Class<?>> paramSchema,
                          Class<?> returnType) {
        this.name = name;
        this.description = description;
        this.tags = tags == null ? new String[0] : tags.clone();
        this.type = type;
        this.modelTier = modelTier;
        this.method = method;
        this.declaringClass = declaringClass;
        this.paramSchema = paramSchema == null ? new LinkedHashMap<>() : new LinkedHashMap<>(paramSchema);
        this.returnType = returnType;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String[] tags() {
        return tags.clone();
    }

    public FunctionType type() {
        return type;
    }

    public ModelTier modelTier() {
        return modelTier;
    }

    public Method method() {
        return method;
    }

    public Class<?> declaringClass() {
        return declaringClass;
    }

    /** Ordered {@code paramName -> type} schema, used to render the tool for the planner. */
    public Map<String, Class<?>> paramSchema() {
        return paramSchema;
    }

    public Class<?> returnType() {
        return returnType;
    }

    public float[] embedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    /** The text embedded for semantic matching: {@code description} followed by tags. */
    public String embeddableText() {
        if (tags.length == 0) {
            return description == null ? "" : description;
        }
        return (description == null ? "" : description) + " " + String.join(" ", tags);
    }

    @Override
    public String toString() {
        return type + " " + name + " (" + modelTier + ")";
    }
}
