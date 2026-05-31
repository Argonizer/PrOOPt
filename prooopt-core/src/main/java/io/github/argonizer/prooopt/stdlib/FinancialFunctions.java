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

import java.util.List;

/**
 * Deterministic financial math. The kind of computation you never want a language model to
 * approximate: compound interest, present value, returns, and loan amortization, all exact.
 */
public final class FinancialFunctions {

    private FinancialFunctions() {
    }

    @CodeFunction(description = "Future value under compound interest: principal grown at an annual "
            + "rate, compounded a number of times per year, over a number of years.",
            tags = {"finance", "financial", "compound interest", "future value", "growth", "investment"})
    public static double compoundInterest(double principal, double annualRate,
                                          int compoundsPerYear, double years) {
        if (compoundsPerYear <= 0) {
            throw new IllegalArgumentException("compoundsPerYear must be positive");
        }
        return principal * Math.pow(1.0 + annualRate / compoundsPerYear, compoundsPerYear * years);
    }

    @CodeFunction(description = "Net present value (NPV) of a series of cash flows at a discount rate; "
            + "the first cash flow occurs at time 0.",
            tags = {"finance", "financial", "net present value", "npv", "discount", "valuation"})
    public static double netPresentValue(double discountRate, List<? extends Number> cashFlows) {
        double npv = 0.0;
        for (int t = 0; t < cashFlows.size(); t++) {
            npv += cashFlows.get(t).doubleValue() / Math.pow(1.0 + discountRate, t);
        }
        return npv;
    }

    @CodeFunction(description = "Internal rate of return (IRR) of a series of cash flows, found "
            + "numerically; the first cash flow occurs at time 0.",
            tags = {"finance", "financial", "internal rate of return", "irr", "return", "yield"})
    public static double internalRateOfReturn(List<? extends Number> cashFlows) {
        // Bisection over a wide, sensible bracket. Robust where Newton's method may diverge.
        double low = -0.9999;
        double high = 10.0;
        double npvLow = npvAt(cashFlows, low);
        double npvHigh = npvAt(cashFlows, high);
        if (npvLow * npvHigh > 0) {
            throw new ArithmeticException("IRR not bracketed in [-0.9999, 10]; check the cash flows");
        }
        for (int i = 0; i < 200; i++) {
            double mid = (low + high) / 2.0;
            double npvMid = npvAt(cashFlows, mid);
            if (Math.abs(npvMid) < 1e-9) {
                return mid;
            }
            if (npvLow * npvMid < 0) {
                high = mid;
                npvHigh = npvMid;
            } else {
                low = mid;
                npvLow = npvMid;
            }
        }
        return (low + high) / 2.0;
    }

    @CodeFunction(description = "Fixed monthly payment to amortize a loan principal at an annual "
            + "interest rate over a number of months.",
            tags = {"finance", "financial", "amortization", "loan", "mortgage", "payment", "installment"})
    public static double amortizedPayment(double principal, double annualRate, int months) {
        if (months <= 0) {
            throw new IllegalArgumentException("months must be positive");
        }
        double monthlyRate = annualRate / 12.0;
        if (monthlyRate == 0.0) {
            return principal / months;
        }
        double factor = Math.pow(1.0 + monthlyRate, months);
        return principal * monthlyRate * factor / (factor - 1.0);
    }

    private static double npvAt(List<? extends Number> cashFlows, double rate) {
        double npv = 0.0;
        for (int t = 0; t < cashFlows.size(); t++) {
            npv += cashFlows.get(t).doubleValue() / Math.pow(1.0 + rate, t);
        }
        return npv;
    }
}
