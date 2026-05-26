---
title: Collection Detail
summary: Affordance inventory for the CollectionDetail surface — Details section, sortable entry list
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"02-collection-detail.inventory.json","against":{"doc":"doc02.02.01","key":"CollectionDetail"}}]
---

# Collection Detail

The affordance inventory for the `CollectionDetail` surface — one Collection seen as its list of Collection Entries. **The source of truth is the carta sidecar `02-collection-detail.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface has three regions. The **topBar** carries the surface title and the Collection-level affordances — *Add a Collection Entry* (`Collection.addEntry`), *Edit the Collection* (opens `CollectionEditor`; `Review.editTemplate`), *Back* to the Collection list, and an overflow menu housing *Export* (`Collection.export`) which produces a portable bundle (KML or GeoJSON) and hands it to the platform share sheet — a self-affordance that does not navigate. The **details** region is a collapsed-by-default section that expands in place to show the Collection's metadata (name, description, appearance, timestamps, template version, entry count) read-only — editing it is done in `CollectionEditor`, not here; collapsing is a self-action, not a navigation. The **content** region is a single list iterating the Collection's Entries; each item shows the Location and a Review summary and opens that Entry on tap. A sort control over the list offers *Date Added*, *Review Time*, and *Score* orderings — *Score* appears only when the Collection's Review template defines a rating field. *Near Me* is named but inert until device location lands.

Each navigating affordance ties to a transition on `CollectionDetail` in the statechart ([[01-navigation]], doc02.02.01); the topBar affordances also carry the surface's `meta.actions`. Beyond the statechart's `meta.reads` (`collection`, `entries`, `appearance`), the surface also reads the Collection's Review template to resolve which field the *Score* sort orders on. The map/list projection toggle (`TAP_TOGGLE_PROJECTION`) is a known affordance of this surface but is not yet inventoried — this inventory covers the list projection only.
