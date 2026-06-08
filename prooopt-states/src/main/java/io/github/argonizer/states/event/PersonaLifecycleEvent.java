/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

public final class PersonaLifecycleEvent extends PersonaEvent {
    private final LifecycleEvent lifecycle;
    private final String reason;

    public PersonaLifecycleEvent(Builder b) {
        super(b);
        this.lifecycle = b.lifecycle;
        this.reason = b.reason;
    }

    public LifecycleEvent lifecycle() { return lifecycle; }
    public String reason()            { return reason; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder extends PersonaEvent.Builder<Builder> {
        private LifecycleEvent lifecycle = LifecycleEvent.NONE;
        private String reason = "";

        public Builder lifecycle(LifecycleEvent e) { this.lifecycle = e; return this; }
        public Builder reason(String r)            { this.reason = r; return this; }

        @Override public PersonaLifecycleEvent build() {
            type(PersonaEventType.LIFECYCLE);
            return new PersonaLifecycleEvent(this);
        }
    }
}
