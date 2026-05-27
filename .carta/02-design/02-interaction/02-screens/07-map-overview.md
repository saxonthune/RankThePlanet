---
title: Map Overview
summary: Affordance inventory for the MapOverview surface — the everything view: pins, collection filter, search, drop-pin
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"07-map-overview.inventory.json","against":{"doc":"doc02.02.01","key":"MapOverview"}}]
---

# Map Overview

The affordance inventory for the `MapOverview` surface — the default landing view that shows everything the user cares about on one map ([[03-concepts]], doc01.03 §4). **The source of truth is the carta sidecar `07-map-overview.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface is full-screen (`meta.modality: fullScreen`) and the **host** for five sheet surfaces — `LocationDraftSheet`, `LocationSheet`, `EntrySheet`, `AddLocationToCollection`, and `CollectionList` — declared in `meta.hostsSheets`. Sheets render over the map without unmounting it; their state lives in this surface's UiState ([[04-surface-composition-rules]], doc02.04). In browse mode the surface shows four chrome regions — **map**, **settingsButton**, **search**, **collectionsButton** — plus the **filterChip** when `filter-context` is set. In add-to-collection mode the regions shift (see the Add-to-collection mode section below).

The **map** region is the canvas itself: it draws the **pins** list — one pin per Collection Entry, colored by its Collection's appearance and styled by reviewed/unreviewed state — and carries *Pan* and *Zoom* (`MapOverview.pan`, `MapOverview.zoom`), which move the viewport. Tapping a pin (`MapOverview.selectPin`) is the pins list's per-item affordance and navigates to [[10-location-detail]] (`LocationSheet`), the skinny bottom-sheet peek of the Location and its Entries; when the tapped Location has exactly one Entry, `LocationSheet` renders [[09-entry-drawer]] (`EntrySheet`) directly instead of the peek (a downstream rendering choice — the statechart routes through `LocationSheet` regardless). A long-press on the map is *Drop a pin* (`DROP_PIN` → `LocationDraftSheet`), which starts the Location-first flow ([[03-concepts]], doc01.03 §2) — there is no dedicated drop-pin button. When `filter-context` is set, the pins list hides every pin whose owning Collection is not in `filter-context.collectionIds`.

The **settingsButton** region is a settings (cog) icon button in the top-left navigationIcon slot; pressing it opens *Open Settings* (`TAP_SETTINGS` → `Settings`). Replaced by the **closeButton** in add-to-collection mode.

The **search** region is a search field across the top-center, sized to roughly 30% of the surface width so it never overlaps the navigationIcon slot. Tapping it widens the field and raises the keyboard; *Search for a place* (`Location.resolve`) runs against the current viewport — the visible center is the query's locality anchor, so a "blue bottle" query in San Francisco surfaces the local café before its sibling in Tokyo. The result list interleaves two sources into one ranked list: **existing-entry hits** (Locations the user already has Entries for, rendered with their owning Collection's color dot) and **provider candidates** (unadopted results from the active `LocationProvider`, doc03.02.02). When a provider candidate's identity already matches a saved Location, only the existing-entry hit appears. Whether the field queries per keystroke or waits for submit is a per-provider trait — Photon (the `osm` default's forward-search backing) supports typeahead, the public Nominatim instance does not, BYOK providers declare their own (doc03.02.02). Picking a provider candidate (`PICK_SEARCH_RESULT`) opens it in `LocationDraftSheet`; picking an existing-entry hit (`PICK_SEARCH_ENTRY`) opens [[10-location-detail]] (`LocationSheet`), with its single-entry rendering shortcut applying when the Location has exactly one Entry.

The **collectionsButton** region is an extended floating action button anchored to the bottom-end corner over the map, holding *Browse Collections* (`TAP_COLLECTIONS_BUTTON` → `CollectionList` as a tall bottom-sheet drawer hosted here).

## Map filter

`MapOverview` carries an optional `filter-context: { collectionIds }` that constrains which Collections' pins render. Two entry points set it: `CollectionList`'s `APPLY_FILTER` (the sheet's selection-mode *See on map* button — multi-Collection set), and `EntrySheet`'s `JUMP_TO_COLLECTION` (the row's *See on Map* affordance — single-Collection set). Either path also fits the viewport to the union of the filtered Collections' Entries (`MapOverview.jumpToCollection`).

When `filter-context` is set the **filterChip** region renders over the upper area of the map (anchored to the safe-area inset, sitting under the search field). Leading content reflects the filter's appearance — the Collection's dot for a single-Collection filter, a stacked-dots glyph for a multi-Collection filter — and the label reads the Collection name (n=1) or `Filtered: {n} Collections` (n>1). The trailing close (X) holds *Clear filter* (`CLEAR_FILTER`), a self-transition that drops the context. Tapping the chip body (not the close) holds *Edit the active filter* (`TAP_FILTER_CHIP`), which reopens `CollectionList` in selection mode with the active filter pre-checked so the user adjusts the set rather than reconstructs it. The chip is a context-driven overlay ([[04-surface-composition-rules]], doc02.04 Rule 1), not a mode — the rest of the chrome is unchanged.

## Search-results mode

The surface has a third mode, **searchResults**, orthogonal to browse/addToCollection — the user can be in either of those *and* in searchResults at the same time. It is entered from the **search** region: typing in the field shows the inline dropdown of hits as before, and the dropdown carries an additional *Search on map* affordance (`COMMIT_SEARCH_TO_MAP`) that pivots the candidate set onto the map as pins.

When that affordance fires, the surface sets a `search-context` carrying `{ query, candidates, viewportAtQuery }` and the dropdown dismisses. The candidates render as the **searchCandidatePins** list — pins styled distinctly from Collection Entry pins so the user can tell *result-to-adopt* from *already-saved*. Existing-entry hits are not re-drawn as candidate pins because their owning Entry's pin is already on the map. Tapping a candidate pin fires `PICK_SEARCH_RESULT` (browse mode) or `PICK_SEARCH_RESULT_FOR_ADD` (when also in add-to-collection mode) — the same events the dropdown emits, reused unchanged.

Two further affordances are live only in searchResults mode: a **searchThisAreaChip** region overlaid on the map renders *Search this area* (`SEARCH_THIS_AREA`) when the live viewport has drifted past a threshold from `search-context.viewportAtQuery` — tapping it re-runs the committed query against the now-current viewport and replaces the candidate pins; and the search field grows a trailing close (X) holding *Clear search* (`CLEAR_SEARCH`), which drops `search-context` and exits the mode. `CLEAR_SEARCH` also fires implicitly when the user begins editing the query field — typing into the field always means *start a new search*, never *filter the committed candidate set*, so a single keystroke pops the surface out of searchResults mode and resumes the normal typing flow.

`search-context` lives in MapOverview's UiState as the host of its four sheets, so it survives every sheet round-trip the same way `collection-context` does: a candidate pin → `LocationDraftSheet` → `AddLocationToCollection` → back to MapOverview re-presents the same candidate pins and viewport rather than a cleared map. This is the loop that makes rapid add-to-collection from a single search work — pick a pin, add it, land back among the rest.

## Add-to-collection mode

`MapOverview` has two entry modes, set by how the user arrived (doc02.02.01). The default is **browse mode** — the landing surface as described above. Entering from `CollectionDetail`'s *Add a Collection Entry* (`TAP_ADD_ENTRY`) instead opens **add-to-collection mode**: a Collection is carried as entry context (`collection-context`), and the chrome shifts to single-task the user on completing the add — competing-flow affordances are suppressed and a cancel-the-flow affordance is added.

The surface is single-tasked on completing the add. Two regions remain untouched — the **map** (the user pans, zooms, and inspects pins to find a place) and the **search** (finding a place is part of completing the flow, not starting a second one). The chrome that would launch a competing flow is suppressed:

- **settingsButton** — suppressed; the user cannot navigate to Settings mid-flow.
- **collectionsButton** (*Browse Collections*) — suppressed; the user cannot bounce into the CollectionList sheet mid-flow.

Two regions replace the suppressed chrome to express the flow itself:

- **closeButton** — in the top-left navigationIcon slot, holding *Cancel adding* (`CANCEL_ADD` → `CollectionDetail`). Renders the close (X) icon. Replaces the settingsButton in the same slot.
- **statusBar** — at the bottom of the surface, naming the target Collection ("Adding to {collectionName}").

The context propagates downstream: `TAP_PIN`, `DROP_PIN`, and `PICK_SEARCH_RESULT` each declare `propagates: ["collection-context"]`, carrying it through `LocationDraftSheet` (and through `LocationSheet`/`EntrySheet` for pin taps) into `AddLocationToCollection`, where the originating Collection is pre-selected — `PICK_COLLECTION` confirms rather than chooses. Sheets reached during the flow surface their own `CANCEL_ADD` affordance so the user can cancel from any depth (see [[05-location-draft]], doc02.02.02.05).

## Post-add review prompt

When `AddLocationToCollection`'s `PICK_COLLECTION` resolves, the new Entry is carried back here as `pending-review-context`. This is **not** a third mode — the surface stays in `browse` chrome, with all regions present and untouched. The context drives a transient overlay only: a Material 3 **Snackbar** anchored over the map, naming the place and offering a *Review* action. Tapping the action fires `GO_TO_REVIEW` into [[06-review-form]] (`ReviewForm`); the Snackbar's timeout or swipe-dismiss fires `DISMISS_REVIEW_BAR`, which drops the context as a self-transition. The Entry stays saved in either case; the prompt only governs whether the user authors the Review immediately. See [[04-surface-composition-rules]] (doc02.04) for why a transient overlay is not modeled as a mode.

*Jump the map to a Collection* (`MapOverview.jumpToCollection`) is invoked on entry by both `APPLY_FILTER` and `JUMP_TO_COLLECTION`, fitting the viewport to the filtered Collections' Entries — no on-surface affordance fires it directly.

`MapOverview.open` is **deferred**: it is the act of the surface becoming the landing view on launch, with no user affordance — the map is simply already open. It is listed in the sidecar's `deferred` array so the `screen-inventory` verifier ([[05-verification-system]], doc01.04.02) counts it as an acknowledged gap. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
