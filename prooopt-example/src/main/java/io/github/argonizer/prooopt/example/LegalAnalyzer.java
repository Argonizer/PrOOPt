/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.example;

import io.github.argonizer.prooopt.annotation.CodeFunction;
import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.annotation.PromptOrchestrator;
import io.github.argonizer.prooopt.annotation.SensitiveData;
import io.github.argonizer.prooopt.model.FunctionCall;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.orchestrator.BaseOrchestrator;

import java.time.LocalDate;

/**
 * A worked example showing PrOOPt's three zones of control in one class:
 *
 * <ul>
 *   <li><b>Deterministic</b> ({@code @CodeFunction}): {@link #normalizeWhitespace} and
 *       {@link #countWords} — pure Java, zero tokens.</li>
 *   <li><b>Bounded AI</b> ({@code @PromptFunction} with {@link ModelTier#LOCAL}):
 *       {@link #extractSigningDate} and {@link #detectRiskLevel} — on-device, nothing leaves the JVM.</li>
 *   <li><b>Elevated AI</b> ({@code @PromptFunction} with {@link ModelTier#CLOUD_ADVANCED}):
 *       {@link #generateSummary} — explicitly granted cloud authority over redacted input.</li>
 * </ul>
 *
 * <p>Extending {@link BaseOrchestrator} is optional; it is done here only to demonstrate lifecycle
 * hooks. The {@code @PromptFunction} bodies return {@code null} — PrOOPt supplies the typed result.
 */
@PromptOrchestrator(
        prompt = "You are a meticulous legal contract analyzer. Given a contract, validate it, extract "
                + "key facts, assess risk, and summarize it. Prefer deterministic tools for exact work "
                + "and on-device models for fact extraction; reserve cloud authority for the summary.",
        model = ModelTier.LOCAL,
        parallel = true,
        name = "legal-analyzer",
        version = "0.1.0")
public class LegalAnalyzer extends BaseOrchestrator {

    // ---- Deterministic zone: pure Java, always correct, zero tokens. ----

    @CodeFunction(description = "Collapse runs of whitespace in contract text into single spaces.",
            tags = {"normalize", "whitespace", "clean", "text", "preprocess", "contract"})
    public String normalizeWhitespace(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }

    @CodeFunction(description = "Count the number of words in the contract text.",
            tags = {"count", "words", "length", "size", "contract", "statistics"})
    public int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.strip().split("\\s+").length;
    }

    // ---- Bounded AI zone: on-device LOCAL inference, no data leaves the JVM. ----

    @PromptFunction(
            prompt = "Extract the signing date from the following contract text. {text}",
            model = ModelTier.LOCAL,
            description = "Extract the contract's signing date as an ISO-8601 date.",
            tags = {"extract", "signing", "date", "contract", "fact"})
    public LocalDate extractSigningDate(String text) {
        return null; // supplied by PrOOPt from the model response
    }

    @PromptFunction(
            prompt = "Assess the overall risk level of this contract as LOW, MEDIUM, or HIGH. {text}",
            model = ModelTier.LOCAL,
            description = "Classify the contract's overall risk level.",
            tags = {"risk", "assess", "classify", "contract", "level"})
    public RiskLevel detectRiskLevel(String text) {
        return null; // supplied by PrOOPt from the model response
    }

    // ---- Elevated AI zone: explicit cloud authority over redacted input. ----

    @PromptFunction(
            prompt = "Summarize the following contract in 2-3 plain-English sentences. {text}",
            model = ModelTier.CLOUD_ADVANCED,
            description = "Summarize the contract in a few plain-English sentences.",
            tags = {"summary", "summarize", "abstract", "contract", "overview"})
    public String generateSummary(@SensitiveData(label = "***CONTRACT-TEXT***") String text) {
        return null; // supplied by PrOOPt from the model response
    }

    // ---- Optional lifecycle hooks (BaseOrchestrator). ----

    @Override
    protected void onRunComplete(String traceId, long totalDurationMs, int functionsCount) {
        audit.info("[EXAMPLE] LegalAnalyzer run {} finished in {}ms across {} functions",
                traceId, totalDurationMs, functionsCount);
    }

    @Override
    protected void onError(FunctionCall call, Throwable error) {
        audit.warn("[EXAMPLE] step '{}' failed: {}", call.name(), error.getMessage());
    }
}
