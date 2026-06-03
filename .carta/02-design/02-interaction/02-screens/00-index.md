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

- **`regions`** — named areas of the surface (`topBar`, `content`), each with a purpose. Optional fields:
  - **`appearsInModes`** — array of mode keys (from the statechart state's `meta.modes`) in which this region appears. Absent = always present.
  - **`reactsToContext`** — array of context keys (from `meta.context`) whose value shapes this region's content (e.g. the statusBar reads `collection-context` to render "Adding to {name}").
- **`affordances`** — what the user can act on. Each has a `region`, a `label`, and either an `event` (a transition from the statechart) or stays a self-action. Events carrying a concept action name it in `action`, matching the surface's `meta.actions`. Optional `appearsInModes` gates the affordance per-mode the same way.
- **`lists`** — repeated content. Each names what domain type it `iterates`, which `meta.reads` key it `reads`, the per-item affordance, and an empty state. Optional `appearsInModes` available.

Coverage stays honest: every `event` must be a real transition on that surface in the statechart, every `action` must appear in its `meta.actions`, every mode named in `appearsInModes` must exist in the state's `meta.modes`, and every key in `reactsToContext` must exist in `meta.context`.

- **`deferred`** (optional, top-level) — a list of statechart events and `meta.actions` the surface knowingly has not inventoried yet. The `screen-inventory` verifier treats deferred items as acknowledged gaps rather than failures, and reports their count as a backlog metric. Use `deferred` for affordances that exist in the statechart but are not yet built (e.g. a toggle whose UI is not yet designed).
- **`invariants`** (optional, top-level) — load-bearing assertions about the surface that prose alone cannot defend against reference rot. Each entry is `{id?, text, predicate?}` — `text` is required, `id` is recommended for traceability and must be unique when present. The `text` is free-form prose; symbol references are marked with backticks (`` `CLEAR_SEARCH` ``, `` `searchResults` ``, `` `search-context.viewportAtQuery` ``). The `invariant-resolution` verifier scans the backticked tokens and resolves each against the surface's address space: events, modes, context keys, region ids, affordance ids, list ids, and chart state ids. Dotted tokens resolve the left side; whitespace-bearing tokens and quote-prefixed tokens are treated as prose and skipped. The optional `predicate` field carries a string in the Phase 2 guard grammar (`has(k)` / `mode(m)` / `eq(k.f, v)` with `not` / `and` / `or` — see [[../../../01-product/04-development-philosophy/02-verification-system]], doc01.04.02 `## The guard-coverage verifier`) and states a *necessary precondition* for the invariant's positive case; the prose `text` refines with the consequence or runtime-only part. The verifier statically validates predicate identifiers against the surface's context keys and modes — trace evaluation is out of scope. Invariants whose claim resists the current grammar stay `text`-only and carry an optional `note` describing the gap.

## Contents

- doc02.02.02.01 — Collection List: the surface that manages the user's Collections.
- doc02.02.02.02 — Collection Detail: one Collection as a collapsible Details section and a sortable list of Entries.
- doc02.02.02.03 — Settings: entry to provider config; sync target and BYOK keys remain stubs.
- doc02.02.02.04 — Collection Entry Detail: one Entry as its Location, Review handoffs, and the full template field set; reviewed vs unreviewed.
- doc02.02.02.05 — Location Draft: an uncommitted dropped pin or search result — coordinates, nearby resolution candidates, keep-or-adopt.
- doc02.02.02.06 — Review Form: authoring a Review against its Collection's template — the field set as editable inputs, save and cancel.
- doc02.02.02.07 — Map Overview: the everything view — pins for every Entry, a collection filter, search, and drop-pin.
- doc02.02.02.08 — Collection Editor: authoring a Collection's metadata and its Review template — field set, types, and built-in templates.
- doc02.02.02.09 — Entry Drawer: a bottom-sheet peek of one Collection Entry over MapOverview, with handoffs to full detail, owning Collection, and review form.
- doc02.02.02.10 — Location Detail: the skinny bottom-sheet peek of a Location and the Collection Entries that reference it; rendered only when multi-entry.
