/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin.internal;

import io.github.argonizer.prooopt.exception.PromptSemanticValidationException;
import io.github.argonizer.prooopt.exception.PromptVerbosityWarningException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The heart of {@code prooopt:validate}, extracted from the Mojo so it is testable with plain JUnit.
 * Diffs the validation cache, classifies uncached methods in a single batch, applies the uncertainty
 * policy, emits prose-at-LOCAL advisories, and always writes the cache and report — even when a
 * failure is pending — before throwing.
 */
public final class ValidationEngine {

    /** How an {@code UNCERTAIN} classification is treated. */
    public enum UncertaintyPolicy { FAIL, WARN }

    private final SemanticClassifier classifier;
    private final UncertaintyPolicy policy;
    private final Consumer<String> warnLog;

    public ValidationEngine(SemanticClassifier classifier, UncertaintyPolicy policy,
                            Consumer<String> warnLog) {
        this.classifier = classifier;
        this.policy = policy;
        this.warnLog = warnLog == null ? msg -> {
        } : warnLog;
    }

    /**
     * Runs validation over {@code methods}, persisting cache and report to the given paths. Returns
     * the report on success; throws {@link PromptSemanticValidationException} on the first INVALID
     * (or UNCERTAIN under {@code FAIL}) — after the report and cache have been written.
     */
    public ValidationReport run(List<PromptMethod> methods, Path cacheFile, Path reportFile)
            throws IOException {
        ValidationCache cache = ValidationCache.load(cacheFile);
        ValidationReport report = new ValidationReport();

        List<PromptMethod> uncached = new ArrayList<>();
        int cachedSkipped = 0;
        for (PromptMethod m : methods) {
            if (cache.contains(ValidationCache.key(m.prompt(), m.returnType()))) {
                cachedSkipped++;
                report.addResult(new ValidationReport.Result(
                        m.methodName(), m.className(), m.prompt(), m.returnType(), "VALID (cached)"));
            } else {
                uncached.add(m);
            }
        }

        Map<Integer, ClassificationResult> classified = uncached.isEmpty()
                ? Map.of() : classifier.classify(uncached);

        PromptSemanticValidationException pending = null;
        for (int i = 0; i < uncached.size(); i++) {
            PromptMethod m = uncached.get(i);
            ClassificationResult result =
                    classified.getOrDefault(i + 1, ClassificationResult.UNCERTAIN);

            if (result == ClassificationResult.VALID) {
                cache.put(ValidationCache.key(m.prompt(), m.returnType()), result);
            } else if (result == ClassificationResult.INVALID && pending == null) {
                pending = PromptSemanticValidationException.invalid(m.label(), m.prompt(),
                        m.returnType(),
                        "The prompt's core intent is not bindable to " + m.returnType()
                                + " regardless of output-format instructions embedded in the prompt.",
                        "Change the return type, or rewrite the prompt to produce a "
                                + m.returnType() + " value.");
            } else if (result == ClassificationResult.UNCERTAIN) {
                if (policy == UncertaintyPolicy.FAIL && pending == null) {
                    pending = PromptSemanticValidationException.uncertain(m.label(), m.prompt(),
                            m.returnType(),
                            "The validator could not confidently determine whether the prompt "
                                    + "produces a value bindable to " + m.returnType() + ".",
                            "Make the prompt unambiguous so its intent maps to exactly one type.");
                } else if (policy == UncertaintyPolicy.WARN) {
                    warnLog.accept("[PrOOPt WARN] UNCERTAIN classification (allowed by policy) for "
                            + m.label());
                }
            }
            report.addResult(new ValidationReport.Result(
                    m.methodName(), m.className(), m.prompt(), m.returnType(), result.name()));
        }

        // Prose-at-LOCAL advisory for every method (cached or not). Never fails the build.
        for (PromptMethod m : methods) {
            if ("LOCAL".equals(m.modelTier())
                    && OutputVerbosity.classify(m.prompt(), m.returnType()) == OutputVerbosity.PROSE) {
                String warning = PromptVerbosityWarningException.message(m.label(), m.prompt());
                report.addWarning(warning);
                warnLog.accept(warning);
            }
        }

        report.setTotals(methods.size(), uncached.size(), cachedSkipped);

        // Always persist cache and report, even when a failure is pending.
        cache.save(cacheFile);
        report.write(reportFile);

        if (pending != null) {
            throw pending;
        }
        return report;
    }
}
