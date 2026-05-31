/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.aop;

import io.github.argonizer.prooopt.annotation.CodeFunction;
import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.audit.AuditLogger;
import io.github.argonizer.prooopt.audit.Redaction;
import io.github.argonizer.prooopt.autobox.PrOOPtAutoBoxer;
import io.github.argonizer.prooopt.context.PrOOPtContext;
import io.github.argonizer.prooopt.invoke.PromptCallEngine;
import io.github.argonizer.prooopt.model.FunctionCall;
import io.github.argonizer.prooopt.model.FunctionType;
import io.github.argonizer.prooopt.model.LogLevel;
import io.github.argonizer.prooopt.model.ModelTier;
import io.github.argonizer.prooopt.router.ModelRouter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The AOP heart of PrOOPt for <em>direct</em> calls. Two {@code @Around} advices turn annotated method
 * invocations into governed calls:
 *
 * <ul>
 *   <li>{@link PromptFunction}: resolve {@code {param}} placeholders from arguments, route to the
 *       configured {@link ModelTier} via the shared {@link PromptCallEngine}, autobox the response into
 *       the declared return type, and record an audit trail throughout.</li>
 *   <li>{@link CodeFunction}: log, run the real Java, log — no model call, no autoboxing.</li>
 * </ul>
 *
 * <p>The interceptor holds no model logic of its own; routing and autoboxing live in the injected
 * {@link PromptCallEngine}, keeping behaviour identical to the orchestrated path and easy to test.
 */
@Aspect
public class PrOOPtLoggingInterceptor {

    private final PromptCallEngine engine;
    private final AuditLogger audit;

    public PrOOPtLoggingInterceptor(ModelRouter router) {
        this(new PromptCallEngine(router, new PrOOPtAutoBoxer()), new AuditLogger());
    }

    public PrOOPtLoggingInterceptor(PromptCallEngine engine, AuditLogger audit) {
        this.engine = engine;
        this.audit = audit;
    }

    @Around("@annotation(promptFunction)")
    public Object aroundPrompt(ProceedingJoinPoint joinPoint, PromptFunction promptFunction) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        LogLevel level = promptFunction.logLevel();
        ModelTier tier = promptFunction.model();

        Map<String, Object> variables = resolveVariables(signature.getParameterNames(), args);
        FunctionCall call = new FunctionCall(method.getName(), promptFunction.description(),
                FunctionType.PROMPT, tier, method, args, variables,
                PrOOPtContext.getTraceId(), System.currentTimeMillis());

        audit.promptStart(call, promptFunction.thinking(), Redaction.redactedInputs(method, args), level);
        try {
            // Let the method body run any optional pre-processing; its return value is discarded —
            // PrOOPt supplies the real return value from the model.
            joinPoint.proceed();

            Object result = engine.call(promptFunction, method.getReturnType(), variables);
            PrOOPtContext.incrementFunctionCount();
            audit.promptEnd(call, Redaction.redactOutput(method, result), level);
            return result;
        } catch (Throwable t) {
            audit.promptError(call, t, level);
            throw t;
        }
    }

    @Around("@annotation(codeFunction)")
    public Object aroundCode(ProceedingJoinPoint joinPoint, CodeFunction codeFunction) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        LogLevel level = codeFunction.logLevel();

        Map<String, Object> variables = resolveVariables(signature.getParameterNames(), args);
        FunctionCall call = new FunctionCall(method.getName(), codeFunction.description(),
                FunctionType.CODE, null, method, args, variables,
                PrOOPtContext.getTraceId(), System.currentTimeMillis());

        audit.codeStart(call, Redaction.redactedInputs(method, args), level);
        try {
            Object result = joinPoint.proceed(); // real, deterministic Java
            PrOOPtContext.incrementFunctionCount();
            audit.codeEnd(call, Redaction.redactOutput(method, result), level);
            return result;
        } catch (Throwable t) {
            audit.codeError(call, t, level);
            throw t;
        }
    }

    private static Map<String, Object> resolveVariables(String[] names, Object[] args) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (names == null) {
            return variables;
        }
        for (int i = 0; i < names.length && i < args.length; i++) {
            variables.put(names[i], args[i]);
        }
        return variables;
    }
}
