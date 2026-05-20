---
title: Entry Drawer
summary: Affordance inventory for the EntryDrawer surface — a bottom-sheet peek of one Collection Entry over MapOverview, with handoffs to full detail, owning Collection, and review form
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"09-entry-drawer.inventory.json","against":{"doc":"doc02.02.01","key":"EntryDrawer"}}]
---

# Entry Drawer

The affordance inventory for the `EntryDrawer` surface — a lightweight peek of one Collection Entry shown as a bottom sheet over `MapOverview`, with handoffs to the full detail screen, the owning Collection, and the Review form. **The source of truth is the carta sidecar `09-entry-drawer.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

`EntryDrawer` renders as a sheet (`meta.modality: sheet`) hosted by `MapOverview` (`meta.host: MapOverview`); the sheet's state lives in the host's UiState rather than being its own route ([[04-surface-composition-rules]], doc02.04).

The surface has two regions. The **summary** region shows enough of the Entry to recognize it without leaving the map — the Location's display name, the owning Collection's name and appearance color, and a compact reflection of the Review (a short visited/unvisited indicator and the highlight field). The **actions** region holds the three navigating handoffs and the dismiss.

`EntryDrawer` is reached from `LocationDetail`'s *Pick an entry* (`TAP_ENTRY`) — and is the surface a single-entry pin tap effectively lands on (see [[07-map-overview]], doc02.02.02.07: when the tapped Location has exactly one Entry, `LocationDetail` renders the `EntryDrawer` directly instead of the skinny peek). It is a peek, not the canonical detail screen — *Open full detail* (`TAP_OPEN_DETAIL` → `CollectionEntryDetail`) is the way out for the user who wants the complete view. *View the Collection* (`VIEW_COLLECTION` → `CollectionDetail`) and *Edit the Review* (`TAP_EDIT_REVIEW` → `ReviewForm`) are direct shortcuts to the two next-most-likely actions. *Dismiss* (`DISMISS` → `MapOverview`) closes the drawer and returns to the bare map; on a bottom sheet, this is typically the swipe-down / scrim-tap gesture rather than a dedicated button.

`EntryDrawer` carries no mode parameter — the add-to-collection-mode behavior of pin taps is a separate open design ([[07-map-overview]], doc02.02.02.07) and is not yet inventoried here.
