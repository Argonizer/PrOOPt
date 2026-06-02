/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.example.bodmas;

import org.junit.jupiter.api.Test;

import static io.github.argonizer.prooopt.example.bodmas.BodmasFunctions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-Java unit tests for every {@code @CodeFunction}. No Spring, no LLM, no network.
 */
class BodmasFunctionsTest {

    @Test void add_positiveNumbers() { assertEquals(7.0, add(3.0, 4.0)); }

    @Test void add_negativeAndPositive() { assertEquals(-2.0, add(-5.0, 3.0)); }

    @Test void subtract_basic() { assertEquals(7.0, subtract(10.0, 3.0)); }

    @Test void subtract_producesNegative() { assertEquals(-7.0, subtract(3.0, 10.0)); }

    @Test void multiply_basic() { assertEquals(12.0, multiply(4.0, 3.0)); }

    @Test void multiply_byZero() { assertEquals(0.0, multiply(999.0, 0.0)); }

    @Test void divide_basic() { assertEquals(3.0, divide(12.0, 4.0)); }

    @Test
    void divide_byZero_throwsArithmeticException() {
        ArithmeticException ex = assertThrows(ArithmeticException.class, () -> divide(10.0, 0.0));
        assertTrue(ex.getMessage().contains("Division by zero"));
    }

    @Test
    void divide_nearZero_throwsArithmeticException() {
        assertThrows(ArithmeticException.class, () -> divide(10.0, 1e-16));
    }

    @Test void power_squaredInteger() { assertEquals(9.0, power(3.0, 2.0), 1e-10); }

    @Test void power_cubed() { assertEquals(8.0, power(2.0, 3.0), 1e-10); }

    @Test void power_zeroExponent() { assertEquals(1.0, power(5.0, 0.0)); }

    @Test void sqrt_perfectSquare() { assertEquals(5.0, sqrt(25.0), 1e-10); }

    @Test
    void sqrt_compositeExpression() {
        assertEquals(5.0, sqrt(add(16.0, 9.0)), 1e-10);
    }

    @Test
    void sqrt_negativeNumber_throwsArithmeticException() {
        ArithmeticException ex = assertThrows(ArithmeticException.class, () -> sqrt(-4.0));
        assertTrue(ex.getMessage().contains("negative"));
    }

    @Test void factorial_five() { assertEquals(120.0, factorial(5)); }

    @Test void factorial_zero() { assertEquals(1.0, factorial(0)); }

    @Test
    void factorial_negative_throwsArithmeticException() {
        assertThrows(ArithmeticException.class, () -> factorial(-1));
    }

    @Test
    void factorial_tooLarge_throwsArithmeticException() {
        ArithmeticException ex = assertThrows(ArithmeticException.class, () -> factorial(21));
        assertTrue(ex.getMessage().contains("overflow"));
    }

    @Test void modulo_basic() { assertEquals(1.0, modulo(10.0, 3.0), 1e-10); }

    @Test void negate_positive() { assertEquals(-5.0, negate(5.0)); }

    @Test void negate_negative() { assertEquals(3.0, negate(-3.0)); }

    @Test void absolute_negative() { assertEquals(7.5, absolute(-7.5)); }

    @Test void absolute_positive() { assertEquals(7.5, absolute(7.5)); }

    @Test void assertAnswer_pass_returnsTrue() { assertTrue(assertAnswer(13.5, 13.5)); }

    @Test
    void assertAnswer_withinTolerance_returnsTrue() {
        assertTrue(assertAnswer(13.5 + 1e-10, 13.5));
    }

    @Test
    void assertAnswer_fail_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> assertAnswer(13.5, 14.0));
    }

    @Test void formatResult_wholeNumber_noDecimal() { assertEquals("13", formatResult(13.0)); }

    @Test void formatResult_decimal_noTrailingZeros() { assertEquals("13.5", formatResult(13.5)); }

    @Test void formatResult_primaryAssertionValue() { assertEquals("13.5", formatResult(13.5)); }

    /**
     * Ground-truth verification: manually chain all 14 {@code @CodeFunction} calls of the primary
     * problem — no LLM, no orchestrator — and prove the result is exactly 13.5.
     */
    @Test
    void primaryAssertion_allStepsVerified() {
        double b1  = add(8, 4);            // 12.0
        double b2  = add(2, 4);            // 6.0
        double b3  = add(16, 9);           // 25.0
        double o1  = power(3, 2);          // 9.0
        double o2  = factorial(5);         // 120.0
        double o3  = power(4, 2);          // 16.0
        double o4  = sqrt(b3);             // 5.0
        double dm1 = multiply(b1, o1);     // 108.0
        double dm2 = multiply(o3, 5);      // 80.0
        double dm3 = subtract(dm1, 6);     // 102.0
        double dm4 = divide(dm3, b2);      // 17.0
        double dm5 = divide(o2, dm2);      // 1.5
        double as1 = add(dm4, dm5);        // 18.5
        double result = subtract(as1, o4); // 13.5

        assertTrue(assertAnswer(result, 13.5));
        assertEquals("13.5", formatResult(result));
    }
}
