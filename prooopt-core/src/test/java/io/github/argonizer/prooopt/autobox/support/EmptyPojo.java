/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.autobox.support;

/** Test POJO with no declared instance fields, for the PojoIntrospectionException path. */
public class EmptyPojo {
    public static final String CONSTANT = "x"; // static — not an instance field
}
