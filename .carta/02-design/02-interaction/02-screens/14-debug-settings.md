---
title: Debug Settings
summary: Affordance inventory for the DebugSettings surface — developer-only tools reachable from Settings, outside the production product spec
tags: [design, interaction, screens, debug]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"14-debug-settings.inventory.json","against":{"doc":"doc02.02.01","key":"DebugSettings"}}]
---

# Debug Settings

The affordance inventory for the `DebugSettings` surface — a developer pocket reached from Settings's *Debug* affordance ([[03-settings]], doc02.02.02.03). **The source of truth is the carta sidecar `14-debug-settings.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

DebugSettings exists to host actions that exercise the data layer without dragging the user through the real surfaces — seeding fixtures, dumping state, resetting stores. The first inhabitant is *Generate sample collection*, which fabricates a fresh `Collection` populated with stock NYC `Location` and `Entry` data, exercising `Collection.create`, `Collection.template.define`, `Location.upsert`, and `Collection.addEntry` ([[03-concepts]], doc01.03) end to end without provider calls.

The surface has two regions. The **topBar** identifies the surface and carries *Back* to Settings. The **tools** region holds debug rows; each row fires a concept action and surfaces the result inline (e.g. a snackbar) rather than navigating elsewhere. Debug rows are not transitions — the user stays on `DebugSettings` — so they are modeled in `meta.actions`, not `on`.

The surface is conditional: it is a developer convenience, not a product affordance. It is wired in code today; gating it behind a debug-only build channel is a candidate refinement once a release channel exists. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
