/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Defines which personas are affected when an event is broadcast.
 */
public final class BlastRadius {

    public enum Scope { ALL, WITNESSED, FACTION, RELATIONSHIP, PROXIMITY, COUNT }

    private final Scope scope;
    private final int count;
    private final List<BlastRadius> union;
    private final List<BlastRadius> intersection;

    private BlastRadius(Scope scope, int count, List<BlastRadius> union, List<BlastRadius> intersection) {
        this.scope = scope;
        this.count = count;
        this.union = union;
        this.intersection = intersection;
    }

    public static final BlastRadius ALL          = new BlastRadius(Scope.ALL, -1, List.of(), List.of());
    public static final BlastRadius WITNESSED    = new BlastRadius(Scope.WITNESSED, -1, List.of(), List.of());
    public static final BlastRadius FACTION      = new BlastRadius(Scope.FACTION, -1, List.of(), List.of());
    public static final BlastRadius RELATIONSHIP = new BlastRadius(Scope.RELATIONSHIP, -1, List.of(), List.of());
    public static final BlastRadius PROXIMITY    = new BlastRadius(Scope.PROXIMITY, -1, List.of(), List.of());

    public static BlastRadius count(int n) {
        return new BlastRadius(Scope.COUNT, n, List.of(), List.of());
    }

    public BlastRadius union(BlastRadius other) {
        List<BlastRadius> combined = new ArrayList<>(this.union);
        combined.add(this);
        combined.add(other);
        return new BlastRadius(null, -1, List.copyOf(combined), List.of());
    }

    public BlastRadius intersection(BlastRadius other) {
        List<BlastRadius> combined = new ArrayList<>(this.intersection);
        combined.add(this);
        combined.add(other);
        return new BlastRadius(null, -1, List.of(), List.copyOf(combined));
    }

    public Scope scope()              { return scope; }
    public int count()                { return count; }
    public List<BlastRadius> unionOf(){ return union; }
    public List<BlastRadius> intersectionOf() { return intersection; }
}
