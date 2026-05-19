---
title: Concepts
summary: Concept-driven design (Jackson): Collection, Location, Review, Map Overview
tags: [product, concepts, design]
deps: [doc01.02]
---

# Concepts

RTP's design follows Jackson's concept-driven design. Each concept below is **freestanding** and has a single **purpose**, **state**, **actions**, and an **operational principle** — an archetypal scenario showing how the actions fulfill the purpose. Concepts compose by **synchronization**, not by inheritance or shared internals.

This doc is sparse on purpose. Concepts unfold (doc00.02). Add detail when the work demands it; do not pre-elaborate edge cases.

Concepts in this doc:

- §1 [Collection](#1-collection)
- §2 [Location](#2-location)
- §3 [Review](#3-review)
- §4 [Map Overview](#4-map-overview)

A **Collection Entry** is the pairing `(Location, Review)` within a Collection. It is not a concept on its own — it's the unit that emerges when Collection, Location, and Review compose.

Use cases that exercise these concepts: doc01.02.

---

## 1. Collection

**Purpose.** Let a user organize a set of Collection Entries under one shared review template, so they can build a personal record of any kind of place-bound activity (cafés ranked, restaurants visited, days journaled).

**State.**

- `name` — user-provided label.
- `entries` — set of `(Location, Review)` pairs. The same Location may appear in many Collections; each appearance is a distinct Collection Entry with its own Review.
- `created`, `last_modified` — for sorting and sync.
- `appearance` — visual identity used on the map (color, pin style). Set on creation; editable.

The Collection's review template lives in the Review concept (§3), not here. The Collection knows that *its* Collection Entries' Reviews share a template, but does not own the template's structure.

**Actions.**

- `create(name)` — start an empty Collection.
- `addEntry(location, review)` — append a Collection Entry. Same Location across Collections = multiple Collection Entries.
- `removeEntry(entry)` — drop a Collection Entry from this Collection. Does not delete the Location or its Reviews in other Collections.
- `import(source)` — ingest an external collection (KML, GeoJSON, RTP bundle, third-party share URL). Becomes a first-class Collection owned by the user.
- `export(format)` — emit the Collection as a portable bundle for backup or sharing.
- `share(audience)` — produce a shareable artifact (deferred to a later sharing concept).

**Operational principle.** A user creates a Collection "Drip Coffee," authors its Review template (via §3) to include stars and coffee-style, and `addEntry(BlueBottleMintPlaza, review)` where the Review carries the per-place data. Later they add Blue Bottle to a different Collection "Geo Diary" with a different Review template; the two Collection Entries are independent. Browsing Drip Coffee shows only the coffee-shaped Collection Entries.

**Notes.**

- The Collection is the binding ring; Locations and Reviews are the pieces it binds.
- Reusable templates (built-in coffee/restaurant/diary templates, copying a template between Collections) are a future concern for §3.

---

## 2. Location

**Purpose.** Let a user designate a real-world place once, and refer to it from anywhere in the app, regardless of which provider supplied its identity.

**State.**

- `coordinates` — `(lat, lng)`. Always present.
- `displayName` — human-readable label.
- `sourceType` — which provider asserted this Location: `google`, `osm`, `apple`, `manual`, etc.
- `sourceId` — stable id within that provider (`place_id`, OSM node id, generated UUID for `manual`).
- `cachedMetadata` — provider-supplied snapshot at import time (address, category, etc.).
- `refreshable` — whether the source can be re-queried for updated metadata.

`(sourceType, sourceId)` is the identity. Two Locations with the same `(lat, lng)` from different providers are distinct.

**Actions.**

- `resolve(query, provider)` — search a provider for a place; return a candidate Location.
- `dropPin(coordinates)` — create a `manual` Location with a generated UUID.
- `import(externalRef)` — adopt a Location from a KML placemark, GeoJSON feature, or shared URL.
- `addToCollection(collection, data)` — make this Location a Collection Entry (this is `Collection.addEntry` viewed from the Location's side; see synchronization below).
- `openExternally(target)` — hand off to an external map app (e.g. Google Maps, Apple Maps) for navigation, street view, or richer details. RTP does not reimplement those affordances; it cedes them by handoff.
- `refresh()` — if `refreshable`, re-query the provider to update `cachedMetadata`.
- `merge(other)` — user-confirmed reconciliation when two Locations from different providers refer to the same real place.

**Operational principle.** A user searches "Blue Bottle Mint Plaza" via the Google provider and resolves a Location. They add it to two Collections. Later they tap the Location and choose "Open in Google Maps"; the OS hands off to the Google Maps app, which opens at the same coordinates. Their Collection Entries in both Collections are unaffected.

**Notes.**

- "Open in Google Maps" is one instance of `openExternally`. Apple Maps, OsmAnd, etc., are equally valid targets. The user picks; RTP does not privilege one.
- A Location with no Collection memberships is allowed but normally garbage-collected. (Decide when this matters.)
- `merge` is the user's tool for "looks like the same place." Never automatic.

---

## 3. Review

**Purpose.** Let a user capture a structured evaluation of a Location within the context of a particular Collection — restaurants reviewed by stars and dish notes; coffee shops by roast style and a power-ranking position; diary entries by date and freeform text.

A Review has two faces, kept inside one concept because they share a single purpose (defining and capturing the evaluation):

1. A **template** — the shape of the form, authored once per Collection.
2. An **instance** — the filled-in data for a specific Collection Entry.

**State.**

Per Collection (template):

- `template` — an ordered list of fields. Each field has a `name`, a `type` (e.g. `stars`, `text`, `enum`, `boolean`, `date`, `power-ranking`), per-type configuration (e.g. `stars: 1–5`; `enum: [light, medium, dark]`), and a `required` flag.
- `template_version` — bumped when the template changes; used to reconcile instances.

Per Collection Entry (instance):

- `data` — a map of `field-name → value`, conforming to the template at the recorded version.
- `recorded_template_version` — the template version this instance was written against.
- `created`, `last_modified`.

**Actions.**

Template authoring (per Collection):

- `defineTemplate(collection, fields)` — initial template at Collection creation.
- `editTemplate(collection, newFields)` — modify. Existing instances reconcile (missing required fields prompt; removed fields archived but not destroyed).
- `useBuiltIn(collection, templateName)` — adopt a built-in template (Coffee Ranking, Wishlist, Geo Diary, etc.) as the starting point.

Instance:

- `start(collection, location)` — produce a draft Review for a Location in this Collection. Pre-filled with sensible defaults (today's date, Unvisited, etc.).
- `submit(review, data)` — commit the instance. Becomes the Collection Entry's Review.
- `edit(review, data)` — change values within the template.
- `clear(field)` — unset a field where "not set" is meaningful (distinct from "false" for booleans, etc.).

**Operational principle.** A user creating "Drip Coffee" authors the template: a 1–5 stars field, a `light|medium|dark` style enum, a Visited boolean, a power-ranking field, a freeform notes field. Later, sitting at a café, they `start(DripCoffee, BlueBottleMintPlaza)`. The app shows the template's form; they fill in stars=4, style=light, visited=true, notes="excellent", and `submit`. The Collection Entry now carries this Review. Browsing the Collection, they see Collection Entries shaped exactly by their template.

**Notes.**

- The interactive **schema builder** (e.g. "add a field shown as stars") is the UI mapping for `defineTemplate` / `editTemplate` — covered when the schema-builder doc is written.
- Built-in templates (Coffee Ranking, Wishlist, Geo Diary) are starting points users can adopt and customize. Built-in field types (stars, power-ranking, visited/unvisited, date) are shared affordances across all templates.
- One concept covers both template and instance for now. If sharing makes the template-author and reviewer different people, this may split into a separate **ReviewTemplate** concept — flagged but not pre-built.
- A Review exists only as part of a Collection Entry. There are no orphan Reviews.

---

## 4. Map Overview

**Purpose.** Give the user, on every app launch, an immediate spatial view of everything they care about — all Collections, all Collection Entries, with enough visual encoding to read the state of their world at a glance.

**State.**

- `viewport` — current `(center, zoom, bearing)`. Persisted across launches.
- `visiblePins` — for each Collection Entry across all Collections, a pin styled by:
  - **fill color** = the owning Collection's `appearance` color
  - **outline** = same color as fill for visited Collection Entries; pin is **gray with a colored outline** for unvisited Collection Entries (where the schema has a Visited/Unvisited concept)
- `collectionFilter` — set of Collections currently shown. Defaults to "all."
- `selectedPin` — currently focused Collection Entry, if any.

**Actions.**

- `open()` — launch action. Restore `viewport`, render `visiblePins` for all Collections in `collectionFilter`. Renders before any background data has loaded (see doc01.01 §"Cold-start playbook").
- `pan(delta)`, `zoom(delta)` — standard map navigation. Updates `viewport`.
- `selectPin(pin)` — focus a Collection Entry; reveal its Collection and per-Collection-Entry data.
- `toggleCollection(collection)` — add/remove a Collection from `collectionFilter`. Persistent.
- `jumpToCollection(collection)` — fit viewport to that Collection's Collection Entries.

**Operational principle.** A user taps the RTP icon. Within ~200ms, they see their last viewport with pins from all their Collections: orange-filled pins for "Drip Coffee" (visited), orange-outlined gray pins for cafés on their wishlist, green-filled pins for "NYT Top 100" places they've been, green-outlined gray pins for the rest of the NYT list. Without any further action, they understand the state of their world. Tapping a pin reveals which Collection it belongs to and the Collection Entry's data.

**Notes.**

- The "everything view" is the default landing surface. Entering a single Collection is a navigation away from it, not the inverse.
- Visited/Unvisited rendering only applies to Collection Entries whose schema has that field. Collection Entries without it render with the standard filled pin.
- Pin appearance is driven by Collection `appearance`, not per-Collection-Entry. Per-Collection-Entry visual encoding (e.g. star count) is reserved for the Collection Entry detail view, not the overview map.
- Color-collision handling (two Collections with similar colors) is an emergent UX problem to address when the schema-builder / appearance-picker is designed.

---

## Composition notes

These are tentative synchronization observations, not a complete composition spec.

- **Collection ↔ Review template.** A Collection has exactly one Review template. Creating a Collection includes authoring (or adopting) its template before the first Collection Entry can be added.
- **Collection.addEntry ↔ Review.start + Review.submit.** Adding a Collection Entry is staged: `Review.start` produces a draft against the Collection's template; `Review.submit` together with `Collection.addEntry` commit the Collection Entry atomically.
- **Collection ↔ Location existence coupling.** Deleting a Location triggers removal of its Collection Entries from all Collections (or prompts the user). Deleting a Collection does not delete its Locations — they may participate in other Collections.
- **Location.addToCollection ↔ Collection.addEntry.** Same underlying action, two viewpoints. The user can initiate from either side (Collection-first or Location-first; see doc01.02 §"Geo Diary"). Both paths must reach the same final state.
- **Map Overview ↔ Collection.appearance.** Changing a Collection's color updates its pins everywhere on the map.
- **Map Overview ↔ Collection.entries.** Adding/removing Collection Entries updates `visiblePins`. Live; no manual refresh.
- **Map Overview ↔ Review.** Pin styling can read Review fields when present (Visited/Unvisited drives gray-pin-with-colored-outline). Other Review data is reserved for the Collection Entry detail view.
- **Open Externally ↔ no concept dependency.** `openExternally` is a leaf action — RTP hands off to the OS and does not track what happens after. Deliberately under-coupled.
