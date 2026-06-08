/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates per-method validation outcomes plus advisory warnings and writes
 * {@code target/prooopt-validation-report.json} after every run.
 */
public final class ValidationReport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** One per-method line in the report. */
    public record Result(String method, String className, String prompt,
                         String returnType, String result) {
    }

    private final List<Result> results = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private int totalMethods;
    private int validated;
    private int cachedSkipped;

    public void setTotals(int totalMethods, int validated, int cachedSkipped) {
        this.totalMethods = totalMethods;
        this.validated = validated;
        this.cachedSkipped = cachedSkipped;
    }

    public void addResult(Result r) {
        results.add(r);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public List<Result> results() {
        return List.copyOf(results);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    /** Serializes the report to JSON. */
    public String toJson() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("timestamp", Instant.now().toString());
        root.put("totalMethods", totalMethods);
        root.put("validated", validated);
        root.put("cachedSkipped", cachedSkipped);
        ArrayNode arr = root.putArray("results");
        for (Result r : results) {
            ObjectNode n = arr.addObject();
            n.put("method", r.method());
            n.put("class", r.className());
            n.put("prompt", r.prompt());
            n.put("returnType", r.returnType());
            n.put("result", r.result());
        }
        ArrayNode warn = root.putArray("warnings");
        warnings.forEach(warn::add);
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize validation report", e);
        }
    }

    /** Writes the report to {@code file}, creating parent directories as needed. */
    public void write(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, toJson());
    }
}
