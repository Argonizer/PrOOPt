/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the PrOOPt persona states module.
 *
 * <pre>
 * prooopt:
 *   persona:
 *     auto-ddl: true
 *     phase-manager:
 *       enabled: false
 *     model-tier: CLOUD_ADVANCED
 * </pre>
 */
@ConfigurationProperties(prefix = "prooopt.persona")
public class PersonaProperties {

    private boolean autoDdl = true;
    private PhaseManagerProperties phaseManager = new PhaseManagerProperties();
    private String modelTier = "CLOUD_ADVANCED";
    private int evolutionChunkSize = 10;
    private int loopBatchSize = 20;

    public boolean isAutoDdl()                           { return autoDdl; }
    public void setAutoDdl(boolean v)                    { this.autoDdl = v; }
    public PhaseManagerProperties getPhaseManager()      { return phaseManager; }
    public void setPhaseManager(PhaseManagerProperties v){ this.phaseManager = v; }
    public String getModelTier()                         { return modelTier; }
    public void setModelTier(String v)                   { this.modelTier = v; }
    public int getEvolutionChunkSize()                   { return evolutionChunkSize; }
    public void setEvolutionChunkSize(int v)             { this.evolutionChunkSize = v; }
    public int getLoopBatchSize()                        { return loopBatchSize; }
    public void setLoopBatchSize(int v)                  { this.loopBatchSize = v; }

    public static class PhaseManagerProperties {
        private boolean enabled = false;
        public boolean isEnabled()        { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
    }
}
