/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.dynamic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ThreadLocal session cache for LLM-generated dynamic prompt functions.
 *
 * <p>Each orchestration run gets its own isolated cache. Functions registered here are invisible to
 * other runs, other threads, and future executions. The cache is cleared automatically when
 * {@code PrOOPtContext.clear()} is called at run end — which MUST invoke {@link #clear()}.
 */
public final class DynamicFunctionCache {

    private static final ThreadLocal<Map<String, DynamicPromptFunction>> SESSION =
            ThreadLocal.withInitial(HashMap::new);

    private DynamicFunctionCache() {
    }

    public static void register(DynamicPromptFunction fn) {
        SESSION.get().put(fn.name(), fn);
    }

    public static Optional<DynamicPromptFunction> find(String name) {
        return Optional.ofNullable(SESSION.get().get(name));
    }

    public static boolean contains(String name) {
        return SESSION.get().containsKey(name);
    }

    public static int count() {
        return SESSION.get().size();
    }

    /** Called by {@code PrOOPtContext.clear()} — do not call directly. */
    public static void clear() {
        SESSION.remove();
    }
}
