/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.report;

public record TraitCorrelation(
        String traitA,
        String traitB,
        double pearsonR,
        String interpretation
) {}
