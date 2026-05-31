/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.stdlib;

import io.github.argonizer.prooopt.annotation.CodeFunction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic list and map operations that compose well inside execution plans.
 */
public final class CollectionFunctions {

    private CollectionFunctions() {
    }

    @CodeFunction(description = "Sort a list into ascending natural order (returns a new list).",
            tags = {"collection", "list", "sort", "order", "ascending"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Object> sortList(List<?> list) {
        List<Object> copy = new ArrayList<>(list);
        copy.sort((a, b) -> ((Comparable) a).compareTo(b));
        return copy;
    }

    @CodeFunction(description = "Remove null elements from a list (returns a new list).",
            tags = {"collection", "list", "filter", "compact", "non-null"})
    public static List<Object> filterNulls(List<?> list) {
        List<Object> out = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                out.add(item);
            }
        }
        return out;
    }

    @CodeFunction(description = "Remove duplicate elements from a list, preserving first-seen order.",
            tags = {"collection", "list", "dedupe", "distinct", "unique"})
    public static List<Object> dedupeList(List<?> list) {
        List<Object> out = new ArrayList<>();
        for (Object item : list) {
            if (!out.contains(item)) {
                out.add(item);
            }
        }
        return out;
    }

    @CodeFunction(description = "Flatten one level of nesting in a list of lists into a single list.",
            tags = {"collection", "list", "flatten", "concat", "nested"})
    public static List<Object> flattenList(List<?> list) {
        List<Object> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof List<?> nested) {
                out.addAll(nested);
            } else {
                out.add(item);
            }
        }
        return out;
    }

    @CodeFunction(description = "Merge two maps; entries from the second map override the first on conflict.",
            tags = {"collection", "map", "merge", "combine", "union"})
    public static Map<Object, Object> mergeMaps(Map<?, ?> first, Map<?, ?> second) {
        Map<Object, Object> out = new LinkedHashMap<>(first);
        out.putAll(second);
        return out;
    }

    @CodeFunction(description = "Invert a map, swapping keys and values (last value wins on collision).",
            tags = {"collection", "map", "invert", "swap", "reverse"})
    public static Map<Object, Object> invertMap(Map<?, ?> map) {
        Map<Object, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            out.put(e.getValue(), e.getKey());
        }
        return out;
    }

    @CodeFunction(description = "Keep only the entries of a map whose value equals a target value.",
            tags = {"collection", "map", "filter", "filterByValue", "select"})
    public static Map<Object, Object> filterMapByValue(Map<?, ?> map, Object value) {
        Map<Object, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (java.util.Objects.equals(e.getValue(), value)) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }
}
