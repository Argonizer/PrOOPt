/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.invoke;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives provider-agnostic constraints from a {@code @PromptFunction} return type so cloud providers
 * (Anthropic {@code tool_use}, OpenAI {@code response_format}) and the local JLama engine can perform
 * <em>constrained generation</em> — the model is steered to emit schema-valid output, driving the
 * autoboxing retry rate toward zero.
 *
 * <p>{@link #toJsonSchema(Class)} produces a JSON-Schema {@code Map} (ready to serialise into an API
 * request); {@link #toGbnf(Class)} produces a GBNF grammar string for grammar-constrained local
 * inference. Both intentionally cover the autoboxer's supported type surface: primitives, enums,
 * {@code java.time}, collections, and flat POJOs/records.
 */
public class SchemaGenerator {

    /** Generates a JSON Schema {@link Map} from a Java {@link Class} for API structured output. */
    public Map<String, Object> toJsonSchema(Class<?> returnType) {
        return schemaFor(returnType, 0);
    }

    private Map<String, Object> schemaFor(Class<?> type, int depth) {
        Map<String, Object> schema = new LinkedHashMap<>();
        if (type == null || type == String.class || type == CharSequence.class) {
            schema.put("type", "string");
        } else if (type == boolean.class || type == Boolean.class) {
            schema.put("type", "boolean");
        } else if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class) {
            schema.put("type", "integer");
        } else if (type == double.class || type == Double.class
                || type == float.class || type == Float.class) {
            schema.put("type", "number");
        } else if (type.isEnum()) {
            schema.put("type", "string");
            List<String> values = new ArrayList<>();
            for (Object c : type.getEnumConstants()) {
                values.add(((Enum<?>) c).name());
            }
            schema.put("enum", values);
        } else if (Temporal.class.isAssignableFrom(type) || type.getName().startsWith("java.time.")) {
            schema.put("type", "string");
            schema.put("format", "date-time");
        } else if (Collection.class.isAssignableFrom(type) || type.isArray()) {
            schema.put("type", "array");
            schema.put("items", Map.of("type", "string"));
        } else if (Map.class.isAssignableFrom(type)) {
            schema.put("type", "object");
        } else if (depth < 3) {
            schema.put("type", "object");
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            for (PropertyInfo p : propertiesOf(type)) {
                properties.put(p.name(), schemaFor(p.type(), depth + 1));
                required.add(p.name());
            }
            schema.put("properties", properties);
            if (!required.isEmpty()) {
                schema.put("required", required);
            }
        } else {
            schema.put("type", "object");
        }
        return schema;
    }

    /** Generates a GBNF grammar string for grammar-constrained local generation. */
    public String toGbnf(Class<?> type) {
        StringBuilder sb = new StringBuilder();
        sb.append("root ::= ").append(gbnfRule(type, 0)).append('\n');
        sb.append("ws ::= [ \\t\\n]*\n");
        sb.append("string ::= \"\\\"\" ([^\"\\\\] | \"\\\\\" .)* \"\\\"\"\n");
        sb.append("number ::= \"-\"? [0-9]+ (\".\" [0-9]+)?\n");
        sb.append("integer ::= \"-\"? [0-9]+\n");
        sb.append("boolean ::= \"true\" | \"false\"\n");
        return sb.toString();
    }

    private String gbnfRule(Class<?> type, int depth) {
        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return "integer";
        }
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return "number";
        }
        if (type.isEnum()) {
            List<String> alts = new ArrayList<>();
            for (Object c : type.getEnumConstants()) {
                alts.add("\"\\\"" + ((Enum<?>) c).name() + "\\\"\"");
            }
            return "(" + String.join(" | ", alts) + ")";
        }
        if (Collection.class.isAssignableFrom(type) || type.isArray()) {
            return "\"[\" ws (string (ws \",\" ws string)*)? ws \"]\"";
        }
        if (depth < 3 && !type.getName().startsWith("java.")) {
            List<PropertyInfo> props = propertiesOf(type);
            if (!props.isEmpty()) {
                StringBuilder obj = new StringBuilder("\"{\" ws ");
                for (int i = 0; i < props.size(); i++) {
                    PropertyInfo p = props.get(i);
                    if (i > 0) {
                        obj.append(" ws \",\" ws ");
                    }
                    obj.append("\"\\\"").append(p.name()).append("\\\"\" ws \":\" ws ")
                            .append(gbnfRule(p.type(), depth + 1));
                }
                obj.append(" ws \"}\"");
                return "(" + obj + ")";
            }
        }
        return "string";
    }

    // ------------------------------------------------------------------ reflection helpers

    private record PropertyInfo(String name, Class<?> type) {
    }

    private static List<PropertyInfo> propertiesOf(Class<?> type) {
        List<PropertyInfo> out = new ArrayList<>();
        if (type.isRecord()) {
            for (RecordComponent rc : type.getRecordComponents()) {
                out.add(new PropertyInfo(rc.getName(), rc.getType()));
            }
        } else {
            for (Field f : type.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                    out.add(new PropertyInfo(f.getName(), f.getType()));
                }
            }
        }
        return out;
    }
}
