/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.api;

import io.github.argonizer.states.engine.PersonaStateEngine;
import io.github.argonizer.states.meta.PersonaMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Fluent builder for generating a population of personas.
 *
 * @param <T> the persona type
 */
public final class PersonaGeneratorBuilder<T> {

    private final Class<T> personaClass;
    private final PersonaMetadata meta;
    private final PersonaStateEngine engine;
    private final PersonaManager<T, ?> manager;

    private int limit = 10;
    private int batchSize = 10;
    private int parallelism = 5;
    private double requestsPerSecond = 5.0;
    private boolean persist = true;
    private String baseSeed = "";
    private final List<Segment> segments = new ArrayList<>();

    public PersonaGeneratorBuilder(Class<T> personaClass, PersonaMetadata meta,
                            PersonaStateEngine engine, PersonaManager<T, ?> manager) {
        this.personaClass = personaClass;
        this.meta = meta;
        this.engine = engine;
        this.manager = manager;
    }

    public PersonaGeneratorBuilder<T> limit(int n)           { this.limit = n; return this; }
    public PersonaGeneratorBuilder<T> batchSize(int n)       { this.batchSize = n; return this; }
    public PersonaGeneratorBuilder<T> parallelism(int n)     { this.parallelism = n; return this; }
    public PersonaGeneratorBuilder<T> rateLimit(double rps)  { this.requestsPerSecond = rps; return this; }
    public PersonaGeneratorBuilder<T> persist(boolean p)     { this.persist = p; return this; }
    public PersonaGeneratorBuilder<T> seed(String seed)      { this.baseSeed = seed; return this; }
    public PersonaGeneratorBuilder<T> distribute(Segment s)  { this.segments.add(s); return this; }

    public List<T> build() {
        return buildWithResult().succeeded();
    }

    public Stream<T> stream() {
        return build().stream();
    }

    public CompletableFuture<List<T>> buildAsync() {
        return CompletableFuture.supplyAsync(this::build);
    }

    public BuildResult<T> buildWithResult() {
        List<T> succeeded = new ArrayList<>();
        List<FailedBatch<T>> failed = new ArrayList<>();

        List<List<String>> batches = createBatches();
        long intervalMs = requestsPerSecond > 0 ? (long)(1000.0 / requestsPerSecond) : 0;

        for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
            List<String> ids = batches.get(batchIdx);
            try {
                for (String id : ids) {
                    String seed = resolveSeed(id, batchIdx);
                    @SuppressWarnings("unchecked")
                    T persona = (T) manager.create(castId(id), seed);
                    succeeded.add(persona);
                    if (intervalMs > 0) {
                        try { Thread.sleep(intervalMs); } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt(); break;
                        }
                    }
                }
            } catch (Exception e) {
                // retry once
                try {
                    List<T> retried = new ArrayList<>();
                    for (String id : ids) {
                        @SuppressWarnings("unchecked")
                        T persona = (T) manager.create(castId(id), resolveSeed(id, batchIdx));
                        retried.add(persona);
                    }
                    succeeded.addAll(retried);
                } catch (Exception e2) {
                    failed.add(new FailedBatch<>(batchIdx, ids, e2));
                }
            }
        }
        return new BuildResult<>(succeeded, failed);
    }

    private List<List<String>> createBatches() {
        List<List<String>> batches = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            current.add("gen-" + (i + 1));
            if (current.size() == batchSize) {
                batches.add(new ArrayList<>(current));
                current.clear();
            }
        }
        if (!current.isEmpty()) batches.add(current);
        return batches;
    }

    private String resolveSeed(String id, int batchIdx) {
        if (!segments.isEmpty()) {
            int segIdx = batchIdx % segments.size();
            return segments.get(segIdx).description();
        }
        return baseSeed.isEmpty() ? id : baseSeed;
    }

    @SuppressWarnings("unchecked")
    private <ID> ID castId(String id) {
        return (ID) id;
    }
}
