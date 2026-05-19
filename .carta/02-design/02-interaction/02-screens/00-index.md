---
title: Screens
summary: Per-surface affordance inventories — regions, affordances, lists
tags: [design, interaction, screens, index]
deps: [doc02.02.01]
---

# Screens

One doc per surface from the navigation graph ([[01-navigation]], doc02.02.01). Each refines a surface into its **affordance inventory**: the regions it shows, the affordances within them, and the lists it iterates.

**The source of truth for each screen is its carta sidecar `NN-<slug>.inventory.json`.** The `.md` is a lean companion — read the JSON for the real inventory.

Inventories stay platform-agnostic, same as the rest of the interaction layer (doc02.02.00): regions and affordances, not Composables or UiState. An affordance names a concept action or a navigation event, never a widget type.

## Inventory shape

A `*.inventory.json` has three arrays, all keyed to the surface's statechart entry:

- **`regions`** — named areas of the surface (`topBar`, `content`), each with a purpose.
- **`affordances`** — what the user can act on. Each has a `region`, a `label`, and either an `event` (a transition from the statechart) or stays a self-action. Events carrying a concept action name it in `action`, matching the surface's `meta.actions`.
- **`lists`** — repeated content. Each names what domain type it `iterates`, which `meta.reads` key it `reads`, the per-item affordance, and an empty state.

Coverage stays honest: every `event` must be a real transition on that surface in the statechart, and every `action` must appear in its `meta.actions`.

## Contents

- doc02.02.02.01 — Collection List: the surface that manages the user's Collections.
- doc02.02.02.02 — Collection Detail: one Collection as a collapsible Details section and a sortable list of Entries.
