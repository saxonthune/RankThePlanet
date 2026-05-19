---
title: Interaction
summary: Platform-agnostic UI design — surfaces, navigation graph, action coverage
tags: [design, interaction, index]
deps: [doc01.03]
---

# Interaction

Platform-agnostic UI design. Names the views the user moves between and verifies every concept action ([[03-concepts]], doc01.03) has a UI affordance.

Every surface is an equal **view** — full screen, sheet, sidebar, popover is a downstream rendering choice, not a structural one. There is no screens-vs-overlays split.

Surfaces are described in concept-language only — no Composables, no UiState, no `NavHost`.

## What belongs here

- The surface inventory (what regions of UI exist and what they're for)
- The per-surface affordance inventory (regions, affordances, lists)
- The navigation graph (how the user moves between surfaces)
- The action-coverage check (every concept action reachable)

## What does not

- Composable shapes, UiState models, ViewModel boundaries
- Framework-specific routing syntax
- Visual design tokens, pin colors, typography — see [[03-theme-tokens]] (doc02.03)

## Verification

The navigation graph is an XState statechart kept as a carta sidecar (`01-navigation.statechart.json`). Transitions are pure navigation; the concept actions performable on each view live in that view's `meta.actions` array. A coverage script diffs the set of `meta.actions` entries against doc01.03's action lists; orphan actions = gulf of execution.

A Luminous pipeline (`.luminous/statechart-canvas.pipeline.mjs`) generates a visual canvas graph from the statechart sidecar. See doc02.02.01 (`01-navigation.md`).

## Contents

- doc02.02.01 — Navigation: surface inventory + statechart sidecar.
- doc02.02.02 — Screens: per-surface affordance inventories.
