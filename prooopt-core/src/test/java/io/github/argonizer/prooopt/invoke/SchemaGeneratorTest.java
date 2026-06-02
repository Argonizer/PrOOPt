/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.invoke;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaGeneratorTest {

    enum Risk { LOW, MEDIUM, HIGH }

    record Person(String name, int age, boolean active) {
    }

    private final SchemaGenerator generator = new SchemaGenerator();

    @Test
    void primitivesMapToJsonSchemaTypes() {
        assertEquals("integer", generator.toJsonSchema(int.class).get("type"));
        assertEquals("number", generator.toJsonSchema(double.class).get("type"));
        assertEquals("boolean", generator.toJsonSchema(boolean.class).get("type"));
        assertEquals("string", generator.toJsonSchema(String.class).get("type"));
    }

    @Test
    void enumProducesEnumConstraint() {
        Map<String, Object> schema = generator.toJsonSchema(Risk.class);
        assertEquals("string", schema.get("type"));
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) schema.get("enum");
        assertEquals(List.of("LOW", "MEDIUM", "HIGH"), values);
    }

    @Test
    void javaTimeMapsToDateTimeFormat() {
        Map<String, Object> schema = generator.toJsonSchema(LocalDate.class);
        assertEquals("string", schema.get("type"));
        assertEquals("date-time", schema.get("format"));
    }

    @Test
    void recordProducesObjectWithProperties() {
        Map<String, Object> schema = generator.toJsonSchema(Person.class);
        assertEquals("object", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        assertTrue(props.containsKey("name") && props.containsKey("age") && props.containsKey("active"));
        @SuppressWarnings("unchecked")
        Map<String, Object> ageSchema = (Map<String, Object>) props.get("age");
        assertEquals("integer", ageSchema.get("type"));
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertTrue(required.containsAll(List.of("name", "age", "active")));
    }

    @Test
    void collectionMapsToArray() {
        assertEquals("array", generator.toJsonSchema(List.class).get("type"));
    }

    @Test
    void gbnfGrammarHasRootRule() {
        String gbnf = generator.toGbnf(Risk.class);
        assertTrue(gbnf.startsWith("root ::="));
        assertTrue(gbnf.contains("LOW") && gbnf.contains("HIGH"));
    }

    @Test
    void gbnfForRecordReferencesFieldNames() {
        String gbnf = generator.toGbnf(Person.class);
        assertTrue(gbnf.contains("name") && gbnf.contains("age") && gbnf.contains("active"));
    }
}
