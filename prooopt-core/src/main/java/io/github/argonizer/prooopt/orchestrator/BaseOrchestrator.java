/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.orchestrator;

import io.github.argonizer.prooopt.audit.AuditLogger;
import io.github.argonizer.prooopt.model.FunctionCall;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optional base class for orchestrators. The {@code @PromptOrchestrator} annotation alone is enough
 * for simple cases; extend this when you want lifecycle visibility. Every hook is a no-op by default,
 * so override only what you need. The executor invokes these around each step and run.
 */
public abstract class BaseOrchestrator {

    /** The dedicated audit logger, available to subclasses for custom governance lines. */
    protected final Logger audit = LogManager.getLogger(AuditLogger.AUDIT_LOGGER_NAME);

    /** Invoked immediately before a function executes. */
    protected void beforeFunction(FunctionCall call) {
    }

    /** Invoked immediately after a function returns successfully. */
    protected void afterFunction(FunctionCall call, Object result) {
    }

    /** Invoked when a function throws. */
    protected void onError(FunctionCall call, Throwable error) {
    }

    /** Invoked once when a run begins. */
    protected void onRunStart(String traceId, Object input) {
    }

    /** Invoked once when a run completes. */
    protected void onRunComplete(String traceId, long totalDurationMs, int functionsCount) {
    }
}
