---
title: Grievance log
summary: Raw observations of what is off in current RTP screens — the source material for visual-language principles
tags: [design, visual-language, grievances, log]
deps: [doc02.05]
---

# Grievance log

Raw, voice-of-the-builder observations of what looks or feels off in current RTP screens. The point of this file is to capture *what is wrong* before having the vocabulary for *why* — so that clusters can be read out later and named in the canon's idiom. See [[00-index]] (doc02.05) for the methodology and the sources principles draw from.

## Format

Each entry is one bullet of the form:

```
- **G{NNN}** — {surface}, {YYYY-MM-DD}: {observation in plain voice}. [candidate cluster: {name or ?}]
```

Conventions:

- IDs are monotonically increasing across the whole log; never reuse.
- "Candidate cluster" is a hint, not a commitment. `?` means unclustered.
- Observations are written *in intent voice* — describe what is wrong with the screen as it stands, not when it was built or what it replaces.
- A grievance whose fix turns out to live in a concept doc or a screen inventory still stays logged here, with a note linking the doc where the fix landed.

## Open grievances

MapOverview (doc02.02.02.07):

- **G008** — MapOverview, 2026-05-23: the Search pill floats over the map without an anchor system — no shared inset from the safe area, no consistent corner radius / elevation with any other map overlay (none yet exist, but the rule needs to be set before a second overlay arrives). [candidate cluster: overlay-placement]
- **G009** — MapOverview, 2026-05-23: the scrim applied behind the EntrySheet darkens the map heavily, breaking the glanceability that justifies a peek sheet. A peek-sized sheet should leave its host readable. [candidate cluster: overlay-placement; possibly its own rule about peek-vs-modal scrim]

CollectionDetail (doc02.02.02.02):

- **G011** — CollectionDetail, 2026-05-26: the sort row is a strip of filter chips (*Near Me*, *Date Added*, *Review Time*, *Score*) that overflows horizontally — *Score* rotates to a vertical stack because it does not fit, and a fourth option (*Power Rank*, when `Collection.powerRanking` is on) makes it worse. The row also conflates two distinct things into one control: *Near Me* is a predicate (filter), the others are orderings (sort). Idiomatic fix: a single *Sort: {key} {arrow}* chip that opens a menu, with the option set derived from Collection state, and the direction arrow as a separate tap target ([[02-collection-detail]], doc02.02.02.02). [candidate cluster: control-overflow; possible future principle *sort is a menu, not a strip*]

App-wide:

- **G010** — App-wide, 2026-05-23: the Material 3 default seed color resolves to a brown/olive that competes with the map (the product's primary content). Chrome should defer to the map; the seed needs to be picked deliberately, not inherited. [candidate cluster: surface-intentionality; related rule candidate: *chrome defers to map*]

## Clusters being watched

Names are provisional; promote to a principle doc when the cluster has ≥3 grievances and the name stabilizes.

- **surface-intentionality** — G010 (G002 resolved). One grievance left. Needs more before graduating. Candidate framing: *no Material default survives past prototype.*
- **overlay-placement** — G008, G009. Two grievances. Watch for a third when a second map overlay is introduced (FAB, attribution, location pill).
- **information-hierarchy** — G001, G003, G004 all resolved by the EntrySheet rework. The cluster is **graduation-ready**: it has the three concrete supporting grievances, the fix has been validated in code, and the vocabulary is stable (*parent context → subject → tertiary action*, expressed by type weight and spacing, not by dividers between heterogeneous items). Promote to a principle doc when the next surface needs it.

## Resolved / withdrawn

- **G001** — EntrySheet, 2026-05-23: collection chip / entry name / review status rendered with equal weight and uniform dividers. **Resolved 2026-05-24**: rebuilt `EntryDrawerSheet` with a small collection breadcrumb, a display-weight Location title, and a tertiary review row — three regions of deliberately unequal visual weight expressing *parent context → subject → tertiary action* (see [[09-entry-drawer]], doc02.02.02.09; principle still to graduate from the `information-hierarchy` cluster).
- **G002** — EntrySheet, 2026-05-23: sheet background stops short of the bottom safe area. **Resolved 2026-05-24**: `PinSheetHost` sets `contentWindowInsets = { WindowInsets(0) }` on the `ModalBottomSheet`, so the sheet's `containerColor` extends through the system gesture inset; sheet content (`EntryDrawerSheet`, `LocationDetailPeek`) applies its own `navigationBarsPadding()` to clear the inset for its content.
- **G003** — EntrySheet, 2026-05-23: hairline dividers between heterogeneous rows. **Resolved 2026-05-24**: all three `HorizontalDivider`s removed from `EntryDrawerSheet`; hierarchy is expressed by spacing and type weight instead.
- **G004** — EntrySheet, 2026-05-23: "Per Sé" not at display weight. **Resolved 2026-05-24**: Location title now renders at Material 3 `headlineMedium` with generous vertical padding, visually dominating the sheet.
- **G005** — EntrySheet, 2026-05-23: status row missing an interactive signifier. **Resolved 2026-05-24** (in addition to the vocabulary portion below): `reviewBlock` now carries a trailing outlined edit (pencil) icon in `onSurfaceVariant`; the whole row remains the tap target.
- **G006** — EntrySheet, 2026-05-23: "Visited" and "Reviewed" used inconsistently across the app. **Resolved 2026-05-23**: the concept settled on **reviewed / unreviewed** as the single vocabulary for the derived state ([[03-concepts]], doc01.03 §3, §4). Use cases ([[02-use-cases]], doc01.02), screen inventories, the store model ([[01-store-model]], doc03.01), and the EntrySheet / LocationSheet / MapOverview code paths were updated in the same change.
- **G007** — EntrySheet, 2026-05-23: `debug: EntryDrawerSheet` label leaked into the sheet. **Resolved 2026-05-24**: `DebugSheetLabel` call removed from `EntryDrawerSheet`. (`LocationDetailPeek` still renders its own debug label; not yet grieved against.)
