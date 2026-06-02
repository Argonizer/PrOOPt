/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A DAG execution plan produced by the two-phase orchestrator.
 *
 * <p>Contains one or more named streams. Each stream is a sequence of {@link ExecutionStep}s. Steps
 * across streams may declare cross-stream dependencies via {@link ExecutionStep#dependsOn()}.</p>
 *
 * <p>A single-stream plan with no branching is equivalent to the legacy sequential plan and is handled
 * identically by {@link io.github.argonizer.prooopt.orchestrator.DagExecutor}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionPlan(
        String traceId,
        List<ExecutionStream> streams,
        String output              // stepId (or assignTo variable ref) of the final result
) {
    public ExecutionPlan {
        streams = streams == null ? List.of() : List.copyOf(streams);
    }

    /**
     * Flattens all steps across all streams into a single map keyed by stepId.
     * Used by {@link io.github.argonizer.prooopt.orchestrator.DagExecutor} for dependency resolution.
     */
    public Map<String, ExecutionStep> allSteps() {
        return streams.stream()
                .flatMap(s -> s.steps().stream())
                .collect(Collectors.toMap(ExecutionStep::stepId, s -> s));
    }
}
