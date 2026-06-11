# States Thesis 3 — Machine Diaries

## Anthropomorphic observability: every service keeps a diary, and the fleet becomes a society you can question

> *Dashboards tell you what a service is doing. Nothing tells you what it has been
> through. The senior SRE who remembered why cart-service can't be trusted on Mondays
> quit last spring — and took the only copy of that knowledge with her.*

---

## The claim

Observability has three pillars — metrics, logs, traces — and a famous blind spot:
**biography**. A service's dashboards are amnesiac; an anomaly detector has no memory
of *why* the last three pages on this alert were false alarms; the knowledge that "the
search cluster has been flaky since the v41 rollout, never fully recovered, and gets
worse under cron pressure" lives in no system at all. It lives in people, decays in
Slack, and exits with every resignation. AIOps promised to fix operations with ML and
delivered anomaly scores — *numbers about numbers* — while the actual texture of
operational knowledge (suspicion, reputation, history, hunches with reasons) remained
unrepresentable.

**Machine Diaries** makes it representable. Give every service, host, database, and
pipeline a `@Persona` twin whose `@Trait` fields are *qualitative operational state* —
degradation suspicion, confidence in dependencies, deploy anxiety, an unindexed
narrative memory of what it has survived — maintained by an LLM from deterministic
telemetry digests, persisted with full history, and **queryable like inventory**:

```java
List<ServiceTwin> worrying =
    fleet.loadWhere("degradation_suspicion > 70 AND deploy_confidence < 40");
```

The fleet becomes a population. Incidents propagate through it along the dependency
graph as events. Twins *reflect on their week* in scheduled internal loops and raise
their own hands. And when the postmortem comes, it is not reconstructed from artifacts —
it is **read out of the diary**, inflection points and all.

---

## Why this is unprecedented

AIOps products score anomalies. Knowledge bases store stale prose. Nothing in the
operations tooling universe maintains **longitudinal, qualitative, causally-attributed
state per component** — because before LLMs there was no mechanism to *update*
qualitative state mechanically, and before PrOOPt States there was no discipline for
letting an LLM update state **governedly**: schema'd by trait contracts, persisted with
source attribution, vetoable by deterministic hooks, and queryable. Four specific
inversions:

1. **Tribal knowledge becomes executable.** The `@Trait` contract is the rule, written
   in the language SREs actually think in:

   ```java
   @Trait("Suspicion that a slow memory leak is present, 0–100. Raise when RSS "
        + "trends upward across deploys without corresponding traffic growth; "
        + "drop to near zero when a fix rollout is followed by two clean weeks.")
   private int memoryLeakSuspicion;

   @Trait("Confidence in each upstream dependency, 0–100 per dependency. Lower "
        + "after each incident where that dependency was implicated; recover "
        + "slowly — two clean weeks per 10 points, never instantly.")
   private Map<String, Integer> dependencyConfidence;

   @Trait(value = "Narrative memory: what this service has been through — incidents "
        + "survived, flaky periods, deploys that went wrong and why.", index = false)
   private String biography;
   ```

   That second contract encodes something profound in one sentence: **trust recovers
   slower than it breaks** — institutional wisdom that no metric system can represent,
   now enforced on every state transition.

2. **Cascading risk assessment before cascading failure.** Dependency edges become
   social ties. When the database twin's outlook darkens, dependents *hear about it*
   and re-evaluate themselves — ahead of any error budget burning:

   ```java
   fleet.phaseManager().onTraitChange("health_outlook", dependent ->
       fleet.update(dependent,
           "Your upstream datastore has turned pessimistic about its own stability. "
         + "Reassess your degradation risk and what you would shed first."));

   fleet.phaseManager().broadcast(
       dbIncidentEvent,
       EventPipeline.create()
           .blastRadius(BlastRadius.RELATIONSHIP)          // dependency graph, not the world
           .attenuation(AttenuationModel.RELATIONAL)       // impact decays with distance
           .minSeverity(EventSeverity.WARNING));
   ```

   `RELATIONAL` attenuation gives the failure-domain model for free: a database
   incident matters intensely to direct dependents, mildly two hops out — the blast
   radius *is* the topology.

