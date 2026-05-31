/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.registry;

import io.github.argonizer.prooopt.annotation.CodeFunction;
import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.ToolDescriptor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovers {@link PromptFunction} and {@link CodeFunction} methods and turns them into
 * {@link ToolDescriptor}s. Enforces the central design rule: a {@code @PromptFunction} must be an
 * instance method (AOP cannot proxy statics), failing fast with an actionable message otherwise.
 */
public final class FunctionScanner {

    private FunctionScanner() {
    }

    /** Scans the given classes for annotated methods. */
    public static List<ToolDescriptor> scan(Class<?>... classes) {
        List<ToolDescriptor> out = new ArrayList<>();
        for (Class<?> type : classes) {
            for (Method method : type.getDeclaredMethods()) {
                ToolDescriptor descriptor = describe(method);
                if (descriptor != null) {
                    out.add(descriptor);
                }
            }
        }
        return out;
    }

    /**
     * Scans whole packages from the classpath (used by {@code @PromptFunctionScan}). Uses Spring's
     * classpath scanner with an include-everything filter, then inspects each concrete class for
     * annotated methods.
     */
    public static List<ToolDescriptor> scanPackages(String... basePackages) {
        var provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter((reader, factory) -> true);
        List<ToolDescriptor> out = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String pkg : basePackages) {
            for (var candidate : provider.findCandidateComponents(pkg)) {
                String className = candidate.getBeanClassName();
                if (className == null) {
                    continue;
                }
                try {
                    out.addAll(scan(Class.forName(className, false, loader)));
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // A class we cannot load holds no tools we can register; skip it.
                }
            }
        }
        return out;
    }

    /** Builds a descriptor for a single method, or {@code null} if it carries no PrOOPt annotation. */
    public static ToolDescriptor describe(Method method) {
        PromptFunction prompt = method.getAnnotation(PromptFunction.class);
        if (prompt != null) {
            if (Modifier.isStatic(method.getModifiers())) {
                throw new PrOOPtConfigException(
                        "@PromptFunction '" + method.getDeclaringClass().getName() + "#" + method.getName()
                                + "' is static, but PrOOPt intercepts prompt functions through an AOP proxy, "
                                + "which cannot wrap static methods. Make it an instance method, or — if it is "
                                + "deterministic Java needing no model — annotate it with @CodeFunction instead.");
            }
            return new ToolDescriptor(
                    method.getName(),
                    prompt.description(),
                    prompt.tags(),
                    FunctionType.PROMPT,
                    prompt.model(),
                    method,
                    method.getDeclaringClass(),
                    paramSchema(method),
                    method.getReturnType());
        }
        CodeFunction code = method.getAnnotation(CodeFunction.class);
        if (code != null) {
            return new ToolDescriptor(
                    method.getName(),
                    code.description(),
                    code.tags(),
                    FunctionType.CODE,
                    null, // deterministic: no model authority
                    method,
                    method.getDeclaringClass(),
                    paramSchema(method),
                    method.getReturnType());
        }
        return null;
    }

    private static Map<String, Class<?>> paramSchema(Method method) {
        Map<String, Class<?>> schema = new LinkedHashMap<>();
        for (Parameter p : method.getParameters()) {
            schema.put(p.getName(), p.getType());
        }
        return schema;
    }
}
