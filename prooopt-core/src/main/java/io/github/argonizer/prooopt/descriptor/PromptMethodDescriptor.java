/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.descriptor;

import java.util.List;

/**
 * Captures the generic return-type information of a single {@code @PromptFunction} method, recorded by
 * the {@code prooopt-apt} processor <em>before</em> type erasure and emitted into a generated
 * {@code <Interface>PromptDescriptor} class. The AutoBoxer reads it at runtime to route structured
 * binding (e.g. {@code List<String>}, {@code Map<String, Integer>}, {@code Optional<Person>}).
 *
 * @param methodName the simple method name (e.g. {@code getLanguages})
 * @param rawType    the erased container type's fully qualified name (e.g. {@code java.util.List})
 * @param typeArgs   the fully qualified type arguments, in order (e.g. {@code [java.lang.String]});
 *                   empty for non-generic scalar return types
 */
public record PromptMethodDescriptor(String methodName, String rawType, List<String> typeArgs) {

    public PromptMethodDescriptor {
        typeArgs = typeArgs == null ? List.of() : List.copyOf(typeArgs);
    }

    /** Factory mirroring the call shape emitted by generated descriptor classes. */
    public static PromptMethodDescriptor of(String methodName, String rawType, List<String> typeArgs) {
        return new PromptMethodDescriptor(methodName, rawType, typeArgs);
    }

    /** True when the descriptor carries at least one resolved generic type argument. */
    public boolean isGeneric() {
        return !typeArgs.isEmpty();
    }
}
