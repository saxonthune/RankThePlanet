---
title: Navigation Journeys
summary: User-intent navigation paths, resolved against the statechart to surface unimplemented transitions and unreachable screens
tags: [design, interaction, navigation, journeys, verification]
deps: [doc02.02.01]
---

# Navigation Journeys

A **journey** is an ordered sequence of navigation events a user expects to be able to take, starting from a named surface. The source of truth is the carta sidecar `03-navigation.journeys.json` — a flat fact-shaped record of journeys and steps.

Journeys are **independent of higher-level docs**. They are not derived from use cases (doc01.02) and not constrained to match concept actions (doc01.03). They encode user-intent paths — what someone reasonably expects the app to do — so that mismatches with the implementation spec ([[01-navigation]], doc02.02.01) can be surfaced mechanically.

## How the artifact roots out bugs

Two surfaces of source-of-truth disagree:

- The **statechart** says what transitions exist.
- The **journeys** say what paths a user expects to take.

A journey is **resolved** by walking the statechart: each step's `event` must exist on the current surface's `on` map, and resolution advances to its `target`. The first step that cannot be resolved marks a divergence:

- The journey is wrong (the user's mental model doesn't match the app) → fix or remove the journey, or
- The statechart is wrong (the app is missing an affordance or a transition the user reasonably expects) → add it, and the journey resolves on the next pipeline run.

Either way the divergence is now visible, named, and reviewed — not implicit.

## Schema

The sidecar declares one top-level array of journeys:

- **`journeys`**: each journey is `{ id, description, start, events, notes? }` where
  - `id` is a kebab-case slug, unique within the sidecar.
  - `start` names the entry surface (must exist in the statechart).
  - `events` is an ordered string array of the navigation events the user takes from `start`.
  - `notes` is an optional `{ "<1-based-index>": string }` map carrying per-step commentary (often used to record what the user *intended* a step to do when it doesn't resolve against the statechart).

Internally the pipeline normalizes each journey into flat `(journey, order, event, note?)` step records before resolution — each row compiles one-for-one to a logic-programming fact, so a Datalog/Datascript-backed verifier is a mechanical refactor away. The authoring shape is concise; the internal shape stays flat.

`statechart` at the top of the file names the sibling statechart sidecar to resolve against. A journeys sidecar without a sibling statechart is a spec gap.

## Visualizing journeys

A Luminous pipeline renders this sidecar as a canvas:

```
node .luminous/journeys-canvas.pipeline.mjs
```

It walks `.carta/` for `*.journeys.json`, loads the sibling statechart, resolves each step, and emits a derived canvas pair under `.luminous/generated/` (gitignored). Nodes are the same `rtp.screen` / `rtp.sheet` kinds the statechart pipeline emits — a journey's path is just a sequence of edges between existing surfaces. Edges are `rtp.journey-step` with `journey`, `order`, `event`, and `status` (`resolved` or `unresolved`). Unresolved steps render with a distinct style and a synthetic placeholder target node, so the visible break-point in the path is the bug.

## Coverage endgame

The journey corpus is intended to grow toward exhaustive coverage. Reverse-coverage checks (every statechart transition exercised by ≥1 journey; every surface reachable by ≥1 journey) belong in `.carta/verify.mjs` alongside the existing screen-inventory check ([[02-verification-system]], doc01.04.02).
