---
title: Map Overview
summary: Affordance inventory for the MapOverview surface — the everything view: pins, collection filter, search, drop-pin
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"07-map-overview.inventory.json","against":{"doc":"doc02.02.01","key":"MapOverview"}}]
---

# Map Overview

The affordance inventory for the `MapOverview` surface — the default landing view that shows everything the user cares about on one map ([[03-concepts]], doc01.03 §4). **The source of truth is the carta sidecar `07-map-overview.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface is a full-screen map with overlays. Four regions are always present — **map**, **filterButton**, **search**, **bottomBar** — plus the **collectionFilterDrawer**, which is present but closed by default, and a sixth, **statusBar**, which appears only in add-to-collection mode (below).

The **map** region is the canvas itself: it draws the **pins** list — one pin per Collection Entry, colored by its Collection's appearance and styled by visited/unvisited state — and carries *Pan* and *Zoom* (`MapOverview.pan`, `MapOverview.zoom`), which move the viewport. Tapping a pin (`MapOverview.selectPin`) is the pins list's per-item affordance and navigates to `LocationDetail`. A long-press on the map is *Drop a pin* (`DROP_PIN` → `LocationDraft`), which starts the Location-first flow ([[03-concepts]], doc01.03 §2) — there is no dedicated drop-pin button.

The **filterButton** region is a button in the top-left corner; pressing it opens the **collectionFilterDrawer**, a left-hand drawer. The drawer holds *Search the collection list*, a field that filters the drawer's Collection list by name, and the **collections** list itself — one row per Collection, each toggleable to show or hide that Collection's pins (`MapOverview.toggleCollection`).

The **search** region is a search field across the top-center, sized to roughly 30% of the surface width so it never overlaps the filter button. Tapping it widens the field and raises the keyboard; submitting a query runs *Search for a place* (`Location.resolve`) and lists the provider's candidates. There is no search-ahead — candidates appear only after the query is submitted. Picking a candidate (`PICK_SEARCH_RESULT`) opens it in `LocationDraft`.

The **bottomBar** region is a panel along the bottom of the surface, holding *Browse Collections* (`TAP_COLLECTIONS_BUTTON` → `CollectionList`) and *Open Settings* (`TAP_SETTINGS` → `Settings`, shown as an icon).

*Jump the map to a Collection* (`MapOverview.jumpToCollection`) has no affordance on this surface yet; it is in the sidecar's `deferred` array as an acknowledged gap.

## Add-to-collection mode

`MapOverview` has two entry modes, set by how the user arrived (doc02.02.01). The default is **browse mode** — the landing surface, every region above as described. Entering from `CollectionDetail`'s *Add a Collection Entry* (`TAP_ADD_ENTRY`) instead opens **add-to-collection mode**: a Collection is carried as entry context (`collection-context`), and the **statusBar** region appears, naming it — "Adding to {collectionName}" — and holding *Cancel adding* (`CANCEL_ADD` → `CollectionDetail`).

The map, pins, search, and every other affordance are identical between the two modes — add-to-collection mode is the same surface with one extra region and one carried value, not a separate screen. What the context changes is downstream: any Location the user opens (tap a pin, drop a pin, pick a search result) carries `collection-context` through `LocationDraft` into `AddLocationToCollection`, where the originating Collection is pre-selected — its `PICK_COLLECTION` confirms rather than chooses. `CANCEL_ADD` is live only in this mode; the verifier sees it as a transition the `statusBar` covers.

`MapOverview.open` is **deferred**: it is the act of the surface becoming the landing view on launch, with no user affordance — the map is simply already open. It is listed in the sidecar's `deferred` array so the `screen-inventory` verifier ([[05-verification-system]], doc01.05) counts it as an acknowledged gap. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
