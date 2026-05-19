---
title: Navigation Wiring
summary: How the platform-agnostic navigation statechart becomes a Compose Multiplatform NavHost — type-safe routes, back stack, per-route ViewModel scoping
tags: [system, navigation, cmp, wiring]
deps: [doc02.02.01, doc03.02]
---

# Navigation Wiring

The navigation statechart ([[01-navigation]], doc02.02.01) is platform-agnostic — surfaces and gestures in concept-language, no `NavHost`, no routes. This doc is the bridge: how that statechart projects onto a Compose Multiplatform navigation graph. The statechart is the source of truth; this wiring is a mechanical projection of it.

## Library

Navigation should use **`org.jetbrains.androidx.navigation:navigation-compose`** — JetBrains' KMP build of Jetpack Navigation, the peer of the `org.jetbrains.androidx.lifecycle` artifacts the app depends on. Type-safe routes require the `kotlin("plugin.serialization")` plugin and `kotlinx-serialization-core` in the version catalog.

## Routes — one per statechart state

Each statechart state maps to one `@Serializable` route type. A state with no entry data is a route `object`; a state that needs an identifier to render is a route `data class` carrying it:

```
@Serializable object MapOverview
@Serializable object CollectionList
@Serializable data class CollectionDetail(val collectionId: String)
@Serializable data class CollectionEntryDetail(val entryId: String)
@Serializable object ReviewForm
// … one per state in 01-navigation.statechart.json
```

A route's parameters carry exactly what a surface's `meta.reads` requires to identify *which* instance it shows — `CollectionDetail` reads one `collection`, so its route carries a `collectionId`. Domain id value classes (`CollectionId`, `EntryId`) are unwrapped to `String` at the route boundary and re-wrapped inside the screen, since routes are serialized.

## The `NavHost`

`App()` holds a single `NavHost`. Each statechart state maps to one `composable<Route>` block; each transition on that state maps to one `navigate()` call (for a `target`) or `popBackStack()` (for a `BACK`-style return), supplied to the screen as a named callback:

```
NavHost(navController, startDestination = MapOverview) {
    composable<CollectionDetail> { entry ->
        val route = entry.toRoute<CollectionDetail>()
        CollectionDetailScreen(
            collectionId = CollectionId(route.collectionId),
            onOpenEntry    = { id -> navController.navigate(CollectionEntryDetail(id.value)) },
            onEditTemplate = { navController.navigate(SchemaBuilder) },
            onBack         = navController::popBackStack,
            /* repositories from doc03.02 */
        )
    }
}
```

A screen takes one callback per navigating affordance — `onOpenEntry`, `onEditTemplate`, `onBack` — not a generic `onNavigate(Screen)`. The callback names the gesture, so a screen's parameter list lines up one-to-one with its affordance inventory ([[00-index]], doc02.02.02.00).

## Back stack

A flat statechart has no history, so its `BACK` transitions name a static `target`. The `NavHost` holds a real back stack: `BACK` is `popBackStack()`, returning the user to wherever they came from. The statechart's static `BACK` target serves only as the cold-entry fallback — the destination used when a surface is opened with no stack beneath it (a deep link, a launch into a non-root route). So `CollectionEntryDetail` opened from `MapOverview` returns to `MapOverview`, not into a Collection it was never opened from.

## ViewModel scoping

A `viewModel()` obtained inside a `composable<Route>` block scopes to that route's `NavBackStackEntry`. Each route instance owns its ViewModel; popping the route clears it. Two properties follow: distinct routes are distinct ViewModel-store owners, so a screen needs no manual `viewModel(key = …)`; and a ViewModel does not outlive the route that owns it.

## Navigation state ownership

Navigation state lives in the `NavHost` back stack — no app-held object holds a single "current screen". Route arguments carry the selected ids (a `collectionId`, an `entryId`), so no separate selection field is needed. Screens navigate through per-affordance callbacks, so there is no global `Screen` enum and no generic dispatch.

## Staying aligned with the statechart

The projection is one-to-one and verifiable: the set of route types equals the set of statechart states, and every `navigate`/`popBackStack` call corresponds to a transition on that state. A `verify.mjs` verifier kind ([[05-verification-system]], doc01.05) could diff the declared route set against the statechart's state set the way `screen-inventory` diffs affordances — a candidate check, not a built one.
