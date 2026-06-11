/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.annotation;

import io.github.argonizer.states.event.Direction;
import io.github.argonizer.states.event.LifecycleEvent;
import io.github.argonizer.states.event.LoopDepth;
import io.github.argonizer.states.event.PersonaEventType;
import io.github.argonizer.states.event.Trend;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Routes persona events to the annotated handler method.
 *
 * <p>This is the <em>single</em> subscriber annotation — there are no per-event-type
 * annotations. The {@link PersonaEventType} discriminator selects which category of
 * event triggers the handler; the optional discriminators ({@link #trait()},
 * {@link #threshold()}, {@link #direction()}, etc.) narrow it further.
 *
 * <p>The handler method must declare exactly one parameter whose type is compatible
 * with the event object for the declared {@link #type()}. The starter's
 * {@code SubscriberBeanPostProcessor} validates this at registration time.
 *
 * <p>Example:
 * <pre>{@code
 * @PersonaSubscriber
 * public class NpcAlertService {
 *
 *     @OnPersonaEvent(persona = GuardNpc.class,
 *                     type    = PersonaEventType.THRESHOLD_CROSSED,
 *                     trait   = "suspicion_level",
 *                     threshold = 80, direction = Direction.ABOVE)
 *     public void onHighSuspicion(TraitChangeEvent<GuardNpc> event) { ... }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnPersonaEvent {

    /** The persona class this handler listens for. */
    Class<?> persona();

    /** The event category that triggers this handler. */
    PersonaEventType type();

    /** Trait name filter (snake_case). Used with {@link PersonaEventType#TRAIT_CHANGED}
     *  or {@link PersonaEventType#THRESHOLD_CROSSED}. Empty = match any trait. */
    String trait() default "";

    /** Multiple trait names; any match triggers the handler. */
    String[] traits() default {};

    /** Custom metric name filter. Used with {@link PersonaEventType#METRIC_CROSSED}. */
    String metric() default "";

    /** Threshold value for crossing events. {@link Double#NaN} = unset. */
    double threshold() default Double.NaN;

    /** Direction of the threshold crossing. */
    Direction direction() default Direction.ABOVE;

    /** Lifecycle event filter. Used with {@link PersonaEventType#LIFECYCLE}. */
    LifecycleEvent lifecycle() default LifecycleEvent.NONE;

    /** Trend filter for population-trend events. */
    Trend trend() default Trend.NONE;

    /** Loop depth filter for escalation events. */
    LoopDepth depth() default LoopDepth.SHALLOW;
}
