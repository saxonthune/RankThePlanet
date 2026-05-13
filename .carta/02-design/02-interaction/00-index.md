---
title: Interaction
summary: Platform-agnostic UI design — surfaces, navigation graph, action coverage
tags: [design, interaction, index]
deps: [doc01.03]
---

# Interaction

Platform-agnostic UI design. Names the surfaces the user moves between and verifies every concept action ([[03-concepts]], doc01.03) has a UI affordance.

This sits between concepts (what RTP is) and CMP-bound system specs (how it's built). Surfaces are described in concept-language only — no Composables, no UiState, no `NavHost`. The CMP layer beneath refines surfaces → screens, transitions → routes, affordances → events.

## What belongs here

- The surface inventory (what regions of UI exist and what they're for)
- The navigation graph (how the user moves between surfaces)
- The action-coverage check (every concept action reachable)

## What does not

- Composable shapes, UiState models, ViewModel boundaries → future `04-system/`
- Visual design tokens, pin colors, typography → future design tokens doc
- Framework-specific routing syntax → future system spec

## Verification

The navigation graph is an XState statechart kept as a carta sidecar (`01-navigation.statechart.json`). Each transition that invokes a concept action carries `meta.concept` + `meta.action` tags. A coverage script diffs the set of tagged actions against doc01.03's action lists; orphan actions = gulf of execution.

The statechart is generated to a TypeScript file at `tools/statechart/generated/` for visualization with the Stately VS Code extension. See doc02.02.01 (`01-navigation.md`) for the loop.

## Contents

- doc02.02.01 — Navigation: surface inventory + statechart sidecar.
