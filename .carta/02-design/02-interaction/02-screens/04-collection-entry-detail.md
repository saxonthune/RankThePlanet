---
title: Collection Entry Detail
summary: Affordance inventory for the CollectionEntryDetail surface — the (Location, Review) pair, reviewed/unreviewed
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"04-collection-entry-detail.inventory.json","against":{"doc":"doc02.02.01","key":"CollectionEntryDetail"}}]
---

# Collection Entry Detail

The affordance inventory for the `CollectionEntryDetail` surface — one Collection Entry, the `(Location, Review)` pair ([[03-concepts]], doc01.03). **The source of truth is the carta sidecar `04-collection-entry-detail.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface has three regions. The **topBar** names the Entry by its Location and carries the Entry-level affordances — *Back* to the owning Collection, *View the Collection* (`VIEW_COLLECTION` → `CollectionDetail`, which opens the owning Collection explicitly — useful when the Entry was reached from a map pin), and *Remove the Collection Entry* (`Collection.removeEntry`, which drops the Entry without deleting the Location). The **location** region shows the Location — display name, coordinates, cached provider metadata — and its two leaf handoffs: *Open in an external map app* (`Location.openExternally`) and *Refresh the Location's cached metadata* (`Location.refresh`). The **review** region renders the Review as the full template field set, with *Edit the Review* (`Review.edit`) opening the `ReviewForm`.

A Review is absent until the user submits one ([[03-concepts]], doc01.03 §3). The review region renders one of two states — **unreviewed**, where the Entry has no Review yet and every template field shows unset, prompting toward *Edit the Review*; and **reviewed**, showing whatever the user has saved, however sparse. There is no "incomplete" state in between — a one-field Review is a finished Review.

Each navigating affordance ties to a transition on `CollectionEntryDetail` in the statechart ([[01-navigation]], doc02.02.01); `Location.openExternally` and `Location.refresh` are in-view actions with no transition. The review field list iterates the Collection's Review template for field shape and reads the Entry's Review (`meta.reads`: `entry`, `location`, `review`) for values. All four of the surface's `meta.actions` are inventoried, so no `deferred` list is needed.
