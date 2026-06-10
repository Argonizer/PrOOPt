# Thesis 1 — Iron Interlock

## Governed conversational autonomy for safety-critical industrial control

> *The model proposes. The interlock disposes.*

---

## The claim

Large language models are categorically banned from operational technology (OT) —
power plants, water treatment, chemical processing, rail signalling — and the ban is
rational. It is not that the models are insufficiently capable. It is that **nobody can
bound what they are allowed to do**. A model with a tool that opens a valve is a model
that can open the valve wrongly, and no prompt, system message, or fine-tune constitutes
a safety case that an IEC 61508 / IEC 62443 assessor will sign.

PrOOPt changes the shape of that problem. Its three-zone governance model turns *"what
the AI may do"* from a policy aspiration into a **compile-time, JVM-enforced property of
the codebase**. The safety envelope is not described to the model and hoped for — it is
physically unavailable for the model to violate, because actuation exists only inside
`@CodeFunction` methods whose bodies clamp, rate-limit, sequence-check, and interlock
every request before anything reaches a PLC.

**Iron Interlock** is the first architecture under which an LLM can credibly sit inside
the operational loop of physical infrastructure: not as an oracle commanding machinery,
but as a *governed advisor with hands that only fit the gloves we made for them*.

---

## Why this is unprecedented

Every "AI for industry" offering today stops at the same line: read-only dashboards,
anomaly scores, maintenance chatbots. The moment actuation appears, vendors retreat —
because in the prevailing agent architectures (function-calling against a flat tool
list), the only thing standing between the model and a destructive action is the
quality of a prompt. Regulators, plant operators, and insurers correctly treat that as
no barrier at all.

The unprecedented step is **architecture-as-safety-case**:

1. **The deterministic zone is the safety envelope.** In PrOOPt, `@CodeFunction` methods
   are pure Java, executed by the JVM, never by the model. The model cannot *perform*
   actuation; it can only *request* it through functions whose deterministic bodies
   embed the interlock logic. A request to raise a setpoint by 40% returns a clamped
   +5% with an audit note — not because the model was polite, but because the function's
   body is the only path to the actuator.

2. **The bounded AI zone respects the air gap.** `ModelTier.LOCAL` runs ONNX inference
   inside the JVM on the OT-network gateway. Alarm-flood summarisation, operator-intent
   parsing, shift-log NLP — all of it executes with **zero bytes leaving the network**.
   This is compatible with data-diode topologies that forbid any outbound connection,
   which categorically rules out every cloud-API agent framework.

3. **The elevated AI zone never touches live operations.** `CLOUD_ADVANCED` exists only
   on the IT side, working on de-identified incident archives — and its output enters
   the OT zone as *reviewed artifacts*, not API responses (see the playbook compiler
   below).

No existing framework offers this combination because no existing framework makes model
tier a **JVM-enforced property of each individual function**. That is PrOOPt's native
trick, and heavy industry is where it stops being a convenience and becomes the entire
value proposition.

---

## Architecture

### The three zones, mapped to a plant

```java
public class CompressorStationAdvisor {

    // ───────────────────────── DETERMINISTIC ZONE ─────────────────────────
    // The safety envelope. Pure Java. The LLM can invoke, never replicate.

    @CodeFunction(description = "Clamp a requested setpoint change to the certified "
            + "rate-of-change envelope and the absolute min/max for this asset.")
    public SetpointCommand clampSetpoint(String assetId, double requested) { /* … */ }

    @CodeFunction(description = "Verify interlock preconditions for a valve transition: "
            + "upstream pressure, permissive chain, lockout/tagout registry.")
    public InterlockVerdict checkInterlocks(String valveId, ValveTarget target) { /* … */ }

    @CodeFunction(description = "Compute the alarm-flood statistics for the last n "
            + "minutes: count, rate, top sources, chattering alarms.")
    public AlarmFloodDigest digestAlarms(int minutes) { /* … */ }

    // ──────────────────────── BOUNDED AI ZONE (LOCAL) ─────────────────────
    // Language understanding inside the air gap. Nothing leaves the JVM.

    @PromptFunction(model = ModelTier.LOCAL,
            prompt = "Given this alarm digest: {digest}, identify the most likely "
                   + "root-cause cluster and state it in one sentence for an operator.")
    public String summarizeAlarmFlood(AlarmFloodDigest digest) { return null; }

    @PromptFunction(model = ModelTier.LOCAL,
            prompt = "Classify the operator's instruction into one of: "
                   + "DIAGNOSE, ADJUST_SETPOINT, ISOLATE, ESCALATE, INFORM. "
                   + "Instruction: {utterance}")
    public OperatorIntent parseIntent(String utterance) { return null; }

    // ─────────────────────── ELEVATED AI ZONE (offline) ───────────────────
    // Runs on the IT network, on de-identified archives. Never live.

    @PromptFunction(model = ModelTier.CLOUD_ADVANCED, thinking = true,
            prompt = "Study these de-identified incident transcripts: {archive}. "
                   + "Synthesise a response playbook as ordered steps referencing "
                   + "only the registered tool names provided.")
    public String synthesizePlaybook(@SensitiveData(label = "***INCIDENT-ARCHIVE***")
                                     String archive) { return null; }
}
```

The orchestrator that fields operator requests runs entirely against LOCAL planning and
pre-compiled plans:

```java
@PromptOrchestrator(
        prompt = "Assist the control-room operator. Diagnose, propose, and execute "
               + "only through registered functions.",
        model = ModelTier.LOCAL,             // planning never leaves the network
        planMode = PlanMode.STATIC,
        planCacheStrategy = PlanCacheStrategy.INTENT,  // alarm floods are an intent class
        allowDynamic = false                 // no self-extension inside the OT zone. Ever.
)
public OperatorResponse assist(String operatorUtterance) { return null; }
```

