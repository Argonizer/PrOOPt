# Thesis 3 — The Boundary Refinery

## A reasoning copilot for proprietary science: frontier models reasoning on top of knowledge they can never contain

> *Every "chat with your data" product ships your data to the model.
> This is "chat with your models" — and the models never leave home.*

---

## The claim

The most valuable computational knowledge in the world economy does not exist in any
training corpus, and never will: a pharmaceutical company's pharmacokinetic models
fitted on decades of proprietary clinical data; a reinsurer's catastrophe models; a
quant fund's alpha signals; a fab's process-yield models. This knowledge shares three
properties: it is **executable** (models, not documents), it is **decisive** (billions
ride on its outputs), and it is **radioactive** (publishing it, or pasting it into a
cloud prompt, destroys its value or violates law).

Today this knowledge and frontier AI cannot touch each other. Ask a frontier model to
apply your unpublished PK/PD model and it will do the only thing it can: hallucinate a
plausible-looking formula and apply it with total confidence — interpolation wearing
the costume of expertise. The PHILOSOPHY document names this precisely: the training
data boundary is *permanent*, and PrOOPt treats it **not as a problem to solve but as a
seam to exploit**.

**The Boundary Refinery** is that seam industrialised. Proprietary executable knowledge
is registered as `@CodeFunction` simulators — a *knowledge injection layer* the model
can invoke but never inspect, weights-wise never absorb. The frontier model contributes
the one thing it genuinely has — fluent scientific reasoning *about* tools — while the
JVM guarantees the tools' contents, and the sensitive data flowing through them, never
cross the boundary. Crude inputs in, refined reasoning out, nothing leaks. A refinery.

---

## Why this is unprecedented

The entire enterprise-AI playbook for proprietary knowledge is variations of *give the
model the knowledge*: fine-tune on it, RAG over it, paste it into context. All three
are category errors for executable knowledge:

- **Fine-tuning** teaches the model to *imitate* your risk model — an approximation of
  a thing whose entire value is exactness, baked irretrievably into weights you now
  have to guard.
- **RAG** retrieves *descriptions* of models; the LLM then performs the computation by
  prediction — the precise failure mode PrOOPt's philosophy identifies. A retrieved
  formula interpolated at temperature is not your formula.
- **Context-pasting** is simultaneously a leak (the formula crosses the wire) and a
  lie (the model still doesn't *execute* it; it predicts what executing it would look
  like).

The unprecedented inversion: **the model never receives the knowledge at all — it
receives an interface.** Reasoning about a tool is within the model's distribution even
when the tool's contents are permanently outside it. PrOOPt is uniquely shaped for this
inversion because it was *designed around* it:

1. `@CodeFunction` registration makes a 20-year-old proprietary simulator a first-class
   reasoning instrument with a one-line annotation — no rewrite, no exposure.
2. The **closed-book property** means the model cannot quietly substitute its own
   interpolated version of the computation: in a PrOOPt orchestration, the arithmetic
   path *only* exists through the registry.
3. `@SensitiveData` masks both patient-level inputs **and proprietary parameters** in
   the audit trail, so even the compliance artifact leaks nothing.
4. `ModelTier.LOCAL` handles every step that must touch raw sensitive data; the cloud
   tier sees only de-identified, schema-shaped abstractions.
5. The audit log itself becomes a **regulatory submission artifact**: every simulator
   invocation, version, input hash, and interpretation step, trace-linked — the
   provenance chain that 21 CFR Part 11-style electronic-records regimes demand.

No agent framework offers this because no agent framework *enforces* the seam; they
all permit — therefore guarantee, at scale — the model freelancing the computation.

---

## Architecture — concrete instantiation: a dose-optimisation copilot

A clinical pharmacology team holds proprietary population-PK/PD models for a marketed
compound. Clinicians and trial designers want to ask questions in natural language:
*"68-year-old, 54 kg, eGFR 31, on a moderate CYP3A4 inhibitor — simulate steady-state
exposure for 200 mg BID vs 150 mg TID and tell me which keeps trough above target
without breaching the AUC cap."*

```java
public class DoseOptimizationCopilot {

    // ───────────── THE KNOWLEDGE INJECTION LAYER (proprietary, deterministic) ─────────────
    // Decades of actuarial-grade science. Invisible to every model, invocable by all.

    @CodeFunction(description = "Proprietary population-PK simulation: steady-state "
            + "concentration-time profile for a dosing regimen given covariates. "
            + "Validated model vX.Y; returns AUC, Cmax, Ctrough.")
    public ExposureProfile simulateExposure(Regimen regimen, Covariates covariates) { /* … */ }

    @CodeFunction(description = "Proprietary exposure-response model: probability of "
            + "efficacy target attainment and of dose-limiting toxicity for an exposure profile.")
    public ResponseProbabilities exposureResponse(ExposureProfile profile) { /* … */ }

    @CodeFunction(description = "Label and protocol constraint checker: hard AUC caps, "
            + "renal adjustment rules, contraindicated co-medications.")
    public ConstraintReport checkConstraints(Regimen regimen, Covariates covariates) { /* … */ }

    // ───────────── BOUNDED AI: raw clinical text never leaves the JVM ─────────────

    @PromptFunction(model = ModelTier.LOCAL,
            prompt = "Extract structured covariates (age, weight, eGFR, comedications, "
                   + "organ function) from this clinical note: {note}")
    public Covariates extractCovariates(@SensitiveData(label = "***CLINICAL-NOTE***")
                                        String note) { return null; }

    // ───────────── ELEVATED AI: sees only de-identified, schema-shaped data ─────────────

    @PromptFunction(model = ModelTier.CLOUD_ADVANCED, thinking = true,
            prompt = "Design a simulation campaign to answer: {question}. You may only "
                   + "propose calls to the registered simulators. Cover the regimen "
                   + "grid, sensitivity analyses, and edge covariates worth testing.")
    public CampaignPlan planCampaign(String question, CovariateSummary deidentified) { return null; }

    @PromptFunction(model = ModelTier.CLOUD_ADVANCED,
            prompt = "Interpret these simulation results {results} against question "
                   + "{question}. State the recommendation, the margin to each "
                   + "constraint, and what additional simulation would reduce uncertainty.")
    public Recommendation interpret(CampaignResults results, String question) { return null; }
}
```

