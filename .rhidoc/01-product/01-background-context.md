---
title: Background Context
summary: Condensed research session: My Maps API, map tech, sync, location abstraction, competitive landscape, cold start
tags: [product, research, background]
deps: []
---

# Background Context

Condensed from a research conversation (2026-05-12 / 2026-05-13). Captures findings and tentative directions; nothing here is a binding decision. Each section is a seed for later docs in this group or in a future system group.

## Premise

App for working through custom user-curated lists of places — e.g. "cheap restaurants", "drip coffee". Each list defines its own review schema (set by user or list creator). One location can appear on many lists with different reviews per list. Goals: open source, fast and responsive, no ads, user-owned data, BYOK for paid APIs.

## Google My Maps and the API question

Google My Maps has no developer API. It is a UI on top of Google Drive; storage is KML/KMZ XML for points/lines/polygons with name/description/styling per placemark. No schema, no per-user reviews, no concept of one place across two lists. The legacy `KmlLayer` JS class is deprecated and requires a public KML URL.

What Google actually exposes:

- **Places API** — `place_id` → name/address/hours. Terms restrict long-term storage to `place_id` plus minimal metadata; rich data must be refetched with attribution.
- **Maps Datasets API** — upload GeoJSON/CSV/KML for data-driven styling. Read-mostly visualization, not a per-user CRUD store.
- **Maps JavaScript / Mobile SDKs** — rendering. BYOK applies here, not to My Maps.

Implication: own database holds lists, schemas, reviews, list-membership. Google provides only `place_id` + tiles. KML/KMZ is the import/export interchange format, not the storage layer.

## KML

XML-based geo format Google built for Google Earth. Universal interchange — Google My Maps, Google Earth, OsmAnd, Gaia GPS all import/export it. KMZ is zipped KML. Limitation: geo-only; arbitrary structured data must go in `<ExtendedData>` and is usually ignored by consumers. Practical pattern: KML for the geo portion, JSON sidecar for schemas and reviews.

## Data model sketch

Standard relational shape, owned by us:

