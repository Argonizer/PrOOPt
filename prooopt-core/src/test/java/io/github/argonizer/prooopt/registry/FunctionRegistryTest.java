/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.registry;

import io.github.argonizer.prooopt.annotation.CodeFunction;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.model.ToolDescriptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionRegistryTest {

    static class MathTools {
        @CodeFunction(description = "add two ints")
        public int add(int a, int b) { return a + b; }

        @CodeFunction(description = "uppercase")
        public String upper(String text) { return text.toUpperCase(); }
    }

    static class StaticTools {
        @CodeFunction(description = "double a number")
        public static int twice(int n) { return n * 2; }
    }

    private ToolDescriptor descriptor(Class<?> owner, String methodName, Class<?>... params) throws Exception {
        Method m = owner.getDeclaredMethod(methodName, params);
        Map<String, Class<?>> schema = new java.util.LinkedHashMap<>();
        for (var p : m.getParameters()) {
            schema.put(p.getName(), p.getType());
        }
        // ToolDescriptor(name, description, tags, type, modelTier, method, declaringClass, paramSchema, returnType)
        return new ToolDescriptor(methodName, "desc", new String[0], FunctionType.CODE, null,
                m, owner, schema, m.getReturnType());
    }

    @Test
    void invokesInstanceMethod() throws Exception {
        FunctionRegistry registry = new FunctionRegistry();
        MathTools instance = new MathTools();
        registry.registerInstance(MathTools.class, instance);
        registry.register(descriptor(MathTools.class, "add", int.class, int.class));
        assertEquals(7, registry.invoke("add", 3, 4));
    }

    @Test
    void invokesStaticMethod() throws Exception {
        FunctionRegistry registry = new FunctionRegistry();
        registry.register(descriptor(StaticTools.class, "twice", int.class));
        assertEquals(10, registry.invoke("twice", 5));
    }

    @Test
    void invokeNamedResolvesParamsInOrder() throws Exception {
        FunctionRegistry registry = new FunctionRegistry();
        registry.registerInstance(MathTools.class, new MathTools());
        registry.register(descriptor(MathTools.class, "add", int.class, int.class));
        assertEquals(9, registry.invokeNamed("add", Map.of("a", 4, "b", 5)));
    }

    @Test
    void throwsOnDuplicateName() throws Exception {
        FunctionRegistry registry = new FunctionRegistry();
        registry.registerInstance(MathTools.class, new MathTools());
        ToolDescriptor d = descriptor(MathTools.class, "add", int.class, int.class);
        registry.register(d);
        assertThrows(PrOOPtConfigException.class, () -> registry.register(d));
    }

    @Test
    void throwsOnUnknownFunction() {
        FunctionRegistry registry = new FunctionRegistry();
        assertThrows(PrOOPtConfigException.class, () -> registry.invoke("nonexistent"));
    }

    @Test
    void coercesStringArgToInt() throws Exception {
        FunctionRegistry registry = new FunctionRegistry();
        registry.registerInstance(MathTools.class, new MathTools());
        registry.register(descriptor(MathTools.class, "add", int.class, int.class));
        assertEquals(11, registry.invoke("add", "6", "5"));
    }

    @Test
    void instantiatesNoArgClass() throws Exception {
        FunctionRegistry registry = new FunctionRegistry();
        registry.register(descriptor(MathTools.class, "upper", String.class));
        assertEquals("HELLO", registry.invoke("upper", "hello"));
    }

    @Test
    void sizeReflectsRegistrations() throws Exception {
        FunctionRegistry registry = new FunctionRegistry();
        registry.registerInstance(MathTools.class, new MathTools());
        registry.register(descriptor(MathTools.class, "add", int.class, int.class));
        registry.register(descriptor(MathTools.class, "upper", String.class));
        assertEquals(2, registry.size());
        assertTrue(registry.names().containsAll(List.of("add", "upper")));
    }
}
