---
title: Theme & Tokens
summary: Three-tier design token system (primitive, semantic, provider) for the CMP theme: RtpColors, RtpSpacing, RtpTypography and the RtpTheme accessor
tags: [design, theme, tokens, styling, cmp]
deps: [doc02.01]
---

# Theme & Tokens

How RTP styles its Compose UI. The mechanism is `CompositionLocal` — ambient values read down the composable tree. A theme is a set of `CompositionLocal`s populated once at the root. `MaterialTheme` is one such system; RTP's tokens are the same mechanism, made ours.

This doc fixes the **token contract** — the names the UI codes against. The CMP framework choice that makes it possible is doc02.01.

## Three tiers

1. **Primitive tokens** — raw values, no semantics. A palette of `Color`s, a scale of `Dp`s. Internal; no composable references them directly.
2. **Semantic tokens** — what a value *means*, held in `@Immutable` data classes. This is the contract. A composable asks for `accent`, never `teal500`.
3. **Provider** — `RtpTheme`, a composable at the app root that selects the active token set (light/dark) and publishes it through `CompositionLocal`.

The indirection earns its keep: semantic names survive a redesign, one token-set swap repaints everything, and nothing is hardcoded at a call site.

## Token categories — first cut

Three categories are specified now because every screen needs them. Others (shapes, elevation, motion) are deferred — add a category when a surface demands it, not before.

### Colors — `RtpColors`

Semantic slots, not Material's component-anatomy slots. An `@Immutable data class` with `val Color` fields. Light and dark are two instances of the same shape. Slots so far: `background`, `surface`, `onSurface`, `accent`, `danger` — grow the set as surfaces need it.

### Spacing — `RtpSpacing`

A fixed scale of `Dp` values (`xs`, `sm`, `md`, `lg`). Every `padding`/`gap` in the UI draws from this scale — no raw `.dp` literals at call sites.

### Typography — `RtpTypography`

Named text roles (e.g. `screenTitle`, `body`, `caption`, `label`), each a `TextStyle`. Roles are named for *where text appears in RTP*, not for a type-ramp number.

## The provider and the accessor

`RtpTheme { }` wraps the app, selects the active `RtpColors` by light/dark, and calls `CompositionLocalProvider` for each token category. It keeps a `MaterialTheme` underneath so Material components still resolve their own locals.

Use `staticCompositionLocalOf` for token locals: theme values change rarely (a light/dark toggle), and a static local recomposes the whole subtree on change — correct here, and cheaper to read.

Call sites read tokens through a single `RtpTheme` accessor object with `@Composable @ReadOnlyComposable` getters, so a call site reads `RtpTheme.colors.accent`, `RtpTheme.spacing.md`, `RtpTheme.typography.body`.

## Why not just `MaterialTheme`

`MaterialTheme` stays underneath — Material components read its locals, so it must be populated. But RTP code targets `RtpTheme`:

- Material's `ColorScheme` slots are named for Material's component anatomy, not RTP's product. Semantic names (`accent`, `danger`) outlive a redesign.
- RTP can add categories Material has no slot for.
- One token-set change repaints everything.

## Boundary: the map is not themed here

The map (`maplibre-compose`, doc02.01) styles via a **MapLibre style JSON / PMTiles basemap**, not the Compose theme. Pin colors and basemap colors live in that style JSON. If brand colors must match across the map and the UI chrome, that is a **sync point** between the style JSON and `RtpColors` — not a shared mechanism. Keep the two color sources deliberately separate; reconcile values by hand.

## Placement in code

All token types and the provider live in `commonMain` — `Color`, `Dp`, `TextStyle`, `CompositionLocal` are all multiplatform; no `expect`/`actual`. Tokens are stable (`@Immutable` data classes of stable types), so they do not break composable skipping.

## Deferred

- Shapes / corner radii — when a component needs a non-default shape.
- Elevation / tint — when a surface needs layering beyond Material's default.
- Motion (durations, easing) — when an animation is specified.
