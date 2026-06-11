/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.personas.human;

import io.github.argonizer.states.annotation.Persona;
import io.github.argonizer.states.annotation.PersonaId;
import io.github.argonizer.states.annotation.Trait;
import io.github.argonizer.prooopt.annotation.PromptFunction;

/**
 * General-purpose human persona with neurobiological, emotional, and
 * psychological trait groups.
 */
@Persona(
    value = "A realistic human individual with neurobiological, emotional, and psychological traits.",
    trackHistory = true,
    internalLoop = true,
    loopDepth = io.github.argonizer.states.event.LoopDepth.MODERATE,
    loopQuietPeriodHours = 24
)
public class Person {

    @PersonaId
    private String id;

    // --- Identity (fixed) ---

    @Trait("[FIXED] The person's full legal name.")
    private String name;

    @Trait("[FIXED] The person's gender identity.")
    private String gender;

    @Trait("[FIXED] The person's cultural background and nationality.")
    private String culturalBackground;

    // --- Neurobiological ---

    @Trait(value = "Baseline stress response sensitivity: 0 (hypo-reactive) to 100 (hyper-reactive).", index = true)
    private int stressSensitivity;

    @Trait(value = "Dopamine baseline tone: 0–100. Higher values correlate with extraversion and reward-seeking.", index = true)
    private int dopamineTone;

    @Trait(value = "Cortisol reactivity under sustained pressure: 0–100.", index = true)
    private int cortisolReactivity;

    @Trait(value = "Neuroplasticity — capacity for belief and habit change: 0–100.", index = true)
    private int neuroplasticity;

    // --- Emotional ---

    @Trait(value = "Current emotional valence: -100 (deeply negative) to 100 (deeply positive).", index = true)
    private int emotionalValence;

    @Trait(value = "Current arousal level: 0 (lethargic) to 100 (agitated).", index = true)
    private int arousal;

    @Trait(value = "Resilience — capacity to recover from setbacks: 0–100.", index = true)
    private int resilience;

    @Trait(value = "Empathy: 0 (no empathy) to 100 (highly empathic).", index = true)
    private int empathy;

    // --- Psychological ---

    @Trait(value = "Openness to experience (Big Five): 0–100.", index = true)
    private int openness;

    @Trait(value = "Conscientiousness (Big Five): 0–100.", index = true)
    private int conscientiousness;

    @Trait(value = "Extraversion (Big Five): 0–100.", index = true)
    private int extraversion;

    @Trait(value = "Agreeableness (Big Five): 0–100.", index = true)
    private int agreeableness;

    @Trait(value = "Neuroticism (Big Five): 0–100.", index = true)
    private int neuroticism;

    @Trait(value = "Locus of control: 0 (fully external) to 100 (fully internal).", index = true)
    private int locusOfControl;

    @Trait(value = "Current self-esteem: 0–100.", index = true)
    private int selfEsteem;

    @Trait(value = "Risk tolerance: 0 (highly risk-averse) to 100 (highly risk-seeking).", index = true)
    private int riskTolerance;

    // --- Memory ---

    @Trait(value = "Summary of significant recent experiences influencing current state.", index = false)
    private String recentMemorySummary;

    @Trait(value = "Core formative memory that shapes deep beliefs.", index = false)
    private String formativeMemory;

    public Person() {}

    public Person(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters / setters

    public String getId()                   { return id; }
    public void setId(String id)            { this.id = id; }
    public String getName()                 { return name; }
    public void setName(String v)           { this.name = v; }
    public String getGender()               { return gender; }
    public void setGender(String v)         { this.gender = v; }
    public String getCulturalBackground()   { return culturalBackground; }
    public void setCulturalBackground(String v) { this.culturalBackground = v; }
    public int getStressSensitivity()       { return stressSensitivity; }
    public void setStressSensitivity(int v) { this.stressSensitivity = v; }
    public int getDopamineTone()            { return dopamineTone; }
    public void setDopamineTone(int v)      { this.dopamineTone = v; }
    public int getCortisolReactivity()      { return cortisolReactivity; }
    public void setCortisolReactivity(int v){ this.cortisolReactivity = v; }
    public int getNeuroplasticity()         { return neuroplasticity; }
    public void setNeuroplasticity(int v)   { this.neuroplasticity = v; }
    public int getEmotionalValence()        { return emotionalValence; }
    public void setEmotionalValence(int v)  { this.emotionalValence = v; }
    public int getArousal()                 { return arousal; }
    public void setArousal(int v)           { this.arousal = v; }
    public int getResilience()              { return resilience; }
    public void setResilience(int v)        { this.resilience = v; }
    public int getEmpathy()                 { return empathy; }
    public void setEmpathy(int v)           { this.empathy = v; }
    public int getOpenness()                { return openness; }
    public void setOpenness(int v)          { this.openness = v; }
    public int getConscientiousness()       { return conscientiousness; }
    public void setConscientiousness(int v) { this.conscientiousness = v; }
    public int getExtraversion()            { return extraversion; }
    public void setExtraversion(int v)      { this.extraversion = v; }
    public int getAgreeableness()           { return agreeableness; }
    public void setAgreeableness(int v)     { this.agreeableness = v; }
    public int getNeuroticism()             { return neuroticism; }
    public void setNeuroticism(int v)       { this.neuroticism = v; }
    public int getLocusOfControl()          { return locusOfControl; }
    public void setLocusOfControl(int v)    { this.locusOfControl = v; }
    public int getSelfEsteem()              { return selfEsteem; }
    public void setSelfEsteem(int v)        { this.selfEsteem = v; }
    public int getRiskTolerance()           { return riskTolerance; }
    public void setRiskTolerance(int v)     { this.riskTolerance = v; }
    public String getRecentMemorySummary()  { return recentMemorySummary; }
    public void setRecentMemorySummary(String v) { this.recentMemorySummary = v; }
    public String getFormativeMemory()      { return formativeMemory; }
    public void setFormativeMemory(String v){ this.formativeMemory = v; }

    // --- Intrinsic rules (used by prompt builder) ---

    @PromptFunction(prompt = "High cortisol reactivity amplifies stress effects; high resilience dampens them.")
    public String applyStressCascade() {
        return "If cortisolReactivity > 70, stress effects on emotionalValence are amplified by 1.5x. "
             + "If resilience > 70, recovery from negative emotionalValence is accelerated.";
    }

    @PromptFunction(prompt = "Neuroplasticity governs belief and habit update rates.")
    public String applyResilienceDecay() {
        return "High neuroplasticity (>70) allows faster trait change. "
             + "Low neuroplasticity (<30) means traits resist change; updates should be incremental.";
    }

    @PromptFunction(prompt = "Memory retention shapes how past experiences influence current state.")
    public String applyMemoryRetention() {
        return "The recentMemorySummary should be updated to reflect significant events. "
             + "The formativeMemory influences deep psychological traits and changes rarely.";
    }
}
