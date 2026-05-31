/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.stdlib;

import io.github.argonizer.prooopt.annotation.CodeFunction;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Deterministic date arithmetic and formatting. Inputs and outputs are ISO-8601 strings so these
 * functions chain cleanly inside an execution plan.
 */
public final class DateFunctions {

    private DateFunctions() {
    }

    @CodeFunction(description = "Add a number of days to an ISO-8601 date (YYYY-MM-DD); returns ISO date.",
            tags = {"date", "time", "addDays", "arithmetic", "offset"})
    public static String addDays(String isoDate, int days) {
        return LocalDate.parse(isoDate).plusDays(days).toString();
    }

    @CodeFunction(description = "Number of days from a start ISO date to an end ISO date (end - start).",
            tags = {"date", "time", "diffDays", "difference", "between", "duration"})
    public static long diffDays(String startIsoDate, String endIsoDate) {
        return ChronoUnit.DAYS.between(LocalDate.parse(startIsoDate), LocalDate.parse(endIsoDate));
    }

    @CodeFunction(description = "Whether an ISO-8601 date falls on a weekend (Saturday or Sunday).",
            tags = {"date", "time", "isWeekend", "weekend", "weekday"})
    public static boolean isWeekend(String isoDate) {
        DayOfWeek day = LocalDate.parse(isoDate).getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    @CodeFunction(description = "The next business day (Mon–Fri) after an ISO-8601 date; returns ISO date.",
            tags = {"date", "time", "nextBusinessDay", "business", "workday", "weekday"})
    public static String nextBusinessDay(String isoDate) {
        LocalDate date = LocalDate.parse(isoDate).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.toString();
    }

    @CodeFunction(description = "Format an ISO-8601 date using a date pattern (for example, dd/MM/yyyy).",
            tags = {"date", "time", "format", "pattern", "display"})
    public static String formatDate(String isoDate, String pattern) {
        return LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern(pattern));
    }

    @CodeFunction(description = "Parse a date in the given pattern and return it as an ISO-8601 date.",
            tags = {"date", "time", "parse", "pattern", "convert"})
    public static String parseDate(String text, String pattern) {
        return LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern)).toString();
    }

    @CodeFunction(description = "Epoch day (days since 1970-01-01) for an ISO-8601 date.",
            tags = {"date", "time", "toEpoch", "epoch", "timestamp"})
    public static long toEpochDay(String isoDate) {
        return LocalDate.parse(isoDate).toEpochDay();
    }
}
