---
title: Add Location to Collection
summary: Affordance inventory for the AddLocationToCollection sheet — pick a Collection (pre-selected in add mode) or create a new one, hosted over MapOverview
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"11-add-location-to-collection.inventory.json","against":{"doc":"doc02.02.01","key":"AddLocationToCollection"}}]
---

# Add Location to Collection

The affordance inventory for the `AddLocationToCollection` surface — where a chosen candidate Location is paired with one of the user's Collections, on the way to a Review. **The source of truth is the carta sidecar `11-add-location-to-collection.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface renders as a sheet (`meta.modality: sheet`) hosted by `MapOverview` (`meta.host: MapOverview`) — the map stays visible behind it and the sheet's state lives in `MapOverviewUiState`, not as a separate route ([[04-surface-composition-rules]], doc02.04). It is reached from `LocationDraftSheet`'s `ADD_TO_COLLECTION`; the projection swaps the sheet content in place rather than stacking two modal sheets over the map. The host carries the candidate Location and any `collection-context` in its UiState, so the swap loses nothing visible to the user.

The surface has no top bar. Two regions. The **candidate** region reflects the Location being added — display name when the user adopted a nearby candidate, raw coordinates otherwise — read-only; the user confirms the candidate by picking a Collection, not by editing it here. The **collections** region is a vertical list of the user's Collections, where tapping one fires `PICK_COLLECTION` (`Location.addToCollection`) and advances to [[06-review-form]] (`ReviewForm`). The list also holds *New Collection* (`TAP_NEW_COLLECTION` → `CollectionEditor`, `Collection.create`) as the first row, so creating a new Collection mid-flow is one tap away. A *Back* affordance in the same region steps one surface back to `LocationDraftSheet` — the draft sheet re-presents with the same candidate intact.

When the user reaches this surface with `collection-context` set (the add-to-collection flow from `CollectionDetail`), the originating Collection is rendered with a pre-selected indicator at the top of the list. `PICK_COLLECTION` then confirms rather than chooses; the user can still pick a different Collection or create a new one. The mode does not change which affordances appear — it only changes the list ordering and the selection indicator (doc02.04 §1).

Two ways out, by gesture polarity. *Back* (`BACK` → `LocationDraftSheet`) is the in-content affordance and is the one-step-back path — the candidate Location is preserved in the host's UiState. *Close* (`CLOSE` → `MapOverview`) is the sheet's swipe-down or scrim-tap dismiss-gesture and is the kill-the-flow path — the candidate and any carried `collection-context` are dropped, returning to a clean map. The two are distinct events in the statechart so the verifier sees them as covering distinct transitions. Nothing here names a Composable or a UiState; that binding is part of doc03.03.
