# States Thesis 2 — The Policy Wind Tunnel

## Synthetic societies for decision rehearsal: stress-testing policies, prices, and messages on populations that cost nothing to convene

> *No aircraft flies before the wind tunnel. Every policy, price change, and public-health
> message still launches straight into the storm — tested on nobody, or tested on you.*

---

## The claim

Institutions that move markets and populations — central banks, health agencies,
insurers, utilities, consumer platforms — share an embarrassing methodological gap.
Before acting, they can model *aggregates* (econometrics, system dynamics) or sample
*opinions* (surveys, focus groups: weeks of lead time, dozens of participants, massive
observer effects). What they cannot do is **rehearse the decision**: subject a
population of heterogeneous, memory-bearing individuals to the intervention and watch
belief and behaviour evolve, individual by individual, day by day — and then rewind and
try a different version.

Classical agent-based modelling (ABM) tried for thirty years and stalled on a single
problem: agents had to be programmed with hand-written rules, so every simulation
encoded the modeller's assumptions twice over. LLM-based agents dissolved that problem —
agents can now *respond to language with humanlike heterogeneity* — but the research
prototypes (generative-agent towns) are scientifically unusable: no persistence schema,
no provenance for state changes, no calibrated population structure, no replayable
history, and cloud costs that cap populations at dozens.

PrOOPt States is the missing instrument grade. **The Policy Wind Tunnel** instantiates
a *census-calibrated* synthetic population (tens of thousands of `@Persona` citizens
generated through `Segment` distributions), subjects it to interventions via targeted
`broadcast()`, propagates second-order social transmission through
`AttenuationModel.RUMOUR`, and reads the results not as anecdotes but as **longitudinal,
source-attributed state history** — treatment-vs-control reports, trait correlations,
inflection points, forward projections. Every synthetic citizen's every opinion shift
is a database row with a cause. That is what turns a demo into an instrument.

---

## Why this is unprecedented

### 1. Census-calibrated generation is a first-class API

```java
List<Citizen> city = citizens.generate()
        .limit(50_000)
        .seed("Adult residents of a mid-size rust-belt metro, 2026")
        .distribute(Segment.of("suburban homeowners, fixed-rate mortgages", 0.34)
                .pinTrait("housing_status", "OWNER_FIXED")
                .normalDistribution("rate_sensitivity", 35, 12)
                .normalDistribution("institutional_trust", 48, 15).build())
        .distribute(Segment.of("urban renters, gig income", 0.22)
                .pinTrait("employment_mode", "GIG")
                .normalDistribution("rate_sensitivity", 72, 9)
                .normalDistribution("financial_buffer_weeks", 3, 2).build())
        .distribute(Segment.of("retirees on fixed income", 0.18)
                .normalDistribution("inflation_anxiety", 70, 11).build())
        // … remaining segments to census margins
        .batchSize(50).parallelism(8).rateLimit(10.0)
        .build();
```

Population structure stops being a modelling afterthought: pinned traits and per-trait
normal distributions reproduce census margins *by construction*, and
`buildWithResult()` reports exactly which batches failed. Nothing in the ABM or
LLM-agent world offers calibrated population synthesis as a fluent builder.

### 2. Interventions are targeted broadcasts; transmission is physics

```java
// The shock itself — delivered to those directly exposed
citizens.broadcast(
    "The central bank raises rates by 200 basis points, effective immediately. "
  + "Mortgage refinancing windows close; savings yields rise.",
    "housing_status = 'OWNER_VARIABLE' OR financial_buffer_weeks < 8");

// The discourse about the shock — spreading socially, degrading as it travels
citizens.phaseManager().broadcast(
    rateHikeNewsCycle,
    EventPipeline.create()
        .blastRadius(BlastRadius.RELATIONSHIP.union(BlastRadius.PROXIMITY))
        .attenuation(AttenuationModel.RUMOUR)
        .timing(EventTiming.IMMEDIATE)
        .backpressure(BackpressureStrategy.BUFFER));
```