```java
@PromptOrchestrator(
        prompt = "Answer dose-optimisation questions strictly via registered simulators.",
        planMode = PlanMode.STATIC,
        planCacheStrategy = PlanCacheStrategy.INTENT,   // titration questions are an intent family
        parallel = true                                  // simulation grids fan out in waves
)
public Recommendation advise(String clinicianQuestion) { return null; }
```

The information-flow geometry is the whole point:

```
 raw PHI ──▶ LOCAL extraction ──▶ typed Covariates ──▶ de-identified summary ──▶ CLOUD planning
                                        │                                            │
                                        ▼                                            ▼
                              @CodeFunction simulators ◀── ExecutionPlan (tool calls only)
                              (proprietary, in-JVM)
                                        │
                                        ▼
                              typed results ──▶ CLOUD interpretation ──▶ Recommendation
```

The cloud model sees: a question, a covariate *summary*, tool names, and numeric
results. It never sees the note, the patient, the model equations, or the fitted
parameters. The audit trail shows `inputs={note=***CLINICAL-NOTE***}` — the masking is
in the log, while the real values flowed only where the annotations permitted.

### The refinery generalises

Swap the simulators and nothing else changes: catastrophe-model orchestration for
reinsurance underwriters; proprietary alpha-model interrogation for quant researchers
("which of our signals disagree about this ticker, and why might that be?"); process
recipe exploration for semiconductor yield engineers. One architecture, every industry
whose crown jewels are executable.

### The optimisation engineering

- **Intent-cached campaign plans**: dose-titration questions cluster tightly; the
  expensive `CLOUD_ADVANCED` campaign design is paid once per question *family* and
  instantiated per patient by `PlanInstantiator` — thousands of patient-specific runs
  per compiled plan.
- **Wave-parallel simulation fan-out**: the DAG executor runs the regimen × covariate
  grid as parallel `@CodeFunction` waves on virtual threads — the expensive part is
  CPU-bound proprietary code, not tokens.
- **Constrained generation** (`SchemaGenerator`) forces `CampaignPlan` and
  `Recommendation` into schema-valid JSON, driving autoboxing retries — and therefore
  repeated cloud calls — toward zero.
- **The cloud-writes-rules-for-local pattern**: periodically, `CLOUD_ADVANCED` reviews
  de-identified extraction errors and rewrites the LOCAL covariate-extraction
  rule-prompt (unit-tested against a golden corpus before adoption). The expensive
  model continuously *teaches* the in-house model to read this organisation's notes —
  cloud spend that converts into permanent local capability.

---

## Why it is breathtaking

There is a specific moment that converts skeptics. A pharmacometrician asks the copilot
a hard, real question. The system plans a 40-simulation campaign, runs it in four
parallel waves in seconds, and returns a recommendation with constraint margins — and
then she checks the audit trail and sees that every number came from *her own validated
model*, version-pinned, every input masked, and the frontier model's contribution
confined to strategy and interpretation. It is the first time she has seen frontier-AI
fluency *attached to* ground truth she is professionally accountable for, rather than
substituting for it.

The strategic claim is larger than pharma. The reason enterprise AI adoption stalls in
the highest-value domains is not capability — it is that the highest-value knowledge
cannot be shown to the model. The Boundary Refinery dissolves the dilemma by proving
you never needed to show it. That unlocks a category of deployment that is currently
*zero*: frontier reasoning over corporate crown jewels, with the crown jewels'
non-exposure guaranteed by architecture rather than by vendor promise. PrOOPt's
PHILOSOPHY calls the training-data boundary permanent; this thesis is the demonstration
that *permanent* and *prosperous* are compatible — that the boundary is where the value
concentrates.

And for the regulated professions specifically, it offers the sentence their audit
committees have been waiting for: **"The AI never computed anything. It decided which
of our validated computations to run, and explained the results — and here is the
complete record."**

---

## Measurable success criteria

1. **Zero knowledge egress**: network- and log-level attestation that neither raw
   sensitive inputs nor proprietary parameters ever appear in cloud payloads
   (red-team + DLP verification).
2. **Computational fidelity 100%**: every numeric in every recommendation traces to a
   registered simulator invocation in the audit log — no model-generated numbers
   (automated trace audit).
3. **Time-to-answer**: complex titration questions answered in minutes vs. the current
   days-long loop through a pharmacometrics queue (baseline study).
4. **Plan-cache leverage**: ≥80% of production questions served from intent-cached
   campaign plans — cloud planning cost amortised across the patient population.
5. **Extraction accuracy ratchet**: LOCAL covariate extraction F1 improving across
   rule-recompilation cycles on the golden corpus, demonstrating the
   teach-the-local-model loop.

## Honest limits

- The refinery's outputs are exactly as good as the registered simulators; it
  amplifies validated science and will amplify validated mistakes equally. Model
  governance (versioning, revalidation) remains a human discipline — now at least
  perfectly recorded.
- The cloud planner can propose *wasteful* campaigns; cost guards live in
  configuration (`dagTimeoutMs`, tool budgets) and in deterministic constraint
  checkers, not in trust.
- This is decision support for qualified professionals, not autonomous prescribing —
  the recommendation object should carry that framing in its type, not its tone.
