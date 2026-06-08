/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.states.meta.TraitMetadata;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds all four LLM prompt variants for persona operations.
 *
 * <p>All prompts:
 * <ul>
 *   <li>Include only {@code @Trait} fields (never id, non-trait, or sensitive fields).</li>
 *   <li>Use snake_case trait names as JSON keys.</li>
 *   <li>Demand JSON-only responses with no prose.</li>
 *   <li>Place fixed-identity traits under a "[Fixed identity]" context block,
 *       never in the "traits that should change" list.</li>
 * </ul>
 *
 * <p>Fixed-identity enforcement: traits whose description starts with {@code [FIXED]}
 * are surfaced as context under "[Fixed identity]" in update/evolution/loop prompts.
 * They are never included in the "Return ONLY" instruction and the LLM therefore
 * has no mandate to emit them in its response.
 */
public final class PersonaPromptBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PersonaPromptBuilder() {}

    /**
     * Builds the initialisation prompt for a new persona.
     *
     * @param meta the persona metadata.
     * @param seed the seed description provided by the developer.
     * @return the full prompt string.
     */
    public static String buildInitPrompt(PersonaMetadata meta, String seed) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are initialising a Persona of type ").append(meta.personaClass().getSimpleName()).append(".\n");
        sb.append("Persona description: ").append(meta.annotation().value()).append("\n");
        sb.append("Seed description: ").append(seed).append("\n\n");
        sb.append("Traits to initialise:\n");
        for (TraitMetadata tm : meta.mutableTraits()) {
            sb.append("- ").append(tm.snakeName())
              .append(" (").append(tm.type().getSimpleName()).append("): ")
              .append(tm.description()).append("\n");
        }
        if (!meta.fixedTraits().isEmpty()) {
            sb.append("\nFixed identity traits (initialise once; never changed thereafter):\n");
            for (TraitMetadata tm : meta.fixedTraits()) {
                sb.append("- ").append(tm.snakeName())
                  .append(" (").append(tm.type().getSimpleName()).append("): ")
                  .append(tm.description().replaceFirst("^\\[FIXED\\]\\s*", "")).append("\n");
            }
        }
        sb.append("\nRespond ONLY with a JSON object setting every trait to a value ");
        sb.append("consistent with the seed. Be specific; avoid neutral defaults unless ");
        sb.append("the seed genuinely implies neutrality.");
        return sb.toString();
    }

    /**
     * Builds the state-update prompt for an external event or developer-supplied prompt.
     *
     * @param meta            persona metadata.
     * @param persona         the current persona instance (for reading trait values).
     * @param seed            the original seed stored at creation time.
     * @param currentPhase    the persona's current lifecycle phase.
     * @param originationDate when the persona was created (age is computed from this).
     * @param prompt          the event/update prompt from the developer.
     * @param populationRules rules registered on the PersonaManager.
     * @return the full prompt string.
     */
    public static String buildUpdatePrompt(PersonaMetadata meta, Object persona,
                                           String seed, String currentPhase,
                                           Instant originationDate, String prompt,
                                           List<String> populationRules) {
        long ageDays = ChronoUnit.DAYS.between(originationDate, Instant.now());
        StringBuilder sb = new StringBuilder();
        sb.append("You are managing the state of a ").append(meta.personaClass().getSimpleName()).append(" persona.\n");
        sb.append("[Seed: ").append(seed).append("]\n");
        sb.append("[Phase: ").append(currentPhase).append("]  ");
        sb.append("[Age: ").append(ageDays).append(" day(s)]\n\n");

        appendIntrinsicRules(sb, meta.personaClass());

        if (!populationRules.isEmpty()) {
            sb.append("[Population rules]\n");
            for (String rule : populationRules) {
                sb.append("- ").append(rule).append("\n");
            }
            sb.append("\n");
        }

        appendCurrentState(sb, meta, persona);
        appendFixedIdentityBlock(sb, meta, persona);
        appendTraitDefinitions(sb, meta.mutableTraits());

        sb.append("[New event]\n").append(prompt).append("\n\n");
        sb.append("Return ONLY a JSON object of traits that should change. Omit unchanged traits. ");
        sb.append("Respect all types and stated ranges.");
        return sb.toString();
    }

    /**
     * Builds the time-based evolution prompt for the Spring Batch evolution job.
     */
    public static String buildEvolutionPrompt(PersonaMetadata meta, Object persona,
                                              String currentPhase, Instant originationDate,
                                              Instant lastEvolved) {
        long ageDays = ChronoUnit.DAYS.between(originationDate, Instant.now());
        long daysSinceLast = lastEvolved != null
                ? ChronoUnit.DAYS.between(lastEvolved, Instant.now())
                : ageDays;

        StringBuilder sb = new StringBuilder();
        sb.append("You are applying natural time-based evolution to a ")
          .append(meta.personaClass().getSimpleName()).append(" persona.\n");
        sb.append("Evolution policy: ").append(meta.annotation().evolutionDescription()).append("\n");
        sb.append("Origination: ").append(originationDate)
          .append("  Now: ").append(Instant.now())
          .append("  Age: ").append(ageDays).append(" day(s)\n");
        sb.append("Days since last evolution: ").append(daysSinceLast)
          .append("  Phase: ").append(currentPhase).append("\n\n");

        appendCurrentState(sb, meta, persona);
        appendFixedIdentityBlock(sb, meta, persona);

        sb.append("\nApply gradual drift consistent with the policy and elapsed time. ");
        sb.append("This is passive ageing, not an event — avoid dramatic changes.\n");
        sb.append("Return ONLY a JSON object of traits that should change.");
        return sb.toString();
    }

    /**
     * Builds the depth-aware internal loop prompt.
     */
    public static String buildLoopPrompt(PersonaMetadata meta, Object persona,
                                         String seed, io.github.argonizer.states.event.LoopDepth depth,
                                         int quietPeriodHours) {
        String loopDesc = meta.annotation().loopDescription().isBlank()
                ? meta.annotation().value()
                : meta.annotation().loopDescription();

        StringBuilder sb = new StringBuilder();
        sb.append("You are simulating the internal thought feedback loop of a ")
          .append(meta.personaClass().getSimpleName()).append(".\n");
        sb.append("No external event has occurred in the last ").append(quietPeriodHours).append(" hours.\n");
        sb.append("Seed: ").append(seed).append("\n");
        sb.append("Loop depth: ").append(depth).append("\n");
        sb.append("Loop description: ").append(loopDesc).append("\n\n");

        appendCurrentState(sb, meta, persona);
        appendFixedIdentityBlock(sb, meta, persona);

        sb.append("\n");
        if (depth == io.github.argonizer.states.event.LoopDepth.SHALLOW) {
            sb.append("Apply surface-level emotional decay and reinforcement only. ");
            sb.append("Return minimal changes — cortisol drift, mood stabilisation.\n");
        } else if (depth == io.github.argonizer.states.event.LoopDepth.MODERATE) {
            sb.append("Recent experiences are being actively reinterpreted. ");
            sb.append("Consider whether emotions are compounding or resolving.\n");
        } else {
            sb.append("Belief revision is occurring. Identity-level processing. ");
            sb.append("Core values and long-term dispositions may shift. ");
            sb.append("Formative memory may be recontextualised.\n");
        }
        sb.append("\nReturn ONLY a JSON object of traits that changed. Do not change stable traits.");
        return sb.toString();
    }

    private static void appendIntrinsicRules(StringBuilder sb, Class<?> cls) {
        List<Method> promptFns = new java.util.ArrayList<>();
        Class<?> cursor = cls;
        while (cursor != null && cursor != Object.class) {
            for (Method m : cursor.getDeclaredMethods()) {
                if (m.isAnnotationPresent(PromptFunction.class)) {
                    promptFns.add(m);
                }
            }
            cursor = cursor.getSuperclass();
        }
        if (!promptFns.isEmpty()) {
            sb.append("[Intrinsic rules]\n");
            for (Method m : promptFns) {
                sb.append("- ").append(m.getAnnotation(PromptFunction.class).prompt()).append("\n");
            }
            sb.append("\n");
        }
    }

    private static void appendCurrentState(StringBuilder sb, PersonaMetadata meta, Object persona) {
        sb.append("[Current state]\n");
        sb.append(toJson(meta.mutableTraits(), persona)).append("\n\n");
    }

    private static void appendFixedIdentityBlock(StringBuilder sb, PersonaMetadata meta, Object persona) {
        if (!meta.fixedTraits().isEmpty()) {
            sb.append("[Fixed identity — context only; do not modify]\n");
            sb.append(toJson(meta.fixedTraits(), persona)).append("\n\n");
        }
    }

    private static void appendTraitDefinitions(StringBuilder sb, List<TraitMetadata> traits) {
        sb.append("[Trait definitions]\n");
        for (TraitMetadata tm : traits) {
            sb.append("- ").append(tm.snakeName())
              .append(" (").append(tm.type().getSimpleName()).append("): ")
              .append(tm.description()).append("\n");
        }
        sb.append("\n");
    }

    private static String toJson(List<TraitMetadata> traits, Object persona) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (TraitMetadata tm : traits) {
            try {
                Object val = tm.field().get(persona);
                map.put(tm.snakeName(), val);
            } catch (IllegalAccessException e) {
                map.put(tm.snakeName(), null);
            }
        }
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
