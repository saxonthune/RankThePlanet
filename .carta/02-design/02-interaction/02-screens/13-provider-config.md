---
title: Provider Config
summary: Affordance inventory for the ProviderConfig surface — per-provider setup parameterized by provider-context, key entry for BYOK providers
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03, doc03.04]
verify: [{"kind":"screen-inventory","sidecar":"13-provider-config.inventory.json","against":{"doc":"doc02.02.01","key":"ProviderConfig"}}]
---

# Provider Config

The affordance inventory for the `ProviderConfig` surface — the per-provider setup page reached from a `ManageProviders` row ([[12-manage-providers]], doc02.02.02.12). **The source of truth is the carta sidecar `13-provider-config.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

ProviderConfig is one surface in two modes, parameterized by `provider-context` (the provider type the user tapped). Same-surface-different-mode per [[04-surface-composition-rules]] (doc02.04): the regions and topBar are identical; what differs is which provider-specific fields render and which actions are live.

- **`osm` mode** — OpenStreetMap is keyless and always configured. The setup region carries provider-identifying metadata only (endpoint host, attribution note); the actions region offers *Set as default* (`LocationProvider.switchProvider`) when osm is not already the default.
- **`google` mode** — Google Places is BYOK. The setup region carries a *Paste API key* field and a *Save key* action (`LocationProvider.addProvider`); a status line reflects whether a key is registered. The actions region offers *Set as default* once a key is saved.

Both modes share four regions. The **topBar** names the provider and carries *Back* to ManageProviders. The **status** region is a one-line read of `provider-state` (e.g. *"Default. Resolving with the public Photon and Nominatim endpoints."* for osm; *"Not configured. Paste a Places API (New) key to enable."* for google). The **setup** region carries the provider-specific fields described above. The **actions** region carries cross-provider actions — *Set as default* — gated on whether the action is meaningful for this provider's current state.

Saving the key and switching the default are in-view concept actions, not navigations; they update the surface in place. Only `BACK` moves the user. The surface deliberately omits any "remove provider" affordance: provider removal is not a concept action (doc01.03 §5), and a BYOK provider that the user wants to stop using is handled by leaving its key in place but switching the default to another row.

Carried context: `provider-context` is set on every entry from ManageProviders and is read by every region — without it, none of the modes resolve. The inventory marks `reactsToContext: ["provider-context"]` on each region to make this load-bearing dependence explicit ([[01-navigation]], doc02.02.01).

Per-provider implementation details — endpoint mapping, caching obligations, key storage — live in [[04-location-providers]] (doc03.04), not here. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