This is the capability that should make social scientists sit up: **misinformation
epidemiology as an off-the-shelf experiment**. Inject a false claim at a chosen seed
set; `RUMOUR` attenuation distorts it along relational paths exactly as the classic
rumour literature describes; per-citizen history records *which version* each citizen
absorbed and what it did to their `institutional_trust`. Penetration curves, mutation
rates, which segments amplify versus dampen, whether a prebunking message inoculates —
all measurable, all replayable, all without exposing one real human to one false claim.
Today this research is done observationally on platform data (post hoc, ethically
fraught, unrepeatable). The wind tunnel makes it *experimental*.

### 3. Readouts are econometrics, not vibes

```java
// Did the message move the treated group — against its own counterfactual window?
EvolutionReport treated  = citizens.reportWhere("employment_mode = 'GIG'", postShock);
EvolutionReport baseline = citizens.reportWhere("employment_mode = 'GIG'", preShock);

// Per-citizen causal anatomy
EvolutionReport one = citizens.report(citizen);   // inflection points + sources

// Emergent structure nobody hypothesised
// → TraitCorrelation: institutional_trust ↔ financial_buffer_weeks, r = 0.61
// → ForwardProjection: discontent trending +14 over next window in two segments
```

Because every transition carries an `UpdateSource` (`EXTERNAL` shock, `EVENT` social
transmission, `INTERNAL_LOOP` private reflection, `EVOLUTION` scheduled drift), the
analyst can decompose an opinion shift into **direct effect vs. social contagion vs.
endogenous drift** — the decomposition real-world social science can almost never make.
`PersonaMetric` thresholds turn the simulation into an early-warning instrument
(`emitEvents(true).threshold(70)` on an `unrest_potential` metric →
`POPULATION_TREND` events caught by an `@OnPersonaEvent(trend = Trend.RISING)`
subscriber). And `customReport("Explain which segments moved, why, and what message
framing would have prevented the trust collapse")` makes the LLM the *analyst of
record* over data it can actually see in full.

### 4. Time travel

`trackHistory = true` plus source attribution means timelines are **forkable**: restore
the population to T₀, apply intervention B instead of A, and diff the two histories —
A/B testing entire societal trajectories. Survey panels cannot rewind. This can.

---

## Architecture of a study

```java
@Persona(
    value = "An adult resident of a mid-size metro with realistic financial life, "
          + "media diet, social ties, and bounded attention.",
    evolutionSchedule = "@daily",
    evolutionDescription = "Attention to last week's news decays; lived financial "
          + "pressure (rent, groceries, rate resets) reasserts itself over headlines.",
    internalLoop = true, loopSchedule = "@daily", loopDepth = LoopDepth.MODERATE,
    loopDescription = "Reconcile what I heard today with what I believe and what my "
          + "neighbours are saying; settle on what I'd tell a pollster.")
public class Citizen {
    @PersonaId private String id;

    @Trait("Trust in public institutions, 0 (none) to 100 (full).")
    private int institutionalTrust;

    @Trait("Perceived personal financial pressure, 0–100.")
    private int financialPressure;

    @Trait("Current belief about the rate decision, in the citizen's own words.")
    private String narrativeBelief;

    @Trait(value = "Private running memory of recent events and conversations.", index = false)
    private String memory;
    // …
}
```

