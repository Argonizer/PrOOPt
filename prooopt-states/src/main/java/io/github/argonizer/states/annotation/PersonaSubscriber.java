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
 * Marks a class as a persona event subscriber.
 *
 * <p>Classes annotated with {@code @PersonaSubscriber} are scanned by the starter's
 * {@code SubscriberBeanPostProcessor}, which registers their
 * {@link OnPersonaEvent}-annotated methods into the event bus.
 *
 * <p>The core module defines this annotation but performs no classpath scanning
 * (no Spring dependency). All wiring happens in the starter.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PersonaSubscriber {}
