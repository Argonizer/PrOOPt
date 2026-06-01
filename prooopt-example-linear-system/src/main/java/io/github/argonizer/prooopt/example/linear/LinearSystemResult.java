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

/**
 * The solution to the 3×3 linear system together with human-readable fraction representations.
 *
 * <p>Exact solution:
 * <ul>
 *   <li>x = −131/5 = −26.2</li>
 *   <li>y =  143/5 = 28.6</li>
 *   <li>z =  113/5 = 22.6</li>
 * </ul>
 *
 * <p>Jackson-deserializable: PrOOPt's autoboxer will reconstruct this from the model's JSON output.
 */
public final class LinearSystemResult {

    private final double x;
    private final double y;
    private final double z;
    private final String xFraction;
    private final String yFraction;
    private final String zFraction;
    private final String interpretation;
    private final boolean verified;

    @JsonCreator
    public LinearSystemResult(
            @JsonProperty("x") double x,
            @JsonProperty("y") double y,
            @JsonProperty("z") double z,
            @JsonProperty("xFraction") String xFraction,
            @JsonProperty("yFraction") String yFraction,
            @JsonProperty("zFraction") String zFraction,
            @JsonProperty("interpretation") String interpretation,
            @JsonProperty("verified") boolean verified) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xFraction = xFraction;
        this.yFraction = yFraction;
        this.zFraction = zFraction;
        this.interpretation = interpretation;
        this.verified = verified;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getXFraction() { return xFraction; }
    public String getYFraction() { return yFraction; }
    public String getZFraction() { return zFraction; }
    public String getInterpretation() { return interpretation; }
    public boolean isVerified() { return verified; }

    @Override
    public String toString() {
        return String.format(
                "LinearSystemResult{x=%s (%s), y=%s (%s), z=%s (%s), verified=%s, interpretation='%s'}",
                x, xFraction, y, yFraction, z, zFraction, verified, interpretation);
    }
}