`allowDynamic = false` is itself a governance statement: inside the plant, the tool
surface is frozen at deploy time and enumerable in a single code review. The audit
log — with `@SensitiveData` masking plant identifiers — is the **incident flight
recorder**: every function invoked, every tier used, every interlock verdict, trace-id
linked, reconstructable months later for the regulator.

### The playbook compiler — frontier reasoning as a reviewed artifact

This is the creative heart of the thesis, and the purest expression of the
*cloud-compiles, local-executes* pattern:

```
        IT network (connected)                       OT network (air-gapped)
┌──────────────────────────────────┐        ┌──────────────────────────────────────┐
│  Nightly batch:                  │        │  PrOOPt runtime on the OT gateway    │
│  CLOUD_ADVANCED reads            │ review │                                      │
│  de-identified incident archives │───────▶│  Playbooks loaded as STATIC plan     │
│  and SYNTHESIZES response        │  gate  │  templates + LOCAL rule-prompts      │
│  playbooks + refined rule-       │ (human │                                      │
│  prompts for the LOCAL tier      │  + CI) │  LOCAL model follows them, per-shift,│
│                                  │        │  at zero cloud cost, zero egress     │
└──────────────────────────────────┘        └──────────────────────────────────────┘
```

The frontier model never talks to the plant. It *writes curriculum* for the small model
that lives there. Each playbook is a plan template in exactly the form
`PlanInstantiator` already consumes — `${input}` placeholders intact — shipped across
the boundary like any other versioned configuration artifact, signed, diffed, and
reviewed by a controls engineer before activation. Frontier-grade reasoning enters the
plant **at the cadence of change management, not at the cadence of API calls** — which
is precisely the cadence safety regulation knows how to govern.

This inverts the usual agent story. Instead of *"trust the big model live"*, it is
*"let the big model teach, let humans approve the lesson, let the small model act."*

### Escalation ladder

| Situation | Tier | Mechanism |
|---|---|---|
| Routine alarm triage, shift Q&A | `LOCAL` | Cached `INTENT` plans, LOCAL inference |
| Novel alarm signature (plan-cache miss) | `LOCAL` planning + conservative default playbook | `PlanCache` miss logged, flagged for nightly compile |
| Actuation request | **No tier** — deterministic | `@CodeFunction` interlocks; model output is advisory input to them |
| Post-incident analysis, playbook synthesis | `CLOUD_ADVANCED` | Offline, de-identified, human-gated |

---

## The optimisation engineering

- **Zero marginal inference cost in operations.** Every in-plant call is LOCAL
  (ONNX, CPU). The audit `estCost` column on OT traces reads $0.0000 by construction —
  an attractive line item when calculating cost-per-recommendation across a fleet of
  stations.
- **`PlanCache` with `INTENT` strategy** collapses the operational request space:
  thousands of distinct utterances reduce to a handful of intents, each bound to a
  pre-approved plan. A warm hit skips discovery and planning entirely — sub-second
  advisory latency on commodity gateway hardware.
- **`@CodeFunction` pre-digestion** keeps the LOCAL model's context tiny: the model
  never reads 4,000 raw alarms; it reads the deterministic `AlarmFloodDigest`. Token
  budgets stay flat regardless of upset severity — exactly when latency matters most.
- **Async audit logging** (LMAX Disruptor buffer) means the flight recorder costs the
  control path nothing.

---

## Why it is breathtaking

Alarm floods kill. In major industrial accidents — Texas City, Buncefield, Three Mile
Island — operators faced hundreds to thousands of alarms per hour and lost situational
awareness precisely when it mattered. Forty years of alarm-management standards (EEMUA
191, ISA 18.2) have not solved this, because the problem is *linguistic and cognitive*:
turning a wall of signals into one true sentence about what is happening.

That is an LLM-shaped problem, in the one industry that cannot accept LLM-shaped risk.
Iron Interlock resolves the deadlock not by making the model trustworthy but by making
trust **unnecessary**: the model's authority is scoped to language, the JVM's authority
covers everything that moves metal, and the seam between them is visible in the code,
in the annotations, and in every audit line.

The deliverable to a regulator is unlike anything they have been shown before: *here is
the enumerated, frozen list of everything the AI can invoke; here is the deterministic
body of each; here is the proof no operational byte leaves the network; here is the
flight recorder.* When the first grid operator deploys this and publishes the safety
case, the era of "LLMs are banned from OT" ends — not because the ban was wrong, but
because an architecture finally satisfied its terms.

---

## Measurable success criteria

1. **Zero envelope violations by construction** — demonstrated by fault-injection
   campaigns where the model is adversarially prompted to request unsafe actions and
   every request is clamped or refused by `@CodeFunction` interlocks (red-team report).
2. **Operator time-to-correct-diagnosis** during simulated upsets, with vs. without the
   advisor (target: >40% reduction in high-fidelity simulator trials).
3. **Alarm-flood comprehension**: fraction of floods where the LOCAL summary matches
   the post-hoc root cause (target: >85% on historical replay).
4. **Cloud egress from the OT zone: 0 bytes**, attested by network capture across the
   full trial.
5. **Playbook coverage growth**: percentage of incident classes with a compiled,
   human-approved playbook, trending up nightly without any live cloud dependency.

## Honest limits

- The LOCAL tier (Phi-3.5-class) bounds linguistic sophistication; the design
  compensates with pre-digestion and compiled playbooks, not with model heroics.
- This is decision *support* with governed micro-actuation — it does not replace the
  safety instrumented system (SIS), and must never be positioned as doing so.
- The human review gate on playbooks is load-bearing; removing it converts a safety
  case back into a hope. The architecture makes the gate cheap, not optional.
