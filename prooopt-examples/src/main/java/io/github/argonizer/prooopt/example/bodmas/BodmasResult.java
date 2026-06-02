/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.example.bodmas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured result of solving a single BODMAS/PEMDAS expression with PrOOPt. The numeric fields are
 * produced exclusively by {@code @CodeFunction} arithmetic — the LLM only contributes the prose
 * {@link #bodmasAnalysis} and {@link #interpretation}.
 *
 * <p>Jackson-deserializable so PrOOPt could autobox a model response into it, though the bundled demo
 * assembles it deterministically via {@code BodmasFunctions.assembleResult}.
 */
public final class BodmasResult {

    /** The original BODMAS expression string that was solved. */
    private final String problem;

    /** LLM analysis: the precedence breakdown and parallelism opportunities of the expression. */
    private final String bodmasAnalysis;

    /** Human-readable summary of the DAG steps that were executed. */
    private final String dagPlanSummary;

    /** The computed answer, produced entirely by {@code @CodeFunction} arithmetic. */
    private final double computedResult;

    /** The computed answer formatted for display, e.g. {@code "13.5"} not {@code "13.500000000001"}. */
    private final String resultFormatted;

    /** The known-correct answer the result is asserted against. */
    private final double expectedResult;

    /** Whether {@code |computedResult - expectedResult| < 1e-9}. */
    private final boolean assertionPassed;

    /** Step-by-step substitution proof / audit trail for the result. */
    private final String verificationSteps;

    /** LLM plain-English explanation of the solution. */
    private final String interpretation;

    /** Number of arithmetic {@code @CodeFunction} calls in the DAG plan. */
    private final int totalDagSteps;

    /** Number of parallel execution waves in the DAG plan. */
    private final int parallelWaves;

    @JsonCreator
    public BodmasResult(
            @JsonProperty("problem") String problem,
            @JsonProperty("bodmasAnalysis") String bodmasAnalysis,
            @JsonProperty("dagPlanSummary") String dagPlanSummary,
            @JsonProperty("computedResult") double computedResult,
            @JsonProperty("resultFormatted") String resultFormatted,
            @JsonProperty("expectedResult") double expectedResult,
            @JsonProperty("assertionPassed") boolean assertionPassed,
            @JsonProperty("verificationSteps") String verificationSteps,
            @JsonProperty("interpretation") String interpretation,
            @JsonProperty("totalDagSteps") int totalDagSteps,
            @JsonProperty("parallelWaves") int parallelWaves) {
        this.problem = problem;
        this.bodmasAnalysis = bodmasAnalysis;
        this.dagPlanSummary = dagPlanSummary;
        this.computedResult = computedResult;
        this.resultFormatted = resultFormatted;
        this.expectedResult = expectedResult;
        this.assertionPassed = assertionPassed;
        this.verificationSteps = verificationSteps;
        this.interpretation = interpretation;
        this.totalDagSteps = totalDagSteps;
        this.parallelWaves = parallelWaves;
    }

    public String getProblem() { return problem; }
    public String getBodmasAnalysis() { return bodmasAnalysis; }
    public String getDagPlanSummary() { return dagPlanSummary; }
    public double getComputedResult() { return computedResult; }
    public String getResultFormatted() { return resultFormatted; }
    public double getExpectedResult() { return expectedResult; }
    public boolean isAssertionPassed() { return assertionPassed; }
    public String getVerificationSteps() { return verificationSteps; }
    public String getInterpretation() { return interpretation; }
    public int getTotalDagSteps() { return totalDagSteps; }
    public int getParallelWaves() { return parallelWaves; }

    @Override
    public String toString() {
        String bar = "═".repeat(63);
        String thin = "─".repeat(63);
        return bar + System.lineSeparator()
                + "PrOOPt BODMAS Solver Result" + System.lineSeparator()
                + bar + System.lineSeparator()
                + "Problem:          " + problem + System.lineSeparator()
                + "Computed Result:  " + resultFormatted + System.lineSeparator()
                + "Expected Result:  " + BodmasFunctions.formatResult(expectedResult) + System.lineSeparator()
                + "Assertion:        " + (assertionPassed ? "PASS ✓" : "FAIL ✗") + System.lineSeparator()
                + "DAG Steps:        " + totalDagSteps + System.lineSeparator()
                + "Parallel Waves:   " + parallelWaves + System.lineSeparator()
                + thin + System.lineSeparator()
                + "Interpretation:   " + (interpretation == null ? "" : interpretation) + System.lineSeparator()
                + bar;
    }
}
