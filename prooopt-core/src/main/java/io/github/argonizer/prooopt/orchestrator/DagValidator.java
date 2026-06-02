/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.exception.CyclicDependencyException;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.ExecutionPlan;
import io.github.argonizer.prooopt.model.ExecutionStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Validates an {@link ExecutionPlan} before execution.
 *
 * <p>Checks performed:
 * <ol>
 *   <li>All step IDs are globally unique across streams.</li>
 *   <li>All {@code dependsOn} references point to existing step IDs.</li>
 *   <li>No circular dependencies exist (Kahn's topological sort).</li>
 *   <li>The designated {@code output} step ID (or variable ref) exists.</li>
 * </ol>
 *
 * <p>Called once when a plan is first received — never during step execution.
 */
public class DagValidator {

    /**
     * Validates the plan. Throws on any violation.
     *
     * @param plan the plan to validate
     * @throws PrOOPtConfigException     if step IDs are duplicate or dangling refs exist
     * @throws CyclicDependencyException if a dependency cycle is detected
     */
    public void validate(ExecutionPlan plan) {
        // 1. Check for duplicate step IDs before calling allSteps() (which throws IllegalStateException).
        Set<String> seen = new HashSet<>();
        for (var stream : plan.streams()) {
            for (var step : stream.steps()) {
                if (!seen.add(step.stepId())) {
                    throw new PrOOPtConfigException(
                            "ExecutionPlan contains duplicate step ID '" + step.stepId() + "'");
                }
            }
        }

        Map<String, ExecutionStep> allSteps = plan.allSteps();

        // 2. Check all dependsOn references are valid step IDs.
        for (ExecutionStep step : allSteps.values()) {
            for (String dep : step.dependsOn()) {
                if (!allSteps.containsKey(dep)) {
                    throw new PrOOPtConfigException(
                            "Step '" + step.stepId() + "' depends on unknown step '" + dep + "'");
                }
            }
        }

        // 3. Topological sort (Kahn's algorithm) — detect cycles.
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();

        for (String id : allSteps.keySet()) {
            inDegree.put(id, 0);
            adj.put(id, new ArrayList<>());
        }
        for (ExecutionStep step : allSteps.values()) {
            for (String dep : step.dependsOn()) {
                adj.get(dep).add(step.stepId());
                inDegree.merge(step.stepId(), 1, Integer::sum);
            }
        }

        Queue<String> queue = new LinkedList<>();
        inDegree.forEach((id, deg) -> {
            if (deg == 0) queue.add(id);
        });

        int processed = 0;
        while (!queue.isEmpty()) {
            String curr = queue.poll();
            processed++;
            for (String next : adj.get(curr)) {
                int newDeg = inDegree.merge(next, -1, Integer::sum);
                if (newDeg == 0) queue.add(next);
            }
        }

        if (processed < allSteps.size()) {
            List<String> cycle = findCycle(allSteps, adj);
            throw new CyclicDependencyException(cycle);
        }

        // 4. Check output step exists (allow $-prefixed variable refs which resolve at execution time).
        String outputKey = canonicalRef(plan.output());
        boolean outputExists = allSteps.containsKey(plan.output())
                || allSteps.containsKey(outputKey)
                || allSteps.values().stream()
                        .anyMatch(s -> outputKey.equals(canonicalRef(s.assignTo())));
        if (!outputExists) {
            throw new PrOOPtConfigException(
                    "ExecutionPlan output '" + plan.output() + "' does not reference any step");
        }
    }

    private List<String> findCycle(Map<String, ExecutionStep> steps,
                                    Map<String, List<String>> adj) {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        List<String> cycle = new ArrayList<>();

        for (String id : steps.keySet()) {
            if (dfsCycle(id, adj, visited, recStack, cycle)) break;
        }
        return cycle;
    }

    private boolean dfsCycle(String node, Map<String, List<String>> adj,
                              Set<String> visited, Set<String> recStack, List<String> cycle) {
        if (recStack.contains(node)) {
            cycle.add(node);
            return true;
        }
        if (visited.contains(node)) return false;

        visited.add(node);
        recStack.add(node);

        for (String next : adj.getOrDefault(node, List.of())) {
            if (dfsCycle(next, adj, visited, recStack, cycle)) {
                cycle.add(0, node);
                return true;
            }
        }

        recStack.remove(node);
        return false;
    }

    private static String canonicalRef(String ref) {
        if (ref == null) return "";
        String r = ref.trim();
        if (r.startsWith("${") && r.endsWith("}")) return r.substring(2, r.length() - 1);
        if (r.startsWith("$")) return r.substring(1);
        return r;
    }
}
