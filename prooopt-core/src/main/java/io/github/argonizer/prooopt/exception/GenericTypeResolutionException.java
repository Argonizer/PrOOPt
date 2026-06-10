/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.exception;

/**
 * Thrown by {@code PrOOPtAutoBoxer} when a method's generated descriptor cannot be located, or when
 * its generic type arguments cannot be resolved — for example a {@code PriorityQueue<T>} or
 * {@code TreeSet<T>} whose element type does not implement {@link java.lang.Comparable}.
 */
public final class GenericTypeResolutionException extends PrOOPtException {

    public GenericTypeResolutionException(String message) {
        super(message);
        AuditErrorLog.record("GenericTypeResolutionException", message);
    }

    public GenericTypeResolutionException(String message, Throwable cause) {
        super(message, cause);
        AuditErrorLog.record("GenericTypeResolutionException", message);
    }

    /** No generated descriptor was found on the classpath for the given service/method. */
    public static GenericTypeResolutionException descriptorNotFound(
            String method, String serviceSimpleName, String descriptorClassName) {
        return new GenericTypeResolutionException("""
                [PrOOPt] GenericTypeResolutionException in %s
                  Method    : %s
                  Cause     : No PromptDescriptor found for %s.
                              The descriptor class '%s' was not
                              found on the classpath.
                  Fix       : Ensure prooopt-apt is declared as an annotationProcessorPath
                              in your maven-compiler-plugin configuration and that
                              'mvn clean compile' has been run to regenerate descriptors.
                              If using an IDE, trigger a full project rebuild.\
                """.formatted(method, method, serviceSimpleName, descriptorClassName));
    }

    /** A natural-ordering collection ({@code PriorityQueue}/{@code TreeSet}) got a non-Comparable element. */
    public static GenericTypeResolutionException notComparable(
            String method, String returns, String elementSimpleName) {
        return new GenericTypeResolutionException("""
                [PrOOPt] GenericTypeResolutionException in %s
                  Method    : %s
                  Returns   : %s
                  Cause     : Element type '%s' does not implement java.lang.Comparable.
                              PriorityQueue and TreeSet require natural ordering on their
                              element type to function correctly.
                  Fix       : Either implement Comparable<%s> on %s, or change the
                              return type to Queue<%s> or List<%s> if ordering is not required.\
                """.formatted(method, method, returns, elementSimpleName,
                elementSimpleName, elementSimpleName, elementSimpleName, elementSimpleName));
    }
}
