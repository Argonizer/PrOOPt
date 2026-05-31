/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables classpath scanning for {@link PromptFunction} and {@link CodeFunction} methods so they are
 * discovered and registered as tools at startup. When neither attribute is set, the annotated type's
 * own package is scanned.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PromptFunctionScan {

    /** Base packages to scan, by name. */
    String[] basePackages() default {};

    /** Base packages to scan, identified by a marker class within each package. */
    Class<?>[] basePackageClasses() default {};
}
