/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

public final class TraitChangeEvent extends PersonaEvent {
    private final String traitName;
    private final Object before;
    private final Object after;

    public TraitChangeEvent(Builder b) {
        super(b);
        this.traitName = b.traitName;
        this.before = b.before;
        this.after = b.after;
    }

    public String traitName() { return traitName; }
    public Object before()    { return before; }
    public Object after()     { return after; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder extends PersonaEvent.Builder<Builder> {
        private String traitName;
        private Object before;
        private Object after;

        public Builder traitName(String n) { this.traitName = n; return this; }
        public Builder before(Object v)    { this.before = v; return this; }
        public Builder after(Object v)     { this.after = v; return this; }

        @Override public TraitChangeEvent build() {
            type(PersonaEventType.TRAIT_CHANGED);
            return new TraitChangeEvent(this);
        }
    }
}
