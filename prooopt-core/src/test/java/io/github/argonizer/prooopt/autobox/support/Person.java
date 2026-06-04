/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.prooopt.autobox.support;

/** Test POJO with declared fields name, age, email — used for List/Map POJO binding tests. */
public record Person(String name, Integer age, String email) {
}
