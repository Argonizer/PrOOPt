/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

import java.time.Instant;

/**
 * Represents a row in {@code prooopt_persona_history}.
 */
public class PersonaHistoryRecord {

    private Long historyId;
    private String personaId;
    private String personaType;
    private Instant changedAt;
    private long stateVersion;
    private String promptInput;
    private String fieldsChanged;
    private String fullStateAfter;
    private String updateSource;

    public PersonaHistoryRecord() {}

    public Long getHistoryId() { return historyId; }
    public void setHistoryId(Long historyId) { this.historyId = historyId; }

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public String getPersonaType() { return personaType; }
    public void setPersonaType(String personaType) { this.personaType = personaType; }

    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }

    public long getStateVersion() { return stateVersion; }
    public void setStateVersion(long stateVersion) { this.stateVersion = stateVersion; }

    public String getPromptInput() { return promptInput; }
    public void setPromptInput(String promptInput) { this.promptInput = promptInput; }

    public String getFieldsChanged() { return fieldsChanged; }
    public void setFieldsChanged(String fieldsChanged) { this.fieldsChanged = fieldsChanged; }

    public String getFullStateAfter() { return fullStateAfter; }
    public void setFullStateAfter(String fullStateAfter) { this.fullStateAfter = fullStateAfter; }

    public String getUpdateSource() { return updateSource; }
    public void setUpdateSource(String updateSource) { this.updateSource = updateSource; }
}
