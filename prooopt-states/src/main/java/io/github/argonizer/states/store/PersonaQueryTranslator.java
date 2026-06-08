/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.states.meta.TraitMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Translates a developer-supplied {@code loadWhere(String)} predicate into a list
 * of parameterised {@link IndexCondition} objects safe for use in prepared statements.
 *
 * <p><strong>Never</strong> string-concatenates user input into SQL. All trait values
 * become bind parameters; the store implementation uses parameterised queries.
 *
 * <p>Supported syntax:
 * <ul>
 *   <li>Comparisons: {@code = != < <= > >=}</li>
 *   <li>Set membership: {@code IN ('a', 'b', 1, 2)}</li>
 *   <li>Connectors: {@code AND}, {@code OR}</li>
 *   <li>String literals in single quotes</li>
 *   <li>Numeric literals (integer or decimal)</li>
 *   <li>Boolean literals: {@code true}, {@code false}</li>
 * </ul>
 */
public final class PersonaQueryTranslator {

    private static final Pattern TOKEN = Pattern.compile(
            "'([^']*)'|(-?\\d+\\.?\\d*)|true|false|(\\bIN\\b)|(\\bAND\\b)|(\\bOR\\b)" +
            "|([!=<>]{1,2})|([A-Za-z_][A-Za-z0-9_]*)|(\\()|(\\))");

    private PersonaQueryTranslator() {}

    /**
     * Translates the condition string into a list of {@link IndexCondition} objects.
     *
     * @param condition  the raw predicate, e.g. {@code "trust_in_player < -50 AND mood = 'ANGRY'"}.
     * @param meta       metadata for the persona type (used to resolve trait types).
     * @return ordered list of index conditions.
     * @throws IllegalArgumentException for unsupported syntax.
     */
    public static List<IndexCondition> translate(String condition, PersonaMetadata meta) {
        if (condition == null || condition.isBlank()) return List.of();

        Map<String, TraitMetadata> traitsByName = meta.traits().stream()
                .collect(Collectors.toMap(TraitMetadata::snakeName, t -> t));

        List<String> tokens = tokenize(condition);
        List<IndexCondition> result = new ArrayList<>();
        String pendingLogical = "AND";

        int i = 0;
        while (i < tokens.size()) {
            String tok = tokens.get(i);
            if (tok.equalsIgnoreCase("AND")) { pendingLogical = "AND"; i++; continue; }
            if (tok.equalsIgnoreCase("OR"))  { pendingLogical = "OR";  i++; continue; }
            if (tok.equals("(") || tok.equals(")")) { i++; continue; }

            // Expect: traitName operator value [IN list]
            String traitName = tok;
            if (i + 2 >= tokens.size()) break;

            String operator = tokens.get(i + 1);
            String valueToken = tokens.get(i + 2);

            TraitMetadata tm = traitsByName.get(traitName);
            TraitType traitType = tm != null ? TraitType.from(tm.type()) : TraitType.STRING;

            if (operator.equalsIgnoreCase("IN") && valueToken.equals("(")) {
                // Collect IN list
                List<Object> inValues = new ArrayList<>();
                int j = i + 3;
                while (j < tokens.size() && !tokens.get(j).equals(")")) {
                    String v = tokens.get(j);
                    if (!v.equals(",")) inValues.add(parseValue(v, traitType));
                    j++;
                }
                result.add(new IndexCondition(traitName, "IN", inValues, traitType, pendingLogical));
                i = j + 1;
            } else {
                Object value = parseValue(valueToken, traitType);
                result.add(new IndexCondition(traitName, operator, value, traitType, pendingLogical));
                i += 3;
            }
            pendingLogical = "AND";
        }
        return result;
    }

    private static List<String> tokenize(String condition) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN.matcher(condition.trim());
        while (m.find()) {
            String tok = m.group().trim();
            if (!tok.isEmpty()) tokens.add(tok);
        }
        return tokens;
    }

    private static Object parseValue(String token, TraitType type) {
        if (token.startsWith("'") && token.endsWith("'")) {
            return token.substring(1, token.length() - 1);
        }
        if (token.equalsIgnoreCase("true")) return true;
        if (token.equalsIgnoreCase("false")) return false;
        if (type == TraitType.INT) { try { return Integer.parseInt(token); } catch (NumberFormatException e) { /**/ } }
        if (type == TraitType.LONG) { try { return Long.parseLong(token); } catch (NumberFormatException e) { /**/ } }
        if (type == TraitType.DOUBLE) { try { return Double.parseDouble(token); } catch (NumberFormatException e) { /**/ } }
        try { return Double.parseDouble(token); } catch (NumberFormatException e) { return token; }
    }
}
