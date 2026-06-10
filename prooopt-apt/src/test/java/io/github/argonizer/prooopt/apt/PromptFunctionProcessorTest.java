/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.apt;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptFunctionProcessorTest {

    /** Wraps method declarations in an interface with the standard imports. */
    private JavaFileObject iface(String body) {
        return JavaFileObjects.forSourceString("com.example.Svc", """
                package com.example;
                import io.github.argonizer.prooopt.annotation.PromptFunction;
                import io.github.argonizer.prooopt.model.ModelTier;
                import java.util.*;
                public interface Svc {
                %s
                }
                """.formatted(body));
    }

    private Compilation compileIface(String body) {
        return Compiler.javac()
                .withProcessors(new PromptFunctionProcessor())
                .withOptions("-parameters")
                .compile(iface(body));
    }

    // -------------------------------------------------------------- heuristic errors

    @Test
    void textualPromptWithIntegerReturnFails() {
        Compilation c = compileIface(
                "@PromptFunction(prompt = \"Generate a random name\") Integer getName();");
        assertThat(c).failed();
        assertThat(c).hadErrorContaining("return type is Integer");
    }

    @Test
    void unmatchedPlaceholderFails() {
        Compilation c = compileIface(
                "@PromptFunction(prompt = \"Translate {lang}\") String translate(String text, String language);");
        assertThat(c).failed();
        assertThat(c).hadErrorContaining("no parameter named 'lang'");
        assertThat(c).hadErrorContaining("Did you mean '{language}'?");
    }

    // -------------------------------------------------------------- generic errors

    @Test
    void wildcardReturnsFail() {
        assertThat(compileIface(
                "@PromptFunction(prompt = \"vals for {t}\") List<?> a(String t);")).failed();
        assertThat(compileIface(
                "@PromptFunction(prompt = \"vals for {t}\") Set<?> a(String t);")).failed();
        assertThat(compileIface(
                "@PromptFunction(prompt = \"vals for {t}\") Queue<?> a(String t);")).failed();
        Compilation map = compileIface(
                "@PromptFunction(prompt = \"vals for {t}\") Map<?,?> a(String t);");
        assertThat(map).hadErrorContaining("Wildcard type '?'");
    }

    @Test
    void nestedGenericsFail() {
        assertThat(compileIface(
                "@PromptFunction(prompt = \"vals for {t}\") List<List<String>> a(String t);"))
                .hadErrorContaining("nested generics at depth 2");
        assertThat(compileIface(
                "@PromptFunction(prompt = \"vals for {t}\") Map<String, List<String>> a(String t);"))
                .hadErrorContaining("nested generics at depth 2");
        assertThat(compileIface(
                "@PromptFunction(prompt = \"vals for {t}\") Queue<Set<String>> a(String t);"))
                .hadErrorContaining("nested generics at depth 2");
    }

    @Test
    void pojoMapKeyFails() {
        Compilation c = Compiler.javac()
                .withProcessors(new PromptFunctionProcessor())
                .withOptions("-parameters")
                .compile(
                        JavaFileObjects.forSourceString("com.example.Bus",
                                "package com.example; public class Bus {}"),
                        JavaFileObjects.forSourceString("com.example.Person",
                                "package com.example; public class Person { public String name; }"),
                        iface("@PromptFunction(prompt = \"vals for {t}\") "
                                + "Map<Bus, Person> a(String t);"));
        assertThat(c).hadErrorContaining("Map key type 'Bus' is not supported");
    }

    @Test
    void treeMapPojoKeyFails() {
        Compilation c = Compiler.javac()
                .withProcessors(new PromptFunctionProcessor())
                .withOptions("-parameters")
                .compile(
                        JavaFileObjects.forSourceString("com.example.Bus",
                                "package com.example; public class Bus {}"),
                        iface("@PromptFunction(prompt = \"vals for {t}\") "
                                + "TreeMap<Bus, String> a(String t);"));
        assertThat(c).hadErrorContaining("Map key type 'Bus' is not supported");
    }

    // -------------------------------------------------------------- clean compiles

    @Test
    void supportedListFamiliesCompileCleanly() {
        assertThat(compileIface("""
                @PromptFunction(prompt = "vals for {t}") List<String> a(String t);
                @PromptFunction(prompt = "vals for {t}") LinkedList<Integer> b(String t);
                @PromptFunction(prompt = "vals for {t}") Queue<String> c(String t);
                @PromptFunction(prompt = "vals for {t}") Deque<String> d(String t);
                @PromptFunction(prompt = "vals for {t}") Stack<String> e(String t);
                """)).succeeded();
    }

    @Test
    void supportedSetFamiliesCompileCleanly() {
        assertThat(compileIface("""
                @PromptFunction(prompt = "vals for {t}") Set<String> a(String t);
                @PromptFunction(prompt = "vals for {t}") HashSet<Integer> b(String t);
                @PromptFunction(prompt = "vals for {t}") TreeSet<String> c(String t);
                @PromptFunction(prompt = "vals for {t}") SortedSet<String> d(String t);
                @PromptFunction(prompt = "vals for {t}") NavigableSet<String> e(String t);
                """)).succeeded();
    }

    @Test
    void supportedMapFamiliesCompileCleanly() {
        assertThat(compileIface("""
                @PromptFunction(prompt = "vals for {t}") Map<String, Integer> a(String t);
                @PromptFunction(prompt = "vals for {t}") TreeMap<String, String> b(String t);
                @PromptFunction(prompt = "vals for {t}") SortedMap<String, Integer> c(String t);
                @PromptFunction(prompt = "vals for {t}") LinkedHashMap<String, Boolean> d(String t);
                @PromptFunction(prompt = "vals for {t}") Hashtable<String, Integer> e(String t);
                """)).succeeded();
    }

    @Test
    void pojoElementsAndOptionalsCompileCleanly() {
        Compilation c = Compiler.javac()
                .withProcessors(new PromptFunctionProcessor())
                .withOptions("-parameters")
                .compile(
                        JavaFileObjects.forSourceString("com.example.Person",
                                "package com.example; public class Person { public String name; }"),
                        iface("""
                                @PromptFunction(prompt = "build for {t}") List<Person> a(String t);
                                @PromptFunction(prompt = "build for {t}") Map<String, Person> b(String t);
                                @PromptFunction(prompt = "find for {t}") Optional<String> c(String t);
                                @PromptFunction(prompt = "find for {t}") Optional<Person> d(String t);
                                """));
        assertThat(c).succeeded();
    }

    // -------------------------------------------------------------- descriptor generation

    @Test
    void generatesDescriptorCapturingRawTypeAndTypeArgs() {
        Compilation c = compileIface(
                "@PromptFunction(prompt = \"vals for {t}\") List<String> getLanguages(String t);");
        assertThat(c).succeeded();
        assertThat(c).generatedSourceFile("com.example.SvcPromptDescriptor")
                .contentsAsUtf8String().contains("\"java.util.List\"");
        assertThat(c).generatedSourceFile("com.example.SvcPromptDescriptor")
                .contentsAsUtf8String().contains("List.of(\"java.lang.String\")");
        assertThat(c).generatedSourceFile("com.example.SvcPromptDescriptor")
                .contentsAsUtf8String().contains("GET_LANGUAGES");
    }

    @Test
    void constantNamingRoundTrips() {
        assertEquals("GET_LANGUAGES", DescriptorWriter.constantName("getLanguages"));
    }
}