- `places` — `place_id` (Google's, or stable UUID for manual) PK; cached `name`, `lat`, `lng`.
- `lists` — `id`, `creator`, `name`, `schema_json` (JSON Schema for the review form).
- `list_places` — join `(list_id, place_id)`. One row per appearance.
- `reviews` — `(user_id, list_id, place_id, data_json)`. JSON shape governed by the list's schema.

Same place on two lists ⇒ two reviews against two schemas. No conflict.

JSON Schema as the review-form format gives free client-side validation and auto-generated UI.

## Location as an abstraction

Treat `Location` as an interface with multiple provider implementations. Future-proofs against Google pricing/terms changes; lets self-hosters stay fully off Google.

```
Location {
  coordinates: LatLng              // always
  displayName: string
  sourceType: ProviderType         // google, osm, manual, mapbox, ...
  sourceId: string?                // place_id, OSM node id, UUID for manual
  cachedMetadata: Json?            // snapshot at import time
  refreshable: bool
}

LocationProvider {
  search(query) -> [LocationCandidate]
  resolve(sourceId) -> Location
  reverseGeocode(latlng) -> Location
}
```

Key rule: **`(sourceType, sourceId)` is the join key, not `lat/lng`**. Two providers will give slightly different coordinates for the same real place. Provide a "looks like the same place — merge?" reconciliation flow.

Providers worth supporting (priority-ordered):

- Google Places — best quality, paid, BYOK, strict storage terms.
- Apple MapKit — iOS-only, free, decent.
- Mapbox Search — paid, more permissive terms.
- HERE — strong outside US.
- Foursquare — best POI density, generous free tier.
- OSM via Nominatim — free, open, self-hostable. Quality uneven.
- Photon — faster autocomplete than Nominatim, OSM-based.
- Pelias — open-source geocoder; ingests OSM, OpenAddresses, GeoNames, Who's On First.
- Overture Maps — newer open data (Linux Foundation).

Raw / user-provided sources: manual lat/lng pin, GeoJSON/KML/KMZ import, address string, shared URL parsing (Google Maps, Apple Maps, geo:, OSM, what3words), photo EXIF, Plus Codes.

Killer affordance this enables: user starts on Google for convenience, later flips a setting to rebuild every location against OSM. Earns trust.

## Map and pin rendering

The map library matters more than the app framework. DOM-based rendering (Leaflet) tops out around ~10k markers. WebGL/native-GL doesn't have that ceiling.

Pattern: don't add per-marker views. Add data as a GeoJSON source with a circle/symbol layer. GPU handles tens of thousands of points. MapLibre's built-in clustering (`cluster: true`, `clusterMaxZoom`, `clusterRadius`) is free and high-quality.

Tentative choice: **MapLibre everywhere**.

- Web: MapLibre GL JS (MIT, no key).
- Mobile: MapLibre Native iOS/Android, plus MapLibre React Native (MIT fork of pre-proprietary Mapbox SDK). Compose Multiplatform wrapper exists.
- Tiles: Protomaps PMTiles (single file, range-request friendly, no tile server) or MapTiler.

## Cross-platform framework

Open question. Both viable; tradeoff is perf vs ecosystem.

| Option | Cold start / perf | Ecosystem | Notes |
|---|---|---|---|
| **Compose Multiplatform** (Kotlin) | ~95–100% native; iOS stable May 2025; Skia/Metal on iOS | smaller | Best perf for map-heavy + list-scrolling |
| **React Native** | 80–90% native; needs Reanimated + FlatList tuning | largest | RN 0.84 (Feb 2026) is New Architecture only; map libs mature |
| **Flutter** | own renderer, consistent | medium | Viable |
| **Native (Swift + Kotlin)** | best | n/a | Double the work |

If cold start is the top priority, native wins. CMP is the strongest cross-platform fit for stated goals. RN if JS/React expertise or library breadth dominates.

## Sync, sharing, backup

Three separate problems. Don't conflate.

**No hosted server.** Self-hosted by the user. Architecture inspired by KeePassium: ship a single self-contained DB file, let the user store it wherever they want, let the OS file picker handle cloud auth. The OS sits between the app and cloud storage; the app never sees credentials.

- iOS: `UIDocumentPicker` → Files app → iCloud, Drive, Dropbox, OneDrive, etc.
- Android: Storage Access Framework → same coverage.
- Web: File System Access API.

Storage format must be a single file (sync engines work whole-file; multi-file tears on partial sync). Options:

- Encrypted SQLite (SQLCipher) — well-understood, indexable.
- Zipped bundle: JSON + KML + manifest.

**Conflict handling.** KeePassXC pattern is "trust file version history, manual merge"; we should do better. Approach:

- Internally model writes as an **append-only op log**, not direct mutations.
- Merging two file versions ⇒ sort two logs by timestamp, dedupe by op-id. Poor-man's CRDT for the small-data case.
- Track `last_synced_hash` per device. Hash mismatch ⇒ fetch both versions, merge logs, write.

Optional direct **WebDAV** support covers self-hosters (Nextcloud, ownCloud, Synology) with a small library footprint.

**Sharing** is mostly orthogonal:

- *Static share*: export a list as a signed JSON bundle (or KML for the geo portion) with the schema embedded. Recipient imports a frozen copy.
- *Live follow*: owner publishes versioned JSON to a URL; followers pull.

**Backup**: encrypted DB dump to user-chosen location, plus per-list KML/KMZ export for portability.

Skip CRDTs (Automerge, Yjs) unless real-time collaborative editing becomes a requirement.

## Competitive landscape

Closest neighbors:

- **Mapstr** — closest analog. Pin places, tag with keywords, no public reviews. Closed-source, freemium.
- **Beli** — restaurant-only; head-to-head ranking instead of stars. Schema baked in.
- **Savor** — restaurant tracking; per-dish ratings; private by default.
- **Truffle / World of Mouth / HappyCow** — niche.

Adjacent: Google Maps lists, Apple Maps Guides, Foursquare Swarm.

Open-source / philosophical peers:

- **OsmAnd** — OSM Android app; favorites are a feature, not the focus; power-user UX.
- **Organic Maps / CoMaps** — OSM, privacy-focused, traveler-oriented.
- **PinPoi** — Android-only, single-purpose POI manager. Closest in spirit; tiny.
- **uMap** — web-only, OSM-based shared custom maps.
- **Dawarich** — self-hostable Google Timeline replacement; different problem, same bucket.

What's missing in the market — our wedge:

1. **User-defined schemas per list.** Every existing app has one review model baked in.
2. **Open-source consumer-grade UI.** OsmAnd/Organic Maps feel like power tools.
3. **User-owned data with cloud-agnostic sync.** Nobody does the KeePassium pattern in this category.
4. **No ads, no social pressure.** Beli and Mapstr both push engagement metrics.

## Mapstr privacy

Stated policy is better than most: no resale, no third-party targeted ads, no external partner sharing, EU-hosted (GDPR). Funded by Mapstr Plus rather than ads.

What they do: data on Google Cloud (Frankfurt) + AWS (Ireland), Batch for notifications, usage-data analysis. Anonymized aggregate location/tag stats are sold to third parties — not individual data, but aggregate insights. Retention = contractual relationship + 5 years.

Real export (CSV + GeoJSON, all addresses/tags/notes) from Profile → Settings → Manage your data → Export.

Verdict: not snooping in the bad-actor sense, but architecture is "trust us, we'll be careful." Our user-owned-file model removes the need to trust at all. Worth marketing honestly — don't imply Mapstr is doing something nefarious.

## Cold-start playbook

Cold start is mostly about deferring work, not doing work faster. Ordered by impact:

1. **Pick the right runtime.** Native > CMP > Flutter > RN for cold start.
2. **Minimize the binary.** App size correlates directly with cold start. R8/ProGuard, dead-code elim, lazy-load. MapLibre Native < Mapbox SDK < Google Maps SDK.
3. **Render before ready.** Persist a tile snapshot + last camera position on backgrounding; blit on launch in <16ms; fade in real map when loaded.
4. **Defer everything non-critical.** First frame needs only: viewport tiles + viewport pins. Everything else (schemas, history, sync, analytics, crash reporters) loads after.
5. **Pre-warm what can't be deferred.** iOS prewarming, Android App Startup. Trace with Macrobenchmark / `os_signpost` to find the long pole.
6. **DB: lazy + smart.** SQLCipher takes 50–200ms to open — off main thread, after first paint. Keep an unencrypted "viewport cache" SQLite for last-known pins in current map area. Index by spatial tile (geohash / H3).
7. **Bundle some tiles.** Protomaps PMTiles low-zoom world basemap (~50MB at zoom 0–5) so first frame never needs network.
8. **Pins as GeoJSON layer, not views.** Startup cost doesn't scale with pin count.
9. **Skip splash screen ideology.** Use the OS launch screen and have first real frame match its layout — invisible transition.
10. **Persist navigation state.** `{last_list_id, camera, zoom, selected_pin}` to a tiny file on background; restore synchronously on launch.
11. **Measure on the worst supported device.** Pixel 6a / iPhone SE. Target <400ms cold, <100ms warm; track in CI macrobenchmarks.

Brutal version (if "fastest" really means fastest): native Swift + native Kotlin, MapLibre Native + bundled PMTiles, plain SQLite hot path with separate encrypted store for sensitive fields, snapshot-based instant render, all init deferred. Sub-200ms cold start on midrange achievable.

Tradeoff: every speed technique adds complexity. Stop when "feels instant" — further optimization is invisible.

## Open questions surfaced

These are seeds for later docs, not decisions:

- Which providers ship in v1 — just Google Places + manual + KML import?
- Schema authoring UX — JSON Schema is the format; what's the editor?
- Sharing flavors at launch — static export only, or also live-follow?

Resolved since this research session:

- Native vs Compose Multiplatform vs React Native → Compose Multiplatform (doc02.01).
- SQLCipher single-file vs zipped bundle → SQLCipher (doc02.01).
- Op-log from day one vs migrate later → the op-log exists day one as a data-layer artifact; whether it becomes the source of truth (event sourcing) vs. an audit log alongside authoritative tables is deferred (doc03.01).
