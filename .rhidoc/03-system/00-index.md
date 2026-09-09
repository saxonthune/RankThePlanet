---
title: System
summary: 
tags: []
deps: []
---

# System


| Ref | Item | Kind | Summary | Tags |
|-----|------|------|---------|------|

| doc03.01 | Store Model | doc | Two-store persistence: local SQLCipher DB + sync replica; table schema, local-first write path, memoized overview projection, op-log | system, storage, sqlcipher, sync, schema |
| doc03.02 | Data Interfaces | group (3) | — | — |
| doc03.03 | Navigation Wiring | doc | How the platform-agnostic navigation statechart becomes a Compose Multiplatform NavHost — type-safe routes, back stack, per-route ViewModel scoping | system, navigation, cmp, wiring |
| doc03.04 | Map Pin Rendering | doc | The maplibre-compose source/layer split, the two source-kind shapes, and what crossing the native boundary implies for state updates | system, maplibre, rendering, source |
| doc03.05 | Pin Render Resilience | doc | The PinRenderController facade and the substrate choices (GeoJsonSource push, JsonString serialization, symbol-collision flags) that keep pins on screen when the native render path misbehaves | system, maplibre, rendering, resilience, controller |

Topics: cmp, controller, maplibre, navigation, rendering, resilience, schema, source, sqlcipher, storage, sync, system, wiring
