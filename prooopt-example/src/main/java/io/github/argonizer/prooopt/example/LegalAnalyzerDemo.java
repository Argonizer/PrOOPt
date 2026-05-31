/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.example;

import io.github.argonizer.prooopt.PrOOPt;
import io.github.argonizer.prooopt.PrOOPtRuntime;
import io.github.argonizer.prooopt.stream.PromptStream;

/**
 * Runs the {@link LegalAnalyzer} end-to-end against the deterministic {@link MockModelRouter}: builds a
 * PrOOPt runtime, orchestrates a contract through a two-phase plan, and also shows the fluent
 * {@link PromptStream} API. No API key or network required — swap in a real runtime module's router for
 * live inference.
 */
public final class LegalAnalyzerDemo {

    private static final String CONTRACT = """
            MUTUAL NON-DISCLOSURE AGREEMENT between Acme Corp and Beta LLC, entered into on January 15,
            2026. Each party agrees to keep the other's confidential information secret for a period of
            three years. The agreement includes broad indemnification obligations for any unauthorized
            disclosure, and is governed by the laws of Delaware.
            """;

    private LegalAnalyzerDemo() {
    }

    public static void main(String[] args) {
        LegalAnalyzer analyzer = new LegalAnalyzer();

        PrOOPtRuntime prooopt = PrOOPt.builder()
                .router(new MockModelRouter())
                .registerInstance(analyzer)
                .build();

        System.out.println("=== Two-phase orchestration ===");
        Object summary = prooopt.orchestrate(analyzer, CONTRACT);
        System.out.println("Summary: " + summary);

        System.out.println("\n=== Fluent PromptStream ===");
        LegalAnalyzer proxied = prooopt.proxy(analyzer);
        String streamed = PromptStream.of(CONTRACT)
                .pipe(proxied::normalizeWhitespace)   // @CodeFunction
                .pipe(proxied::generateSummary)       // @PromptFunction CLOUD_ADVANCED
                .withTimeout(5000)
                .execute();
        System.out.println("Streamed summary: " + streamed);
    }
}
