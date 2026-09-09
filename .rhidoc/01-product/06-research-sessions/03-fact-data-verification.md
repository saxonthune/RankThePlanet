---
title: Fact-data verification — adoption risks
summary: Survey of risks and prior art for moving rhidoc prose into machine-checkable fact data — state explosion, spec drift, coverage criteria, property-based testing, liveness gap, two-sources-of-truth wedge
tags: [research, verification, statechart, datalog, model-checking, property-based-testing]
deps: [doc02.02.01, doc01.06.02, doc01.04.02]
---

# Fact-data verification — adoption risks

## What prompted this

A latent class of UI bug surfaced: activating *Search this area* on `MapOverview`, then navigating into a different Collection, did not cancel the committed search. The navigation statechart ([[01-navigation]], doc02.02.01) describes `search-context`'s lifetime in prose — *"survives every sheet round-trip"* — but says nothing about which non-sheet transitions drop it. The prose carries a fact that ought to be machine-checkable.

The natural shape of the fix is to declare context lifetime in the sidecar (`scope`, `clears`, `retains` fields), extend [[02-verification-system]] (doc01.04.02) with a `context-chain` verifier, and grow journey-trace checks ([[03-navigation-journeys]], doc02.02.03) into a Datalog-style fact corpus. The journeys sidecar already promises *"each row compiles one-for-one to a logic-programming fact, so a Datalog/Datascript-backed verifier is a mechanical refactor away."*

Before adopting that direction the risks need to be known. The patterns survey in [[02-ui-behavioral-spec-patterns]] (doc01.06.02) covered *which* spec layer fits next to RTP's existing tools. This doc covers *what goes wrong* when that layer lands.

## Risks worth thinking through

### 1. State explosion

Statechart model checking literature names this as the dominant failure mode. With three context keys × three modes × ~12 surfaces × filter set cardinality, the reachable configuration space is already large; payload-carrying contexts (a Collection id, a query string, a candidate list) make it unbounded. Hierarchical and abstraction techniques can compress it, but RTP's chart is flat today.

Candidate mitigation: keep the verifier model **presence-only**. The verifier reasons about whether `search-context` is set, not what query it carries. Payloads stay in runtime; the fact layer stays finite.

### 2. Spec drift

The XState community's recurring observation is that statecharts only earn their weight when **executed**, not when held as documents. RTP's chart is doc-only — verifier checks chart-vs-inventory consistency, but neither against running Compose code. Once `clears` / `retains` declares intent, the runtime can silently disagree, the verifier stays green, and the bug ships anyway.

Candidate mitigation: compile the statechart to a runtime interpreter and bind real handlers (the path doc03.03 already gestures at), so the chart is load-bearing in production. Without this, the artifact slowly becomes the third source of truth the workspace forbids ([[01-about]], doc00.01).

### 3. Coverage criterion needs a deliberate pick

Model-based testing surveys are clear that *which* coverage criterion is targeted dominates outcomes: state, edge, N-switch (every pair / triple of consecutive transitions), or path coverage. The reverse-coverage shape — "every transition exercised by ≥1 journey" — is **1-switch (edge) coverage**, and a `set` → `leave` → `return` bug like search-context surviving a sheet-trip is a 2-switch bug that edge coverage cannot catch by construction.

The `journey-trace` verifier ([[../04-development-philosophy/02-verification-system]], doc01.04.02) carries the per-step active-context derivation the bug needs — host-stack-aware, safety-only, diffed against `expects: []` assertions on each journey. The sibling `journeys-verify` emits a 2-switch coverage signal (every `(state, eventIn, eventOut)` triple) as a backlog count rather than a failing check; targeted 2-switch saturation grows quadratically with the corpus and remains a corpus-authoring exercise. Liveness, predicate-shaped expects, and generated-trace coverage stay out of scope for these verifiers — generated traces sit in Kind H below.

### 4. Hypothesis-style property tests are the missing layer

Hand-written journeys are linear and biased toward what authors expect. Stateful property-based testing (Hypothesis, the Bonsai trace-tests pattern in doc01.06.02 §2) **generates** random transition sequences from the chart, evaluates invariants after each step, and **shrinks** failing sequences to minimal counterexamples. The shrink is what makes a 20-step crash debuggable.

Candidate mitigation: the `generated-traces` verifier in `verify.mjs` (doc01.04.02 §Verifier kinds) reuses the same `deriveActiveContext` host-stack walker `journey-trace` uses, plus a guard evaluator over `parseGuard`/`walkGuardLeaves`, and walks uniformly-sampled guard-enabled transitions from the chart's `initial` state. Hand-rolled bisect-truncate + index-drop shrinker, deterministic per-trace seeds, configurable trace count and length. Sibling sheet → sibling sheet over the same host pops the source sheet first (doc02.04 Rule 2) — a small extension of `deriveActiveContext`'s strict push. Properties checked per step: every active context key's owning surface is in the current stack, and no derivation-errors accumulate. Journeys cover intent; generated traces cover adversarial reachability. Both run against the same fact layer in one runtime — no port, no schema drift.

