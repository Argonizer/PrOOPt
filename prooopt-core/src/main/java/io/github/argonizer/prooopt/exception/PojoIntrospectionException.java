/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.exception;

/**
 * Thrown by {@code PrOOPtAutoBoxer} during prompt shaping when a custom POJO used as a generic
 * element/value type exposes no usable declared fields. PrOOPt introspects only fields declared
 * directly on the class via {@link Class#getDeclaredFields()} — inherited fields are not visible.
 */
public final class PojoIntrospectionException extends PrOOPtException {

    public PojoIntrospectionException(String message) {
        super(message);
        AuditErrorLog.record("PojoIntrospectionException", message);
    }

    public PojoIntrospectionException(String message, Throwable cause) {
        super(message, cause);
        AuditErrorLog.record("PojoIntrospectionException", message);
    }

    /** The POJO declares no non-static, non-transient instance fields. */
    public static PojoIntrospectionException noFields(String method, String pojoFqn) {
        return new PojoIntrospectionException("""
                [PrOOPt] PojoIntrospectionException in %s
                  POJO      : %s
                  Cause     : %s declares no accessible fields. PrOOPt requires at least
                               one non-static, non-transient field to generate a prompt
                               format instruction.
                  Fix       : Ensure %s has declared instance fields (public or private).
                               If %s uses a builder or factory pattern with no fields,
                               annotate it with @JsonCreator and @JsonProperty so Jackson
                               and PrOOPt can discover its structure.\
                """.formatted(method, pojoFqn, simpleName(pojoFqn), simpleName(pojoFqn),
                simpleName(pojoFqn)));
    }

    /** Diagnostic for the case where the developer relied on inherited (non-introspected) fields. */
    public static PojoIntrospectionException inheritedFields(
            String method, String pojoFqn, String superFqn, String inheritedFieldList) {
        return new PojoIntrospectionException("""
                [PrOOPt] PojoIntrospectionException in %s
                  POJO      : %s extends %s
                  Cause     : PrOOPt introspects only fields declared directly on %s
                               via Class.getDeclaredFields(). Fields declared on %s
                               (%s) are not visible to PrOOPt's prompt shaping.
                  Fix       : Override the fields in %s, or flatten the inheritance
                               for POJOs used as @PromptFunction return types.
                               Alternatively, annotate %s with @JsonIgnoreProperties
                               to explicitly control which fields the LLM is asked to populate.\
                """.formatted(method, pojoFqn, superFqn, simpleName(pojoFqn), simpleName(superFqn),
                inheritedFieldList, simpleName(pojoFqn), simpleName(pojoFqn)));
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }
}
