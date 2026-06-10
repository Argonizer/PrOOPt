/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.exception;

import io.github.argonizer.prooopt.audit.AuditLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Small internal helper that writes a failure line to the PrOOPt audit logger
 * ({@code io.github.argonizer.prooopt.audit}) at {@code ERROR} level the moment a PrOOPt exception is
 * constructed. This guarantees the audit trail captures every failure even when the caller swallows
 * the exception. Not part of the public API.
 */
final class AuditErrorLog {

    private static final Logger AUDIT = LogManager.getLogger(AuditLogger.AUDIT_LOGGER_NAME);

    private AuditErrorLog() {
    }

    /** Truncates a prompt to 120 characters, appending an ellipsis when longer, for safe logging. */
    static String truncatePrompt(String prompt) {
        if (prompt == null) {
            return "(none)";
        }
        String trimmed = prompt.strip();
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120) + "...";
    }

    /** Emits the exception's full message at ERROR with a stable {@code [PROOOPT][EXCEPTION]} prefix. */
    static void record(String simpleName, String message) {
        AUDIT.error("[PROOOPT][EXCEPTION][{}] {}", simpleName, message);
    }
}
