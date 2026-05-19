---
title: Map Overview
summary: Affordance inventory for the MapOverview surface — the everything view: pins, collection filter, search, drop-pin
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"07-map-overview.inventory.json","against":{"doc":"doc02.02.01","key":"MapOverview"}}]
---

# Map Overview

The affordance inventory for the `MapOverview` surface — the default landing view that shows everything the user cares about on one map ([[03-concepts]], doc01.03 §4). **The source of truth is the carta sidecar `07-map-overview.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface is a full-screen map with overlays, in five regions. The **map** region is the canvas itself: it draws the **pins** list — one pin per Collection Entry, colored by its Collection's appearance and styled by visited/unvisited state — and carries *Pan* and *Zoom* (`MapOverview.pan`, `MapOverview.zoom`), which move the viewport. Tapping a pin (`MapOverview.selectPin`) is the pins list's per-item affordance and navigates to `LocationDetail`. The **topBar** holds *Browse Collections* (`TAP_COLLECTIONS_BUTTON` → `CollectionList`) and *Open Settings* (`TAP_SETTINGS` → `Settings`). The **search** region runs *Search for a place* (`Location.resolve`) and lists the provider's candidates; picking one (`PICK_SEARCH_RESULT`) opens it in `LocationDraft`. The **collectionFilter** region iterates the user's Collections as chips — toggling a chip shows or hides that Collection's pins (`MapOverview.toggleCollection`), and *Jump the map to a Collection* (`MapOverview.jumpToCollection`) recenters the camera. The **mapActions** region holds *Drop a pin here* (`DROP_PIN` → `LocationDraft`), which starts the Location-first flow ([[03-concepts]], doc01.03 §2).

`MapOverview.open` is **deferred**: it is the act of the surface becoming the landing view on launch, with no user affordance — the map is simply already open. It is listed in the sidecar's `deferred` array so the `screen-inventory` verifier ([[05-verification-system]], doc01.05) counts it as an acknowledged gap. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
