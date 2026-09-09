---
title: Geocoding provider options
summary: Survey of BYOK geocoding/search providers RTP could add beyond the osm/Photon default — POI density, free tiers, storage terms, and which double as MapLibre tile sources for a search+tiles bundle
tags: [research, providers, location, geocoding, byok, references]
deps: [doc03.02.02, doc01.01]
---

# Geocoding provider options

## What prompted this

The `osm` default ([[02-location-providers]], doc03.02.02) is backed by Photon, which reads OpenStreetMap data. OSM's street and address coverage is strong, but its **POI/venue coverage is uneven** — many cafés, restaurants, and shops are absent or carry no clean `name` tag, and Photon's prefix-token matching makes partial names feel like they demand the exact string. The always-available drop-pin + `resolveNearby` path ([[03-concepts]], doc01.03 §2) is the zero-config escape hatch, but a denser geocoder is the real fix when the user wants search to *find* the place.

The `LocationProvider` seam and its registry already exist for exactly this: `osm` stays the keyless default, and a denser provider is the opt-in BYOK upgrade ([[03-concepts]], doc01.03 §5). This doc surveys the candidate providers so the choice is made once, deliberately, when the work to add one is scheduled. Nothing here is built; each entry is a candidate.

## What RTP needs from a provider

The seam's shape and RTP's data model constrain the field to providers that satisfy all of:

1. **Storage-permitting terms.** RTP is a geo *diary* — it persists adopted Locations with a `cachedMetadata` snapshot ([[01-store-model]], doc03.01). A provider whose terms forbid retaining results is structurally incompatible, regardless of price. This is the wedge that excludes Apple (see below) and constrains Google.
2. **A free tier or keyless access.** The BYOK provider is a user's upgrade; a usable free tier keeps it reachable for a hobbyist.
3. **Typeahead-capable forward search**, to light up `supportsTypeahead = true` and the per-keystroke field — or an explicitly submit-only fallback.
4. **Any-basemap display rights.** RTP renders OSM tiles via MapLibre (doc02.01). A provider that ties its search results to its own basemap can only be used compliantly inside a search+tiles **bundle** — which is cheap when the provider's tiles are MapLibre-renderable and expensive otherwise.

## Candidates

| Provider | POI density | Free tier | Retain/store results | Typeahead | Doubles as MapLibre tiles |
|---|---|---|---|---|---|
| **Geoapify** | OSM + OpenAddresses + others | ~3k req/day | Yes | Yes | — |
| **Stadia Maps** (Pelias) | OSM + OpenAddresses + Who's On First | Yes | Yes | Yes | **Yes** |
| **Mapbox** | High (own + OSM-derived) | Generous | Yes (permissive) | Yes | **Yes** |
| **Foursquare Places** | **Best for venues/businesses** | Generous | ⚠️ Stricter — verify clauses | Yes | — |
| **Google Places (New)** | Best overall | $200/mo credit | ❌ `place_id` + minimal only; refetch | Yes | — |
| **LocationIQ** | Nominatim + extras | Yes | Yes | Yes | Raster only |

Notes per candidate:

- **Geoapify** — closest in spirit to the OSM default (same open data lineage, broadened) with permissive storage and a real free tier. The lowest-friction "better OSM" upgrade; a clean drop-in behind the seam.
- **Stadia Maps** — backed by Pelias, whose Who's On First ingest gives meaningfully better venue/admin coverage than raw OSM. Stadia *also* serves MapLibre vector tiles, so it is a first-class **bundle** candidate: one switch could set both search and basemap (see Bundle angle).
- **Mapbox** — the densest of the permissive-terms options and MapLibre-native, so likewise a bundle candidate. doc01.01 already flags it as "more permissive terms." Storage is allowed under its terms; the tradeoff is a commercial dependency.
- **Foursquare** — the direct answer when the gap is specifically *businesses* (restaurants, cafés, bars). Venue density is its defining strength. Its storage terms are tighter than Mapbox/Geoapify and would need a clause-level read before adoption; it is not a tile source.
- **Google Places (New)** — already partially modeled as the first BYOK provider (doc03.02.02 §google) with a `GoogleLocationProvider`. Best quality, but its terms forbid retaining rich fields — `refreshable` must be `true` and the cache is a thin `place_id` pointer by obligation. Best reserved for users who explicitly want it.
- **LocationIQ** — Nominatim-derived with a free tier and permissive storage; a budget alternative to Geoapify. Its tiles are raster only, so weaker as a bundle partner.

## Why Apple is excluded

Apple's free on-device search (`MKLocalSearch`, `CLGeocoder`) is attractive on cost but fails criteria 1 and 4 simultaneously: the Apple Developer Program License Agreement (Attachment 6) both restricts displaying Apple "Map Data" to an Apple-branded basemap and forbids persisting it beyond a temporary performance cache, with an anti-derived-database clause that a curated saved list reads against. A persisted geo-diary on an OSM basemap is the opposite of what those terms permit. Apple's compliant role in RTP is the `openExternally` handoff target (doc01.03 §2), not a `LocationProvider`. The detailed clause findings belong in the commit/PR that records this decision.

## Bundle angle

Some providers tie search to their own basemap; for those, switching search should switch tiles together. Stadia and Mapbox are the providers where this is *cheap* — their basemaps are MapLibre-renderable, so a "bundle" is a style-URL swap plus a provider swap, not a second renderer. A future Map Source concept could realise this: a configured bundle pairs a `LocationProvider` with a `TileSource` (the seam named but unbuilt in doc03.02.02) and switches them as a unit. The keyless OSM-search + OSM-tiles pairing is the default bundle; a paid MapLibre-compatible provider is the upgrade bundle. The concept earns its keep only for providers that genuinely couple the two — it is not needed for Geoapify/Foursquare/Google, whose results display on any basemap.

## If one is added first

Geoapify or Stadia are the strongest first additions: permissive storage, real free tiers, denser-than-raw-OSM coverage, and a clean fit behind the existing seam. Stadia additionally opens the bundle path. Foursquare is the targeted choice if the observed gap is specifically restaurants and venues. Adding any of them is the same shape of work: a new `SourceType`, a `LocationProvider` implementation, and ProviderConfig wiring ([[13-provider-config]], doc02.02.02.13) — the seam itself is ready.
