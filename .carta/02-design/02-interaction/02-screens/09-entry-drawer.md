---
title: Entry Drawer
summary: Affordance inventory for the EntrySheet surface — a bottom-sheet peek of one Collection Entry over MapOverview, with three pressable regions that hand off to the owning Collection, the full detail, and the Review form
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"09-entry-drawer.inventory.json","against":{"doc":"doc02.02.01","key":"EntrySheet"}}]
---

# Entry Drawer

The affordance inventory for the `EntrySheet` surface — a lightweight peek of one Collection Entry shown as a bottom sheet over `MapOverview`. **The source of truth is the carta sidecar `09-entry-drawer.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

`EntrySheet` renders as a sheet (`meta.modality: sheet`) hosted by `MapOverview` (`meta.host: MapOverview`); the sheet's state lives in the host's UiState rather than being its own route ([[04-surface-composition-rules]], doc02.04).

The surface is three stacked pressable regions, each a full-width tap target sized for an easy thumb press; there is no separate row of buttons. The **collectionRow** identifies the owning Collection (appearance color and name) and presses through to *View the Collection* (`VIEW_COLLECTION` → `CollectionDetail`). The **locationBlock** shows the Location's display name and presses through to *Peek the Location* (`TAP_LOCATION` → `LocationSheet`) — the fan-out of every Collection Entry that references this Location, plus the entry point for adding a sibling Entry at the same Location. The peek always renders, even when this is the sole Entry referencing the Location — that one-row list is the user's launching pad for *Add another Entry at this Location*. The canonical `CollectionEntryDetail` screen is reached via `CollectionDetail`'s `TAP_ENTRY`, not from this peek. The **reviewBlock** reflects the Review: its first line reads `Unreviewed` or `Reviewed {date}` (formatted from the Review instance's `created` date), and its second line shows the Review's `summaryField` ([[03-concepts]], doc01.03 §3) rendered to a single line — a `score` in its configured style, a `text` field truncated to one line, an `enum`'s picked option, a `power-ranking`'s position; the second line is omitted when the template declares no summary field or the Entry is unreviewed. Pressing the block hands off to *Edit the Review* (`TAP_EDIT_REVIEW` → `ReviewForm`, `Review.edit`).

`EntrySheet` is reached from `LocationSheet`'s *Pick an entry* (`TAP_ENTRY`) — and is the surface a single-entry pin tap effectively lands on (see [[07-map-overview]], doc02.02.02.07: when the tapped Location has exactly one Entry, `LocationSheet` renders the `EntrySheet` directly instead of the skinny peek). It is a peek, not the canonical detail screen — the full detail is one tap away through the `locationBlock`. *Dismiss* (`DISMISS` → `MapOverview`) closes the drawer and returns to the bare map; on a bottom sheet, this is the swipe-down or scrim-tap gesture on the sheet shell, not a tap on any of the three content regions.

`EntrySheet` is reachable through the add-to-collection flow: it is the surface a single-entry pin tap effectively lands on, and `LocationSheet`'s `TAP_ENTRY` propagates `collection-context` into it. The statechart declares a `CANCEL_ADD` transition (target `CollectionDetail`, guard `inAddMode`) so the user can abandon the flow from the drawer; the inventory lists `CANCEL_ADD` in `deferred` because the visual treatment of an in-add-mode drawer is yet to unfold.
