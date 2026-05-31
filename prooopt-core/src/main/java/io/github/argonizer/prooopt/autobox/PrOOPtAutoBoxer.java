/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.autobox;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.argonizer.prooopt.exception.PrOOPtAutoBoxException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts raw LLM text into a function's declared return type — the method signature is the
 * contract. Two responsibilities:
 *
 * <ol>
 *   <li>{@link #buildFormatInstruction(Class)} appends a terse instruction to the prompt so the model
 *       emits parseable output for the target type.</li>
 *   <li>{@link #autobox(String, Class)} parses the response into that type, leniently where it helps
 *       (stray units stripped from numbers, {@code yes}/{@code 1} accepted as booleans, markdown
 *       fences stripped before JSON), and throws {@link PrOOPtAutoBoxException} when it genuinely
 *       cannot.</li>
 * </ol>
 */
public class PrOOPtAutoBoxer {

    private static final Pattern NUMBER = Pattern.compile(
            "[-+]?\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?" // grouped: 1,234.5
                    + "|[-+]?\\d*\\.\\d+(?:[eE][-+]?\\d+)?"            // decimal: .5, -3.2e2
                    + "|[-+]?\\d+(?:[eE][-+]?\\d+)?");                 // integer: 42, 1e3

    private static final Pattern FENCE =
            Pattern.compile("(?s)^\\s*```[a-zA-Z0-9_-]*\\s*(.*?)\\s*```\\s*$");

    private final ObjectMapper mapper;

    public PrOOPtAutoBoxer() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** The configured mapper, reused by callers that already know their concrete type (records, etc.). */
    public ObjectMapper objectMapper() {
        return mapper;
    }

    // ------------------------------------------------------------------ format instructions

    /** A terse, type-appropriate instruction appended to the enriched prompt. */
    public String buildFormatInstruction(Class<?> returnType) {
        if (returnType == null || returnType == void.class || returnType == Void.class) {
            return "";
        }
        if (returnType == String.class || CharSequence.class.isAssignableFrom(returnType)) {
            return ""; // free-form text: do not constrain
        }
        if (isIntegral(returnType)) {
            return "Respond with a single integer only. No explanation, units, or extra text.";
        }
        if (isDecimal(returnType) || returnType == Number.class) {
            return "Respond with a single number only. No explanation, units, or extra text.";
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return "Respond with exactly one word: true or false.";
        }
        if (returnType == char.class || returnType == Character.class) {
            return "Respond with a single character only.";
        }
        if (returnType.isEnum()) {
            StringBuilder names = new StringBuilder();
            Object[] constants = returnType.getEnumConstants();
            for (int i = 0; i < constants.length; i++) {
                if (i > 0) {
                    names.append(", ");
                }
                names.append(((Enum<?>) constants[i]).name());
            }
            return "Respond with exactly one of: [" + names + "]. No other text.";
        }
        if (returnType == LocalDate.class) {
            return "Respond with a single ISO-8601 date only (YYYY-MM-DD). No other text.";
        }
        if (returnType == LocalTime.class) {
            return "Respond with a single ISO-8601 time only (HH:MM:SS). No other text.";
        }
        if (isDateTime(returnType)) {
            return "Respond with a single ISO-8601 date-time only. No other text.";
        }
        if (Map.class.isAssignableFrom(returnType)) {
            return "Respond with a valid JSON object only. No markdown, no commentary.";
        }
        if (Collection.class.isAssignableFrom(returnType) || returnType.isArray()) {
            return "Respond with a valid JSON array only. No markdown, no commentary.";
        }
        return "Respond with a single valid JSON object only, matching the requested fields. "
                + "No markdown, no commentary.";
    }

    /** A stricter variant used on autobox retries; the model's last answer failed to parse. */
    public String buildStricterFormatInstruction(Class<?> returnType) {
        String base = buildFormatInstruction(returnType);
        String reminder = "CRITICAL: your previous answer could not be parsed. Output ONLY the raw "
                + "value with no surrounding prose, quotes, or markdown fences.";
        return base.isEmpty() ? reminder : base + " " + reminder;
    }

    // ------------------------------------------------------------------ conversion

    /** Converts {@code response} into {@code returnType}, or throws {@link PrOOPtAutoBoxException}. */
    public Object autobox(String response, Class<?> returnType) {
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        if (returnType == null || returnType == Object.class) {
            return response == null ? null : response.trim();
        }
        if (returnType == String.class || CharSequence.class.isAssignableFrom(returnType)) {
            return response == null ? null : stripFences(response).trim();
        }
        if (response == null || response.isBlank()) {
            throw new PrOOPtAutoBoxException(
                    "empty model response cannot be converted to " + returnType.getSimpleName(),
                    response, returnType);
        }

        String text = stripFences(response).trim();
        try {
            if (returnType == boolean.class || returnType == Boolean.class) {
                return parseBoolean(text, response, returnType);
            }
            if (returnType == char.class || returnType == Character.class) {
                String t = stripQuotes(text);
                if (t.isEmpty()) {
                    throw fail("no character found", response, returnType);
                }
                return t.charAt(0);
            }
            if (isIntegral(returnType)) {
                return coerceIntegral(extractNumber(text, response, returnType), returnType);
            }
            if (isDecimal(returnType) || returnType == Number.class) {
                return coerceDecimal(extractNumber(text, response, returnType), returnType);
            }
            if (returnType.isEnum()) {
                return parseEnum(text, response, returnType);
            }
            if (isTemporal(returnType)) {
                return parseTemporal(stripQuotes(text), response, returnType);
            }
            // Collections, maps, arrays, and POJOs all go through Jackson.
            return mapper.readValue(text, returnType);
        } catch (PrOOPtAutoBoxException e) {
            throw e;
        } catch (Exception e) {
            throw new PrOOPtAutoBoxException(
                    "could not convert response to " + returnType.getSimpleName() + ": " + e.getMessage(),
                    response, returnType, e);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static boolean isIntegral(Class<?> t) {
        return t == int.class || t == Integer.class
                || t == long.class || t == Long.class
                || t == short.class || t == Short.class
                || t == byte.class || t == Byte.class
                || t == BigInteger.class;
    }

    private static boolean isDecimal(Class<?> t) {
        return t == double.class || t == Double.class
                || t == float.class || t == Float.class
                || t == BigDecimal.class;
    }

    private static boolean isDateTime(Class<?> t) {
        return t == LocalDateTime.class || t == ZonedDateTime.class
                || t == OffsetDateTime.class || t == Instant.class;
    }

    private static boolean isTemporal(Class<?> t) {
        return t == LocalDate.class || t == LocalTime.class || isDateTime(t);
    }

    private Object parseBoolean(String text, String response, Class<?> returnType) {
        String t = stripQuotes(text).toLowerCase().trim();
        if (t.startsWith("true") || t.startsWith("yes") || t.equals("y") || t.equals("t") || t.equals("1")) {
            return Boolean.TRUE;
        }
        if (t.startsWith("false") || t.startsWith("no") || t.equals("n") || t.equals("f") || t.equals("0")) {
            return Boolean.FALSE;
        }
        Matcher m = NUMBER.matcher(t);
        if (m.find()) {
            return !m.group().replace(",", "").matches("[-+]?0*(?:\\.0+)?");
        }
        throw fail("not a recognizable boolean", response, returnType);
    }

    private String extractNumber(String text, String response, Class<?> returnType) {
        Matcher m = NUMBER.matcher(text);
        if (m.find()) {
            return m.group().replace(",", "");
        }
        throw fail("no number found", response, returnType);
    }

    private static Object coerceIntegral(String num, Class<?> t) {
        // Tolerate decimal-looking integers like "42.0".
        BigDecimal d = new BigDecimal(num);
        BigInteger i = d.toBigIntegerExact();
        if (t == BigInteger.class) {
            return i;
        }
        if (t == long.class || t == Long.class) {
            return i.longValueExact();
        }
        if (t == short.class || t == Short.class) {
            return (short) i.intValueExact();
        }
        if (t == byte.class || t == Byte.class) {
            return (byte) i.intValueExact();
        }
        return i.intValueExact();
    }

    private static Object coerceDecimal(String num, Class<?> t) {
        if (t == BigDecimal.class) {
            return new BigDecimal(num);
        }
        if (t == float.class || t == Float.class) {
            return Float.parseFloat(num);
        }
        return Double.parseDouble(num);
    }

    private Object parseEnum(String text, String response, Class<?> returnType) {
        String cleaned = stripQuotes(text).replaceAll("[^A-Za-z0-9_ ]", " ").trim();
        Object[] constants = returnType.getEnumConstants();
        // 1) exact (case-insensitive) match of the whole token.
        String upper = cleaned.toUpperCase();
        for (Object c : constants) {
            if (((Enum<?>) c).name().equalsIgnoreCase(cleaned) || ((Enum<?>) c).name().equals(upper)) {
                return c;
            }
        }
        // 2) the constant name appears as a whole word somewhere in the response.
        for (Object c : constants) {
            String name = ((Enum<?>) c).name();
            if (upper.matches(".*\\b" + Pattern.quote(name) + "\\b.*")) {
                return c;
            }
        }
        throw fail("no matching enum constant for '" + cleaned + "'", response, returnType);
    }

    private Object parseTemporal(String text, String response, Class<?> returnType) {
        try {
            if (returnType == LocalDate.class) {
                return LocalDate.parse(text);
            }
            if (returnType == LocalTime.class) {
                return LocalTime.parse(text);
            }
            if (returnType == LocalDateTime.class) {
                return LocalDateTime.parse(text);
            }
            if (returnType == ZonedDateTime.class) {
                return ZonedDateTime.parse(text);
            }
            if (returnType == OffsetDateTime.class) {
                return OffsetDateTime.parse(text);
            }
            return Instant.parse(text);
        } catch (Exception e) {
            throw fail("not a valid ISO-8601 " + returnType.getSimpleName() + ": '" + text + "'",
                    response, returnType);
        }
    }

    /** Removes a single surrounding markdown code fence, if present. */
    static String stripFences(String s) {
        Matcher m = FENCE.matcher(s);
        return m.matches() ? m.group(1) : s;
    }

    private static String stripQuotes(String s) {
        String t = s.trim();
        if (t.length() >= 2
                && ((t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"')
                || (t.charAt(0) == '\'' && t.charAt(t.length() - 1) == '\''))) {
            return t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    private static PrOOPtAutoBoxException fail(String why, String response, Class<?> type) {
        return new PrOOPtAutoBoxException(
                "could not convert response to " + type.getSimpleName() + ": " + why, response, type);
    }

    /** Convenience wrapper around {@link #autobox(String, Class)} with a checked cast. */
    @SuppressWarnings("unchecked")
    public <T> T autoboxAs(String response, Class<T> returnType) {
        return (T) autobox(response, returnType);
    }
}
