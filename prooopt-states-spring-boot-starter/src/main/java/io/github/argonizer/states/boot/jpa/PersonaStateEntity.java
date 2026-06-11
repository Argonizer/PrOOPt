/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.jpa;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "prooopt_persona_state")
@IdClass(PersonaStateEntity.PersonaStatePK.class)
public class PersonaStateEntity {

    @Id
    @Column(name = "persona_id")
    private String personaId;

    @Id
    @Column(name = "persona_type")
    private String personaType;

    @Lob
    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "retired", nullable = false)
    private boolean retired = false;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "retirement_reason")
    private String retirementReason;

    @Lob
    @Column(name = "seed")
    private String seed;

    @Column(name = "origination_date", nullable = false)
    private Instant originationDate;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Column(name = "last_evolved")
    private Instant lastEvolved;

    @Column(name = "current_phase")
    private String currentPhase;

    @Column(name = "state_version", nullable = false)
    private long stateVersion = 0;

    public PersonaStateEntity() {}

    // Getters / setters
    public String getPersonaId()           { return personaId; }
    public void setPersonaId(String v)     { this.personaId = v; }
    public String getPersonaType()         { return personaType; }
    public void setPersonaType(String v)   { this.personaType = v; }
    public String getState()               { return state; }
    public void setState(String v)         { this.state = v; }
    public boolean isRetired()             { return retired; }
    public void setRetired(boolean v)      { this.retired = v; }
    public Instant getRetiredAt()          { return retiredAt; }
    public void setRetiredAt(Instant v)    { this.retiredAt = v; }
    public String getRetirementReason()    { return retirementReason; }
    public void setRetirementReason(String v) { this.retirementReason = v; }
    public String getSeed()                { return seed; }
    public void setSeed(String v)          { this.seed = v; }
    public Instant getOriginationDate()    { return originationDate; }
    public void setOriginationDate(Instant v){ this.originationDate = v; }
    public Instant getLastUpdated()        { return lastUpdated; }
    public void setLastUpdated(Instant v)  { this.lastUpdated = v; }
    public Instant getLastEvolved()        { return lastEvolved; }
    public void setLastEvolved(Instant v)  { this.lastEvolved = v; }
    public String getCurrentPhase()        { return currentPhase; }
    public void setCurrentPhase(String v)  { this.currentPhase = v; }
    public long getStateVersion()          { return stateVersion; }
    public void setStateVersion(long v)    { this.stateVersion = v; }

    public static class PersonaStatePK implements Serializable {
        private String personaId;
        private String personaType;
        public PersonaStatePK() {}
        public PersonaStatePK(String personaId, String personaType) {
            this.personaId = personaId;
            this.personaType = personaType;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof PersonaStatePK pk)) return false;
            return Objects.equals(personaId, pk.personaId) && Objects.equals(personaType, pk.personaType);
        }
        @Override public int hashCode() { return Objects.hash(personaId, personaType); }
    }
}
