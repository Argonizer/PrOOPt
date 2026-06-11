# States Thesis 1 — The World That Remembers

## Persistent, society-scale NPC populations: a game world as a queryable, evolving database of minds

> *For forty years, NPCs have forgotten you the moment you walked away.
> This is the first world that remembers, gossips, holds grudges — and dreams.*

---

## The claim

Game characters are stateless puppets. Behaviour trees and dialogue graphs simulate
disposition, but the simulation resets: the guard you robbed yesterday greets you
politely today, the town that watched you save it carries no collective memory, and
nothing in the world *changes while you are gone*. The recent wave of LLM-NPC demos
(Stanford's generative agents, voice-driven NPC middleware) proved personality is
possible — and immediately hit three walls: **cost** (every heartbeat of every agent
was a cloud call), **persistence** (no durable, queryable state model — just transcript
soup), and **authorial control** (designers cannot direct, query, or debug a million
free-running chatbots).

PrOOPt States is, almost line for line, the missing infrastructure. It models an NPC
as a `@Persona` POJO whose `@Trait` fields are *LLM-managed but database-persisted*,
whose social world is an **event system with physics** (`BlastRadius`,
`AttenuationModel.RUMOUR`), whose inner life runs on **scheduled internal loops** with
quiet-period gating, and whose entire history is **queryable and reportable** like any
other enterprise data. The repository already ships the seed of this thesis as its own
example: `GuardNpc`, with duty, suspicion, bribability, morale, and an encounter log.

**The World That Remembers** scales that seed into the full claim: a persistent town —
then city, then world — of thousands of NPCs that accumulate memory, propagate
distorted rumours of the player's deeds, shift allegiances on schedule, consolidate
experience into personality change overnight, and present the designer not with a
million chat logs but with a *society they can query in SQL-like predicates and read
as LLM-written chronicles*.

---

## Why this is unprecedented

Three capabilities exist in PrOOPt States that no game-AI stack — engine-native or
LLM-middleware — has ever shipped together, and the third has never shipped at all:

1. **Gossip with physics.** `phaseManager().broadcast(event, pipeline)` where the
   pipeline declares *who is affected and how truth degrades*:

   ```java
   guards.phaseManager().broadcast(
       crimeWitnessed,                       // player seen breaking into the mint
       EventPipeline.create()
           .blastRadius(BlastRadius.WITNESSED
                            .union(BlastRadius.FACTION))   // witnesses + their faction
           .attenuation(AttenuationModel.RUMOUR)           // truth decays as it travels
           .timing(EventTiming.IMMEDIATE)
           .minSeverity(EventSeverity.WARNING));
   ```

   Witnesses receive the event at full fidelity; faction-mates hear a *rumour-
   attenuated* version — secondhand, distorted, weaker. Allport and Postman's classic
   rumour-distortion psychology (levelling, sharpening, assimilation) becomes a
   **declarative one-liner**. No shipped game has ever had information itself — not
   sound, not line-of-sight, but *socially transmitted belief* — as a simulated,
   decaying medium.

2. **NPCs that dream.** `@Persona(internalLoop = true, loopSchedule = "@daily",
   loopDepth = LoopDepth.SHALLOW, loopQuietPeriodHours = 12)` means that during server
   quiet hours, each guard *processes its recent encounters and revises its traits*
   with no player present — `UpdateSource.INTERNAL_LOOP` in the history table. Weekly
   `DEEP` loops reach what the engine's own enum documentation calls *belief revision
   territory*: the corrupt guard rationalises; the zealous one hardens. The world
   genuinely changes overnight, and the morning's `PersonaStateDiff` is the changelog.

3. **The society is a database.** This is the capability that has *never existed*:

   ```java
   List<GuardNpc> mutinous =
       guards.loadWhere("morale < 20 AND political_sympathy < -40 AND fatigue > 70");

   guards.broadcast("Rumours spread that the captain has fled the city.",
                    "patrol_zone = 'East Gate District'");

   EvolutionReport arc = guards.report(sergeantDusk);   // inflection points, projections
   ```

   Designers and live-ops teams query disposition like inventory, target narrative
   events with `WHERE` clauses, and read `report()` output — `InflectionPoint`s
   ("when did Dusk turn against the player?"), `TraitCorrelation`s ("bribability rises
   with fatigue across the watch"), `ForwardProjection`s ("Dusk trends toward
   desertion within two weeks") — as *production telemetry for souls*.

Add `@OnPersonaEvent` for deterministic game-logic reactions, and the loop closes —
LLM-driven psychology triggering hard-coded world events:

```java
@PersonaSubscriber
public class CityAlertService {
    @OnPersonaEvent(persona = GuardNpc.class,
                    type = PersonaEventType.THRESHOLD_CROSSED,
                    trait = "suspicion", threshold = 80, direction = Direction.ABOVE)
    public void lockdown(TraitChangeEvent<GuardNpc> e) { world.triggerGateLockdown(e); }

    @OnPersonaEvent(persona = GuardNpc.class,
                    type = PersonaEventType.POPULATION_TREND, trend = Trend.FALLING)
    public void watchCollapse(PopulationTrendEvent e) { story.unlockMutinyArc(); }
}
```

Emergent narrative stops being a euphemism for randomness: it is threshold mechanics
over LLM-managed psychology, with an audit trail.

---

## Architecture

### Populating the world

```java
List<GuardNpc> watch = guards.generate()
        .limit(400)
        .seed("City watch of Vharanthal, a tense river port under trade embargo")
        .distribute(Segment.of("veteran loyalists", 0.25)
                .pinTrait("faction", "City Watch")
                .normalDistribution("duty_orientation", 78, 9)
                .normalDistribution("bribability", 18, 8).build())
        .distribute(Segment.of("underpaid recruits", 0.45)
                .normalDistribution("morale", 42, 12)
                .normalDistribution("bribability", 55, 15).build())
        .distribute(Segment.of("rebel sympathisers", 0.30)
                .normalDistribution("political_sympathy", -45, 18).build())
        .batchSize(20).parallelism(5).rateLimit(5.0)
        .build();
```

One fluent statement seeds a *statistically shaped society* — coherent individuals
within designed distributions, persisted, indexed, and ready to diverge.

### The cost architecture (the wall every predecessor hit)

The reason generative-agent demos never shipped is arithmetic: naive designs spend a
cloud call per agent per tick. The States design inverts every term:

| Pressure | Mechanism | Effect |
|---|---|---|
| Per-interaction updates | Engine `defaultTier = LOCAL` (ONNX in the game server's JVM) | Routine trait transitions cost **zero cloud tokens** |
| Fan-out explosions | `BlastRadius` scopes + `AttenuationModel` + `EventPipeline.filter`/`minSeverity` | An event touches *who it plausibly touches*, not the population |
| Burst load (battles, festivals) | `BackpressureStrategy.BUFFER` + `EventTiming` deferral + `scheduleNextCycle` | Social consequences batch into the next loop cycle instead of melting the server |
| Idle-time cognition | `loopSchedule` cron + `loopQuietPeriodHours` | Dreaming happens in off-peak compute windows, by design |
| Deep reasoning | Reserved for `LOOP_ESCALATION` / `POPULATION_TREND` events | Cloud tiers touched only at narrative inflection points |

### The Director pattern — cloud writes the drama, local plays it

The crowning optimisation is the *cloud-compiles, local-executes* pattern in theatrical
form. Nightly, a `CLOUD_ADVANCED` "Director" (a plain PrOOPt orchestrator) reads the
population's `report(EvolutionWindow.lastDays(1))` — trends, correlations,
inflections — and *writes tomorrow's dramaturgy* as population rules:

```java
guards.withRule("""
    The embargo has entered its third week. Guards in dockside zones grow
    resentful of merchant-guild privilege; loyalists double down on protocol.
    Rumours about the captain's absence are now assumed partially true.
    """);
guards.withLoopRule("Tonight, veterans weigh desertion against pension; recruits weigh bribes.");
guards.withEvolutionRule("Fatigue accumulates 20% faster during embargo conditions.");
```

Every subsequent LOCAL-tier update across 400 guards executes *under direction written
once by the frontier model*. The expensive intelligence is spent on **narrative
strategy at population scale** (one call per night); the cheap intelligence performs it
per-interaction (thousands of calls, zero marginal cost). It is a writers' room with
one showrunner and four hundred improvising actors who work for electricity.

And the world writes its own lore:

```java
CustomReport chronicle = guards.customReport(
    "Write this week's chronicle of Vharanthal as its town crier would tell it, "
  + "from the watch's collective state changes, mutinies brewing, and rumours abroad.");
```

---

## Why it is breathtaking

The first time a player experiences this world, three uncanny things happen. A guard
refuses their bribe *because of something a different guard saw two days ago* — and the
distorted version he heard is recoverable, verbatim, from the history table. A
shopkeeper's manner has changed overnight, and the morning diff shows
`UpdateSource.INTERNAL_LOOP`: she *thought about it while the server slept*. And when
the player finally provokes the lockdown, it wasn't scripted — it was
`suspicion > 80` crossing a threshold in a mind that had been accumulating reasons.

For players, this is the holy grail genre promised since *Ultima*: consequence that
compounds. For designers, it is something subtler and bigger: **authorship over
societies instead of scripts** — seed distributions, direct nightly, query the result,
and read the chronicle. For the discipline, it is the demonstration that the
generative-agents research line becomes *shippable* the moment agent minds are treated
as governed, persisted, queryable enterprise state rather than as chat transcripts —
which is to say, the moment they are treated the way PrOOPt States treats them.

The audit dimension lands hardest with live-ops: when a player reports "the whole town
hates me and I don't know why," support does not shrug — they run
`report(persona, window)` and find the inflection point, the event, the rumour chain.
**A game world with a paper trail** is a sentence that has never been true before.

---

## Measurable success criteria

1. **Cloud-call budget**: < 1 cloud call per 1,000 NPC state transitions in steady
   state (everything else LOCAL), verified from tier counts in the audit stream.
2. **Persistence integrity**: 100% of trait changes carry source attribution
   (`SEED` / `EXTERNAL` / `EVENT` / `INTERNAL_LOOP` / `EVOLUTION`) in
   `prooopt_persona_history`; any NPC's disposition is explainable end-to-end.
3. **Rumour fidelity gradient**: measurable, monotonic information loss with relational
   distance under `RUMOUR` attenuation (designed distortion, not noise).
4. **Player-perceived memory**: blind playtests where >70% of players report the world
   "remembered" specific actions unprompted.
5. **Server economics**: a 1,000-NPC town sustained on one CPU-only game server (LOCAL
   inference) within a fixed nightly cloud budget for the Director call.

## Honest limits

- The LOCAL tier bounds per-NPC eloquence; this thesis is about *state* (memory,
  disposition, society), with dialogue rendering as a separable concern that may use
  any tier per scene budget.
- LLM-managed psychology can drift implausibly; `PersonaUpdateVetoException` in
  lifecycle hooks is the designer's hard veto, and trait contracts must state bounds
  ("0–100", enumerated states) exactly as `GuardNpc` already does.
- A society that genuinely evolves can evolve *away from the fun*; the Director's
  nightly rules are the steering wheel, and the thesis treats them as mandatory
  authorship, not optional garnish.
