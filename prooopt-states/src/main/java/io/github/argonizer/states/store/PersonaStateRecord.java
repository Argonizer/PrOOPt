/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

import java.time.Instant;

/**
 * Represents a row in {@code prooopt_persona_state}.
 *
 * <p>Intentionally a plain Java object (not a JPA entity) so the core module
 * has no Spring/JPA dependency. The starter provides a JPA entity subtype.
 */
public class PersonaStateRecord {

    private String personaId;
    private String personaType;
    private String state;
    private boolean retired;
    private Instant retiredAt;
    private String retirementReason;
    private String seed;
    private Instant originationDate;
    private Instant lastUpdated;
    private Instant lastEvolved;
    private String currentPhase;
    private long stateVersion;

    public PersonaStateRecord() {}

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public String getPersonaType() { return personaType; }
    public void setPersonaType(String personaType) { this.personaType = personaType; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public boolean isRetired() { return retired; }
    public void setRetired(boolean retired) { this.retired = retired; }

    public Instant getRetiredAt() { return retiredAt; }
    public void setRetiredAt(Instant retiredAt) { this.retiredAt = retiredAt; }

    public String getRetirementReason() { return retirementReason; }
    public void setRetirementReason(String retirementReason) { this.retirementReason = retirementReason; }

    public String getSeed() { return seed; }
    public void setSeed(String seed) { this.seed = seed; }

    public Instant getOriginationDate() { return originationDate; }
    public void setOriginationDate(Instant originationDate) { this.originationDate = originationDate; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public Instant getLastEvolved() { return lastEvolved; }
    public void setLastEvolved(Instant lastEvolved) { this.lastEvolved = lastEvolved; }

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }

    public long getStateVersion() { return stateVersion; }
    public void setStateVersion(long stateVersion) { this.stateVersion = stateVersion; }
}
