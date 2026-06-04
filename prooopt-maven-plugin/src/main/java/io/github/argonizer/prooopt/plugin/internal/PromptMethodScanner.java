/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin.internal;

import io.github.argonizer.prooopt.annotation.PromptFunction;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Discovers {@code @PromptFunction} methods by scanning a compiled {@code classes} directory and
 * loading each class through a supplied {@link ClassLoader} (whose parent is the plugin's own loader,
 * so the {@link PromptFunction} annotation type resolves to a single identity). Captures the fully
 * resolved generic return signature via {@link Method#getGenericReturnType()}.
 */
public final class PromptMethodScanner {

    private PromptMethodScanner() {
    }

    /** Scans {@code classesDir}, returning every discovered {@code @PromptFunction} method. */
    public static List<PromptMethod> scan(Path classesDir, ClassLoader loader) {
        List<PromptMethod> found = new ArrayList<>();
        if (classesDir == null || !Files.isDirectory(classesDir)) {
            return found;
        }
        try (Stream<Path> paths = Files.walk(classesDir)) {
            paths.filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> !p.getFileName().toString().contains("$")) // skip inner/synthetic
                    .forEach(p -> {
                        String className = toClassName(classesDir, p);
                        scanClass(className, loader, found);
                    });
        } catch (Exception e) {
            // A walk failure leaves whatever was discovered so far; the Mojo logs separately.
        }
        return found;
    }

    private static void scanClass(String className, ClassLoader loader, List<PromptMethod> found) {
        final Class<?> clazz;
        try {
            clazz = Class.forName(className, false, loader);
        } catch (Throwable t) {
            return; // unloadable (missing transitive dep, etc.) — skip
        }
        final Method[] methods;
        try {
            methods = clazz.getDeclaredMethods();
        } catch (Throwable t) {
            return;
        }
        for (Method m : methods) {
            PromptFunction ann = m.getAnnotation(PromptFunction.class);
            if (ann == null) {
                continue;
            }
            found.add(new PromptMethod(
                    clazz.getName(),
                    m.getName(),
                    ann.prompt(),
                    m.getGenericReturnType().getTypeName(),
                    ann.model().name()));
        }
    }

    private static String toClassName(Path root, Path classFile) {
        String relative = root.relativize(classFile).toString()
                .replace(java.io.File.separatorChar, '.');
        return relative.substring(0, relative.length() - ".class".length());
    }
}
