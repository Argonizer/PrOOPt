/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.autobox;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.argonizer.prooopt.descriptor.PromptMethodDescriptor;
import io.github.argonizer.prooopt.descriptor.StructuredFamilies;
import io.github.argonizer.prooopt.exception.GenericTypeResolutionException;
import io.github.argonizer.prooopt.exception.PojoIntrospectionException;
import io.github.argonizer.prooopt.exception.PromptTypeBindingException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Descriptor-driven binding for generic {@code @PromptFunction} return types — the structured
 * counterpart to {@link PrOOPtAutoBoxer}'s scalar path. Given a {@link PromptMethodDescriptor}
 * captured by {@code prooopt-apt} before erasure, it routes the model's JSON output into the correct
 * concrete collection, map, or {@link Optional}, coercing each element/value to its resolved type.
 *
 * <p>It also produces the transparent prompt-shaping suffix the orchestration engine appends so the
 * model emits parseable JSON for the target shape.
 */
public final class StructuredAutoBoxer {

    private final PrOOPtAutoBoxer scalar;
    private final ObjectMapper mapper;

    public StructuredAutoBoxer() {
        this(new PrOOPtAutoBoxer());
    }

    public StructuredAutoBoxer(PrOOPtAutoBoxer scalar) {
        this.scalar = scalar;
        // Dedicated mapper: case-insensitive enums and strict unknown-property detection so POJO
        // field mismatches surface as PromptTypeBindingException rather than silently dropping.
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** True when this descriptor describes a container PrOOPt binds structurally (vs. a scalar). */
    public boolean handles(PromptMethodDescriptor d) {
        return d != null && d.isGeneric()
                && (isListFamily(d.rawType()) || isSetFamily(d.rawType())
                || isMapFamily(d.rawType()) || isOptional(d.rawType()));
    }

    // ------------------------------------------------------------------ binding

    /**
     * Binds {@code response} into the concrete type described by {@code d}. {@code methodLabel} is the
     * {@code Class.method()} string used in exception messages.
     */
    public Object bind(String response, PromptMethodDescriptor d, String methodLabel) {
        String raw = d.rawType();
        if (isOptional(raw)) {
            return bindOptional(response, d, methodLabel);
        }
        String text = PrOOPtAutoBoxer.stripFences(response == null ? "" : response).trim();
        if (isMapFamily(raw)) {
            return bindMap(text, d, methodLabel);
        }
        return bindCollection(text, d, methodLabel);
    }

    private Object bindOptional(String response, PromptMethodDescriptor d, String methodLabel) {
        if (response == null) {
            return Optional.empty();
        }
        String t = PrOOPtAutoBoxer.stripFences(response).trim();
        if (t.isEmpty() || t.equalsIgnoreCase("null") || t.equalsIgnoreCase("none")
                || t.equalsIgnoreCase("empty")) {
            return Optional.empty();
        }
        Class<?> inner = resolve(d.typeArgs().get(0), methodLabel, d);
        if (isWellKnownScalar(inner) || inner.isEnum()) {
            return Optional.of(scalar.autobox(t, inner));
        }
        try {
            return Optional.of(mapper.readValue(t, inner));
        } catch (UnrecognizedPropertyException e) {
            throw pojoMismatch(methodLabel, d, t, e, inner);
        } catch (Exception e) {
            throw new PromptTypeBindingException(
                    "[PrOOPt] PromptTypeBindingException in " + methodLabel
                            + "\n  Could not bind Optional value to " + inner.getSimpleName()
                            + ": " + e.getMessage()
                            + "\n  Fix       : Ensure the model returns a single JSON object matching "
                            + inner.getSimpleName() + ".", e);
        }
    }

    private Object bindCollection(String text, PromptMethodDescriptor d, String methodLabel) {
        Class<?> element = resolve(d.typeArgs().get(0), methodLabel, d);
        if (isNaturalOrdering(d.rawType()) && !Comparable.class.isAssignableFrom(element)) {
            throw GenericTypeResolutionException.notComparable(
                    methodLabel, signature(d), element.getSimpleName());
        }
        List<?> items;
        try {
            CollectionType type = mapper.getTypeFactory()
                    .constructCollectionType(ArrayList.class, element);
            items = mapper.readValue(text, type);
        } catch (UnrecognizedPropertyException e) {
            throw pojoMismatch(methodLabel, d, text, e, element);
        } catch (Exception e) {
            throw PromptTypeBindingException.map(methodLabel, "", signature(d), text,
                    "could not parse a JSON array of " + element.getSimpleName() + ": "
                            + e.getMessage());
        }
        return collect(d.rawType(), items);
    }

    private Object bindMap(String text, PromptMethodDescriptor d, String methodLabel) {
        Class<?> keyType = resolve(d.typeArgs().get(0), methodLabel, d);
        Class<?> valueType = resolve(d.typeArgs().get(1), methodLabel, d);
        Map<?, ?> parsed;
        try {
            MapType type = mapper.getTypeFactory().constructMapType(
                    LinkedHashMap.class, keyType, valueType);
            parsed = mapper.readValue(text, type);
        } catch (UnrecognizedPropertyException e) {
            throw pojoMismatch(methodLabel, d, text, e, valueType);
        } catch (Exception e) {
            throw PromptTypeBindingException.map(methodLabel, "", signature(d), text,
                    "a map value cannot be coerced to " + valueType.getSimpleName() + ": "
                            + e.getMessage());
        }
        return collectMap(d.rawType(), parsed);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object collect(String raw, List<?> items) {
        if (raw.equals("java.util.LinkedList") || raw.equals("java.util.Queue")) {
            return new LinkedList<>(items);
        }
        if (raw.equals("java.util.Deque") || raw.equals("java.util.ArrayDeque")) {
            return new ArrayDeque<>(items);
        }
        if (raw.equals("java.util.PriorityQueue")) {
            return new PriorityQueue<>(items);
        }
        if (raw.equals("java.util.Stack")) {
            Stack stack = new Stack();
            stack.addAll(items);
            return stack;
        }
        if (raw.equals("java.util.HashSet")) {
            return new java.util.HashSet<>(items);
        }
        if (raw.equals("java.util.TreeSet") || raw.equals("java.util.SortedSet")
                || raw.equals("java.util.NavigableSet")) {
            return new TreeSet<>(items);
        }
        if (isSetFamily(raw)) {
            return new LinkedHashSet<>(items); // Set / LinkedHashSet: preserve LLM order
        }
        // List / ArrayList and any remaining list-like default.
        return new ArrayList<>(items);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object collectMap(String raw, Map<?, ?> parsed) {
        if (raw.equals("java.util.HashMap")) {
            return new java.util.HashMap<>(parsed);
        }
        if (raw.equals("java.util.TreeMap") || raw.equals("java.util.SortedMap")
                || raw.equals("java.util.NavigableMap")) {
            return new TreeMap<>(parsed);
        }
        if (raw.equals("java.util.Hashtable")) {
            return new Hashtable<>(parsed);
        }
        // Map / LinkedHashMap: preserve LLM order.
        return new LinkedHashMap<>(parsed);
    }

    // ------------------------------------------------------------------ prompt shaping

    /** The transparent format suffix appended to the developer's prompt for a structured return type. */
    public String shape(PromptMethodDescriptor d, String methodLabel) {
        String raw = d.rawType();
        if (isMapFamily(raw)) {
            return "Respond as a flat JSON object only. No explanation. No extra text.";
        }
        if (isOptional(raw)) {
            Class<?> inner = resolve(d.typeArgs().get(0), methodLabel, d);
            if (isWellKnownScalar(inner) || inner.isEnum()) {
                return "Respond with a single value only, or the word 'none' if not applicable. "
                        + "No explanation.";
            }
            return "Respond as a single JSON object only, or the word 'none' if not applicable. "
                    + "No explanation." + pojoFieldClause(inner, methodLabel);
        }
        // array families (List/Set/Queue/...)
        Class<?> element = resolve(d.typeArgs().get(0), methodLabel, d);
        if (isWellKnownScalar(element) || element.isEnum()) {
            return "Respond as a JSON array only. No explanation. No extra text.";
        }
        return "Respond as a JSON array of objects only. No explanation."
                + pojoFieldClause(element, methodLabel);
    }

    private String pojoFieldClause(Class<?> pojo, String methodLabel) {
        List<Field> fields = declaredInstanceFields(pojo);
        if (fields.isEmpty()) {
            throw PojoIntrospectionException.noFields(methodLabel, pojo.getName());
        }
        StringJoiner sj = new StringJoiner(", ");
        for (Field f : fields) {
            sj.add(f.getName() + " (" + f.getType().getSimpleName() + ")");
        }
        return "\nEach object must have these fields: " + sj + ".";
    }

    private static List<Field> declaredInstanceFields(Class<?> pojo) {
        List<Field> out = new ArrayList<>();
        for (Field f : pojo.getDeclaredFields()) {
            int m = f.getModifiers();
            if (Modifier.isStatic(m) || Modifier.isTransient(m) || f.isSynthetic()) {
                continue;
            }
            out.add(f);
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private Class<?> resolve(String fqn, String methodLabel, PromptMethodDescriptor d) {
        try {
            return switch (fqn) {
                case "java.lang.String" -> String.class;
                case "java.lang.Integer" -> Integer.class;
                case "java.lang.Long" -> Long.class;
                case "java.lang.Double" -> Double.class;
                case "java.lang.Float" -> Float.class;
                case "java.lang.Boolean" -> Boolean.class;
                default -> Class.forName(fqn);
            };
        } catch (ClassNotFoundException e) {
            throw new GenericTypeResolutionException(
                    "[PrOOPt] GenericTypeResolutionException in " + methodLabel
                            + "\n  Returns   : " + signature(d)
                            + "\n  Cause     : type argument '" + fqn
                            + "' could not be loaded from the classpath."
                            + "\n  Fix       : Ensure the type is on the runtime classpath and "
                            + "fully qualified in the generated descriptor.", e);
        }
    }

    private PromptTypeBindingException pojoMismatch(
            String methodLabel, PromptMethodDescriptor d, String json,
            UnrecognizedPropertyException e, Class<?> pojo) {
        String declared = declaredInstanceFields(pojo).stream()
                .map(f -> f.getName() + " (" + f.getType().getSimpleName() + ")")
                .reduce((a, b) -> a + ", " + b).orElse("(none)");
        return PromptTypeBindingException.pojo(methodLabel, "", signature(d), json,
                e.getPropertyName(), pojo.getSimpleName(), declared);
    }

    static boolean isWellKnownScalar(Class<?> t) {
        return t == String.class || t == Boolean.class || t == Character.class
                || t == Integer.class || t == Long.class || t == Short.class || t == Byte.class
                || t == Double.class || t == Float.class
                || t == BigInteger.class || t == BigDecimal.class
                || t == LocalDate.class || t == LocalTime.class || t == LocalDateTime.class
                || t == ZonedDateTime.class || t == OffsetDateTime.class || t == Instant.class;
    }

    private static boolean isNaturalOrdering(String raw) {
        return raw.equals("java.util.PriorityQueue") || raw.equals("java.util.TreeSet")
                || raw.equals("java.util.SortedSet") || raw.equals("java.util.NavigableSet");
    }

    static boolean isListFamily(String raw) {
        return StructuredFamilies.isListFamily(raw);
    }

    static boolean isSetFamily(String raw) {
        return StructuredFamilies.isSetFamily(raw);
    }

    static boolean isMapFamily(String raw) {
        return StructuredFamilies.isMapFamily(raw);
    }

    static boolean isOptional(String raw) {
        return StructuredFamilies.isOptional(raw);
    }

    private static String signature(PromptMethodDescriptor d) {
        int dot = d.rawType().lastIndexOf('.');
        String rawSimple = dot < 0 ? d.rawType() : d.rawType().substring(dot + 1);
        StringJoiner sj = new StringJoiner(", ", "<", ">");
        for (String a : d.typeArgs()) {
            int ad = a.lastIndexOf('.');
            sj.add(ad < 0 ? a : a.substring(ad + 1));
        }
        return rawSimple + sj;
    }
}
