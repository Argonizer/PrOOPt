/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

/**
 * Represents a row in {@code prooopt_persona_index}.
 * One row per {@code @Trait(index=true)} field per persona.
 */
public class PersonaIndexRecord {

    private String personaId;
    private String personaType;
    private String traitName;
    private String traitValue;
    private String traitType;

    public PersonaIndexRecord() {}

    public PersonaIndexRecord(String personaId, String personaType,
                              String traitName, String traitValue, String traitType) {
        this.personaId = personaId;
        this.personaType = personaType;
        this.traitName = traitName;
        this.traitValue = traitValue;
        this.traitType = traitType;
    }

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public String getPersonaType() { return personaType; }
    public void setPersonaType(String personaType) { this.personaType = personaType; }

    public String getTraitName() { return traitName; }
    public void setTraitName(String traitName) { this.traitName = traitName; }

    public String getTraitValue() { return traitValue; }
    public void setTraitValue(String traitValue) { this.traitValue = traitValue; }

    public String getTraitType() { return traitType; }
    public void setTraitType(String traitType) { this.traitType = traitType; }
}
