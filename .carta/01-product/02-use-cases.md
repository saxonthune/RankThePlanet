---
title: Use Cases
summary: User-mental-model walkthroughs: Drip Coffee ranking, NYT Top 100 import, Geo Diary
tags: [product, use-cases, ux]
deps: [doc01.01]
---

# Use Cases

Walkthroughs anchored on what the user is thinking and doing — not on storage, sync, or framework details. Each use case is a concrete journey a real person would describe in their own words. Implementation lives in later groups.

A `Collection` is the user-facing word for a curated list of `Location`s. Each Collection is bound to a `Schema` that defines what fields a Collection Entry has. `Location` is a workspace concept implemented by multiple providers (see doc01.01 §"Location as an abstraction").

## 1. Drip Coffee Ranking

**Persona.** Someone who drinks drip coffee at cafés around their city and wants to remember which ones were good.

**Goal.** Build a personal, ranked record of cafés with enough structure to answer "where should I go this morning?"

**Journey.**

1. Opens the app. Taps **New Collection**. Names it "Drip Coffee."
2. Picks a schema. Either selects a built-in **Coffee Ranking** schema or builds one with these fields:
   - **Location** — required, single. Picks via the Location concept (any configured provider: Google Places, OSM, manual pin).
   - **Date** — defaults to today, editable.
   - **Stars** — 1–5.
   - **Power ranking** — the user's ordinal position of this café within the Collection. Implies head-to-head comparison when adding (cf. Beli) or drag-to-reorder in the list view.
   - **Visited / Unvisited** — boolean flag. "Unvisited" means it's on the wishlist but no review yet.
   - **Coffee style** — one of `light | medium | dark`.
   - **Review text** — freeform.
3. Out at a café. Opens the app, taps **+** on the Drip Coffee collection. Searches "Blue Bottle Mint Plaza," picks the result. Marks Visited, sets stars to 4, picks `light`, types a sentence.
4. App asks: "Where does this rank against the cafés you've already reviewed?" Shows a few head-to-head matchups; the resulting power ranking slots Blue Bottle into position 3 of 11.
5. Later, browses the Collection sorted by power ranking. Sees the map view with pins colored by stars. Taps a pin → sees the review.
6. Adds a café to the Collection without visiting yet — marks **Unvisited**. It shows on the map as a hollow pin and is excluded from the power ranking until reviewed.

**What the user never has to think about.** Where the data lives, which provider resolved the location, whether a review schema is JSON-Schema-encoded.

## 2. NYT Top 100 Restaurants in NYC

**Persona.** Someone who saw the NYT list, wants to work through it on visits to New York, and wants to track which they've been to.

**Goal.** Import an existing curated list as a Collection, then use it as a personal checklist with light per-Collection-Entry annotations.

**Journey.**

1. Finds the NYT list shared as a Google My Maps link (or KML, or a list of place names — details TBD).
2. In the app, taps **Import Collection**. Picks the source format. App ingests the list.
3. App asks: "What schema do you want for this Collection?" Offers a default **Wishlist** schema:
   - **Location** — pre-populated from the import.
   - **Visited / Unvisited** — defaults to Unvisited for every imported Collection Entry.
   - **Date visited** — empty until user fills it.
   - **Notes** — freeform.
4. The Collection appears with 100 unvisited pins on the map of NYC.
5. On a trip, the user opens the Collection, taps a pin they're near, marks **Visited**, adds a note. The pin's appearance changes to reflect visited state.
6. Over time the Collection becomes a personal record: 27 visited, 73 to go. The user can sort by distance from current location to plan a day.

**Open detail.** What "import" actually accepts at v1 — Google My Maps share URL? KML upload? Pasted list of names that get geocoded one-by-one with the user confirming each? See doc01.01 open questions.

**What the user never has to think about.** That the imported list is rehoused entirely in their own database, not merely linked to the NYT/Google source.

## 3. Geo Diary

**Persona.** Someone who wants to remember where they were and what happened there — a journal indexed by place rather than by date.

**Goal.** Quickly capture a place + a moment, with minimal structure.

**Journey — Collection-first.**

1. Has a Collection called **Geo Diary** with the simplest schema:
   - **Location** — required.
   - **Date** — defaults to today.
   - **Text** — freeform.
2. Sitting in a park. Opens app, taps the Geo Diary collection, taps **+**. Picks **Use current location**. Writes a paragraph. Done.

**Journey — Location-first.**

1. Sitting in a park. Opens app on the map view. Long-presses the map at their location (or taps **+** with **Use current location**).
2. App: "Add to which Collection?" Shows a list of the user's Collections. The user picks **Geo Diary**.
3. App opens the Geo Diary Collection Entry form (date prefilled, text empty). Writes a paragraph. Done.

**Implication.** Both flows must reach the same final state. The "add to Collection" affordance has to be available from both Collection context and Location context, with no duplication of Collection Entry data.

**What the user never has to think about.** That a Location can belong to many Collections, that today's diary Collection Entry and yesterday's diary Collection Entry at the same park are two separate Collection Entries (not one place with two timestamps).

## Cross-cutting observations

These fall out of the three use cases together. They are observations, not specs:

- **Schemas are first-class.** Built-in schemas (Coffee Ranking, Wishlist, Geo Diary) accelerate common cases. Custom schemas accommodate everything else. Authoring UX is a future doc.
- **Visited/Unvisited recurs.** It surfaces in Drip Coffee (wishlist cafés) and NYT 100 (the entire list starts unvisited). May warrant being a built-in field type rather than re-declared per schema.
- **Power ranking is one schema field, not a global app feature.** A Collection without a power-ranking field has no ranking flow. Keeps the "different lists, different mental models" promise honest.
- **"Add to Collection" must be reachable from both directions.** Either start from a Collection and add a Location, or start from a Location and add to a Collection. Same end state.
- **Maps view and list view are two projections of the same Collection.** Both must be available; neither is privileged.
- **Imports are first-class Collections, not a separate "shared lists" namespace.** Once imported, an NYT list is the user's Collection — they can edit it, change its schema, delete Collection Entries.
