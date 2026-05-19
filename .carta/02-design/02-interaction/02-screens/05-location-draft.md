---
title: Location Draft
summary: Affordance inventory for the LocationDraft surface — coordinates, nearby resolution candidates, keep-or-adopt
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"05-location-draft.inventory.json","against":{"doc":"doc02.02.01","key":"LocationDraft"}}]
---

# Location Draft

The affordance inventory for the `LocationDraft` surface — where a dropped pin or a picked search result is inspected before it becomes a committed Location. **The source of truth is the carta sidecar `05-location-draft.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface renders as a sheet over the map ([[01-navigation]], doc02.02.01) — the map stays visible behind it — and has three regions. The **topBar** marks the surface as an uncommitted draft and carries *Close*, which discards the candidate and returns to `MapOverview`. The **coordinates** region shows the candidate's raw `(lat, lng)` (read from `candidate-location`) and holds the two commitment affordances: *Keep coordinates only* (`Location.dropPin`) commits a `manual` Location as-is, and *Add to a Collection* (`ADD_TO_COLLECTION`) carries the candidate to `AddLocationToCollection`. The **candidates** region runs *Find nearby places* (`Location.resolveNearby`) — automatically on entry, re-runnable — and lists the resolved nearby Locations; adopting one replaces the bare coordinates with that provider-sourced Location.

Keeping the coordinates and adopting a candidate are two outcomes of the same surface: either produces one Location that *Add to a Collection* then carries forward ([[03-concepts]], doc01.03 §2 — resolution is optional enrichment, never required). The candidate list's empty state is not a failure — a point with no nearby matches is still committable as coordinates. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
