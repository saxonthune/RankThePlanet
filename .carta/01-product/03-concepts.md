---
title: Concepts
summary: Concept-driven design (Jackson): Collection, Location, Review, Map Overview, Location Provider
tags: [product, concepts, design]
deps: [doc01.02]
verify: [{"kind":"action-concept","sidecar":"03-concepts.json"}]
---

# Concepts

RTP's design follows Jackson's concept-driven design. Each concept below is **freestanding** and has a single **purpose**, **state**, **actions**, and an **operational principle** — an archetypal scenario showing how the actions fulfill the purpose. Concepts compose by **synchronization**, not by inheritance or shared internals.

This doc is sparse on purpose. Concepts unfold (doc00.02). Add detail when the work demands it; do not pre-elaborate edge cases.

Concepts in this doc:

- §1 [Collection](#1-collection)
- §2 [Location](#2-location)
- §3 [Review](#3-review)
- §4 [Map Overview](#4-map-overview)
- §5 [Location Provider](#5-location-provider)

A **Collection Entry** is a Location placed within a Collection, optionally paired with a Review. It is not a concept on its own — it's the unit that emerges when Collection, Location, and Review compose. The Review is absent until the user submits one (§3).

Use cases that exercise these concepts: doc01.02.

---

## 1. Collection

**Purpose.** Let a user organize a set of Collection Entries under one shared review template, so they can build a personal record of any kind of place-bound activity (cafés ranked, restaurants visited, days journaled).

**State.**

- `name` — user-provided label.
- `description` — optional longer text describing what the Collection is for.
- `entries` — set of `(Location, Review)` pairs. The same Location may appear in many Collections; each appearance is a distinct Collection Entry with its own Review.
- `created`, `last_modified` — for sorting and sync.
- `appearance` — visual identity used on the map (color, pin style).
- `powerRanking` — whether the Collection's entries are ordered by the user. When on, the Collection's entries carry a user-controlled rank position; when off, no ranking is captured. Toggled on the Collection itself, not as a Review field.

The Collection's review template lives in the Review concept (§3), not here. The Collection knows that *its* Collection Entries' Reviews share a template, but does not own the template's structure.

**Actions.**

- `create(name)` — start an empty Collection.
- `edit(metadata)` — change the Collection's `name`, `description`, or `appearance`.
- `addEntry(location, review)` — append a Collection Entry. Same Location across Collections = multiple Collection Entries.
- `removeEntry(entry)` — drop a Collection Entry from this Collection. Does not delete the Location or its Reviews in other Collections.
- `import(source)` — ingest an external collection (KML, GeoJSON, RTP bundle, third-party share URL). Becomes a first-class Collection owned by the user.
- `export(format)` — emit the Collection as a portable bundle for backup or sharing.
- `share(audience)` — produce a shareable artifact (deferred to a later sharing concept).

**Operational principle.** A user creates a Collection "Drip Coffee," authors its Review template (via §3) to include a score and coffee-style, and `addEntry(BlueBottleMintPlaza, review)` where the Review carries the per-place data. Later they add Blue Bottle to a different Collection "Geo Diary" with a different Review template; the two Collection Entries are independent. Browsing Drip Coffee shows only the coffee-shaped Collection Entries.

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

- `resolve(query, provider)` — forward search: query a provider with text; return candidate Locations.
- `resolveNearby(coordinates, provider)` — reverse search: query a provider for places near a coordinate; return ranked candidate Locations. Used when the user has a point (a dropped pin) and wants to know what real places sit there.
- `dropPin(coordinates)` — create a `manual` Location with a generated UUID.
- `addToCollection(collection, data)` — make this Location a Collection Entry (this is `Collection.addEntry` viewed from the Location's side; see synchronization below).
- `openExternally(target)` — hand off to an external map app (e.g. Google Maps, Apple Maps) for navigation, street view, or richer details. RTP does not reimplement those affordances; it cedes them by handoff.
- `refresh()` — if `refreshable`, re-query the provider to update `cachedMetadata`.

**Operational principle.** A user searches "Blue Bottle Mint Plaza" via the Google provider and resolves a Location. They add it to two Collections. Later they tap the Location and choose "Open in Google Maps"; the OS hands off to the Google Maps app, which opens at the same coordinates. Their Collection Entries in both Collections are unaffected.

**Notes.**

- "Open in Google Maps" is one instance of `openExternally`. Apple Maps, OsmAnd, etc., are equally valid targets. The user picks; RTP does not privilege one.
- A Location with no Collection memberships is allowed but normally garbage-collected. (Decide when this matters.)
- A dropped pin need not be resolved. Keeping it coordinates-only yields a `manual` Location — a first-class outcome, not a degraded one. `resolveNearby` only *offers* provider candidates; adopting one is the user's choice. The uncommitted candidate before that choice is interaction-layer draft state (the `candidate-location` a surface holds), not a concept state.
- Identity is a single `(sourceType, sourceId)` pair. A multi-identity model — one Location carrying several provider identities — would not disturb Collection Entries, since an entry references a Location by surrogate id, not by identity. Adopting it would move identity (with its `cachedMetadata` and `refreshable`) into a child record per provider. Flagged, not pre-built.

---

## 3. Review

**Purpose.** Let a user capture a structured evaluation of a Location within the context of a particular Collection — restaurants reviewed by stars and dish notes; coffee shops by roast style and a power-ranking position; diary entries by date and freeform text.

A Review has two faces, kept inside one concept because they share a single purpose (defining and capturing the evaluation):

1. A **template** — the shape of the form, authored once per Collection.
2. An **instance** — the filled-in data for a specific Collection Entry.

**State.**

Per Collection (template):

- `template` — an ordered list of fields. Each field has a `name` (stable machine key, derived from `label` at create time and immutable thereafter), a `label` (user-facing string the form shows — the editor surfaces this as the field's **section**), a `type`, and per-type `config`. `name` is what's stored as the key in an instance's `data` map; `label` is presentation only and may be renamed freely. Fields are never required — `required` is not part of the template; a sparse Review is always valid (see Operational principle).
- `template_version` — bumped when the template changes; used to reconcile instances.
- `summaryField` — optional name of one field in `template`. The field whose value stands in for the Review in compact surfaces (notably `EntrySheet`, doc02.02.02.09). Any field type is eligible: a `score` renders in its configured style (stars, number, icon), a `text` field shows a truncated first line, an `enum` shows the picked option. When absent, compact surfaces show only the reviewed/unreviewed state and the instance's `created` date.

Every template carries an implicit `date` field at the head of the form, populated with today's date when the Review is started. The user cannot remove it, reorder it, or change its label or type from the Collection editor; the editor renders it as a locked row above the user-authored fields.

The field types and their per-type `config` (stored as JSON on the field, parsed at the data boundary into a typed `TemplateFieldConfig`):

- `score` — a number within a user-chosen range. Config: `{"min": 0, "max": 5, "step": 0.5, "render": "stars"}`. `step` granularity goes down to one decimal place (e.g. 0–5 by 0.5, 0–10 by 0.1). `render` governs presentation without changing the stored number — `number`, `stars`, `icon` (config carries the chosen glyph name), `slider`, or `bar`. A 4.5-of-5 star rating and an 8.3-of-10 numeric rating are the same `score` type with different config.
- `text` — freeform multi-line text. Config: `null`.
- `enum` — one choice from an ordered option list. Config: `{"options": ["light", "medium", "dark"]}`. The stored value is the chosen option string.
- `boolean` — a yes/no value. Config: `null`.
- `date` — a calendar date. Stored as ISO-8601 `YYYY-MM-DD`. Config: `null`. Only the implicit head-of-form date field uses this type; the user does not add `date` fields from the editor.

Power ranking is not a field type. It lives on the Collection as a `powerRanking` toggle (§1); when on, the Collection's entries carry a user-controlled rank position and the entry list surfaces a reorder affordance.

Per Collection Entry (instance) — a Review instance exists only once the user submits one:

- `data` — a map of `field-name → value`, conforming to the template at the recorded version. Any subset of the template's fields may be filled; a single field is as valid as the whole form. **Absent key means unset**; an empty string is a distinct value (a deliberately blank text field), not the same as unset. `Review.clear` removes the key; it never writes an empty string in its place.
- `recorded_template_version` — the template version this instance was written against.
- `created`, `last_modified`.

An entry is **unreviewed** while the Location sits in the Collection with no Review instance, and **reviewed** once the user submits one. There is no "incomplete" state in between — a sparse Review is a finished Review. The same `reviewed`/`unreviewed` distinction drives the entry's pin styling on the Map Overview (§4); it is derived from instance existence, not stored as a field.

**Actions.**

Template authoring (per Collection):

- `defineTemplate(collection, fields)` — initial template at Collection creation.
- `editTemplate(collection, newFields)` — modify. Existing instances keep conforming; removed fields are archived, not destroyed.
- `useBuiltIn(collection, templateName)` — adopt a built-in template (Coffee Ranking, Wishlist, Geo Diary, etc.) as the starting point.

Instance:

- `start(collection, location)` — produce a draft Review for a Location in this Collection. Pre-filled with sensible defaults (today's date, etc.). The draft is interaction-layer state; the instance does not exist until `submit`.
- `submit(review, data)` — commit the instance. Becomes the Collection Entry's Review.
- `edit(review, data)` — change values within the template.
- `clear(field)` — unset a field where "not set" is meaningful (distinct from "false" for booleans, etc.).

**Operational principle.** A user creating "Drip Coffee" turns on the Collection's power-ranking toggle and authors the template: a 0–5 `score` field shown as stars, a `light|medium|dark` style enum, and a freeform notes field. Later, sitting at a café, they `start(DripCoffee, BlueBottleMintPlaza)`. The app shows the template's form; they fill in score=4, style=light, notes="excellent", and `submit`. The Collection Entry now carries this Review — and reads as reviewed on the map. Browsing the Collection, they see Collection Entries ordered by their power-ranking position and shaped by their template.

**Notes.**

- The **Collection editor** is the UI mapping for `defineTemplate` / `editTemplate` (and for the Collection's own metadata) — see doc02.02.02.08.
- Built-in templates (Coffee Ranking, Wishlist, Geo Diary) are starting points users can adopt and customize. The field types above are shared affordances across all templates.
- One concept covers both template and instance for now. If sharing makes the template-author and reviewer different people, this may split into a separate **ReviewTemplate** concept — flagged but not pre-built.
- A Review exists only as part of a Collection Entry, and only once submitted — there are no orphan Reviews, and no empty ones. An entry with no Review instance is unreviewed (§4).
- A Review is valid with any subset of its template's fields filled — like a Letterboxd review, it need not be complete to count. The template author cannot mark a field required; the form never blocks save on missing values.
- **Edit against a newer template version.** A version mismatch is not, by itself, an error. The reconciliation rule is per-key, not per-version, and exploits the store's existing shape (immutable field `name`s; old `template_field` rows archived, not deleted — doc03.01 §table_schema):
  - **Read** an instance at any `recorded_template_version`. Keys whose `name` is absent at the Collection's current `template_version` are not rendered — the field is gone, the value is invisible — but the key stays in `data` (archived in place).
  - On **edit**, the data layer reconciles: keys whose `name` exists at the current version are kept; keys whose `name` is absent are dropped from the saved object. The instance is rewritten with the current `template_version` stamped as `recorded_template_version`. No UI is required for the common case.
  - The one case this rule does not silently resolve is when a `name` exists at both versions but the **stored value no longer parses** against the new `type`/`config` (a `score` field whose `min`/`max` changed, an `enum` whose option set shrank, a type swap). The value is preserved in `data` until the user touches that field's row; the form renders the row's input widget against the current `type`/`config`, and submitting overwrites the stale value. A future surface could expose archived values explicitly; the cheap design defers that.

---

## 4. Map Overview

**Purpose.** Give the user, on every app launch, an immediate spatial view of everything they care about — all Collections, all Collection Entries, with enough visual encoding to read the state of their world at a glance.

**State.**

- `viewport` — current `(center, zoom, bearing)`. Persisted across launches.
- `visiblePins` — for each Collection Entry across all Collections, a pin styled by:
  - **fill color** = the owning Collection's `appearance` color
  - **outline** = same color as fill for reviewed Collection Entries (those with a Review); pin is **gray with a colored outline** for unreviewed Collection Entries (those with no Review yet)
- `collectionFilter` — set of Collections currently shown. Defaults to "all."
- `selectedPin` — currently focused Collection Entry, if any.

**Actions.**

- `open()` — launch action. Restore `viewport`, render `visiblePins` for all Collections in `collectionFilter`. Renders before any background data has loaded (see doc01.01 §"Cold-start playbook").
- `pan(delta)`, `zoom(delta)` — standard map navigation. Updates `viewport`.
- `selectPin(pin)` — focus a Collection Entry; reveal its Collection and per-Collection-Entry data.
- `applyFilter(collections)` — set the active Collection filter to the given set, replacing any prior value; clear it via the filter chip's close affordance. Constrains which Collections' pins render.
- `jumpToCollection(collection)` — fit viewport to that Collection's Collection Entries.

**Operational principle.** A user taps the RTP icon. Within ~200ms, they see their last viewport with pins from all their Collections: orange-filled pins for "Drip Coffee" (reviewed), orange-outlined gray pins for cafés on their wishlist (unreviewed), green-filled pins for "NYT Top 100" places they've been, green-outlined gray pins for the rest of the NYT list. Without any further action, they understand the state of their world. Tapping a pin reveals which Collection it belongs to and the Collection Entry's data.

**Notes.**

- The "everything view" is the default landing surface. Entering a single Collection is a navigation away from it, not the inverse.
- Reviewed/Unreviewed is derived from whether a Collection Entry has a Review (§3) — it applies to every entry, with no dedicated template field.
- Pin appearance is driven by Collection `appearance`, not per-Collection-Entry. Per-Collection-Entry visual encoding (e.g. star count) is reserved for the Collection Entry detail view, not the overview map.
- Color-collision handling (two Collections with similar colors) is an emergent UX problem to address when the Collection editor's appearance picker is designed.

---

## 5. Location Provider

**Purpose.** Let a user choose and configure which mapping services RTP uses to resolve Locations, so they control the cost, quality, and data-ownership tradeoffs themselves.

**State.**

- `configured` — the set of providers the user has set up. Each carries a `type` (`google`, `osm`, `apple`, `mapbox`, etc.), an optional `key` (BYOK credential, when the provider requires one), and an `enabled` flag.
- `default` — the provider used for new `resolve` queries unless one is named explicitly.

**Actions.**

- `addProvider(type, key?)` — configure a provider, supplying a BYOK key if it requires one. Keyless providers (e.g. `osm`) need none.
- `switchProvider(type)` — set `default` to an already-configured provider. Affects only future resolutions.

**Operational principle.** A user pastes a Google Places key via `addProvider(google, key)` and resolves places against it. Later, wanting to leave Google, they `addProvider(osm)` (no key needed) and `switchProvider(osm)`. New searches now hit OSM; the Locations they already saved keep their Google `(sourceType, sourceId)` and cached snapshot, unaffected.

**Notes.**

- A keyless provider (`osm` via Nominatim) is the always-available fallback, so RTP works with zero configuration.
- `switchProvider` changes the default only — it does not migrate or re-resolve existing Locations. Rebuilding a library against a new provider is a separate, per-place-confirmed flow, deferred.
- The full provider catalogue and the `LocationProvider` data-layer seam live in doc03.02.02; this concept covers only what the user does.

---

## Composition notes

These are tentative synchronization observations, not a complete composition spec.

- **Collection ↔ Review template.** A Collection has exactly one Review template. Creating a Collection includes authoring (or adopting) its template before the first Collection Entry can be added.
- **Collection.addEntry ↔ Review.start + Review.submit.** Adding a Collection Entry is staged: `Review.start` produces a draft against the Collection's template; `Review.submit` together with `Collection.addEntry` commit the Collection Entry atomically.
- **Collection ↔ Location existence coupling.** Deleting a Location triggers removal of its Collection Entries from all Collections (or prompts the user). Deleting a Collection does not delete its Locations — they may participate in other Collections.
- **Location.addToCollection ↔ Collection.addEntry.** Same underlying action, two viewpoints. The user can initiate from either side (Collection-first or Location-first; see doc01.02 §"Geo Diary"). Both paths must reach the same final state.
- **Map Overview ↔ Collection.appearance.** Changing a Collection's color updates its pins everywhere on the map.
- **Map Overview ↔ Collection.entries.** Adding/removing Collection Entries updates `visiblePins`. Live; no manual refresh.
- **Map Overview ↔ Review.** Pin styling reads whether a Collection Entry has a Review — an entry without one renders gray (unreviewed). Review field data is reserved for the Collection Entry detail view.
- **Open Externally ↔ no concept dependency.** `openExternally` is a leaf action — RTP hands off to the OS and does not track what happens after. Deliberately under-coupled.
