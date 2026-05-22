---
title: Location Providers
summary: The LocationProvider seam: osm default backed by Photon + Nominatim endpoints, BYOK providers, per-provider caching rules, ODbL export obligations
tags: [system, providers, location, osm, odbl, licensing]
deps: [doc01.03, doc03.02]
---

# Location Providers

The data-layer realisation of the Location Provider concept (doc01.03 §5). The Location Provider concept covers what the *user* does — configure providers, switch the default. This doc covers the `LocationProvider` seam: the implementation contract, the default `osm` provider, the caching rules each provider type imposes, and the licensing obligations that follow Locations into exported bundles.

The `LocationProvider` seam is one of the swappable external seams named in doc03.02 — it plays no part in a review submission and sits outside the repository contract. A repository persists a `Location`; a `LocationProvider` is what produced that `Location` in the first place.

## The seam

All of the following is pure Kotlin in `commonMain` — no Compose, no platform, no SQL. The seam imports the domain `Location` shape (doc03.02) but produces *candidates*, not `Location`s: a candidate is an unadopted search result; turning one into a `Location` is the user's choice on the LocationDraftSheet surface (doc02.02.02.05).

### Related types

The seam reuses the domain types already defined in `domain/Model.kt` (doc03.02) — `Coordinates`, `SourceType` (`Google`, `Osm`, `Apple`, `Manual`), `Location` — rather than introducing parallel ones. It adds only what a *search result* needs:

```kotlin
// An unadopted search result. Carries everything needed to construct a
// Location, but is not one until the user adopts it.
data class LocationCandidate(
    val coordinates: Coordinates,
    val displayName: String,
    val sourceType: SourceType,
    val sourceId: String?,          // provider-stable id; null => no stable
                                    // identity (e.g. an interpolated address)
                                    // => adoption yields a Manual Location
    val cachedMetadata: String?,    // opaque provider JSON, retained per the
                                    // caching rules below; the name matches
                                    // Location.cachedMetadata
)

// Outcome of a provider call. A provider never throws across the seam;
// network, rate-limit, and parse failures are values.
sealed interface ProviderResult<out T> {
    data class Ok<T>(val value: T) : ProviderResult<T>
    data class Failed(val reason: ProviderError) : ProviderResult<Nothing>
}

enum class ProviderError { NETWORK, RATE_LIMITED, NOT_CONFIGURED, PROVIDER_ERROR }
```

### The interface

```kotlin
interface LocationProvider {
    val type: SourceType

    // Forward / text search. Backs Location.resolve (doc01.03 §2).
    suspend fun resolve(query: String): ProviderResult<List<LocationCandidate>>

    // Reverse / proximity search. Backs Location.resolveNearby (doc01.03 §2).
    suspend fun resolveNearby(coordinates: Coordinates): ProviderResult<List<LocationCandidate>>
}
```

`resolve` and `resolveNearby` are the seam's whole surface — they back the two search actions of the Location concept and nothing else. `dropPin` produces a `Manual` Location with no provider involved, so it is not on this interface. `SourceType.Manual` is therefore never a provider `type`.

### The registry

The Location Provider concept's `configured` set and `default` (doc01.03 §5) are realised as a registry. It is the only object UI and Settings (doc02.02.02.03) hold; individual providers are reached through it.

```kotlin
interface LocationProviderRegistry {
    fun providerFor(type: SourceType): LocationProvider?
    fun default(): LocationProvider                  // never null — Osm backstops it
    fun configured(): List<SourceType>
    fun setDefault(type: SourceType)                 // concept action: switchProvider
    fun addProvider(type: SourceType, key: String?)  // concept action: addProvider
}
```

`default()` is total: `Osm` is always registered and keyless, so a registry can always answer (doc01.03 §5 — RTP resolves with zero configuration). A call that names an unconfigured provider returns `ProviderResult.Failed(NOT_CONFIGURED)` rather than throwing.

## Default provider: `osm`

`osm` is the default and the always-available keyless fallback, so RTP resolves Locations with zero configuration. Every other provider type is opt-in and BYOK.

`osm` is one provider in the user-facing sense — one entry in the concept's `configured` set, identified by `type`, not by endpoint. Its implementation is backed by **two services**, both interfaces to the same OpenStreetMap database:

- **Photon** backs `resolve` — built for forward search and typeahead.
- **Nominatim** backs `resolveNearby` — reverse geocoding from a coordinate.

Because both services speak OSM object identity (`osm_type` + `osm_id`, e.g. `N240109189`), a place found through either resolves to the **same `(sourceType, sourceId)`**. The two services are sub-configuration of one provider, not two providers. The Location concept therefore needs no multi-resolution model: a single resolution per Location holds regardless of which endpoint answered. (The multi-identity model flagged in doc01.03 §2 is for genuinely distinct providers — a Google `place_id` *and* an OSM node for one real place — which is `merge` territory, not this.)

A Photon result that carries no `osm_id` (an interpolated address) yields a candidate with no stable `sourceId`. Adopting it produces a `manual`-style Location — the same coordinates-only outcome as a dropped pin (doc01.03 §2), not a special case.

### Endpoint constraints

