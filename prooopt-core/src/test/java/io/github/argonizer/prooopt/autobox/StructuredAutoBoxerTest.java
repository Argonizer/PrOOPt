/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.autobox;

import io.github.argonizer.prooopt.autobox.support.EmptyPojo;
import io.github.argonizer.prooopt.autobox.support.Person;
import io.github.argonizer.prooopt.autobox.support.Task;
import io.github.argonizer.prooopt.descriptor.PromptMethodDescriptor;
import io.github.argonizer.prooopt.exception.GenericTypeResolutionException;
import io.github.argonizer.prooopt.exception.PojoIntrospectionException;
import io.github.argonizer.prooopt.exception.PromptTypeBindingException;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredAutoBoxerTest {

    private final StructuredAutoBoxer boxer = new StructuredAutoBoxer();

    private static final String STR = "java.lang.String";
    private static final String INT = "java.lang.Integer";
    private static final String PERSON = "io.github.argonizer.prooopt.autobox.support.Person";
    private static final String TASK = "io.github.argonizer.prooopt.autobox.support.Task";
    private static final String EMPTY = "io.github.argonizer.prooopt.autobox.support.EmptyPojo";

    private static PromptMethodDescriptor d(String raw, String... args) {
        return PromptMethodDescriptor.of("m", raw, List.of(args));
    }

    private Object bind(String json, PromptMethodDescriptor d) {
        return boxer.bind(json, d, "Svc.m()");
    }

    // -------------------------------------------------------------- list family

    @Test
    void listOfStringBecomesArrayList() {
        Object out = bind("[\"a\",\"b\"]", d("java.util.List", STR));
        assertInstanceOf(ArrayList.class, out);
        assertEquals(List.of("a", "b"), out);
    }

    @Test
    void linkedListBindsToLinkedList() {
        assertInstanceOf(LinkedList.class, bind("[\"a\"]", d("java.util.LinkedList", STR)));
    }

    @Test
    void queueBindsToLinkedList() {
        Object out = bind("[\"a\"]", d("java.util.Queue", STR));
        assertInstanceOf(LinkedList.class, out);
        assertInstanceOf(Queue.class, out);
    }

    @Test
    void dequeBindsToArrayDeque() {
        Object out = bind("[\"a\"]", d("java.util.Deque", STR));
        assertInstanceOf(ArrayDeque.class, out);
        assertInstanceOf(Deque.class, out);
    }

    @Test
    void stackBindsToStack() {
        assertInstanceOf(Stack.class, bind("[\"a\",\"b\"]", d("java.util.Stack", STR)));
    }

    // -------------------------------------------------------------- set family

    @Test
    void setBecomesLinkedHashSetPreservingOrder() {
        Object out = bind("[\"c\",\"a\",\"b\"]", d("java.util.Set", STR));
        assertInstanceOf(LinkedHashSet.class, out);
        assertEquals(List.of("c", "a", "b"), new ArrayList<>((Set<?>) out));
    }

    @Test
    void hashSetBindsToHashSet() {
        assertInstanceOf(java.util.HashSet.class, bind("[\"a\"]", d("java.util.HashSet", STR)));
    }

    @Test
    void treeSetSortsNaturally() {
        Object out = bind("[\"c\",\"a\",\"b\"]", d("java.util.TreeSet", STR));
        assertInstanceOf(TreeSet.class, out);
        assertEquals(List.of("a", "b", "c"), new ArrayList<>((Set<?>) out));
    }

    @Test
    void sortedSetAndNavigableSetBecomeTreeSet() {
        assertInstanceOf(TreeSet.class, bind("[\"a\"]", d("java.util.SortedSet", STR)));
        assertInstanceOf(TreeSet.class, bind("[\"a\"]", d("java.util.NavigableSet", STR)));
    }

    // -------------------------------------------------------------- map family

    @Test
    void mapBecomesLinkedHashMapPreservingOrder() {
        Object out = bind("{\"b\":2,\"a\":1}", d("java.util.Map", STR, INT));
        assertInstanceOf(LinkedHashMap.class, out);
        assertEquals(List.of("b", "a"), new ArrayList<>(((Map<?, ?>) out).keySet()));
    }

    @Test
    void hashMapBindsToHashMap() {
        assertInstanceOf(java.util.HashMap.class,
                bind("{\"a\":1}", d("java.util.HashMap", STR, INT)));
    }

    @Test
    void treeMapSortsKeys() {
        Object out = bind("{\"b\":2,\"a\":1}", d("java.util.TreeMap", STR, INT));
        assertInstanceOf(TreeMap.class, out);
        assertEquals(List.of("a", "b"), new ArrayList<>(((Map<?, ?>) out).keySet()));
    }

    @Test
    void sortedNavigableMapBecomeTreeMapAndHashtable() {
        assertInstanceOf(TreeMap.class, bind("{\"a\":\"x\"}", d("java.util.SortedMap", STR, STR)));
        assertInstanceOf(TreeMap.class, bind("{\"a\":\"x\"}", d("java.util.NavigableMap", STR, STR)));
        assertInstanceOf(Hashtable.class, bind("{\"a\":1}", d("java.util.Hashtable", STR, INT)));
    }

    // -------------------------------------------------------------- POJO elements

    @Test
    void listOfPersonDeserializes() {
        Object out = bind("[{\"name\":\"Alice\",\"age\":30,\"email\":\"a@x.com\"}]",
                d("java.util.List", PERSON));
        @SuppressWarnings("unchecked")
        List<Person> people = (List<Person>) out;
        assertEquals(new Person("Alice", 30, "a@x.com"), people.get(0));
    }

    @Test
    void mapOfStringToPersonDeserializes() {
        Object out = bind("{\"alice\":{\"name\":\"Alice\",\"age\":30,\"email\":\"a@x.com\"}}",
                d("java.util.Map", STR, PERSON));
        @SuppressWarnings("unchecked")
        Map<String, Person> map = (Map<String, Person>) out;
        assertEquals(new Person("Alice", 30, "a@x.com"), map.get("alice"));
    }

    @Test
    void pojoFieldMismatchThrowsWithDeclaredFields() {
        PromptTypeBindingException ex = assertThrows(PromptTypeBindingException.class, () ->
                bind("[{\"fullName\":\"Alice\",\"age\":30,\"email\":\"x\"}]", d("java.util.List", PERSON)));
        assertTrue(ex.getMessage().contains("fullName"), ex.getMessage());
        assertTrue(ex.getMessage().contains("name (String)"), ex.getMessage());
    }

    // -------------------------------------------------------------- Optional

    @Test
    void optionalEmptyForNullAndBlankAndNone() {
        assertEquals(Optional.empty(), bind(null, d("java.util.Optional", STR)));
        assertEquals(Optional.empty(), bind("", d("java.util.Optional", STR)));
        assertEquals(Optional.empty(), bind("none", d("java.util.Optional", STR)));
    }

    @Test
    void optionalOfValue() {
        assertEquals(Optional.of("Alice"), bind("Alice", d("java.util.Optional", STR)));
    }

    // -------------------------------------------------------------- Comparable enforcement

    @Test
    void priorityQueueOfNonComparableThrows() {
        GenericTypeResolutionException ex = assertThrows(GenericTypeResolutionException.class, () ->
                bind("[{\"title\":\"x\"}]", d("java.util.PriorityQueue", TASK)));
        assertTrue(ex.getMessage().contains("Comparable<Task>"), ex.getMessage());
        assertTrue(ex.getMessage().contains("Queue<Task>"), ex.getMessage());
    }

    @Test
    void treeSetOfNonComparableThrows() {
        assertThrows(GenericTypeResolutionException.class, () ->
                bind("[{\"title\":\"x\"}]", d("java.util.TreeSet", TASK)));
    }

    @Test
    void priorityQueueOfStringWorks() {
        assertInstanceOf(PriorityQueue.class, bind("[\"b\",\"a\"]", d("java.util.PriorityQueue", STR)));
    }

    // -------------------------------------------------------------- prompt shaping

    @Test
    void shapeAppendsJsonArrayForArrayFamilies() {
        assertTrue(boxer.shape(d("java.util.List", STR), "Svc.m()").contains("JSON array"));
        assertTrue(boxer.shape(d("java.util.Set", STR), "Svc.m()").contains("JSON array"));
    }

    @Test
    void shapeAppendsJsonObjectForMapFamilies() {
        assertTrue(boxer.shape(d("java.util.Map", STR, INT), "Svc.m()").contains("flat JSON object"));
    }

    @Test
    void shapeIntrospectsPojoDeclaredFields() {
        String shape = boxer.shape(d("java.util.List", PERSON), "Svc.m()");
        assertTrue(shape.contains("name (String)"), shape);
        assertTrue(shape.contains("age (Integer)"), shape);
        assertTrue(shape.contains("email (String)"), shape);
    }

    @Test
    void shapeThrowsForPojoWithNoDeclaredFields() {
        assertThrows(PojoIntrospectionException.class, () ->
                boxer.shape(d("java.util.List", EMPTY), "Svc.m()"));
    }
}