3. **The 3 a.m. reflection.** Internal loops are scheduled introspection with exactly
   the right safety valve:

   ```java
   @Persona(value = "The payments service: latency-sensitive, PCI-scoped, "
                  + "dependent on auth, ledger-db, and the card-network gateway.",
       evolutionSchedule = "@weekly",
       evolutionDescription = "Unexercised code paths and unrotated credentials decay "
                            + "confidence; a quiet week without incident slowly heals trust.",
       internalLoop = true, loopSchedule = "0 0 3 * * *",
       loopDepth = LoopDepth.SHALLOW, loopQuietPeriodHours = 6,
       loopDescription = "Review the day's digests against my biography. Is anything "
                       + "slowly going wrong that no single day would reveal?")
   public class PaymentsServiceTwin { … }
   ```

   `loopQuietPeriodHours = 6` means twins do not philosophise mid-incident — they
   reflect only after stability. The loop is built to catch exactly the failure class
   that pages cannot: **slow rot** — the leak, the drift, the creeping p99 — visible
   only as a *narrative across weeks*. `LOOP_ESCALATION` events
   (`UpdateSource.INTERNAL_LOOP_DEEP`: the engine's own "belief revision territory")
   are the twin raising its hand: *I have revised my opinion of myself; a human should
   look.*

4. **Postmortems are read, not reconstructed.**

   ```java
   EvolutionReport pm = fleet.report(cartService, EvolutionWindow.lastDays(30));
   // → InflectionPoint: degradation_suspicion began rising 11 days before the page
   // → TraitCorrelation: deploy_confidence ↔ memory_leak_suspicion across the fleet
   // → ForwardProjection: ledger-db trends toward trouble within two weeks

   CustomReport draft = fleet.customReport(
       "Draft the postmortem for the cart-service incident: when belief in its "
     + "health diverged from its dashboards, which neighbours saw it coming, and "
     + "what the fleet should learn.");
   ```

   The inflection point — *when the system started believing something was wrong* —
   routinely precedes the page by days. That gap is the product.

   And governance is not decorative: a deterministic lifecycle hook can **veto** any
   implausible LLM transition (`PersonaUpdateVetoException` when, say, health leaps
   0→100 with incidents open). The LLM proposes psychology; Java retains a constitution.
   Even decommissioning gains memory: `retire(twin, "replaced by checkout-v2")` keeps
   the biography queryable — `loadRetired()` is the fleet's graveyard, and the
   graveyard is searchable the next time someone proposes the same architecture.

---

## Architecture

```
   Prometheus / OTel / deploy events / pager history
                      │
                      ▼
   ┌──────────────────────────────────────┐
   │ @CodeFunction telemetry digesters    │   deterministic, zero-token:
   │  computeRssTrend(), p99Drift(),      │   numbers → small typed digests
   │  deployOutcomeSummary(), cronLoad()  │
   └──────────────┬───────────────────────┘
                  ▼
   fleet.update(twin, digestPrompt)          ← LOCAL tier, hourly, per twin
                  │
   ┌──────────────┼──────────────────────────────────────────────┐
   ▼              ▼                          ▼                    ▼
 trait deltas   events (RELATIONSHIP       3 a.m. internal      history rows
 + veto hook    blast radius)              loops (quiet-gated)  (full provenance)
                  │                          │
                  ▼                          ▼
   @OnPersonaEvent subscribers      LOOP_ESCALATION → CLOUD_ADVANCED consult
   (THRESHOLD_CROSSED → ticket;     (the expensive model is a specialist,
    POPULATION_TREND → fleet alert)  summoned, never resident)
```

```java
@PersonaSubscriber
public class FleetGovernor {

    @OnPersonaEvent(persona = ServiceTwin.class,
                    type = PersonaEventType.THRESHOLD_CROSSED,
                    trait = "degradation_suspicion", threshold = 75,
                    direction = Direction.ABOVE)
    public void onSuspicion(TraitChangeEvent<ServiceTwin> e) { tickets.open(e); }

    @OnPersonaEvent(persona = ServiceTwin.class,
                    type = PersonaEventType.POPULATION_TREND, trend = Trend.FALLING)
    public void onFleetMoodDrop(PopulationTrendEvent e) { warRoom.convene(e); }
}
```

### The cost architecture

A 500-service fleet updated hourly is 12,000 twin-updates a day — absurd on cloud
pricing, trivial on the engine's `defaultTier = LOCAL`:

- **`@CodeFunction` digesters do the heavy lifting for free.** The LLM never reads
  raw telemetry; it reads five computed sentences. Token budgets stay flat through the
  worst incident storm — and the deterministic zone is where PrOOPt's stdlib
  (`StatisticalFunctions`) already lives.
- **LOCAL for the heartbeat, cloud for the consult.** Hourly updates and nightly
  SHALLOW loops never leave the JVM. `CLOUD_ADVANCED` is summoned by exception —
  a `LOOP_ESCALATION` or `METRIC_CROSSED` event — like paging the specialist rather
  than having her watch every monitor all day.
- **The cloud-writes-rules-for-local pattern, SRE edition.** After each real
  postmortem, `CLOUD_ADVANCED` reads the org's incident archive and *rewrites the
  fleet's loop rules*: `fleet.withLoopRule("Treat connection-pool exhaustion following
  a deploy as guilty until proven innocent — see incidents 2026-014, 2026-019")`.
  The frontier model converts each incident into **standing suspicion** that ten
  thousand subsequent LOCAL reflections apply for free. The organisation's scar tissue
  compounds instead of evaporating.
- **Event discipline**: `RELATIONSHIP` blast radius keeps incident fan-out
  proportional to the dependency graph's degree, `BackpressureStrategy.BUFFER` +
  `bufferCapacity` absorb event storms, and metric refresh is pinned to
  `refreshOn(UpdateSource.EXTERNAL)` so derived scores recompute on evidence, not on
  every drift tick.

---

## Why it is breathtaking

The first convert is whoever runs the first quarter of this and watches a twin be
*right early*. The `payments` twin's leak suspicion crosses 60 nine days before any
alert fires — because suspicion was defined as a pattern across deploys, not a
threshold on a gauge. The on-call engineer reads the twin's biography and finds the
hunch *with its reasons attached*: which deploys, which trends, which neighbour's
incident first planted doubt. It reads like the handover note from the best SRE you
ever worked with. That is the product: **the hunch, industrialised**.

The cultural shift is bigger than the tooling. Operations teams already anthropomorphise
their systems — "the search cluster is cranky," "never trust ledger-db on failover" —
because narrative is how humans actually encode operational knowledge. The industry's
response has been to treat that as unserious and double down on dashboards. Machine
Diaries takes the opposite bet: the narrative *was* the knowledge, it just had no
substrate. Give it a schema (`@Trait` contracts), a custodian (the LLM), a constitution
(veto hooks, provenance), and a query language (`loadWhere`), and the most human part
of operations becomes the most durable — searchable years later, surviving every
resignation, queryable in one line at 3 a.m.

And it is the thesis that shows PrOOPt States' true breadth: personas were never just
people. *Anything with identity, history, and change* — a microservice, a wind turbine,
a delivery truck, a long-running scientific instrument — can keep a diary now. This is
the design's quiet generality made vivid: state management for everything that has a
story.

---

## Measurable success criteria

1. **Early-warning lead time**: median days between a twin's suspicion threshold
   crossing and the first conventional alert for confirmed slow-burn incidents
   (target: ≥5 days on leaks/drift/degradation classes).
2. **Precision of suspicion**: fraction of `THRESHOLD_CROSSED` tickets that
   postmortems confirm as real precursors (target: >60% — pages, by comparison,
   famously do far worse on slow-burn classes).
3. **Postmortem cost**: time-to-first-draft via `customReport` versus manual
   reconstruction (target: hours → minutes, with inflection timeline included).
4. **Knowledge retention**: questions like "why don't we trust X under Y?" answerable
   from twin biographies alone, validated against departed-engineer test cases.
5. **Economics**: full fleet (500 twins, hourly cadence) on one CPU-only sidecar node,
   cloud spend confined to escalation consults and post-incident rule compilation —
   single-digit dollars per month, attested by the audit trail.

## Honest limits

- A twin's beliefs are an *interpretive layer over* telemetry, never a replacement for
  it; alerting on raw SLOs remains primary, and the veto hook exists precisely because
  LLM-maintained state can drift from ground truth.
- Anthropomorphism is the interface, not the ontology — the design must keep trait
  contracts tied to observable evidence ("raise when RSS trends up across deploys"),
  or the diary degenerates into fiction.
- Trust builds slowly here too: the system should run in shadow mode (diary visible,
  no tickets) until its precision record earns it the right to open them — a rollout
  discipline the thesis considers part of the design, not an apology.
