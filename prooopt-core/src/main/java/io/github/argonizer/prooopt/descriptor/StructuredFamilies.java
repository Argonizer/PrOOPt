/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.descriptor;

import java.util.Set;

/**
 * Classifies a container's fully qualified raw type into the PrOOPt collection families that bind to
 * JSON arrays (list/set), JSON objects (map), or a nullable scalar ({@link java.util.Optional}).
 * Shared by the {@code prooopt-apt} processor and the runtime {@code StructuredAutoBoxer} so both
 * agree on exactly which types are supported.
 */
public final class StructuredFamilies {

    private static final Set<String> LIST_FAMILY = Set.of(
            "java.util.List", "java.util.ArrayList", "java.util.LinkedList", "java.util.Stack",
            "java.util.Queue", "java.util.Deque", "java.util.ArrayDeque", "java.util.PriorityQueue");

    private static final Set<String> SET_FAMILY = Set.of(
            "java.util.Set", "java.util.HashSet", "java.util.LinkedHashSet", "java.util.TreeSet",
            "java.util.SortedSet", "java.util.NavigableSet");

    private static final Set<String> MAP_FAMILY = Set.of(
            "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap", "java.util.TreeMap",
            "java.util.SortedMap", "java.util.NavigableMap", "java.util.Hashtable");

    private StructuredFamilies() {
    }

    public static boolean isListFamily(String rawTypeFqn) {
        return LIST_FAMILY.contains(rawTypeFqn);
    }

    public static boolean isSetFamily(String rawTypeFqn) {
        return SET_FAMILY.contains(rawTypeFqn);
    }

    public static boolean isMapFamily(String rawTypeFqn) {
        return MAP_FAMILY.contains(rawTypeFqn);
    }

    public static boolean isOptional(String rawTypeFqn) {
        return "java.util.Optional".equals(rawTypeFqn);
    }

    /** True for any type PrOOPt binds structurally (list/set/map/Optional). */
    public static boolean isStructured(String rawTypeFqn) {
        return isListFamily(rawTypeFqn) || isSetFamily(rawTypeFqn)
                || isMapFamily(rawTypeFqn) || isOptional(rawTypeFqn);
    }
}
