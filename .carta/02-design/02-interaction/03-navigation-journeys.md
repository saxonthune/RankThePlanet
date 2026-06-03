---
title: Navigation Journeys
summary: User-intent navigation paths declared as event/target pairs; diffed against the statechart to surface chart-missing transitions, target mismatches, and undeclared screens; per-step active-context assertions
tags: [design, interaction, navigation, journeys, verification]
deps: [doc02.02.01]
verify: [{"kind":"journeys-verify","sidecar":"03-navigation.journeys.json"},{"kind":"journey-trace","sidecar":"03-navigation.journeys.json"}]
---

# Navigation Journeys

A **journey** is an ordered sequence of navigation events a user expects to be able to take, starting from a named surface. The source of truth is the carta sidecar `03-navigation.journeys.json` — a flat fact-shaped record of journeys and steps.

Journeys are **independent of higher-level docs**. They are not derived from use cases (doc01.02) and not constrained to match concept actions (doc01.03). They encode user-intent paths — what someone reasonably expects the app to do — so that mismatches with the implementation spec ([[01-navigation]], doc02.02.01) can be surfaced mechanically.

## How the artifact roots out bugs

Two surfaces of source-of-truth disagree:

- The **statechart** says what transitions the implementation has.
- The **journeys** say what paths the user is expected to take, *fully declared* — each step names both the event and the screen it lands on.

`journeys-verify` does a pure diff between the two. For each declared `(from, event → to)`, it reports one of:

- **match** — chart and journey agree.
- **chart-missing** — journey asserts a transition the chart does not implement. The fix is to add the transition (or to retract the journey).
- **target-mismatch** — chart has the event but goes elsewhere. Either the chart is wrong or the journey is wrong; the diff makes the disagreement explicit.
- **unknown-surface** — journey names a screen the chart does not define. The fix is to add the screen or rename the journey's target.

There is no implicit fallback, no "resolver walks the statechart" — the journey IS the path declaration; intent-vs-reality is derived, not encoded.

## Schema

The sidecar declares one top-level array of journeys:

- **`journeys`**: each journey is `{ id, description, start, events, targets, notes?, expects? }` where
  - `id` is a kebab-case slug, unique within the sidecar.
  - `start` names the entry surface.
  - `events` and `targets` are parallel ordered arrays of the same length — the i-th event lands on the i-th target.
  - `notes` is an optional `{ "<1-based-index>": string }` map carrying per-step commentary.
  - `expects` is an optional array of trace assertions; see `## Trace assertions` below.

Internally the pipelines normalize each journey into flat `(journey, order, event, from, to, note?)` step records — each row compiles one-for-one to a logic-programming fact, so a Datalog/Datascript-backed verifier is a mechanical refactor away.

`statechart` at the top of the file names the sibling statechart sidecar to compare against. A journeys sidecar without a sibling statechart is a spec gap.

## Visualizing journeys

```
node .luminous/journeys-tree.pipeline.mjs
```

Emits to `.luminous/generated/` (gitignored).

The pipeline projects the corpus as a prefix trie rooted at each `start` surface, using `rtp.path-step` nodes keyed by `(start, event-prefix)` and `rtp.trie-edge` edges labelled with the single distinguishing event. Journeys sharing an event-prefix share trie nodes; they diverge at the first differing event. Leaves are journey endpoints.

The trie is an **acyclic tree by construction**: each step extends the event path, so revisiting the same screen along a journey produces a *new* trie node rather than a back-edge. A node carrying the same `screen` as an ancestor reads as a return visit; a candidate styling could mark such return-leaves visually distinct from forward-progress leaves.

## Trace assertions

A journey may carry an optional `expects: [{ afterEvent, active?, inactive?, note? }]` array. Each entry asserts, *after the step whose event slug matches `afterEvent`*, which owned context keys must be active and which must be inactive. `active` and `inactive` are string arrays of context-key names declared in the chart's `meta.context.owns`. The `note` is optional author commentary.

The `journey-trace` verifier ([[../../01-product/04-development-philosophy/02-verification-system]], doc01.04.02) walks each journey through a **host-stack derivation** to compute the active-context set at every step:

- Each step's `set` side activates keys whose owning surface declares `meta.context.owns[k].set` entries matching `"<from-state>.<event>"`.
- The transition's `clears` array deactivates listed keys.
- The transition's `target` reshapes the surface stack: a sheet pushes over its declared `host`; a fullScreen target that already sits lower in the stack pops down to it (BACK-style return); any other fullScreen target replaces the entire stack and unmounts every prior surface, deactivating every key owned by an unmounted surface.

The derivation is **safety-only** — it asks whether a key is set at a point in time, not whether something good eventually happens. Liveness assertions and predicate-shaped expects are deferred. The verifier is static: it does not evaluate guards or run the chart against generated transition sequences.

`afterEvent` matches by event-slug equality. If a journey's `events[]` contains the slug more than once, the entry is ambiguous and the verifier emits `ambiguous-after-event` — split the journey or rephrase. Index-based anchors (`afterStep: N`) are deliberately not supported because they rot under step insertion.

A journey without `expects` is asserted as a path (by `journeys-verify`) but not as a context lifetime.

## Coverage endgame

The journey corpus is intended to grow toward exhaustive coverage. The `journeys-verify` verifier emits 2-switch coverage (every `(state, eventIn, eventOut)` triple) as a backlog signal — a count + a short sample of uncovered triples — alongside the chart-vs-journey diff. Hardening 2-switch coverage from signal to failing check, and `journeys-verify`'s diff warnings to failing issues, both wait on the chart catching up to the journey corpus's intentional documentation-of-pending-work divergences.