The public Nominatim instance enforces a [usage policy](https://operations.osmfoundation.org/policies/nominatim/) that shapes the `osm` implementation:

- One request per second, single-threaded — the `osm` provider throttles and debounces client-side.
- Per-keystroke autocomplete against public Nominatim is disallowed. Photon carries forward search precisely so typeahead stays inside policy.
- A request must send an identifying `User-Agent` naming RTP with a contact.
- Bulk geocoding is disallowed — relevant when `Collection.import` ingests a large KML/GeoJSON.

The endpoint is overridable: a self-hoster can point the `osm` provider at their own Photon and Nominatim instances. The override is endpoint configuration, never a second provider slot.

## BYOK provider: `google`

Google Places is the first BYOK provider. The user supplies an API key on the `ProviderConfig` surface in `google` mode ([[../02-design/02-interaction/02-screens/13-provider-config]], doc02.02.02.13); the surface invokes `addProvider(Google, key)` on the registry, which constructs a `GoogleLocationProvider` and registers it under `SourceType.Google`. With a key registered, the user may switch the default to `google` from the same surface (`switchProvider`).

### Endpoint mapping

`GoogleLocationProvider` is backed by the Places API (New) — a single Google product family that maps cleanly onto the two seam methods. The legacy Places API is out of scope; new keys default to the new API.

- **Text Search** (`places:searchText`) backs `resolve(query)` — forward search across the full place catalogue.
- **Nearby Search** (`places:searchNearby`) backs `resolveNearby(coordinates)` — proximity search around a coordinate.

Both endpoints accept a field mask via the `X-Goog-FieldMask` header; the provider requests only the fields needed to build a candidate (`places.id`, `places.displayName`, `places.location`, `places.formattedAddress`) plus a small tail of fields safe to cache (see below). The API key travels in the `X-Goog-Api-Key` header — never in the URL — so it does not land in logs.

A candidate carries `sourceType = Google`, `sourceId = place.id` (the stable `places/XXXX` resource name), and a minimal `cachedMetadata` payload conforming to the caching rule below.

### Endpoint constraints

The Places API (New) has its own usage shape that the implementation respects:

- Keys are billed per request, scoped by SKU (Text Search and Nearby Search are separate SKUs with different per-1000-call prices). The provider does not throttle artificially — the user owns their quota — but does coalesce identical-query bursts client-side.
- A field mask is **required** on every request; omitting it returns a 400. The provider always sends one.
- The contractual ceiling on retained fields is encoded in the field mask itself: requesting only cacheable fields keeps the cached payload compliant by construction.

### Key storage

Where the API key persists across launches is a Settings store concern — a small secrets seam (Android Keystore-backed EncryptedSharedPreferences, iOS Keychain, jvm file stub) that ProviderConfig calls into when *Save key* fires. That seam is not specified here; this doc only states the requirement: a saved key survives process death and is never logged. Until that seam exists, the registry can hold the key in memory only — usable for a session, lost on restart.

## `manual`

Not a provider — `SourceType.Manual` records a Location with no provider involved (dropped pin, or a Photon result with no `osm_id`). It is never a provider type and never appears in the `ManageProviders` row list.

## Caching follows the provider

`Location.cachedMetadata` is a provider snapshot taken at adoption time; `Location.refreshable` marks whether the source can be re-queried (doc01.03 §2). Whether RTP may *retain* that snapshot is provider-specific — caching policy is a property of `SourceType`:

| Provider | Retain rich metadata | `refreshable` |
|---|---|---|
| `osm` | Yes — ODbL permits retention | optional |
| `mapbox` | Yes — permissive terms | optional |
| `google` | No — `place_id` plus minimal fields only; rich data must be refetched with attribution | must be `true` |

The `osm` default is therefore also the cache-friendly default: an adopted Location stays fully populated offline indefinitely. The `refreshable` flag exists to model the `google` case, where the cache is a thin pointer by obligation rather than choice.

## Licensing and export

OpenStreetMap data is licensed under the [ODbL](https://opendatacommons.org/licenses/odbl/). The obligation reaches only the **OSM-derived fields** of `osm`-sourced Locations — `coordinates`, `displayName`, the metadata snapshot. It never reaches user-authored content: Collection names, Review templates, Review data, notes, and ratings are original work and carry no ODbL obligation. ODbL's Collective Database notion makes this split explicit — independent data assembled alongside an extract is not infected by it.

How the obligation lands on `Collection.export` / `share` (doc01.03 §1):

- **Private export** (backup to the user's own storage) conveys nothing to anyone — no ODbL obligation is triggered.
- **Sharing a personal-scale collection** conveys an insubstantial extract of OSM. ODbL's substantiality threshold leaves insubstantial extracts unrestricted; the obligation reduces to **attribution** — "© OpenStreetMap contributors".
- **Publishing at scale** (a very large collection, or a live-follow feed) can cross into Derivative-Database territory, where share-alike applies to the OSM-derived portion. This is the one case that needs a deliberate decision, and it belongs with the future sharing concept.

`Location.sourceType` is the provenance ledger that makes attribution mechanical: an export scans its Locations, collects the distinct `sourceType`s present, and embeds a matching attribution manifest in the bundle — OSM credit when any `osm` entry is present, Google credit (under Google's own stricter terms) when any `google` entry is present. A `google`-sourced entry carries no ODbL obligation; `sourceType` records that per entry.

RTP's stance: every export embeds the computed attribution manifest; the app does not police share-alike — bulk re-publishing is the user's responsibility, consistent with the user-owned-file model. Map-display attribution ("© OpenStreetMap contributors" on the basemap) is a separate, always-on obligation of the tile layer.

## Not yet built

- The rest of the provider catalogue (`apple`, `mapbox`, `here`, `foursquare`, …) — candidates surveyed in doc01.01. Only `osm`, `google`, and `manual` are addressed in this doc.
- The Settings store seam that persists BYOK keys across launches — referenced from the `google` subsection but not specified here. It unfolds when the first cross-platform secrets need arise.
- The other external seams named in doc03.02 — `ImportSource`, `ExportFormat`, `TileSource`, `FieldType` — could each unfold their own doc when a work item demands it.
