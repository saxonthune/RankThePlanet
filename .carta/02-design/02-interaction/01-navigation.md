---
title: Navigation
summary: Platform-agnostic surface graph as XState statechart; verifies every concept action has a UI affordance
tags: [design, interaction, navigation, statechart]
deps: [doc01.03]
verify: [{"kind":"context-chain","sidecar":"01-navigation.statechart.json"},{"kind":"guard-coverage","sidecar":"01-navigation.statechart.json"},{"kind":"modality-host","sidecar":"01-navigation.statechart.json"},{"kind":"generated-traces","sidecar":"01-navigation.statechart.json","traces":200,"length":20,"seed":1}]
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
- **`meta.context.owns`** on a state — keys whose lifetime this state controls. Each key declares `set: [<State>.<EVENT>, …]` (the transitions that establish it) and a description.
- **`meta.context.receives`** on a state — keys forwarded into this state by an inbound transition's `propagates`. Lifetime managed at the owner.
- **`propagates`** on a transition — context keys forwarded to the target state.
- **`clears` / `retains`** on a transition — per owned key of the source state's `owns`, whether the transition drops or preserves the key. Verified by the `context-chain` check ([[02-verification-system]], doc01.04.02): every outbound transition on an owner state must classify every owned key, exactly once.

`MapOverview` is the worked example. It owns `collection-context`, `location-context`, `pending-review-context`, `search-context`, and `filter-context`, and hosts five sheets; its outbound transitions classify all five. The `browse` / `addToCollection` mode split is driven by `collection-context`: a mode-only transition (`CANCEL_ADD`) is a **guarded** transition (`guard: "has(collection-context)"`) and clears every owned key on its way back to `CollectionDetail`. Guards are typed predicates in the grammar described in [[02-verification-system]] (doc01.04.02) and checked by the `guard-coverage` verifier against each event's inventory affordance. Sheet trips (`TAP_PIN`, `DROP_PIN`, `PICK_SEARCH_RESULT`, …) retain the sheet-stable keys and only drop the transient `pending-review-context`. The Compose projection — a nullable route argument resolved to a sealed mode type — is doc03.03; the surface's regions are doc02.02.02.07.

## Conventions

The JSON is flat: states are surfaces, transitions are navigation gestures. Concept actions performed *within* a surface live in `meta.actions`, not as transitions, because they don't move the user.

- **States**: `description`, `tags` (visual chips), `meta.surface`, `meta.modality` (`fullScreen | sheet | drawer | overlay`), `meta.host` (required when `modality != fullScreen`), `meta.hostsSheets` (optional; surfaces this state presents), `meta.modes` (optional; named entry modes), `meta.context.owns` / `meta.context.receives` (optional; see *Entry modes and carried context* above), `meta.reads`, `meta.actions`. State id = surface name in PascalCase; internal substates lowerCamelCase.
- **Transitions**: event key is UPPER_SNAKE, named for the gesture (`TAP_PIN`, `BACK`). `description` explains the gesture; optional `actions` array names `"Concept.action"` strings; optional `propagates` array names context keys forwarded to the target; optional `clears` / `retains` arrays declare lifetime per owned key of the source state; optional `guard` is a predicate string in the grammar of [[02-verification-system]] (doc01.04.02) — checked by `guard-coverage` against the inventory affordance for the same event. `target` omitted for self-transitions. **No `label` field** — the gesture's human label is sourced from the matching affordance in the per-surface inventory ([[00-index]], doc02.02.02.00); the statechart-canvas pipeline merges affordance labels into transitions when it emits the derived canvas. The fallback is only used for transitions no inventory affordance covers — system-driven events (e.g., the bare-map back gesture) or stubs (e.g., `ImportFlow.DONE`) — and a transition without a covering affordance and without a fallback label simply has no label drawn.

`meta.actions` is load-bearing for coverage: `.carta/verify.mjs` cross-checks every surface's affordance inventory against its statechart state — missing events or actions are gaps, phantom ones are stale refs. The `context-chain` verifier additionally requires every outbound transition on an owner state to classify every owned key as `clears` or `retains`. Further mechanical consistency checks are listed in [[02-verification-system]] (doc01.04.02).

## Visualizing the graph

A **Luminous pipeline** renders this sidecar as a visual canvas:

```
node .luminous/statechart-canvas.pipeline.mjs
```

It walks `.carta/` for `*.statechart.json` sidecars and emits a derived Luminous canvas pair (`*.canvas.graph.json` + `*.canvas.pack.json`) under `.luminous/generated/` (gitignored) — one graph generated from the statechart, with the concept inventory ([[03-concepts]], doc01.03) feeding the action-coverage check. Edit the sidecar and re-run; never hand-edit the output.

## Status

Happy-path slice covering the Drip Coffee and Geo Diary use cases. NYT Top 100 import (`ImportFlow`) and secondary affordances remain stubs. Retiring `LocationLookup` left `Location.import` (single-Location adoption from a KML placemark or shared URL) without a surface — it is an uncovered concept action until the import flow is unfolded.
