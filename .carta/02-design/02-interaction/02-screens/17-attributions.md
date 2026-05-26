---
title: Attributions
summary: Affordance inventory for the Attributions surface — open-source library list generated from the build's dependency graph
tags: [design, interaction, screens]
deps: [doc02.02.01]
verify: [{"kind":"screen-inventory","sidecar":"17-attributions.inventory.json","against":{"doc":"doc02.02.01","key":"Attributions"}}]
---

# Attributions

The affordance inventory for the `Attributions` surface — the open-source library list reachable from `About` ([[15-about]], doc02.02.02.15). **The source of truth is the carta sidecar `17-attributions.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

Attributions discharges the notice obligations of RTP's permissive upstream dependencies — Apache 2.0 NOTICE preservation and BSD-style copyright notice reproduction. The list is **derived, not hand-curated**: the `aboutlibraries` Gradle plugin walks the resolved dependency graph at build time and emits a JSON asset that the in-app renderer consumes. This keeps the surface honest as dependencies change — a new library added in `build.gradle.kts` lands in Attributions on the next build, and a removed library disappears, without anyone having to remember to update a list. The content of each row (name, version, license name + text, copyright holders) is whatever the upstream POM declares; entries that omit a license can be supplemented from the plugin's config file when the upstream metadata is thin.

The surface has two regions. The **topBar** identifies the surface and carries *Back* to `About`. The **list** region holds one row per library. A row expands in place to show its license text; expansion is an in-view detail toggle, not a transition. The surface declares no concept actions in `meta.actions` — reading the list and expanding rows are display behaviors below the concept layer.

Map data attribution — "© OpenStreetMap contributors" under ODbL — is a separate obligation tied to map display rather than dependency notices, and is not satisfied here ([[02-location-providers]], doc03.02.02 names ODbL; the credit lives on the map surface itself).
