/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.apt;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates the generic shape of a {@code @PromptFunction} return type at compile time and captures
 * the rawType + type-argument FQNs (before erasure) for descriptor generation. Enforces PrOOPt's
 * rules: no wildcards, no unbound type variables, maximum nesting depth of 1, and String/primitive
 * map keys only.
 */
public final class GenericTypeValidator {

    /** Result of analysing a return type: captured shape plus any compile errors to emit. */
    public record AnalyzedReturn(String rawTypeFqn, List<String> typeArgFqns, List<String> errors) {
    }

    private static final Set<String> ALLOWED_MAP_KEYS = Set.of(
            "java.lang.String", "java.lang.Integer", "java.lang.Long", "java.lang.Double",
            "java.lang.Float", "java.lang.Short", "java.lang.Byte", "java.lang.Boolean",
            "java.math.BigInteger", "java.math.BigDecimal");

    private static final Set<String> MAP_FAMILY = Set.of(
            "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap", "java.util.TreeMap",
            "java.util.SortedMap", "java.util.NavigableMap", "java.util.Hashtable");

    private GenericTypeValidator() {
    }

    /** Analyses {@code returnType}, returning its captured shape and any errors found. */
    public static AnalyzedReturn analyze(TypeMirror returnType) {
        List<String> errors = new ArrayList<>();

        if (returnType.getKind().isPrimitive()) {
            return new AnalyzedReturn(returnType.toString(), List.of(), errors);
        }
        if (returnType.getKind() == TypeKind.VOID) {
            return new AnalyzedReturn("void", List.of(), errors);
        }
        if (returnType instanceof TypeVariable tv) {
            errors.add("Unbound type parameter '" + tv
                    + "' cannot be resolved. @PromptFunction methods must declare concrete return types.");
            return new AnalyzedReturn(tv.toString(), List.of(), errors);
        }
        if (!(returnType instanceof DeclaredType dt)) {
            return new AnalyzedReturn(returnType.toString(), List.of(), errors);
        }

        String rawTypeFqn = dt.asElement().toString();
        List<String> typeArgFqns = new ArrayList<>();
        List<? extends TypeMirror> args = dt.getTypeArguments();

        for (int i = 0; i < args.size(); i++) {
            TypeMirror arg = args.get(i);
            if (arg instanceof WildcardType) {
                errors.add("Wildcard type '?' is not bindable. Use a concrete type. "
                        + "Example: List<String>");
                typeArgFqns.add("?");
                continue;
            }
            if (arg instanceof TypeVariable tv) {
                errors.add("Unbound type parameter '" + tv + "' cannot be resolved. "
                        + "@PromptFunction methods must declare concrete return types.");
                typeArgFqns.add(tv.toString());
                continue;
            }
            if (arg instanceof DeclaredType argDt && !argDt.getTypeArguments().isEmpty()) {
                errors.add("""
                        Return type '%s' contains nested generics at depth 2.
                          PrOOPt supports a maximum generic nesting depth of 1.
                          Fix: Introduce a wrapper POJO to flatten the structure. Example:
                                 record BusManifest(String busId, List<Person> passengers) {}
                                 List<BusManifest> getManifests(String routeId);"""
                        .formatted(renderSimple(returnType)));
                typeArgFqns.add(((DeclaredType) arg).asElement().toString());
                continue;
            }
            if (arg instanceof DeclaredType argDt) {
                String argFqn = argDt.asElement().toString();
                // Map key (position 0 of a map-family type) must be String/primitive-equivalent.
                if (i == 0 && MAP_FAMILY.contains(rawTypeFqn) && !ALLOWED_MAP_KEYS.contains(argFqn)) {
                    errors.add("""
                            Map key type '%s' is not supported.
                              Map keys must be String or a primitive-equivalent (Integer, Long, Double, etc.).
                              Custom POJO keys have no reliable JSON key representation.
                              Fix: Use a String identifier for %s (e.g. busId, busNumber). Example:
                                     Map<String, Person> getPassengersByBusId(String routeId);"""
                            .formatted(simple(argFqn), simple(argFqn)));
                }
                typeArgFqns.add(argFqn);
                continue;
            }
            typeArgFqns.add(arg.toString());
        }
        return new AnalyzedReturn(rawTypeFqn, typeArgFqns, errors);
    }

    /** Renders a TypeMirror using simple names: {@code java.util.Map<...>} → {@code Map<String, ...>}. */
    static String renderSimple(TypeMirror t) {
        if (t instanceof DeclaredType dt) {
            String base = simple(dt.asElement().toString());
            if (dt.getTypeArguments().isEmpty()) {
                return base;
            }
            StringBuilder sb = new StringBuilder(base).append('<');
            List<? extends TypeMirror> args = dt.getTypeArguments();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(renderSimple(args.get(i)));
            }
            return sb.append('>').toString();
        }
        return t.toString();
    }

    private static String simple(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }
}
