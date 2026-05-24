---
title: Map Pin Rendering
summary: The maplibre-compose source/layer split, the two source-kind shapes, and what crossing the native boundary implies for state updates
tags: [system, maplibre, rendering, source]
deps: [doc02.01]
---

# Map Pin Rendering

## The source/layer split

maplibre-compose separates *data* from *appearance*:

- A **Source** owns the features (points, geometries, properties). It is a wrapper over a native MapLibre data object — Android `MapLibreMap` / iOS `MLNStyle` — that lives on the render thread.
- A **Layer** (`SymbolLayer`, `CircleLayer`, …) reads from a Source by id and decides how each feature looks via styling expressions.

This split is the boundary worth respecting. The Kotlin/Compose side describes what the data and styling *should be*; everything past the Source/Layer composables crosses into the native MapLibre engine. State updates pushed across that boundary are subject to native scheduling, render-thread queues, and tile invalidation semantics — not Compose recomposition.

## Two source shapes

Two source kinds matter for any custom-feature use case:

- **`GeoJsonSource`** is push-based. The composable holds a `GeoJsonData` blob; whenever it changes, the library calls native `setData` to ship the whole feature set across. On iOS this call path is asynchronous (see `maplibre-compose#738` and `maplibre-native#3968`) — features are not necessarily in the render pipeline by the next frame. A user gesture that forces a re-render integrates them; a quiet load may not.
- **`ComputedSource`** is pull-based. The composable supplies a `getFeatures(bounds, zoom) -> FeatureCollection` lambda. The library calls it per visible tile when it needs data, synchronously from the native data-source protocol. Backed by `MLNComputedShapeSource` on iOS, which is a different code path from `MLNShapeSource` and carries a per-tile feature cache that persists until explicitly invalidated.

RTP's pin pipeline picks one of these and layers a recovery facade over it — see [[pin-render-resilience]].

## Style readiness

The Compose tree composes the `MaplibreMap` composable; under the hood the library begins loading the basemap style asynchronously and constructs the native style only after the style URL resolves. Source and Layer composables nested inside `MaplibreMap` defer their native creation until that point — `rememberStyleComposition` is what governs that delay.

Implication: any "the source is loaded" assumption is really "the style is loaded *and* the source has been added to it." `onMapLoadFinished` is the explicit signal that this gap has closed and is the right hook for behaviors that must not fire until the native style is ready.
