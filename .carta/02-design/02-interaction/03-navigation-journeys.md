---
title: Navigation Journeys
summary: User-intent navigation paths declared as event/target pairs; diffed against the statechart to surface chart-missing transitions, target mismatches, and undeclared screens
tags: [design, interaction, navigation, journeys, verification]
deps: [doc02.02.01]
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

- **`journeys`**: each journey is `{ id, description, start, events, targets, notes? }` where
  - `id` is a kebab-case slug, unique within the sidecar.
  - `start` names the entry surface.
  - `events` and `targets` are parallel ordered arrays of the same length — the i-th event lands on the i-th target.
  - `notes` is an optional `{ "<1-based-index>": string }` map carrying per-step commentary.

Internally the pipelines normalize each journey into flat `(journey, order, event, from, to, note?)` step records — each row compiles one-for-one to a logic-programming fact, so a Datalog/Datascript-backed verifier is a mechanical refactor away.

`statechart` at the top of the file names the sibling statechart sidecar to compare against. A journeys sidecar without a sibling statechart is a spec gap.

## Visualizing journeys

```
node .luminous/journeys-tree.pipeline.mjs
```

Emits to `.luminous/generated/` (gitignored).

The pipeline projects the corpus as a prefix trie rooted at each `start` surface, using `rtp.path-step` nodes keyed by `(start, event-prefix)` and `rtp.trie-edge` edges labelled with the single distinguishing event. Journeys sharing an event-prefix share trie nodes; they diverge at the first differing event. Leaves are journey endpoints.

The trie is an **acyclic tree by construction**: each step extends the event path, so revisiting the same screen along a journey produces a *new* trie node rather than a back-edge. A node carrying the same `screen` as an ancestor reads as a return visit; a candidate styling could mark such return-leaves visually distinct from forward-progress leaves.

## Coverage endgame

The journey corpus is intended to grow toward exhaustive coverage. Reverse-coverage checks (every statechart transition exercised by ≥1 journey; every surface reachable by ≥1 journey) belong in `.carta/verify.mjs` alongside the existing screen-inventory check ([[02-verification-system]], doc01.04.02).
