/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the unique identifier field of a {@link Persona}-annotated class.
 *
 * <p>Exactly one field per persona class must carry this annotation.
 * {@link io.github.argonizer.states.meta.PersonaMetaReader} validates the constraint
 * at startup and throws a configuration exception if it is violated.
 *
 * <p>The id field is never sent to the LLM and is never modified by it.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PersonaId {}
