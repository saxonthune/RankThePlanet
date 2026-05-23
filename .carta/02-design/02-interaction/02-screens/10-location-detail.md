---
title: Location Detail
summary: Affordance inventory for the LocationSheet surface — the skinny bottom-sheet peek of a Location, the Collection Entries that reference it, and the add-another-Entry entry point
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"10-location-detail.inventory.json","against":{"doc":"doc02.02.01","key":"LocationSheet"}}]
---

# Location Detail

The affordance inventory for the `LocationSheet` surface — a skinny bottom-sheet peek of one Location and every Collection Entry that references it, opened by tapping a pin on `MapOverview` ([[03-concepts]], doc01.03 §2). **The source of truth is the carta sidecar `10-location-detail.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

`LocationSheet` renders as a sheet (`meta.modality: sheet`) hosted by `MapOverview` (`meta.host: MapOverview`); the sheet's state lives in the host's UiState rather than being its own route ([[04-surface-composition-rules]], doc02.04).

The surface is a skinny bottom sheet split into two halves. The **locationSummary** region (left half) shows the Location — its display name and coordinates. The **entries** region (right half) is a vertically scrolling list of every Entry that references this Location, one per Collection; each row shows the owning Collection's name and appearance color and the Entry's visited/unvisited state. Tapping a row picks that Entry and opens the [[09-entry-drawer]] ([[01-navigation]], doc02.02.01: `TAP_ENTRY` → `EntrySheet`). Below the list sits *Add another Entry at this Location* (`TAP_ADD_ENTRY` → `AddLocationToCollection`), which propagates the Location as `location-context` so the picked Collection's new Entry binds to this Location.

The peek renders the same way for any Entry count ≥ 1; a one-Entry list is a single row above the add-entry affordance. *Dismiss* is the bottom-sheet's swipe-down or scrim-tap gesture and is modeled as `BACK` → `MapOverview`.

`LocationSheet` is also reachable from [[09-entry-drawer]] via `TAP_LOCATION` on the EntrySheet's `locationBlock` — the user pivots from one Entry's peek to the Location's fan-out of sibling Entries, or uses *Add another Entry at this Location* to bind a new Entry to the same Location without leaving the map context.

`LocationSheet` is traversed by the add-to-collection flow: `MapOverview`'s `TAP_PIN` declares `propagates: ["collection-context"]`, so the surface receives the carried Collection when entered from add-mode. The statechart declares a `CANCEL_ADD` transition (target `CollectionDetail`, guard `inAddMode`) so the user can abandon the flow from this peek; the inventory lists `CANCEL_ADD` in `deferred` because the visual treatment of an in-add-mode peek is yet to unfold.

The surface's two in-view concept actions, `Location.openExternally` and `Location.refresh`, were inherited from the earlier full-screen framing and have no peek affordance; they are listed in the sidecar's `deferred` array.
