/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.config;

import io.github.argonizer.prooopt.model.ModelTier;

/**
 * Controls two-phase orchestration: which model discovers needed capabilities, which model builds the
 * plan, and how aggressively capabilities are matched to tools.
 */
public class OrchestrationConfig {

    /** {@code two-phase} (discovery then matching), {@code semantic}, or {@code all}. */
    private String strategy = "two-phase";

    /** Cheap model used for Phase 1 capability discovery. */
    private ModelTier discoveryModel = ModelTier.LOCAL;

    /** Model used for Phase 3 plan construction. */
    private ModelTier executionModel = ModelTier.AUTO;

    /** Minimum match score for a discovered capability to bind to a tool. */
    private double minMatchScore = 0.40;

    /** Hard cap on the number of tools handed to the execution model. */
    private int maxTools = 15;

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public ModelTier getDiscoveryModel() {
        return discoveryModel;
    }

    public void setDiscoveryModel(ModelTier discoveryModel) {
        this.discoveryModel = discoveryModel;
    }

    public ModelTier getExecutionModel() {
        return executionModel;
    }

    public void setExecutionModel(ModelTier executionModel) {
        this.executionModel = executionModel;
    }

    public double getMinMatchScore() {
        return minMatchScore;
    }

    public void setMinMatchScore(double minMatchScore) {
        this.minMatchScore = minMatchScore;
    }

    public int getMaxTools() {
        return maxTools;
    }

    public void setMaxTools(int maxTools) {
        this.maxTools = maxTools;
    }
}
