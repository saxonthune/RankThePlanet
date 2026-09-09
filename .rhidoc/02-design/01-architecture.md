---
title: Architecture
summary: Tech stack decisions: Compose Multiplatform + maplibre-compose for the map-first cross-platform app
tags: [design, architecture, stack, cmp, maplibre]
deps: [doc01.01]
---

# Architecture

Decisions only. Rationale lives in [[background-context]] (doc01.01).

## Cross-platform framework: Compose Multiplatform

Kotlin + Compose Multiplatform for Android, iOS, and (later) desktop/web.

**Why:** the load-bearing reason is that MapLibre publishes a first-party Compose wrapper under its own org. Framework cold-start overhead is small relative to what the cold-start playbook in doc01.01 already buys back.

## Map rendering: maplibre-compose

`maplibre/maplibre-compose` for the map view; MapLibre Native underneath on iOS/Android. Pins as a GeoJSON source with a circle/symbol layer, not per-marker views. Built-in clustering.

## Tiles

Protomaps PMTiles. Bundle a low-zoom world basemap for offline first-frame; range-request higher zooms.

## Persistence: SQLCipher

The store is an encrypted SQLite database (SQLCipher), not a zipped JSON+KML bundle. A single file satisfies the whole-file sync constraint; SQLite gives indexing and transactional writes the bundle would not.

**Why:** the cold-start playbook (doc01.01) already assumes an indexed local DB; the sync model needs a single self-contained file. SQLCipher meets both. The store model — two stores, write path, op-log — is specified in doc03.01.

## Not yet decided

- Search/geocoding provider order — see doc01.01 location-abstraction section.
- Desktop/web rollout timing — gated on `maplibre-compose` parity.
