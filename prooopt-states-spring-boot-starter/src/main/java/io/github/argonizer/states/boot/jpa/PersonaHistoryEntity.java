/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "prooopt_persona_history")
public class PersonaHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(name = "persona_id", nullable = false)
    private String personaId;

    @Column(name = "persona_type", nullable = false)
    private String personaType;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "state_version", nullable = false)
    private long stateVersion;

    @Lob
    @Column(name = "prompt_input")
    private String promptInput;

    @Column(name = "fields_changed")
    private String fieldsChanged;

    @Lob
    @Column(name = "full_state_after")
    private String fullStateAfter;

    @Column(name = "update_source", nullable = false)
    private String updateSource;

    public PersonaHistoryEntity() {}

    public Long getHistoryId()         { return historyId; }
    public String getPersonaId()       { return personaId; }
    public void setPersonaId(String v) { this.personaId = v; }
    public String getPersonaType()     { return personaType; }
    public void setPersonaType(String v){ this.personaType = v; }
    public Instant getChangedAt()      { return changedAt; }
    public void setChangedAt(Instant v){ this.changedAt = v; }
    public long getStateVersion()      { return stateVersion; }
    public void setStateVersion(long v){ this.stateVersion = v; }
    public String getPromptInput()     { return promptInput; }
    public void setPromptInput(String v){ this.promptInput = v; }
    public String getFieldsChanged()   { return fieldsChanged; }
    public void setFieldsChanged(String v){ this.fieldsChanged = v; }
    public String getFullStateAfter()  { return fullStateAfter; }
    public void setFullStateAfter(String v){ this.fullStateAfter = v; }
    public String getUpdateSource()    { return updateSource; }
    public void setUpdateSource(String v){ this.updateSource = v; }
}
