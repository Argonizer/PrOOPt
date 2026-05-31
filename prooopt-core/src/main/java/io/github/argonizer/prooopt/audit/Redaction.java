/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.audit;

import io.github.argonizer.prooopt.annotation.SensitiveData;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies {@link SensitiveData} redaction when building the values that go into audit lines. A
 * parameter is redacted when it (or its declared type) is annotated; a return value is redacted when
 * its type is annotated. Redaction governs logging only — it never alters the values passed to a model
 * or returned to the caller.
 */
public final class Redaction {

    private Redaction() {
    }

    /** Builds a {@code paramName -> value-or-label} map with sensitive parameters masked. */
    public static Map<String, Object> redactedInputs(Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < params.length; i++) {
            Object value = (args != null && i < args.length) ? args[i] : null;
            out.put(params[i].getName(), redactParameter(params[i], value));
        }
        return out;
    }

    /** Returns the parameter's value, or its redaction label when the parameter is sensitive. */
    public static Object redactParameter(Parameter parameter, Object value) {
        SensitiveData annotation = parameter.getAnnotation(SensitiveData.class);
        if (annotation == null) {
            annotation = parameter.getType().getAnnotation(SensitiveData.class);
        }
        return annotation != null ? annotation.label() : value;
    }

    /** Whether a method's return value must be redacted (its return type is {@code @SensitiveData}). */
    public static boolean isSensitiveReturn(Method method) {
        return method.getReturnType().isAnnotationPresent(SensitiveData.class);
    }

    /** The label to log in place of a sensitive return value. */
    public static String returnLabel(Method method) {
        SensitiveData annotation = method.getReturnType().getAnnotation(SensitiveData.class);
        return annotation != null ? annotation.label() : "***REDACTED***";
    }

    /** Masks an output value for logging when its method declares a sensitive return type. */
    public static Object redactOutput(Method method, Object value) {
        return isSensitiveReturn(method) ? returnLabel(method) : value;
    }
}
