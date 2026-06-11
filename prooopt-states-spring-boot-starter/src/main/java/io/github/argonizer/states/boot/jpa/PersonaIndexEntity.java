/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "prooopt_persona_index",
        uniqueConstraints = @UniqueConstraint(columnNames = {"persona_id", "persona_type", "trait_name"}))
public class PersonaIndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "persona_id", nullable = false)
    private String personaId;

    @Column(name = "persona_type", nullable = false)
    private String personaType;

    @Column(name = "trait_name", nullable = false)
    private String traitName;

    @Column(name = "trait_value")
    private String traitValue;

    @Column(name = "trait_type", nullable = false)
    private String traitType;

    public PersonaIndexEntity() {}

    public Long getId()              { return id; }
    public String getPersonaId()     { return personaId; }
    public void setPersonaId(String v){ this.personaId = v; }
    public String getPersonaType()   { return personaType; }
    public void setPersonaType(String v){ this.personaType = v; }
    public String getTraitName()     { return traitName; }
    public void setTraitName(String v){ this.traitName = v; }
    public String getTraitValue()    { return traitValue; }
    public void setTraitValue(String v){ this.traitValue = v; }
    public String getTraitType()     { return traitType; }
    public void setTraitType(String v){ this.traitType = v; }
}
