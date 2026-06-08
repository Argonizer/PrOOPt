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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent validation cache at {@code target/prooopt-validation-cache.json}. Each entry is keyed by
 * {@code SHA-256(prompt + "|" + fullyQualifiedReturnType)} so a method is re-validated only when its
 * prompt or return type changes. Stores the prior {@link ClassificationResult} for transparency.
 */
public final class ValidationCache {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, String> entries;

    private ValidationCache(Map<String, String> entries) {
        this.entries = entries;
    }

    /** Loads the cache from {@code file}, or returns an empty cache if it is absent/unreadable. */
    public static ValidationCache load(Path file) {
        if (file == null || !Files.exists(file)) {
            return new ValidationCache(new LinkedHashMap<>());
        }
        try {
            Wrapper w = MAPPER.readValue(Files.readString(file), Wrapper.class);
            return new ValidationCache(w.entries == null ? new LinkedHashMap<>() : w.entries);
        } catch (Exception e) {
            return new ValidationCache(new LinkedHashMap<>());
        }
    }

    /** The cache key for a prompt + fully qualified return type. */
    public static String key(String prompt, String fqReturnType) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(((prompt == null ? "" : prompt) + "|"
                    + (fqReturnType == null ? "" : fqReturnType)).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** True when this key is already present (and thus may be skipped). */
    public boolean contains(String key) {
        return entries.containsKey(key);
    }

    /** Records a key as validated with the given result. */
    public void put(String key, ClassificationResult result) {
        entries.put(key, result.name());
    }

    public int size() {
        return entries.size();
    }

    /** Writes the cache to {@code file}, creating parent directories as needed. */
    public void save(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Wrapper w = new Wrapper();
        w.entries = entries;
        Files.writeString(file, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(w));
    }

    /** Jackson DTO for the on-disk shape. */
    static final class Wrapper {
        public Map<String, String> entries = new LinkedHashMap<>();
    }
}
