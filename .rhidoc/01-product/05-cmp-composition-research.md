---
title: CMP composition patterns & reference apps
summary: Research session: how to design shared CMP surfaces that bend to context (mode parameter, sheet-not-route), with a Tier-1..4 audit of reference apps to compare against
tags: [product, research, cmp, patterns, references]
deps: [doc02.01, doc02.02.01]
---

# CMP composition patterns & reference apps

A research session prompted by two implementation drifts from the navigation spec ([[01-navigation]], doc02.02.01) — MapOverview's add-to-collection mode rendering with weak hierarchy, and LocationDraftSheet being routed as a full screen rather than presented as a sheet. Both bugs point at the same underlying question: how does one design shared UI that bends to context. This doc captures the answer once so future surfaces inherit it; the prescriptive distillation lives in [[04-surface-composition-rules]] (doc02.04).

## 1. The decision spine: four rungs

Most "should this be one composable or two" questions have the same answer if asked in order. Start at the top rung; drop down only when a concrete trigger forces you.

1. **Same composable, mode parameter + conditional invocation.** The shared body is identical; the delta is *visibility* of a region or *one extra affordance*. The mode is a sealed type, not a bag of booleans. The delta is a conditional (`if (mode is X) Banner(...)` or `AnimatedVisibility(visible = mode is X) { ... }`).
2. **Slot API.** A region's *content* varies, not just whether it shows. The parent accepts a `@Composable () -> Unit` for the slot. Material's `Scaffold(topBar = {…}, bottomBar = {…}, content = {…})` is the archetype.
3. **Sibling screens sharing extracted leaves.** Two modes truly diverge — different effects, different lifecycle work, less than roughly half of the tree shared — but the leaf pieces (search bar, menu, pin layer) are reusable. The screens are arrangements; the leaves are independent composables.
4. **Two screens, no sharing.** The modes have nothing structural in common.

Drop down a rung when any one of these triggers:

- The body accumulates more than ~3 distinct `if (mode is …)` blocks. One is fine; five is a refactor signal.
- One mode needs state or effects the other shouldn't allocate.
- One mode evolves on a different cadence — week after week, edits touch only one branch.
- A new reader can't picture mode X without mentally executing every branch.

If none fire, **stay on rung 1**. Three similar lines beats a premature abstraction.

## 2. The CMP-native shape of "context"

Asked another way: what does it mean to "inject context into a component that decides what appears." In CMP the answer is concrete.

- **Carried context = a sealed mode type.** `MapMode.Browse` vs `MapMode.AddingToCollection(collectionId, collectionName)`. The compiler enforces exhaustiveness; adding a third mode is a refactor the compiler walks you through.
- **State down, events up (UDF).** The composable doesn't *ask* what mode it's in — it receives the mode as part of `UiState` (or as a screen-level argument) and renders. State holders never expose `MutableState` to the UI; they expose `StateFlow`.
- **Propagation is just nullable route args.** Every route in the carrying chain takes the same nullable; at the screen boundary it resolves to the sealed type. `MapOverview(addToCollectionId)` → `LocationDraftSheet(addToCollectionId)` → `AddLocationToCollection(addToCollectionId)`.
- **Not a `CompositionLocal`.** Reserve those for ambient values genuinely needed at many unrelated depths (theme, density, haptics). Mode has a single propagation path; making it ambient hides the data flow.

## 3. The two rules that catch most cases

Distilled prescriptively in [[04-surface-composition-rules]] (doc02.04):

- **Same surface, different mode = one composable + mode parameter + conditional invocation of the deltas.**
- **A surface that overlays another surface is not a route. It is sheet state owned by the host screen's UiState.**

These two rules answer most of RTP's compositional questions for the rest of the interaction layer: add-modes, edit-modes, draft-over-map, peek-over-map, pickers-over-anything.

## 4. Anti-patterns observed in RTP and around CMP

- **Parallel booleans (`isAddMode`, `isEditMode`, `hideFab`) instead of a sealed mode.** Each new boolean doubles the implicit state space and decouples from the spec.
- **Sheet modeled as a route.** Forces unmount of the host beneath; loses any in-memory context that hasn't been encoded as a nav argument; precludes the host's UiState from being the single source of truth.
- **Mode marked only by a banner, no other visual hierarchy change.** The user reads the banner as decoration, not as a state change of the whole surface.
- **Passing `ViewModel` down as a parameter.** kotlin-cmp anti-pattern (§13). The state holder stops at the screen-level composable; pass `uiState` and event lambdas instead.

## 5. Application back to RTP

