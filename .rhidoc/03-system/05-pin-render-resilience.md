---
title: Pin Render Resilience
summary: The PinRenderController facade and the substrate choices (GeoJsonSource push, JsonString serialization, symbol-collision flags) that keep pins on screen when the native render path misbehaves
tags: [system, maplibre, rendering, resilience, controller]
deps: [doc02.01, doc03.04]
---

# Pin Render Resilience

The map's pin pipeline crosses three execution contexts: the Compose main thread, maplibre-native's render thread, and (depending on the source kind) one or more library-internal dispatch queues that ask Kotlin code for features. Each handoff is a place where the on-screen pin set can drift from the in-memory pin set — through serialization quirks, symbol collision, asynchronous tile races, or feature caches that go stale and never refresh. The resilience layer described here exists so a single Kotlin call always brings the on-screen state back into agreement with the in-memory state.

## The facade — `PinRenderController`

`PinRenderController` holds the current pin set and a monotonic revision counter. It exposes:

- `setPins(...)` — replace the current pin set. Called from the ViewModel whenever upstream domain state produces a new derived pin list.
- `forceRedraw()` — bump the revision without changing pins. The next emission carries the same pins with a fresh revision, which is enough to defeat any structural-equality short-circuit downstream and force a full native push.
- `frames: Flow<PinFrame>` — a `combine` over pins and revision. Each `PinFrame` carries the snapshot the renderer should currently be displaying.

The controller has no Compose imports and no platform types; it sits next to the ViewModel in `commonMain` and is testable as plain Kotlin. Pin data flows in from one direction (the VM); recovery triggers flow in from any other direction (map lifecycle, future debug actions, future "pins look stuck" detectors) — same method, same effect on screen.

## Substrate — `GeoJsonSource` push, not `ComputedSource` pull

The choice of source kind is the load-bearing decision underneath the controller. `GeoJsonSource` carries the entire feature set as a single value; on each new value the library calls native `setData`, which **atomically replaces** the feature set in the source. There is no per-tile cache to corrupt, no protocol queue that can drop a fetch, no race between symbol collision and re-fetch ordering. Whatever the controller emits is what the map renders next frame.

The trade against `ComputedSource` (described in [[doc03.04]]) is real but small: every push serializes the full pin set, and the iOS native `setData` path is asynchronous (`maplibre-compose#738`) so a quiet first push may not be in the render pipeline by the next frame. The startup race has a deterministic fix below; the asynchrony cost is acceptable for our pin volume.

## Serialization — `GeoJsonData.JsonString`, not `GeoJsonData.Features`

`GeoJsonData.Features(geoJsonObject)` routes through spatialk-geojson's polymorphic serializer. That serializer assumes a `FeatureCollection` has at least one feature with non-null properties — its dispatch path calls `features.firstNotNullOf { it.properties }`, which throws `NoSuchElementException` on an empty collection. The controller's initial emission is empty by design, so this path crashes on launch.

`GeoJsonData.JsonString(rawJson)` hands raw GeoJSON straight to MapLibre's native parser without invoking any Kotlin-side polymorphic dispatch. The pin renderer builds the FeatureCollection as a string via `kotlinx.serialization`'s `buildJsonObject` / `buildJsonArray` builders — small, explicit, and immune to spatialk's empty-collection bug. The build cost per frame is negligible compared to the render work that follows.

## Symbol collision — always-visible markers

Pins compete with basemap labels for screen real estate. maplibre's collision algorithm walks symbols in z-order and lets the first reservation win; a marker that only sets `iconAllowOverlap = true` still reserves placement space and can be suppressed under some basemaps. The pin layers set **both** `iconAllowOverlap = true` and `iconIgnorePlacement = true` (and the `text*` counterparts on the labels layer). The combination is the documented maplibre idiom for "always visible, does not participate in placement as winner or as blocker" and removes collision as a feature-loss trigger.

## Startup race fix — `onMapLoadFinished` triggers `forceRedraw`

Source and Layer composables defer their native creation until `rememberStyleComposition` reports the basemap style is loaded. Pushes that happen before that point hit an iOS `setData` path that may not integrate features into the next render. `MaplibreMap`'s `onMapLoadFinished` callback fires the moment the gap closes; wiring it to `vm.pinController.forceRedraw()` makes the startup race a single named call rather than a camera-animation hack. Any later state where features drift away — for any reason we cannot reach from Kotlin — uses the same recovery hook.

## How the seams compose

Domain pin list arrives in the ViewModel from `combine(collections, entries, hidden)`. A collector pushes it to `pinController.setPins(...)`. The screen reads `vm.pinController` and hands it to `PinLayers`. `PinLayers` collects `controller.frames`, rebuilds GeoJSON as a string per frame, and binds the result to `rememberGeoJsonSource`. Three `SymbolLayer`s — body, kind-conditional mark, labels above z=12 — read from that source by id, with collision flags set to guarantee placement. The `MaplibreMap` host wires `onMapLoadFinished` to the controller's `forceRedraw`. Anything that needs to force a redraw — recovery flow, debug menu item, future heuristic — calls the same method.
