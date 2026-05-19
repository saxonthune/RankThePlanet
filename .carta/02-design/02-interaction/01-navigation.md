---
title: Navigation
summary: Platform-agnostic surface graph as XState statechart; verifies every concept action has a UI affordance
tags: [design, interaction, navigation, statechart]
deps: [doc01.03]
---

# Navigation

The platform-agnostic UI surface graph. **The source of truth is the carta sidecar `01-navigation.statechart.json`** — a flat XState v5 machine config (`createMachine(json)`-loadable). This `.md` is a lean companion; read the JSON for the real graph.

A **surface** is a UI region the user perceives as one place. Whether it renders as a full screen, bottom sheet, sidebar, or popover is a downstream choice — surfaces are named in concept-language only.

## Surfaces

States in the JSON, keyed by `meta.surface`:

| Surface | Purpose |
|---|---|
| `MapOverview` | Everything view; default landing. Pins for every Collection Entry. |
| `LocationDetail` | One Location (tapped pin) + every Collection Entry that references it. |
| `CollectionList` | Manage the user's Collections. |
| `CollectionDetail` | One Collection's entries, map or list projection (in-place toggle). |
| `CollectionEntryDetail` | One Collection Entry: the (Location, Review) pair. |
| `ReviewForm` | Author/edit a Review instance against the template. |
| `SchemaBuilder` | Author/edit a Collection's Review template. |
| `LocationLookup` | Resolve/drop/import a Location — a map-projection surface with lookup chrome. |
| `AddLocationToCollection` | Pick which Collection to add an already-chosen Location to. |
| `ImportFlow` | Import an external collection (stub). |
| `Settings` | Providers, BYOK keys, sync target (stub). |
| `LocationProvider` | Configure mapping providers — add a provider with its BYOK key, choose the default. Reached from `Settings`. |

The inventory is not closed — surfaces are added when a use case demands one. Today's set covers the journeys in [[02-use-cases]] (doc01.02): the Location-first track (pin → `LocationDetail` → add-to-collection or open an entry) and the Collection-first track (`CollectionList` → `CollectionDetail` → `LocationLookup`).

## Conventions

The JSON is flat: states are surfaces, transitions are navigation gestures. Concept actions performed *within* a surface live in `meta.actions`, not as transitions, because they don't move the user.

- **States**: `description`, `tags` (visual chips), `meta.surface`, `meta.reads`, `meta.actions`. State id = surface name in PascalCase; internal substates lowerCamelCase.
- **Transitions**: event key is UPPER_SNAKE, named for the gesture (`TAP_PIN`, `BACK`). `description` explains the gesture; optional `actions` array names `"Concept.action"` strings. `target` omitted for self-transitions.

`meta.actions` is load-bearing for coverage: `.carta/verify.mjs` cross-checks every surface's affordance inventory against its statechart state — missing events or actions are gaps, phantom ones are stale refs. A future verifier kind will also diff `Concept.action` strings against doc01.03's concept action lists to detect orphan actions (gulf of execution) and phantom tags (stale concept).

## Visualizing the graph

A **Luminous pipeline** renders this sidecar as a visual canvas:

```
node .luminous/statechart-canvas.pipeline.mjs
```

It walks `.carta/` for `*.statechart.json` sidecars and emits a derived Luminous canvas pair (`*.canvas.graph.json` + `*.canvas.pack.json`) under `.luminous/generated/` (gitignored) — one graph generated from the statechart, with the concept inventory ([[03-concepts]], doc01.03) feeding the action-coverage check. Edit the sidecar and re-run; never hand-edit the output.

## Status

Happy-path slice covering the Drip Coffee and Geo Diary use cases. NYT Top 100 import (`ImportFlow`), `Settings`, and secondary affordances remain stubs.
