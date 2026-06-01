/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.registry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ArgumentCoercionTest {

    @Test
    void parsesBracketedStringIntoDoubleArray() {
        Object result = ArgumentCoercion.coerce("[1.0, 1.0, 1.0, 25.0]", double[].class);
        assertInstanceOf(double[].class, result);
        assertArrayEquals(new double[]{1.0, 1.0, 1.0, 25.0}, (double[]) result, 1e-12);
    }

    @Test
    void parsesBareCommaSeparatedStringIntoDoubleArray() {
        Object result = ArgumentCoercion.coerce("5,3,2,0", double[].class);
        assertArrayEquals(new double[]{5, 3, 2, 0}, (double[]) result, 1e-12);
    }

    @Test
    void coercesListIntoIntArrayElementByElement() {
        Object result = ArgumentCoercion.coerce(List.of("1", 2, 3.0), int[].class);
        assertArrayEquals(new int[]{1, 2, 3}, (int[]) result);
    }

    @Test
    void passesExistingDoubleArrayThroughUnchanged() {
        double[] original = {-26.2, 28.6, 22.6};
        Object result = ArgumentCoercion.coerce(original, double[].class);
        assertArrayEquals(original, (double[]) result, 1e-12);
    }

    @Test
    void parsesQuotedStringArray() {
        Object result = ArgumentCoercion.coerce("[\"-131/5\", \"143/5\", \"113/5\"]", String[].class);
        assertArrayEquals(new String[]{"-131/5", "143/5", "113/5"}, (String[]) result);
    }

    @Test
    void emptyBracketsYieldEmptyArray() {
        Object result = ArgumentCoercion.coerce("[]", double[].class);
        assertEquals(0, ((double[]) result).length);
    }
}
