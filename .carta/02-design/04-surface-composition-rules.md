---
title: Surface composition rules
summary: Three design rules for how surfaces compose — same-surface-different-mode, overlay-surfaces-are-not-routes, flows-are-modal
tags: [design, interaction, composition, rules]
deps: [doc02.02.01, doc01.05]
---

# Surface composition rules

Three rules govern how surfaces compose in RankThePlanet. Stated prescriptively here; researched and justified in [[05-cmp-composition-research]] (doc01.05).

## Rule 1 — Same surface, different mode

> A surface reached two or more ways with the same regions but different carried context is **one statechart state with a mode parameter**, not multiple states or multiple screens.

The mode is a sealed type carried on the route (a nullable arg resolved at the screen boundary) and surfaces in three places in the spec:

- `meta.modes` on the statechart state names the modes and their predicates.
- `meta.context` on the same state declares the carried keys.
- Per-region and per-affordance `appearsInModes` in the inventory gates what shows up.

Modes change *visibility* and *carried values*. They do not change the surface's identity or its place in the navigation graph. If two intended modes share less than roughly half of their tree, they are two surfaces, not two modes — see [[05-cmp-composition-research]] (doc01.05) §1 for the rung-3 trigger conditions.

Anti-pattern: parallel booleans (`isAddMode`, `isEditMode`, `hideFab`). Each boolean doubles the implicit state space and decouples the code from the spec. Use a sealed mode type and the compiler's exhaustiveness check.

**Worked example.** `MapOverview` has modes `browse` and `addToCollection`. The statusBar region declares `appearsInModes: ["addToCollection"]` and `reactsToContext: ["collection-context"]`. Every other region's `appearsInModes` is set by Rule 3 below. The mode-only transition `CANCEL_ADD` is guarded by `inAddMode` in the statechart. See [[07-map-overview]] (doc02.02.02.07).

## Rule 2 — Overlay surfaces are not routes

> A surface that visually overlays another surface — sheet, drawer, popover — is **state owned by the host's UiState**, not a separate route in the NavHost.

The statechart records the relationship with two fields:

- `meta.modality` on the overlay state: `sheet`, `drawer`, or `overlay`.
- `meta.host` on the overlay state: the surface it renders over.
- `meta.hostsSheets` on the host state: the inverse, for discoverability and verifier coverage.

Why a route is wrong:

- Bottom sheets are not first-class destinations in `navigation-compose` multiplatform; modelling one as a route forces the host to unmount, losing the user's context (the map's camera, any in-memory selection) and breaking the visual continuity that makes the overlay feel like a peek.
- Carried context (mode, draft Location, etc.) survives naturally when it lives in the host's UiState. Pushing it through a nav arg per layer is fragile — when one route forgets to forward the arg, the chain breaks silently.

CMP projection: the host screen owns a sealed `…Sheet` type on its UiState (or several, one per modality slot). Rendering uses `ModalBottomSheet` / `ModalNavigationDrawer` / similar; dismissal mutates the UiState back to `None`. The pattern is canonical for `PinSheet` on `MapOverviewUiState`; new sheets follow the same shape.

**Worked example.** `LocationDraft` is `modality: sheet`, `host: MapOverview`. The CMP projection adds a `draft: LocationDraftSheet` entry to `MapOverviewUiState` (alongside `pinSheet`); the sheet reads `collection-context` from the same UiState and propagates it when the user picks "Add to a Collection." There is no `composable<LocationDraft>` in the NavHost.

## Rule 3 — Flows are modal: no nesting

> While the user is in a multi-step flow, the host suppresses every transition that would start a second flow. The flow surface offers exactly two ways out: **complete** or **cancel**.

A *flow* is a sequence of surfaces driven by carried context to a single committing terminal — the user is "in the middle of something" until they reach the terminal or back out. Examples: the add-to-collection flow (`CollectionDetail` → `MapOverview` (add-mode) → `LocationDraft` → `AddLocationToCollection` → `ReviewForm` → committed). The collection-editor and review-form flows are similar.

While a flow is active, every surface it traverses:

- **Suppresses** any affordance that would start a competing flow. Inventories encode this with `appearsInModes` — affordances live in the browse mode only.
- **Offers complete** — the terminal transition that commits the flow's work (e.g., `PICK_COLLECTION` then `SUBMIT`).
- **Offers cancel** — a single explicit affordance (typically an X icon in the navigationIcon slot of a top bar) whose transition is `CANCEL_*` with a `guard: inFlowMode`, returning to the flow's originating surface and dropping the carried context.

A dismiss-gesture on a sheet (swipe-down, scrim-tap) is **not** cancel — it only dismisses the sheet, returning the user to the host, which is still in the flow. Sheets in a flow therefore offer both a dismiss-gesture (go back one step) and an explicit cancel affordance (kill the whole flow).

Why this rule:

- Flows carry state in nav arguments and UiState. Letting the user start a second flow mid-stream forces the system to either model nested flows (a combinatorial explosion of mode states) or drop the first flow silently (data loss the user did not consent to).
- The user's mental model is single-tasking. "I am adding a place to a Collection" is one intent; offering a "go to Settings" button mid-flow violates the intent.
- Explicit cancel is a contract: the user knows how to exit, and the system knows when to drop carried context.

**Worked example.** In `MapOverview`'s `addToCollection` mode:

- The `menuButton`, `menuDrawer`, and `bottomBar` regions (and every affordance inside them) declare `appearsInModes: ["browse"]` — suppressed.
- A new `closeButton` region (X icon, top-left navigationIcon slot) declares `appearsInModes: ["addToCollection"]` and hosts the `CANCEL_ADD` affordance.
- The `statusBar` region declares `appearsInModes: ["addToCollection"]` and occupies the bottomBar slot, naming the target Collection.
- The `search` and `map` regions remain in both modes — search and pan-zoom are part of *completing* the flow (finding the place to add), not starting a second flow.
- Sheets reached during the flow (`LocationDraft`, `LocationDetail`, `EntryDrawer`) carry the same `CANCEL_ADD` transition guarded `inAddMode`, so the user can cancel from any sheet.

## Consequences for the rest of the stack

- **Route definitions** only exist for full-screen surfaces (`modality: fullScreen`). Sheets, drawers, and overlays do not.
- **ViewModels** are scoped to full-screen surfaces. Sheets read from and event into their host's ViewModel; a sheet that grows ViewModel-shaped responsibilities is the rung-3 signal to reconsider whether it should be a full screen.
- **System back** on Android in a host with an open sheet dismisses the sheet (not the host). Wire a `BackHandler` per sheet to intercept.
- **Verification.** Candidate verifier kinds in [[02-verification-system]] (doc01.04.02) cover modality/host consistency, context propagation, and per-mode coverage; a `flow-coverage` kind could additionally enforce that every flow-traversed surface declares a `CANCEL_*` transition with the matching guard.

## When these rules don't apply

- A surface that visually overlays another but is reached from outside the host's flow (e.g., a system-level confirmation prompt that can appear anywhere). These remain platform-native or app-global, not modeled as host state.
- A "mode" that genuinely changes the navigation graph — different affordances *and* different outgoing transitions *and* different concept actions. If the `when` over modes spans every region, you have two surfaces, not one with a mode.
- A surface whose work is single-step and atomic (a tap that commits immediately). It is not a flow; Rule 3 does not apply.

When in doubt, default to the rule. The cost of demoting a route to sheet state or tightening a flow's chrome later is small; the cost of promoting sheet state to a route or loosening a flow once code depends on the looser shape is large.
