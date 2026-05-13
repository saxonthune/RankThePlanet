---
title: Navigation
summary: Platform-agnostic surface graph as XState statechart; verifies every concept action has a UI affordance
tags: [design, interaction, navigation, statechart]
deps: [doc01.03]
---

# Navigation

The platform-agnostic UI surface graph. Source of truth is the carta sidecar `01-navigation.statechart.json` — an XState v5 machine config. This `.md` is the human-readable companion.

## Surface inventory

A **surface** is a UI region the user perceives as one place. It may render as a full screen, bottom sheet, sidebar, popover — that's a downstream choice. Each surface is described in concept-language only.

| Surface | Purpose | Opened with | Reads (concept state) | Invokes (concept actions) |
|---|---|---|---|---|
| `MapOverview` | Everything view; default landing | — | all Collections + entries | toggleCollection, selectPin, jumpToCollection |
| `CollectionList` | Manage the user's Collections | — | Collections | Collection.create, Collection.import |
| `CollectionDetail` | One Collection's entries (map + list projections) | `collection_id` | Collection.entries, appearance | Collection.addEntry, Collection.removeEntry, Collection.share, Collection.export, Review.editTemplate |
| `EntryDetail` | One entry: full Location + Review | `entry_id` | Location, Review | Review.edit, Location.openExternally, Collection.removeEntry |
| `ReviewForm` | Author/edit a Review instance against a template | `(collection_id, location_id)` | template, draft Review | Review.start, Review.edit, Review.submit, Review.clear |
| `SchemaBuilder` | Author/edit a Collection's Review template | `collection_id` | template | Review.defineTemplate, Review.editTemplate, Review.useBuiltIn |
| `LocationPicker` | Resolve / drop / import a Location | `(mode, collection_id?)` | provider results | Location.resolve, Location.dropPin, Location.import |
| `AddToCollection` | Pick which Collection to add a Location to | `location_id` | Collections list | Collection.addEntry (forwards) |
| `Settings` | Providers, BYOK keys, sync target | — | settings | (settings actions, deferred) |

The inventory is not closed — surfaces are added when a use case demands one. Today's set covers the three journeys in [[02-use-cases]] (doc01.02).

## Structure: parallel `nav` + `overlay`

The machine is a **parallel statechart** with two regions. The user's experience at any moment is the product of the active `nav` state and the active `overlay` state.

- **`nav`** — which primary screen the user is on. Initial: `MapOverview`. Members: `MapOverview`, `CollectionList`, `CollectionDetail` (with substates `mapProjection` / `listProjection`), `EntryDetail`, `Settings`.
- **`overlay`** — which sheet/modal is currently on top. Initial: `none`. Members: `none`, `LocationPicker`, `AddToCollection`, `ReviewForm` (with substates `editing` / `submitting`), `SchemaBuilder`, `ImportFlow`.

Why this shape:

- A modal opens *over* the current screen and closes back to it. Modeling that as a separate region matches the user's mental model: "I'm on Collection Detail with the Schema Builder open" is two facts, not one.
- The auto-layout algorithm produces two cleanly nested boxes side-by-side instead of a tangle of crossings.
- Concept actions that don't change screen (`Collection.export`, `Location.openExternally`) are now obvious self-transitions; visually they don't compete with real navigation.

Most transitions stay inside one region. Cross-region transitions use **absolute paths** like `#rtp-navigation.overlay.AddToCollection`. A few transitions target both regions at once (e.g. `ReviewForm.submitting.SUBMITTED` simultaneously closes the overlay and navigates the underlying nav to `EntryDetail`) — written as an array of paths.

## Statechart conventions

Loadable as a valid XState v5 machine config via `createMachine(json)`. Conventions are chosen so the Stately VS Code extension surfaces them in its inspector when you click a node.

**On every state:**

- `description` — human-readable summary. Shows in the inspector. Markdown supported.
- `tags` — short string array used as visual chips on the state (e.g. `["landing", "map"]`, `["form"]`, `["picker"]`, `["modal"]`, `["rest"]`).
- `meta.surface` — surface name from the inventory (matches the state id, but lets tooling key off `meta` without parsing ids). Sub-projection states (`mapProjection`, `editing`) omit `meta.surface` since they're internal.
- `meta.reads` — concept state slices the surface displays.
- State id is the surface name in PascalCase; internal substates use lowerCamelCase.

**On every transition:**

- `description` — what the user gesture means. Shows in the inspector when the transition is selected.
- `actions` — array of `"Concept.action"` strings naming the concept actions this transition invokes. Matches the headings in [[03-concepts]] (doc01.03). Example: `["Review.edit"]`. Pure navigation transitions (no concept action) omit this field.
- Event name (the key in `on:`) is UPPER_SNAKE, named for the user gesture (`TAP_PIN`, `BACK`, `SUBMIT`).
- `target` is omitted for self-transitions where the surface doesn't change (e.g. `MapOverview.toggleCollection`).
- Cross-region targets use absolute paths: `#rtp-navigation.overlay.ReviewForm`. Multi-region transitions pass an array of paths.

The `actions` field is the load-bearing one for coverage: a future `coverage.mjs` walks every transition's `actions[]`, collects the strings matching `^[A-Z][a-z]+\.[a-zA-Z]+$`, and diffs against doc01.03's concept action lists. Orphan actions = gulf of execution; phantom tags = typo or stale concept.

## The loop

The carta sidecar is the source of truth. The Stately VS Code extension wants inline `createMachine(...)` literals, so a small build step generates a TypeScript file from the sidecar.

```
.carta/02-design/02-interaction/01-navigation.statechart.json   ← AI edits this
tools/statechart/build.mjs                                       ← codegen
tools/statechart/generated/navigation.machine.ts                 ← committed; what VS Code reads
```

To view the graph:

```
cd tools/statechart && npm install && npm run build
# Then open tools/statechart/generated/navigation.machine.ts in VS Code with the
# Stately extension installed; click the "Open Visual Editor" code lens.
```

Or drag `01-navigation.statechart.json` onto stately.ai for a no-install render.

## Verification

The build step calls `createMachine(json)` to validate the sidecar parses. A future `coverage.mjs` will diff the set of `(meta.concept, meta.action)` tags against the action lists in doc01.03 and report:

- **Orphan actions** — concept actions with no UI affordance (gulf of execution).
- **Phantom tags** — UI references to non-existent concept actions (typo or stale concept).

That diff is the load-bearing verification artifact for this layer.

## Status

The sidecar covers a happy-path slice — enough surfaces and transitions to support the Drip Coffee and Geo Diary use cases. NYT Top 100 import flow, Sharing surface, and many secondary affordances are stubs to be filled in. Coverage will be incomplete until those land — that incompleteness is what the script is for.
