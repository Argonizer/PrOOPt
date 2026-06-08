/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.plugin.internal;

import io.github.argonizer.prooopt.exception.PromptSemanticValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationEngineTest {

    /** Deterministic classifier returning a fixed id → result map. */
    private record StubClassifier(Map<Integer, ClassificationResult> fixed)
            implements SemanticClassifier {
        @Override
        public Map<Integer, ClassificationResult> classify(List<PromptMethod> batch) {
            return fixed;
        }
    }

    private static PromptMethod m(String prompt, String returnType, String tier) {
        return new PromptMethod("com.example.Svc", "fn", prompt, returnType, tier);
    }

    private ValidationEngine engine(Map<Integer, ClassificationResult> results,
                                    ValidationEngine.UncertaintyPolicy policy) {
        return new ValidationEngine(new StubClassifier(results), policy, msg -> {
        });
    }

    @Test
    void cacheWrittenAfterFirstRunAndSkippedOnSecond(@TempDir Path tmp) throws Exception {
        Path cache = tmp.resolve("cache.json");
        Path report = tmp.resolve("report.json");
        PromptMethod method = m("Return the value", "java.lang.String", "CLOUD_ADVANCED");

        engine(Map.of(1, ClassificationResult.VALID), ValidationEngine.UncertaintyPolicy.FAIL)
                .run(List.of(method), cache, report);
        assertTrue(Files.exists(cache));
        assertTrue(ValidationCache.load(cache)
                .contains(ValidationCache.key(method.prompt(), method.returnType())));

        // Second run: the classifier would throw if invoked (empty map → absent → UNCERTAIN),
        // but the method is cached so it is skipped entirely.
        ValidationReport second = engine(Map.of(), ValidationEngine.UncertaintyPolicy.FAIL)
                .run(List.of(method), cache, report);
        assertTrue(second.results().stream()
                .anyMatch(r -> r.result().equals("VALID (cached)")));
    }

    @Test
    void invalidResultThrowsButStillWritesReport(@TempDir Path tmp) throws Exception {
        Path cache = tmp.resolve("cache.json");
        Path report = tmp.resolve("report.json");
        PromptMethod method = m("Generate a random name", "java.lang.Integer", "CLOUD_ADVANCED");

        assertThrows(PromptSemanticValidationException.class, () ->
                engine(Map.of(1, ClassificationResult.INVALID), ValidationEngine.UncertaintyPolicy.FAIL)
                        .run(List.of(method), cache, report));
        assertTrue(Files.exists(report), "report must be written even on failure");
        assertTrue(Files.readString(report).contains("INVALID"));
    }

    @Test
    void uncertainFailsUnderFailPolicy(@TempDir Path tmp) {
        Path cache = tmp.resolve("cache.json");
        Path report = tmp.resolve("report.json");
        PromptMethod method = m("Evaluate the sentiment", "java.lang.Integer", "CLOUD_ADVANCED");
        assertThrows(PromptSemanticValidationException.class, () ->
                engine(Map.of(1, ClassificationResult.UNCERTAIN), ValidationEngine.UncertaintyPolicy.FAIL)
                        .run(List.of(method), cache, report));
    }

    @Test
    void uncertainWarnsAndContinuesUnderWarnPolicy(@TempDir Path tmp) throws Exception {
        Path cache = tmp.resolve("cache.json");
        Path report = tmp.resolve("report.json");
        PromptMethod method = m("Evaluate the sentiment", "java.lang.Integer", "CLOUD_ADVANCED");
        ValidationReport r = engine(Map.of(1, ClassificationResult.UNCERTAIN),
                ValidationEngine.UncertaintyPolicy.WARN).run(List.of(method), cache, report);
        assertTrue(r.results().stream().anyMatch(e -> e.result().equals("UNCERTAIN")));
    }

    @Test
    void proseAtLocalEmitsAdvisoryWarning(@TempDir Path tmp) throws Exception {
        Path cache = tmp.resolve("cache.json");
        Path report = tmp.resolve("report.json");
        PromptMethod method = m("Summarize this document", "java.lang.String", "LOCAL");
        ValidationReport r = engine(Map.of(1, ClassificationResult.VALID),
                ValidationEngine.UncertaintyPolicy.FAIL).run(List.of(method), cache, report);
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("Prose-level output")));
    }

    @Test
    void reportJsonHasExpectedShape(@TempDir Path tmp) throws Exception {
        Path cache = tmp.resolve("cache.json");
        Path report = tmp.resolve("report.json");
        engine(Map.of(1, ClassificationResult.VALID), ValidationEngine.UncertaintyPolicy.FAIL)
                .run(List.of(m("Return the value", "java.lang.String", "CLOUD_ADVANCED")), cache, report);
        String json = Files.readString(report);
        assertTrue(json.contains("\"totalMethods\""));
        assertTrue(json.contains("\"validated\""));
        assertTrue(json.contains("\"cachedSkipped\""));
        assertTrue(json.contains("\"results\""));
    }

    @Test
    void emptyMethodListProducesEmptyReport(@TempDir Path tmp) throws Exception {
        Path cache = tmp.resolve("cache.json");
        Path report = tmp.resolve("report.json");
        ValidationReport r = engine(Map.of(), ValidationEngine.UncertaintyPolicy.FAIL)
                .run(List.of(), cache, report);
        assertEquals(0, r.results().size());
    }
}
