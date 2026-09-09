---
title: Collection Detail
summary: Affordance inventory for the CollectionDetail surface — Details section, sortable entry list
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"02-collection-detail.inventory.json","against":{"doc":"doc02.02.01","key":"CollectionDetail"}}]
---

# Collection Detail

The affordance inventory for the `CollectionDetail` surface — one Collection seen as its list of Collection Entries. **The source of truth is the rhidoc sidecar `02-collection-detail.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface has three regions. The **topBar** carries the surface title — leading with a colored dot that renders the Collection's appearance, so the user identifies the Collection at a glance the same way pins do on the map — and the Collection-level affordances: *Add a Collection Entry* (`Collection.addEntry`), *Edit the Collection* (opens `CollectionEditor`; `Review.editTemplate`), *View on map* (`JUMP_TO_COLLECTION` → `MapOverview` with `filter-context` set to this Collection's id and the viewport fit to its Entries — the single-Collection variant of `CollectionList`'s `APPLY_FILTER`), *Back* to the Collection list, and an overflow menu housing *Export* (`Collection.export`) which produces a portable bundle (KML or GeoJSON) and hands it to the platform share sheet — a self-affordance that does not navigate. The **details** region is a collapsed-by-default section that expands in place to show the Collection's metadata (name, description, appearance, timestamps, template version, entry count) read-only — editing it is done in `CollectionEditor`, not here; collapsing is a self-action, not a navigation. The **content** region is a single list iterating the Collection's Entries; each item shows the Location and a Review summary and opens that Entry on tap.

Above the list sits a single **sort chip** reading `Sort: {active key} {direction-arrow}`. Tapping the label opens a menu of sort keys; tapping the arrow toggles ascending/descending. The menu's options are derived from Collection state: *Date Added* (the default) and *Review Time* are always present, *Score* appears only when the Collection's Review template defines a rating field, and *Power Rank* appears only when `Collection.powerRanking` is on ([[03-concepts]], doc01.03 §1, §3). *Near Me* is named in the use cases but inert until device location lands. When the active sort is *Power Rank*, the list renders with leading drag handles and the user can drag an entry to a new position — releasing writes back the entry's rank position. Switching to any other key removes the handles. The direction toggle is suppressed in *Power Rank* mode — the user's authored order has no asc/desc axis.

Each navigating affordance ties to a transition on `CollectionDetail` in the statechart ([[01-navigation]], doc02.02.01); the topBar affordances also carry the surface's `meta.actions`. Beyond the statechart's `meta.reads` (`collection`, `entries`, `appearance`), the surface also reads the Collection's Review template to resolve which field the *Score* sort orders on. The map/list projection toggle (`TAP_TOGGLE_PROJECTION`) is a known affordance of this surface but is not yet inventoried — this inventory covers the list projection only.
