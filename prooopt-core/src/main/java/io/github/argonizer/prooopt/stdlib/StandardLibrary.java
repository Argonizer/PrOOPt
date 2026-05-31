/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.stdlib;

import io.github.argonizer.prooopt.model.ToolDescriptor;
import io.github.argonizer.prooopt.registry.FunctionScanner;

import java.util.List;

/**
 * The PrOOPt standard library: an extensive set of ready-to-use, deterministic {@code @CodeFunction}s
 * so users never redefine the basics. These are auto-indexed at startup and compete for selection on
 * semantic relevance exactly like user-defined functions.
 */
public final class StandardLibrary {

    private StandardLibrary() {
    }

    /** All standard-library function-holder classes. */
    public static Class<?>[] classes() {
        return new Class<?>[] {
                ArithmeticFunctions.class,
                FinancialFunctions.class,
                StatisticalFunctions.class,
                StringFunctions.class,
                DateFunctions.class,
                CollectionFunctions.class,
                ValidationFunctions.class
        };
    }

    /** Descriptors for every standard-library function, ready to register and index. */
    public static List<ToolDescriptor> descriptors() {
        return FunctionScanner.scan(classes());
    }
}
