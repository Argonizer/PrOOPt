/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

public final class InternalLoopEvent extends PersonaEvent {
    private final LoopDepth depth;
    private final boolean escalated;

    public InternalLoopEvent(Builder b) {
        super(b);
        this.depth = b.depth;
        this.escalated = b.escalated;
    }

    public LoopDepth depth()  { return depth; }
    public boolean escalated(){ return escalated; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder extends PersonaEvent.Builder<Builder> {
        private LoopDepth depth = LoopDepth.SHALLOW;
        private boolean escalated = false;

        public Builder depth(LoopDepth d)  { this.depth = d; return this; }
        public Builder escalated(boolean e){ this.escalated = e; return this; }

        @Override public InternalLoopEvent build() {
            type(PersonaEventType.LOOP_ESCALATION);
            return new InternalLoopEvent(this);
        }
    }
}
