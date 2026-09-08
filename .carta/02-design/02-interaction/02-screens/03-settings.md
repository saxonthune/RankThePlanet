---
title: Settings
summary: Affordance inventory for the Settings surface — entry to provider config, sync and BYOK as stubs
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"03-settings.inventory.json","against":{"doc":"doc02.02.01","key":"Settings"}}]
---

# Settings

The affordance inventory for the `Settings` surface — where the user configures providers, sync target, and BYOK keys. **The source of truth is the carta sidecar `03-settings.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The app host exposes developer tools only when it explicitly enables them. Production builds omit the *Debug* row; the inventory retains the affordance for developer-enabled builds.

The surface has two regions. The **topBar** identifies the surface and carries *Back* to the everything view. The **content** region holds the configuration groups. Three affordances are inventoried: *Manage providers* (`TAP_MANAGE_PROVIDERS`) opens `ManageProviders` ([[01-navigation]], doc02.02.01), the provider catalogue from which each provider is configured individually; *About* (`TAP_ABOUT`) opens `About` ([[15-about]], doc02.02.02.15), the legal-and-attribution hub; *Debug* (`TAP_DEBUG`) opens `DebugSettings` ([[14-debug-settings]], doc02.02.02.14), a developer pocket that is in the spec because it is wired in the app, even though it is outside the production product surface set. Sync target and BYOK key entry are named in the surface's purpose but stay stubs: they have no statechart transitions or `meta.actions` yet, so they are not affordances and cannot be `deferred` (deferred items must exist in the statechart). They unfold into affordances once the concept actions behind them exist.

Both inventoried affordances tie to a transition on `Settings` in the statechart. The surface's `meta.actions` is empty — provider work happens on `LocationProvider`, not here — so the inventory carries no in-surface actions. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
