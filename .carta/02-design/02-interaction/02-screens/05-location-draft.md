---
title: Location Draft
summary: Affordance inventory for the LocationDraft surface — coordinates, nearby resolution candidates, keep-or-adopt
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"05-location-draft.inventory.json","against":{"doc":"doc02.02.01","key":"LocationDraft"}}]
---

# Location Draft

The affordance inventory for the `LocationDraft` surface — where a dropped pin or a picked search result is inspected before it becomes a committed Location. **The source of truth is the carta sidecar `05-location-draft.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface renders as a sheet (`meta.modality: sheet`) hosted by `MapOverview` (`meta.host: MapOverview`) — the map stays visible behind it, and the sheet's state lives in the host's UiState, not as a separate route ([[04-surface-composition-rules]], doc02.04). It has three regions. The **topBar** marks the surface as an uncommitted draft and carries *Close*, which discards the candidate and returns to `MapOverview`. The **coordinates** region shows the candidate's raw `(lat, lng)` (read from `candidate-location`) and holds the two commitment affordances: *Keep coordinates only* (`Location.dropPin`) commits a `manual` Location as-is, and *Add to a Collection* (`ADD_TO_COLLECTION`) carries the candidate to `AddLocationToCollection`. The **candidates** region runs *Find nearby places* (`Location.resolveNearby`) — automatically on entry, re-runnable — and lists the resolved nearby Locations; adopting one replaces the bare coordinates with that provider-sourced Location.

Carried context: when the user reaches `LocationDraft` from `MapOverview` in add-to-collection mode, `collection-context` is set. This surface does not consume the context — no region or affordance reads it — but `ADD_TO_COLLECTION` declares `propagates: ["collection-context"]`, forwarding it to `AddLocationToCollection` where the originating Collection is pre-selected. The propagation is the load-bearing link in the Collection-first add flow; dropping it silently breaks the flow without any other visible symptom.

Keeping the coordinates and adopting a candidate are two outcomes of the same surface: either produces one Location that *Add to a Collection* then carries forward ([[03-concepts]], doc01.03 §2 — resolution is optional enrichment, never required). The candidate list's empty state is not a failure — a point with no nearby matches is still committable as coordinates. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
