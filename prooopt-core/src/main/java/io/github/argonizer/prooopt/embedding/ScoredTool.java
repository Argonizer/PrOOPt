/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.embedding;

import io.github.argonizer.prooopt.model.ToolDescriptor;

/** A tool paired with its cosine-similarity score against a query. */
public record ScoredTool(ToolDescriptor tool, double score) {
}