- **Bug #1 — "chrome still there in add-mode."** Per [[07-map-overview]] (doc02.02.02.07) the regions are deliberately identical in both modes; only the `statusBar` appears in add-mode. The remaining question is whether the *spec is right* (search/filter/menu remain useful in add-mode — a strong argument) or whether more chrome should be suppressed. Decided in the inventory's `appearsInModes` rather than in code. Default position: keep the spec; treat the bug as a look-and-feel issue (banner needs stronger hierarchy).
- **Bug #2 — LocationDraftSheet as a route loses context and unmounts the map.** Per [[05-location-draft]] (doc02.02.02.05) the surface is a sheet over the map. The statechart declares modality `sheet` with host `MapOverview`; the CMP projection is a `LocationDraftSheet` entry on `MapOverviewUiState`, mirroring the `PinSheet` shape. Context flows naturally: the host holds the mode, and the sheet reads it from the same UiState.

## 6. Reference apps audit

Tiered by what is actually useful, given that the open-source CMP ecosystem is thin on pro-grade reference apps. Most "Compose Multiplatform clean architecture" repos on GitHub are tutorial-grade two-screen demos and reinforce patterns already captured in `.claude/skills/kotlin-cmp/SKILL.md` — net-negative to read.

### Tier 1 — pro codebases worth opening

- **`JetBrains/compose-multiplatform/examples`** — the official examples directory. Non-trivial: `imageviewer` (camera, file access, expect/actual done right), `nio` (Now in Android port), Material-3 reference patterns. Canonical "how JetBrains thinks you should write it."
- **`Kotlin/kmp-production-sample`** — JetBrains' shipped-to-stores RSS reader. Redux-shaped state container in shared code, real platform integrations, App Store / Play Store packaging.
- **`chrisbanes/tivi`** — Chris Banes (Google Compose team). Long-running, was Android-only, migrating to KMP/CMP. First-principles architecture; the *why* is in commit messages and posts.
- **`android/nowinandroid`** — Android-only but the most-cited reference. Many CMP teams adopt its layering and modularization as the template; the platform-specific bits are removable.

### Tier 2 — production case studies (closed source, public writeups)

- **JetBrains Toolbox** — 1M+ MAU; desktop-focused; the most candid public writeup of CMP tradeoffs at scale.
- **Wrike** — Calendars, Boards, Dashboards, Charts shipped in CMP across iOS/Android; conference talks on state-holder patterns.
- **Markaz** — 5M+ downloads, 100+ screens entirely CMP, native shims for camera/QR/payments. The cleanest "fully shared UI + native islands" study.
- **Feres** — 1M+ downloads, taxi/map-first; 90% UI shared via CMP, native islands for map and payments. Closest in shape to RankThePlanet — same map-vs-shared-UI seam.
- **Physics Wallah** — 17M MAU; ~20% of the app on KMP/CMP. Evidence for incremental adoption inside an existing app.

### Tier 3 — same-domain (map-first)

- **`maplibre/maplibre-compose` examples** — the only canonical reference for the GeoJSON-source approach, camera-state hoisting, marker click handling in the exact library RTP depends on. Read first when writing new map code.
- **Organic Maps**, **OsmAnd** — not CMP. Useful as production-grade *interaction* reference for map apps: how a serious map app shapes drawers, draft pins, search overlays.

### Tier 4 — skip

The "compose-multiplatform-app", "Compose-Multiplatform-Clean-Architecture", "RecipeApp KMP", "BookPedia-KMP" cluster. Tutorial-grade two-screen demos with stale MVI/Koin boilerplate. Net-negative reading time.

## 7. Why the reference shelf is short

CMP-for-iOS only went stable in Compose Multiplatform 1.8.0 (May 2025). The pro ecosystem is roughly where SwiftUI's was in late 2021: production teams exist, but they have not open-sourced their best work yet. RankThePlanet's `.rhidoc/` corpus is part of the reference work being produced — not a substitute for an external reference shelf, but a recognition that the shelf is shorter than it should be.

## 8. External sources consulted

- API guidelines for components in Jetpack Compose — `android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md`
- Best practices for composition patterns in Jetpack Compose (GetStream) — `getstream.io/blog/composition-pattern-compose/`
- Slot APIs in Compose UI (Chris Banes) — `chrisbanes.me/posts/slotting-in-with-compose-ui/`
- Conditional rendering in Jetpack Compose — Joseph James — `iamjosephmj.medium.com/optimizing-conditional-rendering-in-jetpack-compose-keep-it-simple-or-keep-it-together-6879f9849c15`
- JetBrains: Kotlin and CMP in production — `jetbrains.com/help/kotlin-multiplatform-dev/use-cases-examples.html`
- JetBrains: KMP case studies — `jetbrains.com/help/kotlin-multiplatform-dev/case-studies.html`
- JetBrains Toolbox case study — `blog.jetbrains.com/kotlin/2021/12/compose-multiplatform-toolbox-case-study/`
- Compose Multiplatform 1.9.0 release — `blog.jetbrains.com/kotlin/2025/09/compose-multiplatform-1-9-0-compose-for-web-beta/`
