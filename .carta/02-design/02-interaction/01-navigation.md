---
title: Navigation
summary: Platform-agnostic surface graph as XState statechart; verifies every concept action has a UI affordance
tags: [design, interaction, navigation, statechart]
deps: [doc01.03]
---

# Navigation

The platform-agnostic UI surface graph. **The source of truth is the carta sidecar `01-navigation.statechart.json`** — a flat XState v5 machine config (`createMachine(json)`-loadable). This `.md` is a lean companion; read the JSON for the real graph.

A **surface** is a UI region the user perceives as one place. Surfaces are named in concept-language; the **modality** (full screen, sheet, drawer, overlay) is declared on each state in `meta.modality`, and sheets/drawers/overlays additionally name their **host** in `meta.host`. The two rules that govern this — *same surface, different mode = mode parameter*; *overlay surface = sheet state owned by its host's UiState, not a separate route* — are stated in [[04-surface-composition-rules]] (doc02.04) and researched in [[05-cmp-composition-research]] (doc01.05).

## Surfaces

States in the JSON, keyed by `meta.surface`:

| Surface | Purpose |
|---|---|
| `MapOverview` | Everything view; default landing. Pins for every Collection Entry. |
| `LocationSheet` | One Location (tapped pin) + every Collection Entry that references it. |
| `CollectionList` | Manage the user's Collections. |
| `CollectionDetail` | One Collection's entries, map or list projection (in-place toggle). |
| `CollectionEntryDetail` | One Collection Entry: the (Location, Review) pair. |
| `ReviewForm` | Author/edit a Review instance against the template. |
| `CollectionEditor` | Author/edit a Collection — its metadata (name, description, appearance) and its Review template. |
| `LocationDraftSheet` | A dropped pin or picked search result, not yet committed — coordinates plus nearby resolution candidates, rendered as a sheet over the map. |
| `AddLocationToCollection` | Pick which Collection to add an already-chosen Location to. |
| `ImportFlow` | Import an external collection (stub). |
| `Settings` | Providers, BYOK keys, sync target (stub). |
| `LocationProvider` | Configure mapping providers — add a provider with its BYOK key, choose the default. Reached from `Settings`. |

The inventory is not closed — surfaces are added when a use case demands one. Today's set covers the journeys in [[02-use-cases]] (doc01.02): the Location-first track — drop a pin or search on `MapOverview` → `LocationDraftSheet` → add-to-collection, or tap an existing pin → `LocationSheet` — and the Collection-first track (`CollectionList` → `CollectionDetail`, whose add-entry re-enters `MapOverview` in add-to-collection mode).

## Entry modes and carried context

Most surfaces render the same way however they are reached. A few instead carry an **entry context** — a value supplied by the transition that opened them, which changes how the surface renders without making it a different state. The schema makes this explicit:

- **`meta.modes`** on a state — named modes the surface can be entered in. Each mode is a label with a one-line predicate (e.g., browse = "no carried context"; addToCollection = "`collection-context` is set"). Optional; absent means the surface has only one mode.
- **`meta.context`** on a state — the carried values it can receive. Each key maps to a one-line description of its source and consumers. Optional.
- **`propagates`** on a transition — an array of context keys the transition forwards to the target state. Lets a verifier walk the chain from origin to consumer and prove no link is missing.

`MapOverview` is the worked example. It is reached two ways: as the landing surface (`browse` mode), and from `CollectionDetail`'s `TAP_ADD_ENTRY` (`addToCollection` mode), which hands it a `collection-context`. Add-to-collection mode is the *same* state — same pins, same search, same transitions — plus a status-bar region and a carried Collection that pre-selects the target downstream in `AddLocationToCollection`. Modelling it as a second state would fork every `MapOverview` transition; instead the mode is a parameter, and a mode-only transition (`CANCEL_ADD`) is a **guarded** transition (`guard: inAddMode`) live only when the context is set. From there, `TAP_PIN` / `DROP_PIN` / `PICK_SEARCH_RESULT` each declare `propagates: ["collection-context"]`, carrying the context through `LocationDraftSheet` into `AddLocationToCollection`. The Compose projection — a nullable route argument resolved to a sealed mode type — is doc03.03; the surface's regions are doc02.02.02.07.

## Conventions

The JSON is flat: states are surfaces, transitions are navigation gestures. Concept actions performed *within* a surface live in `meta.actions`, not as transitions, because they don't move the user.

- **States**: `description`, `tags` (visual chips), `meta.surface`, `meta.modality` (`fullScreen | sheet | drawer | overlay`), `meta.host` (required when `modality != fullScreen`), `meta.hostsSheets` (optional; surfaces this state presents), `meta.modes` (optional; named entry modes), `meta.context` (optional; carried-context keys), `meta.reads`, `meta.actions`. State id = surface name in PascalCase; internal substates lowerCamelCase.
- **Transitions**: event key is UPPER_SNAKE, named for the gesture (`TAP_PIN`, `BACK`). `description` explains the gesture; optional `actions` array names `"Concept.action"` strings; optional `propagates` array names context keys forwarded to the target. `target` omitted for self-transitions.

`meta.actions` is load-bearing for coverage: `.carta/verify.mjs` cross-checks every surface's affordance inventory against its statechart state — missing events or actions are gaps, phantom ones are stale refs. The modality, host, modes, context, and propagates fields admit further mechanical consistency checks; candidate verifier kinds are listed in [[02-verification-system]] (doc01.04.02).

## Visualizing the graph

A **Luminous pipeline** renders this sidecar as a visual canvas:

```
node .luminous/statechart-canvas.pipeline.mjs
```

It walks `.carta/` for `*.statechart.json` sidecars and emits a derived Luminous canvas pair (`*.canvas.graph.json` + `*.canvas.pack.json`) under `.luminous/generated/` (gitignored) — one graph generated from the statechart, with the concept inventory ([[03-concepts]], doc01.03) feeding the action-coverage check. Edit the sidecar and re-run; never hand-edit the output.

## Status

Happy-path slice covering the Drip Coffee and Geo Diary use cases. NYT Top 100 import (`ImportFlow`) and secondary affordances remain stubs. Retiring `LocationLookup` left `Location.import` (single-Location adoption from a KML placemark or shared URL) without a surface — it is an uncovered concept action until the import flow is unfolded.