### 5. Liveness is outside what `clears` / `retains` can express

The proposed scheme is pure **safety** — something bad never happens (a context survives past its declared scope). The other half of UI bugs are **liveness** — something good eventually happens (after add, the pin appears; after save, the form clears). Datalog over finite traces handles safety natively; liveness over infinite traces really wants LTL — Quickstrom's territory, surveyed in doc01.06.02 §3.

Candidate mitigation: name this gap explicitly in [[02-verification-system]] (doc01.04.02). `clears` / `retains` is safety-only by design; a `liveness-invariants` verifier kind is a separately scoped future addition, not an oversight.

### 6. Logseq's split is the right precedent for what belongs in facts

Logseq stores **document state in DataScript, UI state in plain atoms**. The lesson: facts are for things queried and reasoned over; transient ephemeral state stays in plain code. Pulling scroll positions, animation state, or anything that wants `mutableStateOf` into the fact layer would be a category error.

Candidate mitigation: keep the fact layer scoped to the **spec** (surfaces, contexts, lifetimes, invariants) plus at most a derived runtime snapshot for test assertions. Resist drift.

### 7. The TLA+ authoring cost lesson

TLA+ experience reports consistently land on the same shape: steep authoring curve, payoff that scales with system criticality. RTP's proposed scheme is much lighter — typed JSON plus a `node` walker, no temporal logic engine — but the same dynamic applies: cost lands on whoever writes a new affordance, and they have to know what `clears: []` *means*.

Candidate mitigation: a short author's guide alongside the schema, and verifier error messages that name the exact field to add. A confused author who can't fix a red verifier will work around it.

### 8. The two-sources-of-truth wedge

Lamport's repeated point is that specs only stay honest under a **mechanical** tie back to the code. CLAUDE.md already states the two sources (product expectation vs. source code) and explicitly forbids a third. The fact data is an *artifact* bridging the two — fine. But a verifier that only checks artifact-internal consistency tolerates a quiet third source.

Candidate mitigation: same JSON, runtime-loaded by Compose (#2 above). Without it, every new fact field is a small step toward becoming the third source.

## Implications for the rollout

- The phased rollout sketched in conversation needs a Phase 4 that is *not* "more verifiers" but "statechart at runtime" — the drift mitigation in #2 above.
- Generated-trace property tests in `:jvmTest` should land as soon as `clears` / `retains` does, to satisfy the ≥2-switch coverage requirement in #3.
- The verification doc should name safety vs. liveness explicitly so the scheme's boundary is visible (#5).
- The fact layer's scope should stay declared in MEMORY-style prose: spec + derived test snapshots, not runtime UI state (#6).

## Sources

- [Model Checking of Statechart Models: Survey and Research Directions](https://arxiv.org/pdf/cs/0407038) — state explosion as the canonical limit on flat-chart verification.
- [Exploiting Hierarchy in the Abstraction-Based Verification of Statecharts Using SMT Solvers](https://arxiv.org/pdf/1703.07350) — hierarchy and abstraction as the standard mitigation.
- [Statechart Verification with iState](https://arxiv.org/pdf/0909.1361) — a worked statechart verifier as reference.
- [Model-based testing in practice: An experience report (web applications)](https://arxiv.org/pdf/2104.02152) — adoption costs, tooling lock-in.
- [Overview of Test Coverage Criteria for Test Case Generation](https://arxiv.org/pdf/2203.09604) — N-switch, edge, path coverage definitions.
- [DataScript — immutable database and Datalog query engine](https://github.com/tonsky/datascript) — the runtime substrate Roam and Logseq build on.
- [Logseq codebase overview — DataScript for document state, atoms for UI](https://github.com/logseq/logseq/blob/master/CODEBASE_OVERVIEW.md) — the document/UI split that informs what belongs in facts.
- [Statecharts: hierarchical state machines — HN discussion](https://news.ycombinator.com/item?id=47908833) — practitioner stance on charts-as-documents vs. charts-as-executed.
- [TLA+ in Practice and Theory, Part 1 — Pron](https://pron.github.io/posts/tlaplus_part1) — authoring cost, payoff scaling.
- [The Specification Language TLA+ — Leslie Lamport](https://lamport.azurewebsites.net/pubs/commentary-web.pdf) — two-sources framing.
- [Mastering Property-Based Testing: Hypothesis + Stateful Design](https://earezki.com/ai-news/2026-04-18-a-coding-guide-for-property-based-testing-using-hypothesis-with-stateful-differential-and-metamorphic-test-design/) — rule-based state machines, shrinking.
- [Property-Based Testing: Generative Testing for System Invariants](https://yrkan.com/blog/property-based-testing/) — invariant-after-each-step pattern.
- [Quickstrom — Specifying and Testing Web Applications (LTL-over-DOM)](https://owickstrom.github.io/specifying-and-testing-web-applications/) — liveness via LTL.
- [Advances in Model-Based Testing of GUIs (ScienceDirect)](https://www.sciencedirect.com/science/chapter/bookseries/abs/pii/S006524581730030X) — MBT for GUIs survey.
