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
 * Root of the {@code prooopt.*} configuration tree. A pure JavaBean so it binds equally under Spring
 * Boot ({@code @ConfigurationProperties(prefix="prooopt")}) and the plain-Java
 * {@code PrOOPtConfigLoader}.
 */
public class PrOOPtProperties {

    private Models models = new Models();
    private ToolSelectionConfig toolSelection = new ToolSelectionConfig();
    private OrchestrationConfig orchestration = new OrchestrationConfig();

    public Models getModels() {
        return models;
    }

    public void setModels(Models models) {
        this.models = models;
    }

    public ToolSelectionConfig getToolSelection() {
        return toolSelection;
    }

    public void setToolSelection(ToolSelectionConfig toolSelection) {
        this.toolSelection = toolSelection;
    }

    public OrchestrationConfig getOrchestration() {
        return orchestration;
    }

    public void setOrchestration(OrchestrationConfig orchestration) {
        this.orchestration = orchestration;
    }

    /** Resolves the {@link ModelConfig} for a concrete tier (AUTO maps to the {@code auto} block). */
    public ModelConfig forTier(ModelTier tier) {
        return models.forTier(tier);
    }

    /** Container for the four tier blocks under {@code prooopt.models}. */
    public static class Models {

        private ModelConfig local;
        private ModelConfig cloudFast;
        private ModelConfig cloudAdvanced;
        private ModelConfig auto;

        public ModelConfig getLocal() {
            return local;
        }

        public void setLocal(ModelConfig local) {
            this.local = local;
        }

        public ModelConfig getCloudFast() {
            return cloudFast;
        }

        public void setCloudFast(ModelConfig cloudFast) {
            this.cloudFast = cloudFast;
        }

        public ModelConfig getCloudAdvanced() {
            return cloudAdvanced;
        }

        public void setCloudAdvanced(ModelConfig cloudAdvanced) {
            this.cloudAdvanced = cloudAdvanced;
        }

        public ModelConfig getAuto() {
            return auto;
        }

        public void setAuto(ModelConfig auto) {
            this.auto = auto;
        }

        public ModelConfig forTier(ModelTier tier) {
            return switch (tier) {
                case LOCAL -> local;
                case CLOUD_FAST -> cloudFast;
                case CLOUD_ADVANCED -> cloudAdvanced;
                case AUTO -> auto;
            };
        }

        /** True when at least one tier has been configured. */
        public boolean anyConfigured() {
            return local != null || cloudFast != null || cloudAdvanced != null || auto != null;
        }
    }
}
