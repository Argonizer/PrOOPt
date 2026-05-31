/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.registry;

/**
 * Supplies the owning instance for an instance-method tool. Spring wires this to the application
 * context (returning managed beans); plain-Java usage falls back to a pooled, reflectively-created
 * singleton.
 */
@FunctionalInterface
public interface InstanceResolver {

    /** Returns an instance of {@code type}, or {@code null} to defer to the registry's own pool. */
    Object resolve(Class<?> type);
}
