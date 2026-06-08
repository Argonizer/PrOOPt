/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Base class for all persona events.
 *
 * <p>Use the fluent builder via {@link #define()} to construct named, described,
 * severity-tagged events for broadcast through the event bus.
 */
public class PersonaEvent {

    private final String name;
    private final String description;
    private final PersonaEventType type;
    private final EventSeverity severity;
    private final String personaId;
    private final String personaType;
    private final List<String> affectedTraits;
    private final Instant occurredAt;

    protected PersonaEvent(Builder<?> b) {
        this.name = b.name;
        this.description = b.description;
        this.type = b.type;
        this.severity = b.severity;
        this.personaId = b.personaId;
        this.personaType = b.personaType;
        this.affectedTraits = List.copyOf(b.affectedTraits);
        this.occurredAt = b.occurredAt != null ? b.occurredAt : Instant.now();
    }

    public String name()           { return name; }
    public String description()    { return description; }
    public PersonaEventType type() { return type; }
    public EventSeverity severity(){ return severity; }
    public String personaId()      { return personaId; }
    public String personaType()    { return personaType; }
    public List<String> affectedTraits() { return affectedTraits; }
    public Instant occurredAt()    { return occurredAt; }

    public static Builder<?> define() { return new ConcreteBuilder(); }

    @SuppressWarnings("unchecked")
    public static abstract class Builder<B extends Builder<B>> {
        private String name = "";
        private String description = "";
        private PersonaEventType type = PersonaEventType.TRAIT_CHANGED;
        private EventSeverity severity = EventSeverity.LOW;
        private String personaId = "";
        private String personaType = "";
        private List<String> affectedTraits = List.of();
        private Instant occurredAt;

        public B named(String name)               { this.name = name; return (B) this; }
        public B description(String d)            { this.description = d; return (B) this; }
        public B type(PersonaEventType t)         { this.type = t; return (B) this; }
        public B severity(EventSeverity s)        { this.severity = s; return (B) this; }
        public B personaId(String id)             { this.personaId = id; return (B) this; }
        public B personaType(String pt)           { this.personaType = pt; return (B) this; }
        public B affects(String... traits)        { this.affectedTraits = List.of(traits); return (B) this; }
        public B occurredAt(Instant t)            { this.occurredAt = t; return (B) this; }

        public PersonaEvent build() { return new PersonaEvent(this); }
    }

    private static final class ConcreteBuilder extends Builder<ConcreteBuilder> {}
}
