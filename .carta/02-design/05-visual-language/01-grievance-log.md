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

EntrySheet (doc02.02.02.09):

- **G001** — EntrySheet, 2026-05-23: collection chip ("NYT Top 100"), entry name ("Per Sé"), and review status row are rendered with equal typographic weight and uniform dividers between them, so the sheet reads as a list of peers rather than as parent context / subject / status. [candidate cluster: information-hierarchy]
- **G002** — EntrySheet, 2026-05-23: the sheet's background color stops short of the bottom safe area, leaving a darker strip below; the container does not extend through window insets. [candidate cluster: surface-intentionality]
- **G003** — EntrySheet, 2026-05-23: every row in the sheet has the same hairline divider, which signals peer relationships the rows don't actually have. [candidate cluster: information-hierarchy]
- **G004** — EntrySheet, 2026-05-23: "Per Sé" — the subject of the sheet — does not sit at display weight relative to the surrounding rows; spacing around it is the same as around the chip and the status row. [candidate cluster: information-hierarchy]
- **G005** — EntrySheet, 2026-05-23: the row reads as a passive status string sitting in a slot users expect to be interactive. There is no signifier for *edit review*, *write review*, or *re-visit*. [candidate cluster: affordance-vs-status; **partly resolved 2026-05-23 — see Resolved/withdrawn**]
- **G007** — EntrySheet, 2026-05-23: a `debug: EntryDrawerSheet` label is rendered in the sheet. Build-config leak, not a visual-language issue — recorded here only so it does not get conflated with a real grievance.

MapOverview (doc02.02.02.07):

- **G008** — MapOverview, 2026-05-23: the Search pill floats over the map without an anchor system — no shared inset from the safe area, no consistent corner radius / elevation with any other map overlay (none yet exist, but the rule needs to be set before a second overlay arrives). [candidate cluster: overlay-placement]
- **G009** — MapOverview, 2026-05-23: the scrim applied behind the EntrySheet darkens the map heavily, breaking the glanceability that justifies a peek sheet. A peek-sized sheet should leave its host readable. [candidate cluster: overlay-placement; possibly its own rule about peek-vs-modal scrim]

App-wide:

- **G010** — App-wide, 2026-05-23: the Material 3 default seed color resolves to a brown/olive that competes with the map (the product's primary content). Chrome should defer to the map; the seed needs to be picked deliberately, not inherited. [candidate cluster: surface-intentionality; related rule candidate: *chrome defers to map*]

## Clusters being watched

Names are provisional; promote to a principle doc when the cluster has ≥3 grievances and the name stabilizes.

- **information-hierarchy** — G001, G003, G004. Three grievances against the same surface; close to graduating. Likely first principle doc.
- **surface-intentionality** — G002, G010. Two grievances. Needs a third before graduating. Candidate framing: *no Material default survives past prototype.*
- **affordance-vs-status** — G005. The vocabulary half (visited vs reviewed) is settled in [[03-concepts]] (doc01.03); what remains is the visual question of whether a status string in an interactive slot needs an explicit edit signifier (pencil, chevron) or whether the whole-row tap target is enough. Watch for a second grievance before promoting.
- **overlay-placement** — G008, G009. Two grievances. Watch for a third when a second map overlay is introduced (FAB, attribution, location pill).

## Resolved / withdrawn

- **G006** — EntrySheet, 2026-05-23: "Visited" and "Reviewed" used inconsistently across the app. **Resolved 2026-05-23**: the concept settled on **reviewed / unreviewed** as the single vocabulary for the derived state ([[03-concepts]], doc01.03 §3, §4). Use cases ([[02-use-cases]], doc01.02), screen inventories, the store model ([[01-store-model]], doc03.01), and the EntrySheet / LocationSheet / MapOverview code paths were updated in the same change.
- **G005** (vocabulary portion) — same change: the EntrySheet `reviewBlock` now reads `Reviewed {date}` or `Unreviewed`, never `Visited`. The unresolved portion (whether the row needs an explicit edit signifier) stays open above.
