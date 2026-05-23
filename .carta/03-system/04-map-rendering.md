---
title: Map Pin Rendering
summary: How pins reach the map: maplibre-compose source/layer split, why pins use a pull-based ComputedSource, and the invalidation contract
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

Two source kinds matter for our pin use case:

- **`GeoJsonSource`** is push-based. The composable holds a `GeoJsonData` blob; whenever it changes, the library calls native `setData` to ship the whole feature set across. On iOS this call path is asynchronous (see `maplibre-compose#738` and `maplibre-native#3968`) — features are not necessarily in the render pipeline by the next frame. A user gesture that forces a re-render integrates them; a quiet load may not.
- **`ComputedSource`** is pull-based. The composable supplies a `getFeatures(bounds, zoom) -> FeatureCollection` lambda. The library calls it per visible tile when it needs data, synchronously from the native data-source protocol. Backed by `MLNComputedShapeSource` on iOS, which is a different code path from `MLNShapeSource`.

Pull-based sourcing is the right shape when:

- Features are derived from in-memory app state (filterable, mutable).
- Viewport panning into new tiles should reveal features without a forced re-render.
- The cost per visible tile is small (our pin count is small; the bounds filter is a cheap inclusion check).

## The pin pipeline

Pin rendering lives in `PinLayers.kt`, which is the seam where domain state meets the native engine:

1. A `List<PinUi>` arrives from the ViewModel.
2. `rememberUpdatedState(pins)` captures it behind a stable State holder so the `getFeatures` lambda has a single identity for the life of the source.
3. `rememberComputedSource(getFeatures = ...)` constructs a source once. The lambda filters pins to the requested `BoundingBox` and emits a spatialk `FeatureCollection`.
4. Three `SymbolLayer`s read from that source: a body, a kind-conditional mark icon, and a labels layer above z=12. Layer expressions read `feature["kind"]`, `feature["color"]`, `feature["name"]`.
5. When `pins` changes, `LaunchedEffect(pins)` calls `source.invalidateBounds(world)` so visible tiles re-request features via the captured State.

## Invalidation contract

`rememberComputedSource` keys the source on the lambda identity. If the lambda's identity changes on recomposition, the source is destroyed and recreated — which is heavy and resets tile state. Therefore the lambda must be wrapped in `remember { }` (not `remember(pins) { }`), and any pin-dependent state must be read indirectly via a stable holder like `rememberUpdatedState`. Invalidation is the explicit signal that data changed; lambda identity is *not*.

Concretely, three things should never change together:

- The lambda passed to `rememberComputedSource` (stable across recompositions).
- The Source instance (one per logical layer group).
- The Layer references to that Source (by id, never re-keyed for data reasons).

Pin updates flow through `invalidateBounds` / `invalidateTile`. Anything else is a churn signal.

## Where this matters at startup

The Compose tree composes the `MaplibreMap` composable; under the hood the library begins loading the basemap style asynchronously and constructs the native style only after the style URL resolves. Source and Layer composables nested inside `MaplibreMap` defer their native creation until that point — `rememberStyleComposition` is what governs that delay.

Implication: any "the source is loaded" assumption is really "the style is loaded *and* the source has been added to it." For push-based sources this gap is where the iOS `setData` race lives. For pull-based sources the gap is benign — the first tile request happens after style readiness, and our lambda runs synchronously on that request.

`onMapLoadFinished` is the explicit signal that this gap has closed. It is rarely needed for pin rendering itself, but it is the right hook for behaviors that *must* not fire until the native style is ready (e.g. programmatic camera moves that should land on the rendered viewport, not a transient one).
