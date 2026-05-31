/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.registry;

import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.exception.PrOOPtException;
import io.github.argonizer.prooopt.model.ToolDescriptor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The hot path of execution. Reflection is slow only when repeated, so the registry pays that cost
 * exactly once: at registration it unreflects each tool's {@link Method} into a {@link MethodHandle}
 * (binding the receiver for instance methods) and caches it by function name. Subsequent calls go
 * through {@link MethodHandle#invokeWithArguments}, which the JIT inlines to near-direct speed.
 *
 * <p>Static {@code @CodeFunction}s cache an unbound handle; instance methods resolve their owner from
 * the {@link InstanceResolver} (Spring bean) or a reflectively-created pooled singleton.
 */
public class FunctionRegistry {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private final Map<String, MethodHandle> handles = new ConcurrentHashMap<>();
    private final Map<String, ToolDescriptor> tools = new ConcurrentHashMap<>();
    private final Map<String, Class<?>[]> paramTypes = new ConcurrentHashMap<>();
    private final Map<String, String[]> paramNames = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> instancePool = new ConcurrentHashMap<>();

    private final InstanceResolver instanceResolver;

    public FunctionRegistry() {
        this(null);
    }

    public FunctionRegistry(InstanceResolver instanceResolver) {
        this.instanceResolver = instanceResolver;
    }

    /** Pre-seeds the instance pool with an explicit instance (Spring beans, test doubles). */
    public void registerInstance(Class<?> type, Object instance) {
        instancePool.put(type, instance);
    }

    /** Registers all descriptors. */
    public void registerAll(Collection<ToolDescriptor> descriptors) {
        descriptors.forEach(this::register);
    }

    /** Registers a single tool, caching its bound/unbound handle and parameter metadata. */
    public void register(ToolDescriptor tool) {
        String name = tool.name();
        if (handles.containsKey(name)) {
            throw new PrOOPtConfigException(
                    "duplicate function name '" + name + "'. Function names must be unique across all "
                            + "registered @PromptFunction and @CodeFunction methods (offending class: "
                            + tool.declaringClass().getName() + ").");
        }
        Method method = tool.method();
        try {
            method.setAccessible(true);
            MethodHandle handle = LOOKUP.unreflect(method);
            if (!Modifier.isStatic(method.getModifiers())) {
                handle = handle.bindTo(resolveInstance(tool.declaringClass()));
            }
            handles.put(name, handle);
            tools.put(name, tool);
            paramTypes.put(name, method.getParameterTypes());
            paramNames.put(name, tool.paramSchema().keySet().toArray(new String[0]));
        } catch (IllegalAccessException e) {
            throw new PrOOPtConfigException("cannot access method for function '" + name + "'", e);
        }
    }

    /** Invokes a function with positional arguments, coercing each to its declared parameter type. */
    public Object invoke(String name, Object... args) {
        MethodHandle handle = require(name);
        Class<?>[] types = paramTypes.get(name);
        Object[] coerced = coerce(name, args, types);
        try {
            return handle.invokeWithArguments(coerced);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new PrOOPtException("invocation of function '" + name + "' failed: " + t.getMessage(), t);
        }
    }

    /** Invokes a function using a name→value argument map, ordering by the function's parameter names. */
    public Object invokeNamed(String name, Map<String, ?> namedArgs) {
        String[] names = paramNames.get(name);
        if (names == null) {
            throw new PrOOPtConfigException("unknown function '" + name + "'");
        }
        Object[] ordered = new Object[names.length];
        for (int i = 0; i < names.length; i++) {
            ordered[i] = namedArgs.get(names[i]);
        }
        return invoke(name, ordered);
    }

    public boolean contains(String name) {
        return handles.containsKey(name);
    }

    public ToolDescriptor get(String name) {
        return tools.get(name);
    }

    public Collection<ToolDescriptor> tools() {
        return List.copyOf(tools.values());
    }

    public Set<String> names() {
        return Set.copyOf(handles.keySet());
    }

    public int size() {
        return handles.size();
    }

    // ------------------------------------------------------------------ internals

    private MethodHandle require(String name) {
        MethodHandle handle = handles.get(name);
        if (handle == null) {
            throw new PrOOPtConfigException("unknown function '" + name + "'. Registered: " + handles.keySet());
        }
        return handle;
    }

    private Object[] coerce(String name, Object[] args, Class<?>[] types) {
        if (args.length != types.length) {
            throw new PrOOPtException("function '" + name + "' expects " + types.length
                    + " argument(s) but received " + args.length);
        }
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = ArgumentCoercion.coerce(args[i], types[i]);
        }
        return out;
    }

    private Object resolveInstance(Class<?> type) {
        if (instanceResolver != null) {
            Object resolved = instanceResolver.resolve(type);
            if (resolved != null) {
                instancePool.putIfAbsent(type, resolved);
                return resolved;
            }
        }
        return instancePool.computeIfAbsent(type, FunctionRegistry::instantiate);
    }

    private static Object instantiate(Class<?> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new PrOOPtConfigException(
                    "cannot instantiate '" + type.getName() + "' for an instance @PromptFunction/"
                            + "@CodeFunction. Provide it as a Spring bean or give it a no-arg constructor.", e);
        }
    }
}
