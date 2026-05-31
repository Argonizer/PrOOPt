/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.autobox;

import io.github.argonizer.prooopt.exception.PrOOPtAutoBoxException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrOOPtAutoBoxerTest {

    enum Priority { LOW, MEDIUM, HIGH }

    record Person(String name, int age) {
    }

    private final PrOOPtAutoBoxer autoBoxer = new PrOOPtAutoBoxer();

    @Test
    void boxesStringByStrippingFences() {
        assertEquals("hello", autoBoxer.autobox("```\nhello\n```", String.class));
        assertEquals("plain", autoBoxer.autobox("plain", String.class));
    }

    @Test
    void boxesIntegerLeniently() {
        assertEquals(42, autoBoxer.autobox("42", Integer.class));
        assertEquals(42, autoBoxer.autobox("The answer is 42 units.", int.class));
        assertEquals(1234, autoBoxer.autobox("1,234", Integer.class));
        assertEquals(42, autoBoxer.autobox("42.0", Integer.class));
    }

    @Test
    void boxesLongAndDoubleAndFloat() {
        assertEquals(9_000_000_000L, autoBoxer.autobox("9000000000", Long.class));
        assertEquals(3.14, autoBoxer.autobox("about 3.14 ish", Double.class));
        assertEquals(2.5f, autoBoxer.autobox("2.5", Float.class));
    }

    @Test
    void boxesBooleanLeniently() {
        assertEquals(true, autoBoxer.autobox("yes", Boolean.class));
        assertEquals(true, autoBoxer.autobox("TRUE", boolean.class));
        assertEquals(true, autoBoxer.autobox("1", Boolean.class));
        assertEquals(false, autoBoxer.autobox("no", Boolean.class));
        assertEquals(false, autoBoxer.autobox("0", Boolean.class));
    }

    @Test
    void boxesEnums() {
        assertEquals(Priority.HIGH, autoBoxer.autobox("HIGH", Priority.class));
        assertEquals(Priority.HIGH, autoBoxer.autobox("high", Priority.class));
        assertEquals(Priority.MEDIUM, autoBoxer.autobox("The priority is MEDIUM.", Priority.class));
    }

    @Test
    void boxesJavaTimeTypes() {
        assertEquals(LocalDate.of(2026, 1, 15), autoBoxer.autobox("2026-01-15", LocalDate.class));
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30),
                autoBoxer.autobox("2026-01-15T09:30:00", LocalDateTime.class));
        assertEquals(ZonedDateTime.parse("2026-01-15T09:30:00Z"),
                autoBoxer.autobox("\"2026-01-15T09:30:00Z\"", ZonedDateTime.class));
    }

    @Test
    void boxesCollections() {
        assertEquals(List.of(1, 2, 3), autoBoxer.autobox("[1, 2, 3]", List.class));
        assertEquals(Map.of("a", 1), autoBoxer.autobox("{\"a\": 1}", Map.class));
        assertEquals(Set.of("x", "y"), autoBoxer.autobox("[\"x\", \"y\", \"x\"]", Set.class));
    }

    @Test
    void boxesPojoStrippingMarkdown() {
        Person person = (Person) autoBoxer.autobox(
                "```json\n{\"name\": \"Ada\", \"age\": 36}\n```", Person.class);
        assertEquals(new Person("Ada", 36), person);
    }

    @Test
    void boxesVoidToNull() {
        assertNull(autoBoxer.autobox("anything", void.class));
        assertNull(autoBoxer.autobox("anything", Void.class));
    }

    @Test
    void throwsOnUnparseableNumber() {
        assertThrows(PrOOPtAutoBoxException.class, () -> autoBoxer.autobox("not a number", Integer.class));
    }

    @Test
    void throwsOnEmptyResponseForNonString() {
        assertThrows(PrOOPtAutoBoxException.class, () -> autoBoxer.autobox("   ", Integer.class));
    }

    @Test
    void buildsTypeAppropriateFormatInstructions() {
        assertTrue(autoBoxer.buildFormatInstruction(int.class).toLowerCase().contains("integer"));
        assertTrue(autoBoxer.buildFormatInstruction(Boolean.class).toLowerCase().contains("true"));
        assertTrue(autoBoxer.buildFormatInstruction(Map.class).toLowerCase().contains("json object"));
        String enumInstruction = autoBoxer.buildFormatInstruction(Priority.class);
        assertTrue(enumInstruction.contains("LOW") && enumInstruction.contains("HIGH"));
        assertEquals("", autoBoxer.buildFormatInstruction(String.class));
    }

    @Test
    void stricterInstructionEscalates() {
        assertTrue(autoBoxer.buildStricterFormatInstruction(int.class).toLowerCase().contains("critical"));
    }
}
