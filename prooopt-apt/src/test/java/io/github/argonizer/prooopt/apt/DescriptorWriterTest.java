/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.apt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescriptorWriterTest {

    @Test
    void constantNameIsUpperSnake() {
        assertEquals("GET_LANGUAGES", DescriptorWriter.constantName("getLanguages"));
        assertEquals("COUNT", DescriptorWriter.constantName("count"));
    }

    @Test
    void descriptorSimpleNameAppendsSuffix() {
        assertEquals("UserServicePromptDescriptor",
                DescriptorWriter.descriptorSimpleName("UserService"));
    }

    @Test
    void writesPackageImportsAndDescriptorConstant() {
        String src = DescriptorWriter.write("com.example", "UserService", List.of(
                new DescriptorWriter.Entry("getLanguages", "java.util.List",
                        List.of("java.lang.String"))));
        assertTrue(src.contains("package com.example;"), src);
        assertTrue(src.contains("import io.github.argonizer.prooopt.descriptor.PromptMethodDescriptor;"));
        assertTrue(src.contains("public final class UserServicePromptDescriptor"));
        assertTrue(src.contains(
                "PromptMethodDescriptor.of(\n            \"getLanguages\",\n"
                        + "            \"java.util.List\",\n            List.of(\"java.lang.String\"));"),
                src);
    }

    @Test
    void scalarReturnTypeUsesEmptyTypeArgs() {
        String src = DescriptorWriter.write("com.example", "Svc", List.of(
                new DescriptorWriter.Entry("getName", "java.lang.String", List.of())));
        assertTrue(src.contains("List.of())"), src);
    }
}
