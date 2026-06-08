/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.personas.npc;

import io.github.argonizer.states.annotation.Persona;
import io.github.argonizer.states.annotation.PersonaId;
import io.github.argonizer.states.annotation.Trait;
import io.github.argonizer.states.event.LoopDepth;
import io.github.argonizer.prooopt.annotation.PromptFunction;

/**
 * NPC guard persona with duty, suspicion, and relationship traits.
 */
@Persona(
    value = "A city guard NPC with duty orientation, suspicion levels, and relationships with players/factions.",
    trackHistory = true,
    evolutionSchedule = "@daily",
    evolutionDescription = "Guards age, accumulate experiences, and shift political allegiances over time.",
    internalLoop = true,
    loopDepth = LoopDepth.SHALLOW,
    loopQuietPeriodHours = 12,
    loopDescription = "Process recent encounters and update suspicion and trust levels."
)
public class GuardNpc {

    @PersonaId
    private String id;

    // --- Identity (fixed) ---

    @Trait("[FIXED] Guard's name and rank, e.g. 'Sergeant Maren Dusk'.")
    private String nameAndRank;

    @Trait("[FIXED] Assigned patrol zone, e.g. 'East Gate District'.")
    private String patrolZone;

    @Trait("[FIXED] Faction affiliation, e.g. 'City Watch', 'Merchant Guild Guard'.")
    private String faction;

    // --- Core attributes ---

    @Trait(value = "Duty orientation — how strictly the guard follows orders: 0 (corrupt/negligent) to 100 (zealously dutiful).", index = true)
    private int dutyOrientation;

    @Trait(value = "General suspicion level: 0 (naive) to 100 (paranoid).", index = true)
    private int suspicion;

    @Trait(value = "Trust in the player character: -100 (wants to arrest) to 100 (trusted ally).", index = true)
    private int trustInPlayer;

    @Trait(value = "Bribability: 0 (incorruptible) to 100 (will take any bribe).", index = true)
    private int bribability;

    @Trait(value = "Current alert state: NORMAL, ELEVATED, HIGH, LOCKDOWN.", index = true)
    private String alertState;

    @Trait(value = "Morale: 0 (desertion risk) to 100 (highly motivated).", index = true)
    private int morale;

    @Trait(value = "Fatigue: 0 (fully rested) to 100 (exhausted).", index = true)
    private int fatigue;

    @Trait(value = "Political sympathy: -100 (rebel sympathiser) to 100 (loyalist).", index = true)
    private int politicalSympathy;

    @Trait(value = "Narrative of notable encounters and events affecting current disposition.", index = false)
    private String encounterLog;

    public GuardNpc() {}

    public GuardNpc(String id, String nameAndRank) {
        this.id = id;
        this.nameAndRank = nameAndRank;
    }

    // Getters / setters

    public String getId()               { return id; }
    public void setId(String v)         { this.id = v; }
    public String getNameAndRank()      { return nameAndRank; }
    public void setNameAndRank(String v){ this.nameAndRank = v; }
    public String getPatrolZone()       { return patrolZone; }
    public void setPatrolZone(String v) { this.patrolZone = v; }
    public String getFaction()          { return faction; }
    public void setFaction(String v)    { this.faction = v; }
    public int getDutyOrientation()     { return dutyOrientation; }
    public void setDutyOrientation(int v){ this.dutyOrientation = v; }
    public int getSuspicion()           { return suspicion; }
    public void setSuspicion(int v)     { this.suspicion = v; }
    public int getTrustInPlayer()       { return trustInPlayer; }
    public void setTrustInPlayer(int v) { this.trustInPlayer = v; }
    public int getBribability()         { return bribability; }
    public void setBribability(int v)   { this.bribability = v; }
    public String getAlertState()       { return alertState; }
    public void setAlertState(String v) { this.alertState = v; }
    public int getMorale()              { return morale; }
    public void setMorale(int v)        { this.morale = v; }
    public int getFatigue()             { return fatigue; }
    public void setFatigue(int v)       { this.fatigue = v; }
    public int getPoliticalSympathy()   { return politicalSympathy; }
    public void setPoliticalSympathy(int v){ this.politicalSympathy = v; }
    public String getEncounterLog()     { return encounterLog; }
    public void setEncounterLog(String v){ this.encounterLog = v; }

    // --- Intrinsic rules ---

    @PromptFunction(prompt = "Evaluate whether the guard would attempt to flee or fight based on current state.")
    public String evaluateFlight() {
        return "If morale < 20 and fatigue > 80 and trustInPlayer < -30, the guard will attempt to flee. "
             + "If dutyOrientation > 80, the guard will not flee regardless of morale. "
             + "High suspicion combined with low trustInPlayer increases alert escalation probability.";
    }
}
