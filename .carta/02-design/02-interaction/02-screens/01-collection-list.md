---
title: Collection List
summary: Affordance inventory for the CollectionList surface — a tall bottom-sheet over MapOverview that browses Collections and filters the map
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"01-collection-list.inventory.json","against":{"doc":"doc02.02.01","key":"CollectionList"}}]
---

# Collection List

The affordance inventory for the `CollectionList` surface — where the user browses Collections and decides which ones the map renders. **The source of truth is the carta sidecar `01-collection-list.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface is a **sheet** hosted by [[07-map-overview]] (`meta.modality: sheet`, `meta.host: MapOverview`) — a tall bottom-sheet drawer that takes up most of the screen, with the map still mounted behind it ([[04-surface-composition-rules]], doc02.04 Rule 2). It is reached two ways from MapOverview: the `collectionsButton` FAB fires `TAP_COLLECTIONS_BUTTON` and opens the sheet in browse mode; the `filterChip` fires `TAP_FILTER_CHIP` and opens the sheet in selection mode with the active filter pre-checked. The sheet's dismiss-gesture (swipe-down, scrim-tap) is `BACK` → `MapOverview`; the active `filter-context` on MapOverview is unaffected by a plain dismiss.

The surface has two modes:

- **browse** — default. Each row in the **content** list is tappable to open that Collection's `CollectionDetail`. The **topBar** holds the two creation affordances — *New Collection* (`Collection.create`) and *Import a list* (`Collection.import`) — plus a *Search collections by name* field that filters the rendered list (in-surface filter, not a navigation event).
- **selection** — entered by long-pressing any row (`ENTER_SELECT_MODE`), or pre-entered when the sheet is opened via `TAP_FILTER_CHIP`. Each row grows a leading checkbox; tapping a row toggles selection instead of opening it. The **topBar** swaps to a selection action bar: `{n} selected` on the left, *Cancel* (`EXIT_SELECT_MODE`) and *See on map* (`APPLY_FILTER` → `MapOverview`, with `filter-context` carrying the selected Collection ids) on the right. *See on map* is disabled when `n = 0`. The two creation affordances and the in-surface search are suppressed in this mode.

Every navigating affordance ties to a transition on `CollectionList` in the statechart ([[01-navigation]], doc02.02.01); the two creation affordances also carry the surface's `meta.actions`. Nothing here names a Composable or a UiState — that binding is not part of the interaction layer (doc02.02.00).
