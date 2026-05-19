---
title: Collection List
summary: Affordance inventory for the CollectionList surface — regions, affordances, lists
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"01-collection-list.inventory.json","against":{"doc":"doc02.02.01","key":"CollectionList"}}]
---

# Collection List

The affordance inventory for the `CollectionList` surface — where the user manages their Collections. **The source of truth is the carta sidecar `01-collection-list.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface has two regions. The **topBar** carries the two collection-level creation affordances — *New Collection* (`Collection.create`) and *Import a list* (`Collection.import`) — plus *Back* to the everything view. The **content** region is a single list iterating every `Collection`; each item shows the Collection's name, its appearance marker, and an entry count, and opens that Collection on tap. With no Collections, the content region shows a hint pointing at the two creation affordances.

Every affordance ties to a transition on `CollectionList` in the statechart ([[01-navigation]], doc02.02.01); the two creation affordances also carry the surface's `meta.actions`. Nothing here names a Composable or a UiState — that binding is not part of the interaction layer (doc02.02.00).
