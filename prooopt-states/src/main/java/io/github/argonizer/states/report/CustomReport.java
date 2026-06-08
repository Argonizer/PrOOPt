/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

import java.time.Instant;

public record CustomReport(
        String prompt,
        String content,
        Instant generatedAt
) {
    public String toMarkdown() { return "# Custom Report\n\n" + content; }
}
