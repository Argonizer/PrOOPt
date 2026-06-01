/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.example.linear;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

/**
 * The solution to an n×n linear system together with human-readable fraction representations.
 *
 * <p>For the canonical 3×3 demo system:
 * <ul>
 *   <li>x₀ = −131/5 = −26.2</li>
 *   <li>x₁ =  143/5 = 28.6</li>
 *   <li>x₂ =  113/5 = 22.6</li>
 * </ul>
 *
 * <p>Jackson-deserializable: PrOOPt's autoboxer will reconstruct this from the model's JSON output.
 * The {@code values} and {@code fractions} arrays are parallel and length {@code n}.
 */
public final class LinearSystemResult {

    private final int n;
    private final double[] values;
    private final String[] fractions;
    private final String interpretation;
    private final boolean verified;

    @JsonCreator
    public LinearSystemResult(
            @JsonProperty("n") int n,
            @JsonProperty("values") double[] values,
            @JsonProperty("fractions") String[] fractions,
            @JsonProperty("interpretation") String interpretation,
            @JsonProperty("verified") boolean verified) {
        this.n = n;
        this.values = values != null ? values.clone() : new double[0];
        this.fractions = fractions != null ? fractions.clone() : new String[0];
        this.interpretation = interpretation;
        this.verified = verified;
    }

    public int getN() { return n; }
    public double[] getValues() { return values.clone(); }
    public String[] getFractions() { return fractions.clone(); }
    public String getInterpretation() { return interpretation; }
    public boolean isVerified() { return verified; }

    /** Value of the i-th variable (0-indexed). */
    public double value(int i) { return values[i]; }

    /** Fraction string of the i-th variable (0-indexed). */
    public String fraction(int i) { return fractions[i]; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LinearSystemResult{n=").append(n).append(", ");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append('x').append(i).append('=').append(values[i])
              .append(" (").append(i < fractions.length ? fractions[i] : "?").append(')');
        }
        sb.append(", verified=").append(verified)
          .append(", interpretation='").append(interpretation).append("'}");
        return sb.toString();
    }
}
