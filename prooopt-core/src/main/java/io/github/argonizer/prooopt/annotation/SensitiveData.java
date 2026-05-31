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
 * Marks a parameter, field, or type whose value must be redacted in audit logs. The annotated value
 * is replaced with {@link #label()} wherever PrOOPt would otherwise record it.
 *
 * <p>Redaction governs what is <em>logged</em>, not what is sent to a model — granting a function a
 * cloud tier is the decision that lets data leave the JVM.
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveData {

    /** The placeholder written to the audit log in place of the real value. */
    String label() default "***REDACTED***";
}
