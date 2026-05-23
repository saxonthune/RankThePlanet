---
title: Manage Providers
summary: Affordance inventory for the ManageProviders surface — current default header, list of provider rows that route to per-provider config
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"12-manage-providers.inventory.json","against":{"doc":"doc02.02.01","key":"ManageProviders"}}]
---

# Manage Providers

The affordance inventory for the `ManageProviders` surface — the provider catalogue reached from Settings's *Manage providers* affordance ([[03-settings]], doc02.02.02.03). **The source of truth is the carta sidecar `12-manage-providers.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface has three regions. The **topBar** identifies the surface and carries *Back* to Settings. The **currentProvider** region is a read-only header naming the default provider (read from `default-provider`) — this is what answers "who is resolving Locations right now?" without making the user scan rows. The **providers** region is the list: one row per provider type the app implements, browsable as a vertical list (`reads: configured-providers`). Each row reports the provider's name, a status pill (configured / not configured / default), and presses through to `ProviderConfig` for that provider type ([[13-provider-config]], doc02.02.02.13) via `TAP_PROVIDER`, propagating `provider-context`.

This surface only *lists and routes* — it carries no concept actions of its own. `LocationProvider.addProvider` and `LocationProvider.switchProvider` (doc01.03 §5) are both expressed on `ProviderConfig`: a row tap goes one level deeper, and the work happens there. That keeps the catalogue read-only and the per-provider screen the single place where setup state changes.

The provider list is the **whole** provider catalogue the app supports — not just the configured ones. An unconfigured BYOK provider still has its row, so the path to "I want to start using Google Places" is one tap on the row, not a hidden "add provider" affordance ([[04-location-providers]], doc03.02.02). Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