Note what the annotation encodes: **forgetting** (`evolutionDescription` — attention
decay as a daily force), **private deliberation** (the internal loop reconciles social
input with prior belief — the mechanism behind opinion *stickiness*), and the
crucial distinction between `narrativeBelief` (what they'd tell a pollster) and
`memory` (unindexed inner state). Synthetic respondents that *misreport their own
beliefs under social pressure* — because the two are different fields — is a level of
methodological realism survey research will recognise immediately.

### The cost architecture

50,000 citizens × daily updates is flatly impossible on cloud-per-call designs — and is
exactly the regime the engine's `defaultTier = LOCAL` exists for:

- **All routine transitions LOCAL** (ONNX in-JVM): the marginal cost of a
  simulated-citizen-day is CPU time, not tokens. This single property moves population
  ceilings from ~10² (research demos) to ~10⁴–10⁵.
- **Blast radius + attenuation bound fan-out**: social transmission touches plausible
  contacts, not N²; `bufferCapacity` and `BackpressureStrategy` keep shock days
  tractable; `EventTiming` defers low-severity discourse to the nightly cycle.
- **The cloud-writes-rules-for-local pattern, sociologist edition**: weekly,
  `CLOUD_ADVANCED` reads the population `EvolutionReport` and *authors refined
  population rules* — `withRule("Citizens now interpret all rate news through the
  lens of last month's bank failure")` — which then govern tens of thousands of LOCAL
  updates. Frontier-model social reasoning is spent **once per week per population**,
  not once per citizen per day. The expensive model calibrates the society; the cheap
  model lives it.
- **Metric refresh discipline**: `PersonaMetric.refreshOn(UpdateSource.EXTERNAL)`
  recomputes only on real shocks, not on every drift tick.

---

## Why it is breathtaking

The demo writes itself, and it is chilling in the best way. A public-health team types
two candidate vaccination-campaign messages. The wind tunnel runs both against the same
50,000-citizen restore point. Message A penetrates cleanly through high-trust segments
and *dies* in low-trust ones; worse, its rumour-distorted version measurably lowers
trust in exactly the segments that mattered. Message B, reframed, survives distortion.
The team watches the divergence as penetration curves, drills into single synthetic
citizens to read *the actual distorted sentence* that citizen absorbed on day 3, and
walks out having rehearsed a decision that previously would have been a bet placed on
an entire real population.

The deeper breakthrough is epistemic: this is **computational social science with a
chain of custody**. Every prior attempt at LLM-driven social simulation produced
results no reviewer could interrogate — opaque transcripts, irreproducible runs. Here,
every belief of every agent at every timestep has a source-attributed row in
`prooopt_persona_history`; every experiment is a forkable timeline; every aggregate
claim decomposes to auditable individual transitions. It is the difference between an
anecdote generator and an instrument — and it is precisely PrOOPt's enterprise
DNA (persistence, provenance, governance) applied where the science was missing it.

A candid framing of what this is *for*: hypothesis generation, message hardening,
fragility discovery, decision rehearsal — a wind tunnel, not an oracle. Wind tunnels
do not promise the flight; they catch the wing that falls off. Institutions buy wind
tunnels.

---

## Measurable success criteria

1. **Calibration**: generated population reproduces target census margins on pinned
   and distributed traits within sampling error (automated check at `build()` time);
   response distributions to historical shocks track real polling movements
   directionally on a held-out validation set.
2. **Decomposability**: 100% of trait transitions attributable to
   shock / contagion / drift via `UpdateSource`; aggregate effects reconcile to the
   sum of individual histories.
3. **Replayability**: identical restore-point + intervention reruns produce
   statistically congruent population trajectories (instrument stability).
4. **Scale economics**: 50,000 citizens × 30 simulated days within a fixed cloud
   budget measured in *tens of dollars* (weekly calibration calls only), all routine
   inference LOCAL.
5. **Discovery rate**: in pilot studies, the tunnel surfaces at least one
   segment-level fragility per campaign that the commissioning team had not
   hypothesised — the metric that converts skeptics.

## Honest limits

- Synthetic citizens are not citizens. The tunnel's validity is bounded by the LLM's
  social priors and the calibration data; it must be validated against historical
  shocks before any forward-looking use, and findings are hypotheses for real-world
  testing, not forecasts with confidence intervals.
- LLM priors carry cultural skew; segment seeds and population rules must be
  calibrated per society studied, and that calibration documented in the study record.
- Dual-use is real: the same instrument that hardens a public-health message against
  distortion could tune a manipulative one. Deployments should carry use governance —
  and PrOOPt's audit-everything posture is, fittingly, the enforcement surface.
