/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin.internal;

/** Outcome of semantically classifying a prompt against its declared return type. Internal only. */
public enum ClassificationResult {
    VALID,
    INVALID,
    UNCERTAIN
}
