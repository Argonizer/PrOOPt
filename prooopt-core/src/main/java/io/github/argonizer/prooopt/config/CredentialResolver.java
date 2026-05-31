/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves credentials with a fixed precedence so secrets never need to live in source control:
 *
 * <ol>
 *   <li>{@link System#getenv(String) environment variable}</li>
 *   <li>JVM {@link System#getProperty(String) system property}</li>
 *   <li>the literal YAML value (only when it is not a {@code ${ENV_VAR}} placeholder)</li>
 *   <li>a future encrypted in-JAR store (not yet implemented)</li>
 * </ol>
 *
 * <p>A configured value of the form {@code ${NAME}} is treated as a reference: it resolves from the
 * environment or a system property named {@code NAME}, never from the YAML text itself. This keeps
 * API keys out of version control. A {@code ${NAME:default}} form supplies a literal fallback.
 */
public class CredentialResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{([^:}]+)(?::([^}]*))?}$");

    /**
     * Resolves a configured value. Placeholders resolve from env then system property (then any
     * inline default); literals are returned unchanged.
     *
     * @return the resolved secret, or {@code null} when a placeholder cannot be satisfied
     */
    public String resolve(String configuredValue) {
        if (configuredValue == null) {
            return null;
        }
        String value = configuredValue.trim();
        Matcher m = PLACEHOLDER.matcher(value);
        if (!m.matches()) {
            return configuredValue; // literal YAML value
        }
        String name = m.group(1).trim();
        String inlineDefault = m.group(2);

        String env = System.getenv(name);
        if (isPresent(env)) {
            return env;
        }
        String prop = System.getProperty(name);
        if (isPresent(prop)) {
            return prop;
        }
        return inlineDefault; // may be null
    }

    /** True when {@code configuredValue} references an env var / system property by placeholder. */
    public boolean isPlaceholder(String configuredValue) {
        return configuredValue != null && PLACEHOLDER.matcher(configuredValue.trim()).matches();
    }

    private static boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }
}
