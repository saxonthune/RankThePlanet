---
title: Map Overview
summary: Affordance inventory for the MapOverview surface — the everything view: pins, collection filter, search, drop-pin
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"07-map-overview.inventory.json","against":{"doc":"doc02.02.01","key":"MapOverview"}}]
---

# Map Overview

The affordance inventory for the `MapOverview` surface — the default landing view that shows everything the user cares about on one map ([[03-concepts]], doc01.03 §4). **The source of truth is the carta sidecar `07-map-overview.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface is full-screen (`meta.modality: fullScreen`) and the **host** for four sheet surfaces — `LocationDraftSheet`, `LocationSheet`, `EntrySheet`, `AddLocationToCollection` — declared in `meta.hostsSheets`. Sheets render over the map without unmounting it; their state lives in this surface's UiState ([[04-surface-composition-rules]], doc02.04). In browse mode the surface shows five regions — **map**, **menuButton**, **search**, **bottomBar**, and the **menuDrawer** (closed by default). In add-to-collection mode the regions shift (see the Add-to-collection mode section below).

The **map** region is the canvas itself: it draws the **pins** list — one pin per Collection Entry, colored by its Collection's appearance and styled by visited/unvisited state — and carries *Pan* and *Zoom* (`MapOverview.pan`, `MapOverview.zoom`), which move the viewport. Tapping a pin (`MapOverview.selectPin`) is the pins list's per-item affordance and navigates to [[10-location-detail]] (`LocationSheet`), the skinny bottom-sheet peek of the Location and its Entries; when the tapped Location has exactly one Entry, `LocationSheet` renders [[09-entry-drawer]] (`EntrySheet`) directly instead of the peek (a downstream rendering choice — the statechart routes through `LocationSheet` regardless). A long-press on the map is *Drop a pin* (`DROP_PIN` → `LocationDraftSheet`), which starts the Location-first flow ([[03-concepts]], doc01.03 §2) — there is no dedicated drop-pin button.

The **menuButton** region is a button in the top-left corner; pressing it opens the **menuDrawer**, a left-hand drawer that is the app menu. The drawer holds a **Filter section** — *Search the collection list*, a field that filters the drawer's Collection list by name, and the **collections** list itself, one row per Collection, each toggleable to show or hide that Collection's pins (`MapOverview.toggleCollection`) — and *Open Settings* (`TAP_SETTINGS` → `Settings`). The collection filter is one option the menu offers, not the menu's whole identity.

The **search** region is a search field across the top-center, sized to roughly 30% of the surface width so it never overlaps the menu button. Tapping it widens the field and raises the keyboard; submitting a query runs *Search for a place* (`Location.resolve`) and lists the provider's candidates. There is no search-ahead — candidates appear only after the query is submitted. Picking a candidate (`PICK_SEARCH_RESULT`) opens it in `LocationDraftSheet`.

The **bottomBar** region is a panel along the bottom of the surface, holding *Browse Collections* (`TAP_COLLECTIONS_BUTTON` → `CollectionList`).

*Jump the map to a Collection* (`MapOverview.jumpToCollection`) has no affordance on this surface yet; it is in the sidecar's `deferred` array as an acknowledged gap.

## Add-to-collection mode

`MapOverview` has two entry modes, set by how the user arrived (doc02.02.01). The default is **browse mode** — the landing surface as described above. Entering from `CollectionDetail`'s *Add a Collection Entry* (`TAP_ADD_ENTRY`) instead opens **add-to-collection mode**: a Collection is carried as entry context (`collection-context`), and the chrome shifts to single-task the user on completing the add — competing-flow affordances are suppressed and a cancel-the-flow affordance is added.

The surface is single-tasked on completing the add. Two regions remain untouched — the **map** (the user pans, zooms, and inspects pins to find a place) and the **search** (finding a place is part of completing the flow, not starting a second one). The chrome that would launch a competing flow is suppressed:

- **menuButton** (hamburger) and the **menuDrawer** it opens — suppressed; the user cannot navigate to Settings or toggle Collection filters mid-flow.
- **bottomBar** (*Browse Collections*) — suppressed; the user cannot bounce into the Collection list mid-flow.

Two regions replace the suppressed chrome to express the flow itself:

- **closeButton** — in the top-left navigationIcon slot, holding *Cancel adding* (`CANCEL_ADD` → `CollectionDetail`). Renders the close (X) icon. Replaces the menuButton in the same slot.
- **statusBar** — at the bottom of the surface, naming the target Collection ("Adding to {collectionName}"). Replaces the bottomBar in the same slot.

The context propagates downstream: `TAP_PIN`, `DROP_PIN`, and `PICK_SEARCH_RESULT` each declare `propagates: ["collection-context"]`, carrying it through `LocationDraftSheet` (and through `LocationSheet`/`EntrySheet` for pin taps) into `AddLocationToCollection`, where the originating Collection is pre-selected — `PICK_COLLECTION` confirms rather than chooses. Sheets reached during the flow surface their own `CANCEL_ADD` affordance so the user can cancel from any depth (see [[05-location-draft]], doc02.02.02.05).

## Post-add review prompt

When `AddLocationToCollection`'s `PICK_COLLECTION` resolves, the new Entry is carried back here as `pending-review-context`. This is **not** a third mode — the surface stays in `browse` chrome, with all regions present and untouched. The context drives a transient overlay only: a Material 3 **Snackbar** anchored over the map, naming the place and offering a *Review* action. Tapping the action fires `GO_TO_REVIEW` into [[06-review-form]] (`ReviewForm`); the Snackbar's timeout or swipe-dismiss fires `DISMISS_REVIEW_BAR`, which drops the context as a self-transition. The Entry stays saved in either case; the prompt only governs whether the user authors the Review immediately. See [[04-surface-composition-rules]] (doc02.04) for why a transient overlay is not modeled as a mode.

`MapOverview.open` is **deferred**: it is the act of the surface becoming the landing view on launch, with no user affordance — the map is simply already open. It is listed in the sidecar's `deferred` array so the `screen-inventory` verifier ([[05-verification-system]], doc01.04.02) counts it as an acknowledged gap. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
