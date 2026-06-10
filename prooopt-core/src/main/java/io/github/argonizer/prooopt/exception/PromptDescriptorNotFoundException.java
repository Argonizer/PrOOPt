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
 * Thrown at proxy initialization time when the interceptor cannot find the generated
 * {@code <Interface>PromptDescriptor} class for an annotated interface — usually because the
 * {@code prooopt-apt} processor did not run, or the interface was added since the last compile.
 */
public final class PromptDescriptorNotFoundException extends PrOOPtException {

    public PromptDescriptorNotFoundException(String message) {
        super(message);
        AuditErrorLog.record("PromptDescriptorNotFoundException", message);
    }

    public PromptDescriptorNotFoundException(String message, Throwable cause) {
        super(message, cause);
        AuditErrorLog.record("PromptDescriptorNotFoundException", message);
    }

    /** Builds the canonical message naming the expected descriptor and the annotationProcessorPaths fix. */
    public static PromptDescriptorNotFoundException forInterface(
            String interfaceFqn, String descriptorFqn) {
        return new PromptDescriptorNotFoundException("""
                [PrOOPt] PromptDescriptorNotFoundException
                  Interface : %s
                  Expected  : %s
                  Cause     : The descriptor class was not found on the classpath.
                               This typically means the APT did not run, or the interface
                               was added after the last full compilation.
                  Fix       : Run 'mvn clean compile' to trigger APT descriptor generation.
                               Ensure prooopt-apt is listed under <annotationProcessorPaths>
                               in your maven-compiler-plugin configuration:
                               <annotationProcessorPaths>
                                 <path>
                                   <groupId>io.github.argonizer</groupId>
                                   <artifactId>prooopt-apt</artifactId>
                                 </path>
                               </annotationProcessorPaths>\
                """.formatted(interfaceFqn, descriptorFqn));
    }
}
