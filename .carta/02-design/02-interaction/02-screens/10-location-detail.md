---
title: Location Detail
summary: Affordance inventory for the LocationDetail surface — the skinny bottom-sheet peek of a Location and the Collection Entries that reference it; rendered only when multi-entry
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"10-location-detail.inventory.json","against":{"doc":"doc02.02.01","key":"LocationDetail"}}]
---

# Location Detail

The affordance inventory for the `LocationDetail` surface — a skinny bottom-sheet peek of one Location and every Collection Entry that references it, opened by tapping a pin on `MapOverview` ([[03-concepts]], doc01.03 §2). **The source of truth is the carta sidecar `10-location-detail.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

`LocationDetail` renders as a sheet (`meta.modality: sheet`) hosted by `MapOverview` (`meta.host: MapOverview`); the sheet's state lives in the host's UiState rather than being its own route ([[04-surface-composition-rules]], doc02.04).

The surface is a skinny bottom sheet split into two halves. The **locationSummary** region (left half) shows the Location — its display name and coordinates. The **entries** region (right half) is a vertically scrolling list of every Entry that references this Location, one per Collection; each row shows the owning Collection's name and appearance color and the Entry's visited/unvisited state. Tapping a row picks that Entry and opens the [[09-entry-drawer]] ([[01-navigation]], doc02.02.01: `TAP_ENTRY` → `EntryDrawer`).

The peek is meaningful only when a Location has multiple Entries. When the tapped Location has exactly one Entry, the surface renders the `EntryDrawer` directly instead of the split peek — this is a downstream rendering choice ([[01-navigation]], doc02.02.01) and stays out of the statechart. *Dismiss* is the bottom-sheet's swipe-down or scrim-tap gesture and is modeled as `BACK` → `MapOverview`.

`LocationDetail` is traversed by the add-to-collection flow: `MapOverview`'s `TAP_PIN` declares `propagates: ["collection-context"]`, so the surface receives the carried Collection when entered from add-mode. Per Rule 3 ([[04-surface-composition-rules]], doc02.04), the inventory must offer a `CANCEL_ADD` affordance in add-mode so the user can abandon the flow from this peek. The statechart declares the `CANCEL_ADD` transition (target `CollectionDetail`, guard `inAddMode`); the inventory lists `CANCEL_ADD` in `deferred` because the visual treatment of an in-add-mode peek is yet to unfold.

The surface's two in-view concept actions, `Location.openExternally` and `Location.refresh`, were inherited from the earlier full-screen framing and have no peek affordance; they are listed in the sidecar's `deferred` array.
