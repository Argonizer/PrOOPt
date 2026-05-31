/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.registry;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Coerces loosely-typed argument values — JSON literals from an execution plan, or upstream step
 * results — into the concrete parameter type a {@code MethodHandle} expects. Keeps the registry's
 * invocation path tolerant of the inevitable {@code "5"}-where-an-{@code int}-is-wanted mismatches a
 * planner model produces.
 */
public final class ArgumentCoercion {

    private ArgumentCoercion() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object coerce(Object value, Class<?> target) {
        if (target == null || target == Object.class) {
            return value;
        }
        if (value == null) {
            if (target.isPrimitive()) {
                throw new IllegalArgumentException("null cannot be passed to primitive " + target.getName());
            }
            return null;
        }
        if (target.isInstance(value)) {
            return value;
        }
        if (target == String.class || CharSequence.class.isAssignableFrom(target)) {
            return String.valueOf(value);
        }
        if (target == boolean.class || target == Boolean.class) {
            return toBoolean(value);
        }
        if (target == char.class || target == Character.class) {
            String s = String.valueOf(value);
            if (s.isEmpty()) {
                throw new IllegalArgumentException("cannot coerce empty value to char");
            }
            return s.charAt(0);
        }
        if (isNumeric(target)) {
            return toNumber(value, target);
        }
        if (target.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) target, String.valueOf(value).trim().toUpperCase());
        }
        // Best effort: hand the value through and let the call fail loudly if truly incompatible.
        return value;
    }

    private static boolean isNumeric(Class<?> t) {
        return t == int.class || t == Integer.class
                || t == long.class || t == Long.class
                || t == double.class || t == Double.class
                || t == float.class || t == Float.class
                || t == short.class || t == Short.class
                || t == byte.class || t == Byte.class
                || t == BigInteger.class || t == BigDecimal.class;
    }

    private static Object toNumber(Object value, Class<?> target) {
        double d;
        if (value instanceof Number n) {
            d = n.doubleValue();
            if (target == int.class || target == Integer.class) {
                return n.intValue();
            }
            if (target == long.class || target == Long.class) {
                return n.longValue();
            }
            if (target == double.class || target == Double.class) {
                return n.doubleValue();
            }
            if (target == float.class || target == Float.class) {
                return n.floatValue();
            }
            if (target == short.class || target == Short.class) {
                return n.shortValue();
            }
            if (target == byte.class || target == Byte.class) {
                return n.byteValue();
            }
            if (target == BigInteger.class) {
                return BigInteger.valueOf(n.longValue());
            }
            return BigDecimal.valueOf(d);
        }
        String s = String.valueOf(value).trim();
        if (target == int.class || target == Integer.class) {
            return (int) Math.rint(Double.parseDouble(s));
        }
        if (target == long.class || target == Long.class) {
            return (long) Math.rint(Double.parseDouble(s));
        }
        if (target == double.class || target == Double.class) {
            return Double.parseDouble(s);
        }
        if (target == float.class || target == Float.class) {
            return Float.parseFloat(s);
        }
        if (target == short.class || target == Short.class) {
            return (short) Math.rint(Double.parseDouble(s));
        }
        if (target == byte.class || target == Byte.class) {
            return (byte) Math.rint(Double.parseDouble(s));
        }
        if (target == BigInteger.class) {
            return new BigInteger(s);
        }
        return new BigDecimal(s);
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue() != 0.0;
        }
        String s = String.valueOf(value).trim().toLowerCase();
        return s.equals("true") || s.equals("yes") || s.equals("y") || s.equals("t") || s.equals("1");
    }
}
