---
title: Navigation Wiring
summary: How the platform-agnostic navigation statechart becomes a Compose Multiplatform NavHost — type-safe routes, back stack, per-route ViewModel scoping
tags: [system, navigation, cmp, wiring]
deps: [doc02.02.01, doc03.02]
---

# Navigation Wiring

The navigation statechart ([[01-navigation]], doc02.02.01) is platform-agnostic — surfaces and gestures in concept-language, no `NavHost`, no routes. This doc is the bridge: how that statechart becomes a running Compose Multiplatform navigation graph. The statechart stays the source of truth; the wiring here is a mechanical projection of it.

## Library

Navigation uses **`org.jetbrains.androidx.navigation:navigation-compose`** — JetBrains' KMP build of Jetpack Navigation, the peer of the `org.jetbrains.androidx.lifecycle` artifacts the app already depends on. Type-safe routes need the `kotlin("plugin.serialization")` plugin and `kotlinx-serialization-core`. Both the plugin and the dependency are added to the version catalog when this wiring is built.

## Routes — one per statechart state

Each statechart state becomes one `@Serializable` route type. A state with no entry data is a route `object`; a state that needs an identifier to render is a route `data class` carrying it:

```
@Serializable object MapOverview
@Serializable object CollectionList
@Serializable data class CollectionDetail(val collectionId: String)
@Serializable data class CollectionEntryDetail(val entryId: String)
@Serializable object ReviewForm
// … one per state in 01-navigation.statechart.json
```

The route's parameters carry exactly what a surface's `meta.reads` requires to identify *which* instance it shows — `CollectionDetail` reads one `collection`, so its route carries a `collectionId`. Domain id value classes (`CollectionId`, `EntryId`) are unwrapped to `String` at the route boundary and re-wrapped inside the screen, because routes are serialized.

## The `NavHost`

`App()` holds a `NavHost`. Each statechart state is one `composable<Route>` block; each transition on that state is one `navigate()` (for a `target`) or `popBackStack()` (for `BACK`-style returns) call, passed into the screen as a named callback:

```
NavHost(navController, startDestination = MapOverview) {
    composable<CollectionDetail> { entry ->
        val route = entry.toRoute<CollectionDetail>()
        CollectionDetailScreen(
            collectionId = CollectionId(route.collectionId),
            onOpenEntry   = { id -> navController.navigate(CollectionEntryDetail(id.value)) },
            onEditTemplate = { navController.navigate(SchemaBuilder) },
            onBack        = navController::popBackStack,
            /* repositories from doc03.02 */
        )
    }
}
```

A screen receives one callback per affordance that navigates — `onOpenEntry`, `onEditTemplate`, `onBack` — never a generic `onNavigate(Screen)`. The callback names the gesture, so a screen's parameter list lines up one-to-one with its affordance inventory ([[00-index]], doc02.02.02.00).

## Back stack

The statechart's `BACK` transitions name a static `target` because a flat machine has no history. The `NavHost` has a real back stack, so `BACK` is `popBackStack()` — it returns to wherever the user actually came from. The statechart's static `BACK` target is then only a *fallback*: the destination used when a surface is opened cold (a deep link, a launch into a non-root route) and the stack below it is empty. This resolves the wart doc02.02.01 records — `CollectionEntryDetail` reached from `MapOverview` no longer forces a return into a Collection it was not opened from.

## ViewModel scoping

`viewModel()` inside a `composable<Route>` block scopes the ViewModel to that route's `NavBackStackEntry`. A new route instance gets its own ViewModel; popping the route clears it. This retires two mockup-era hazards:

- The `viewModel(key = …)` keying needed while every screen rendered at a fixed `when`-branch call-site — distinct routes are distinct store owners, so no manual key is required.
- ViewModels accumulating in a single shared store for the lifetime of the app — a popped route disposes its ViewModel.

## What this retires

The mockup navigation holds a single current `Screen` in a `NavState` object, with `selectedCollectionId` / `selectedEntryId` fields standing in for route arguments, and screens dispatch through a generic `onNavigate(Screen)`. The `NavHost` wiring replaces all of it: `NavState` and the `Screen` enum are removed, route arguments carry the selected ids, and the generic dispatch becomes per-affordance callbacks.

## Staying aligned with the statechart

The projection is one-to-one and verifiable: the set of route types equals the set of statechart states, and every `navigate`/`popBackStack` call corresponds to a transition on that state. A future `verify.mjs` verifier kind ([[05-verification-system]], doc01.05) could diff the declared route set against the statechart's state set the way `screen-inventory` diffs affordances — flagged, not pre-built.
