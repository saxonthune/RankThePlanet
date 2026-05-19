# Look & Feel — Compose Multiplatform

Companion to `SKILL.md`. Read that one for architecture; read this one when the question is **"why does this app look cheap / feel sluggish / disagree with itself screen-to-screen?"** Goal: convert visual intent into a coherent, fast, accessible UI — not produce a moodboard.

## When This Triggers

- Reviews of a screen that "looks bad" without a specific complaint.
- Choosing colors, type, spacing, elevation, corner radii, iconography.
- Animation work: transitions, gestures, list reordering, shared elements, micro-interactions.
- Loading / empty / error states; perceived performance.
- Anything that crosses Android ↔ iOS where the result must feel native-ish on both.

---

## 1. The four pillars

Most "looks bad" diagnoses reduce to one of these. State the failing pillar before proposing a fix.

1. **Consistency** — one type scale, one color scheme, one spacing rhythm, one shape language, one motion language. Anything off-system is **deliberate or wrong**. There is no third option.
2. **Hierarchy** — every screen has exactly one primary thing. Size, weight, color, and isolation cooperate to point at it. If everything is bold, nothing is.
3. **Rhythm** — spacing follows a small numeric scale (4/8/12/16/24/32). Margins, padding, and gaps **sample from the same scale**. Random px values are how apps end up looking off-the-shelf.
4. **Motion serves the user** — animation exists to (a) hide latency, (b) preserve continuity across state changes, (c) confirm causality after input. Decoration is the enemy. If an animation doesn't do one of those three jobs, delete it.

---

## 2. The design system is non-negotiable

Pick Material 3 — it is the only system with first-class Compose support and **built-in WCAG-compliant tonal palettes**. Use a custom system only if you have a designer on the team producing one; otherwise you will reinvent M3 badly.

### Theme shape

```kotlin
@Composable
fun RtpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RtpTypography,
        shapes = RtpShapes,
        content = content,
    )
}
```

- **One** `MaterialTheme` at the root. Nested themes are an anti-pattern; they hide where colors come from.
- Color, typography, shapes are the only three knobs. Resist adding a fourth (`Spacing`, `Elevation` objects) until you have *three independent uses* of the same value — `4.dp` and `Spacing.xs` are the same thing until then.
- Generate the initial palette with the [Material Theme Builder](https://material.io/material-theme-builder) and export Compose code. Don't hand-pick colors from a wheel — tonal palettes encode contrast ratios you will get wrong by eye.

### Reading the theme

```kotlin
// right
Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant)
Surface(shape = MaterialTheme.shapes.medium) { … }

// wrong — hardcoded; breaks dark mode; drifts over time
Text("…", color = Color(0xFF666666))
Surface(shape = RoundedCornerShape(12.dp)) { … }
```

If a composable hardcodes a color, radius, or text size, it is **lying** about being themed. The fix is to read from `MaterialTheme.*`, or to extend the theme.

---

## 3. Color: roles, not paint

M3 colors are **roles**, not swatches. Use the role; the role chooses the swatch.

| Role | Use for |
|---|---|
| `primary` / `onPrimary` | Primary actions (FAB, key buttons) |
| `primaryContainer` / `onPrimaryContainer` | Tinted surfaces that should feel "owned" by primary |
| `secondary` / `onSecondary` | Less-prominent interactive elements (filter chips) |
| `tertiary` / `onTertiary` | Contrasting accents — sparingly |
| `surface` / `onSurface` | Default backgrounds and text |
| `surfaceVariant` / `onSurfaceVariant` | Secondary surfaces, dividers, secondary text |
| `error` / `onError` | Destructive / failure states only |
| `outline` | Borders, dividers |

### Pairing rule

**`onX` always rides on `X`.** `onPrimary` on `primary`, `onSurface` on `surface`, etc. The palette is calibrated for contrast — break the pair and you break accessibility.

```kotlin
// right — contrast guaranteed
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ),
)

// wrong — random pair; fails WCAG on some seeds
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.primaryContainer,
    ),
)
```

### Emphasis

Three tiers; use them in order:

1. **High** — `primary` / `primaryContainer` surface, full-opacity content.
2. **Medium** — `surface` background, `onSurface` content.
3. **Low** — `surfaceVariant` background, `onSurfaceVariant` content.

Don't fake low emphasis with `Color.Gray` or `alpha = 0.6f`. There is a role for it.

### Dark mode is a separate palette, not a tint

`DarkColors` is its own `ColorScheme`, not `LightColors` with reduced alpha. M3 dark mode uses elevation-tinted surfaces (`tonalElevation`) instead of stacking shadows on a near-black surface. If your dark mode looks "muddy," you're probably using shadows when you should be using `tonalElevation`.

---

## 4. Typography: pick five styles, use those

M3 ships 15 styles. **You will use 5–7.** A typical app:

- `headlineMedium` or `headlineSmall` — screen titles
- `titleMedium` — section headers, list-item titles
- `bodyLarge` — primary content
- `bodyMedium` — secondary content
- `labelLarge` — buttons, tabs
- `labelSmall` — captions, metadata

Rules:

- **Never set `fontSize` inline** in a `Text(...)`. It bypasses the scale and guarantees inconsistency. Use a `style = MaterialTheme.typography.X` and customize via `.copy(...)` if needed.
- **No more than three weights** in the whole app (e.g., 400 / 500 / 700). Variable fonts make more weights tempting; resist.
- **Line height matters more than font size** for legibility. M3 defaults are calibrated — don't override unless you have a reason.
- **Letter-spacing** is set per-style in M3. Leave it alone.
- **System font scaling**: users can set 200% font. Test at 130% and 200%. Layouts that overflow at scale are bugs, not "edge cases."

---

## 5. Spacing, shape, elevation

### Spacing

Use a **4dp grid**. The legal values are `4, 8, 12, 16, 24, 32, 48, 64`. Anything else (`14.dp`, `18.dp`) is wrong unless it's a measured optical correction.

Most common patterns:

- 16dp — screen edge padding (default)
- 8dp — within-component padding, gap between related items
- 24dp — between unrelated sections
- 4dp — fine-grained chip-internal spacing

### Shape

M3 shape scale: `extraSmall (4)`, `small (8)`, `medium (12)`, `large (16)`, `extraLarge (28)`. Pick one for cards, one for buttons, one for sheets, and stop. Mixing seven radii looks unprofessional even when each is fine in isolation.

### Elevation

M3 prefers **tonal elevation** (color shift) over **shadow elevation**. Shadows on iOS look heavier than on Android — tonal elevation reads identically across platforms.

```kotlin
Surface(tonalElevation = 3.dp) { … }     // preferred — color shift
Surface(shadowElevation = 3.dp) { … }    // only when you specifically need a cast shadow
```

Flat ≠ low-quality. M3 Expressive trends *lower* elevation; rely on color, spacing, and motion for hierarchy.

---

## 6. Motion: a small vocabulary, used carefully

### When to animate

- **State change** — content swaps in/out (`AnimatedContent`, `Crossfade`).
- **Continuity** — the same conceptual object moves to a new location (shared element, list reorder).
- **Causality** — input → consequence (press ripple, drag follow).
- **Latency cover** — the animation runs *while* work happens, not after.

### When not to animate

- To make a thin UI feel "richer."
- On scroll-bound positions (unless you're driving them off `scrollProvider()` in the draw phase).
- On every recomposition (a tween that retriggers on each state read is a bug).
- After the user is already looking at the result.

### Duration vocabulary

| Token | ms | Use for |
|---|---|---|
| short | 100–200 | Selection, ripple, micro-feedback |
| medium | 250–400 | Standard transitions, content swaps, FAB→sheet |
| long | 450–600 | Full-screen transitions, large shared elements |

Anything **>600ms is a bug** outside of intentionally cinematic moments. Anything **<80ms** isn't an animation, just a flash — either remove it or extend it.

### Easing / springs

- M3 standard easing: `FastOutSlowInEasing` for most enter/exit pairs.
- **Prefer springs** for interruptible motion (gestures, draggables). `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)` is the M3 Expressive default for primary motion.
- Linear easing belongs to **looping/ambient** motion only (progress indicators, ticker scrolls).

### API decision tree

| Need | API |
|---|---|
| Show/hide a single composable | `AnimatedVisibility` |
| Cross-fade between content shapes | `AnimatedContent` |
| Tween one prop on a single composable | `animateFloatAsState` / `animateDpAsState` / `animateColorAsState` |
| Coordinate multiple props on the same state change | `updateTransition` |
| Sequential or gesture-driven control | `Animatable` + `LaunchedEffect` / `rememberCoroutineScope` |
| Continuous ambient motion | `rememberInfiniteTransition` |
| Shared element across nav | `SharedTransitionLayout` + `Modifier.sharedElement(...)` (Compose 1.7+) |

### Performance rules for animation

Animations are the place where the recomposition model bites hardest.

- **Animate in the draw phase**, not the composition phase. Use the **lambda modifier** form:

  ```kotlin
  // wrong — recomposes every frame
  Box(Modifier.offset(x = animX.dp, y = animY.dp))

  // right — skips composition; only redraws
  Box(Modifier.offset { IntOffset(animX.toInt(), animY.toInt()) })
  ```

- For animated colors on large surfaces, prefer `drawBehind { drawRect(color) }` over `.background(color)` — same reason.
- `Modifier.graphicsLayer { … }` is the canonical "animate on the GPU compositor" tool. Translate, scale, rotate, and alpha go here. It runs in the draw phase and is GPU-accelerated.
- `derivedStateOf` for animation triggers driven by scroll/text/etc. — only recompose when the *boolean* flips, not on every pixel.
- **60fps is the floor.** A janky animation is worse than no animation; if you cannot hold frame rate, simplify or remove.
- Never start expensive work (image decode, network) **on tap**. Start the animation first; let the work finish during it. The animation *is* the loading state.

### List motion

- `LazyColumn` / `LazyRow` with **stable `key = { it.id }`** — without it, reorder animates wrong and recomposition explodes.
- `Modifier.animateItem()` for list reorder/insert/remove. Apply it inside the item composable; do not animate the list itself.

---

## 7. Navigation chrome — one tree, native feel on both platforms

Android and iOS agree on the physical layout of navigation, even though the chrome differs. **You can build one Compose tree that reads natively on both** by picking M3 components whose affordances match iOS expectations — not by forking per platform.

### The cross-platform consensus

| Concern | Android (M3) | iOS (HIG) | Verdict |
|---|---|---|---|
| Back button | Top-left, `navigationIcon` | Top-left, labelled with parent | **Top-left, both.** |
| Screen title | Top, in app bar | Top (inline or large) | **Top app bar.** |
| 1–3 screen actions | `actions` slot, top-right | Top-right of nav bar | **Top-right.** |
| Recurring "create" action | FAB bottom-end | Bottom toolbar / top-right | **FAB bottom-end.** |
| 3–5 peer sections | `NavigationBar` bottom | `TabBar` bottom | **Bottom bar.** |
| Filters, settings, secondary nav | Drawer | Drawer / sheet | **Drawer.** |
| Back gesture | System back button | Edge-swipe from start | **Free in CMP** — don't reimplement. |

iOS users read M3 `TopAppBar` as "navigation bar" and `NavigationBar` as "tab bar"; the affordances are identical. Theme it, don't fork it (§9).

### Fitts's law in one hand

The thumb-reachable zone on a phone is an arc — bottom-end is closest, top-end is farthest. Two consequences:

- **Frequent → bottom.** Tab switching, the primary "add/create" action, the dominant CTA.
- **Infrequent or destructive → top.** Back, settings, overflow, drawer.
- **Targets ≥48dp** (M3) / ≥44pt (HIG). Use `IconButton`; never bare clickable `Icon`s. Bare text labels for nav are an anti-pattern — they're hard to hit and they look like body content (§11).
- **Edge-flush beats edge-near.** The bezel stops the thumb; a button at the screen edge is easier to hit than one inset 8dp.

### Screen taxonomy

Three categories, three chrome patterns. Pick one per screen.

**Root.** No back button. Drawer for secondary nav. FAB or bottom bar for primary action. Search bar (if any) top.

**List.** `CenterAlignedTopAppBar` with `navigationIcon = Back`, title, optional one-icon `actions`. FAB bottom-end for "new {thing}." Overflow menu for rare actions (import, export). Daily actions never live in a top-bar text button.

**Detail / form.** `TopAppBar` with `navigationIcon = Back`, screen-specific title, zero-to-one action. For forms with save: either bottom-paired buttons (Cancel outlined-left, Save filled-right) **or** top-bar Cancel-left + Save-right (iOS modal pattern). Pick one, apply uniformly.

### The canonical drill-down scaffold

```kotlin
@Composable
fun RtpDrillDownScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = actions,
            )
        },
        floatingActionButton = fab,
        bottomBar = bottomBar,
        content = content,
    )
}
```

One helper, every drill-down screen, identical chrome. `Icons.AutoMirrored.Filled.ArrowBack` mirrors for RTL and reads correctly on iOS — do **not** fork to a chevron via `expect`/`actual`.

### Back, system back, and the swipe-back gesture

In CMP, the Android system back button and the iOS left-edge swipe both pop the nav stack **for free**. Do not add a `BackHandler` unless you specifically need to intercept (unsaved-changes dialog, multi-step form with internal steps). A `BackHandler { onBack() }` mirroring the default behaviour is dead code and risks suppressing the swipe gesture.

For a modal screen (sheet, full-screen editor), the convention is "Cancel" top-left + "Save"/"Done" top-right — no back arrow. The semantic is *dismiss*, not *go up the hierarchy*.

### When to add a bottom `NavigationBar`

Only when you have **3+ peer top-level destinations** users switch between repeatedly. With one or two, a bottom bar is ceremony and visual noise. The drawer + FAB pattern handles single-root apps better.

If/when you do add tabs: `NavigationBar` + `NavigationBarItem`. The selected item must **never** change programmatically (HIG); it represents the user's stated location.

### Map-as-root: a special case

When the content is a map (or any full-bleed canvas), chrome competes with content for the same pixels. Two rules:

- **Top chrome must be transparent or scrim-light.** `TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))`, sitting over the map. The drawer trigger and search live here.
- **Bottom chrome must be a labelled, anchored bar — not a floating icon.** `BottomAppBar` with one or two labelled actions and the FAB attached. A lone `FilledIconButton` floating at screen-bottom is the smell that says "the designer gave up."

The FAB on a map screen is the primary creation action ("Drop pin here", "Add this location"). The bottom bar holds navigation away from the map ("Collections," "Profile") if any.

### The anti-pattern this section exists to kill

```kotlin
// wrong — three TextButtons in a Row pretending to be a top bar
Row {
    Text("Collections", Modifier.weight(1f))
    TextButton(onClick = onNew) { Text("New") }
    TextButton(onClick = onImport) { Text("Import") }
    TextButton(onClick = onBack) { Text("Back") }
}
```

Every problem with this is structural, not cosmetic:

- "Back" is top-right (hardest reach) and indistinguishable from "New."
- "New" is a daily action sitting in the worst Fitts zone, drawn the smallest possible way.
- "Import" is a rare action given equal prominence to the daily one.
- Nothing has a 48dp target.
- iOS users read it as nothing in particular; Android users read it as a developer placeholder.

Replace with: `TopAppBar(navigationIcon = Back, title = "Collections", actions = overflow)` + `FloatingActionButton(onClick = onNew)`. Import moves to the overflow or to Settings.

---

## 8. Loading, empty, error — the three states that get skipped

The "looks bad" verdict often comes from these, not from the happy path.

- **Loading.** Below ~400ms, show nothing — a flash of skeleton is worse than a moment of stillness. Above ~400ms, show **skeleton placeholders shaped like the real content**, not a spinner. Above ~3s, show progress + cancellation.
- **Empty.** An empty list with a single line of body text is bad. Provide: a one-line explanation, an illustrative icon, and a primary action that resolves the emptiness ("Add your first place").
- **Error.** Never `Toast` an error and call it done. The empty area becomes an error state with: cause (human-readable), suggested action (retry button or instructions), and — if recoverable — auto-retry on reconnect.

These states are **first-class UI**, not afterthoughts. They go in the screen's `UiState` sealed hierarchy (see `SKILL.md` §3).

---

## 9. Accessibility is part of look-and-feel

If it fails accessibility, it looks bad — to someone.

- **Touch targets ≥ 48dp.** Use `Modifier.minimumInteractiveComponentSize()` or wrap clickable icons in a `Box(Modifier.size(48.dp), contentAlignment = Center)`. A 24dp icon with a 24dp tap area is a bug.
- **Content descriptions** on every icon-only button. `null` is correct only for purely decorative icons; "" is never correct.
- **Semantics merging.** Use `Modifier.semantics(mergeDescendants = true) { … }` on a clickable card so screen readers announce one item, not five.
- **Color is never the only signal.** A red border *and* an error message; a checkmark *and* "Selected."
- **Test with system font at 200% and TalkBack/VoiceOver.** The bugs hide there.

---

## 10. Platform feel: Android vs iOS via the same Compose tree

CMP renders the same Compose UI on both platforms. M3 is Android's native idiom; on iOS it looks **slightly foreign** by default. Two acceptable strategies:

1. **One look everywhere.** Lean into M3 as the brand. Acceptable when the app's identity outweighs platform conventions (most consumer apps).
2. **Platform-tinted same-tree.** Same Compose tree, but expose a few theme knobs (corner radius, font, button style) that vary per platform. Achieved via `expect val PlatformTheme: ThemeOverrides` — keep it small (typography family, default corner, switch style).

Do not fork screens per platform. A divergent screen tree is a maintenance grave.

What to actually adjust on iOS:

- Back gesture / swipe-back animation feel.
- Default font (iOS feels off without SF; use the system font, not Roboto).
- Bottom sheet drag affordance and dismissal physics.
- Switch styling (Material switch reads loud on iOS).

---

## 11. Diagnosing a screen that "looks bad"

Run this checklist top-to-bottom. The first failure is usually *the* failure.

1. **Hardcoded values?** Grep the file for `Color(0x`, `.sp`, raw `.dp` outside the spacing scale. Each one is a tear in the system.
2. **Multiple competing primaries?** Count the "loud" elements (filled buttons, FABs, brand-color blocks). >1 → demote some to outlined/text.
3. **Mixed corner radii?** List every shape in the screen. >2 → consolidate.
4. **Spacing off-grid?** Are paddings drawn from `{4, 8, 12, 16, 24}`? If `13.dp` shows up, that's the smell.
5. **Type using >5 styles?** If yes, you're not on a scale.
6. **All shadows, no tonal elevation?** Switch to tonal; recheck contrast.
7. **Animations on layout properties?** Convert to `graphicsLayer` / lambda modifiers.
8. **No loading/empty/error states?** Add them — they're 30% of perceived quality.
9. **Tap targets < 48dp anywhere?** Fix before anything else.
10. **Looks fine at 100% font, breaks at 150%?** Layout is brittle — replace fixed sizes with `weight`, `wrapContentHeight()`, `BasicTextField` with auto-resize.
11. **Navigation is bare `TextButton`s in a `Row`?** That's §7's anti-pattern. Replace with `Scaffold` + `TopAppBar` + FAB.
12. **Back button lives anywhere but top-left?** It's wrong on both platforms. Fix before anything else — users won't find it.
13. **Daily creation action in a top-bar `TextButton`?** Move to a FAB. The bottom-end is the thumb's resting place; that's where "add" belongs.

---

## 12. Anti-patterns — name them, refuse them

- **Inline `Color(0xFF…)`** in a screen composable. Color belongs in the scheme.
- **Inline `fontSize = 14.sp`** on a `Text`. Style belongs in `Typography`.
- **`alpha = 0.6f`** to imply disabled or secondary state. There's a role (`onSurfaceVariant`, `disabled` color tokens).
- **Spinners > 400ms.** Use a skeleton; a spinner is admitting defeat.
- **Toasts as the error UI.** Errors belong in the screen, with a recovery path.
- **`Modifier.padding(13.dp)`** off-grid. Snap to the scale.
- **Multiple `MaterialTheme {}`** nested inside a screen. One root only.
- **`.background(animatedColor)`** on a large surface in an animation. Use `drawBehind`.
- **`Modifier.offset(x.dp, y.dp)`** in an animation. Use the lambda form.
- **`AnimatedVisibility` wrapping a tiny opacity tween** that could be `animateFloatAsState`. Match the API to the job.
- **`Spring.StiffnessHigh`** on a full-screen transition. Reserve for snappy micro-interactions.
- **Icons in 24dp `IconButton` with no `contentDescription`.** Either describe it or mark it decorative.
- **Linear easing on enter/exit.** Reserved for loops.
- **Animating to mask jank** (e.g., a fade-in that hides a slow render). Fix the render.
- **Mixing M2 and M3 imports** (`androidx.compose.material.*` vs `androidx.compose.material3.*`). All M3, no exceptions.
- **Per-platform screen forks** ("`IosListScreen` / `AndroidListScreen`"). Theme it, don't fork it.
- **`TextButton`s in a `Row` masquerading as a top bar.** Use `TopAppBar`. See §7.
- **Back button in the top-right** (or anywhere but top-left). Wrong on both Android and iOS.
- **Daily "create" action in a top-bar text button.** Move it to a FAB. See §7 Fitts.
- **`BackHandler { onBack() }` mirroring the default.** Dead code; risks suppressing the iOS swipe-back gesture. Only intercept when you have a real reason (unsaved changes, etc.).
- **`expect`/`actual` for a back-arrow chevron** to "feel iOS-y." `Icons.AutoMirrored.Filled.ArrowBack` reads correctly on both platforms.
- **A `NavigationBar` with fewer than 3 destinations.** Ceremony. Use a drawer or root-level navigation instead.
- **A `NavigationBar`'s selected tab changing programmatically.** Violates HIG; disorients users.

---

## 13. RTP specifics

- **Map** is the load-bearing visual. Everything else is chrome around it. The chrome's job is to **stay out of the way** — minimal scrim, light tonal elevation, no shadows over the map.
- **Pin styling** lives in MapLibre style JSON (symbol/circle layer paint properties), not in Compose. Do not try to overlay Compose pins on the map — see `SKILL.md` §11.
- **Bottom sheets** (entry drawer, location detail) are the primary UI surface other than the map. Use M3 `ModalBottomSheet` with a drag handle; respect insets (`WindowInsets.safeContent`).
- **Color**: the map basemap defines the dominant palette of the screen. The app's `primary` should *contrast* with the basemap, not match it. A green basemap + green pins + green FAB = visual collapse.
- **Type**: place names appear on the map (rendered by MapLibre) *and* in lists/sheets (rendered by Compose). Match the font where possible to avoid "two apps in one."
- **Motion**: sheet expand/collapse is the most-used animation in the app. Spring physics, interruptible, drag-driven. Get this right before anything else.

---

## 14. Verifying claims before pinning them

Design tokens and APIs drift. Before quoting a specific easing curve, duration token, or M3 API, verify against:

- M3 motion: https://m3.material.io/styles/motion/overview/how-it-works
- M3 Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose animation guide: https://developer.android.com/develop/ui/compose/animation/quick-guide
- Compose performance best practices: https://developer.android.com/develop/ui/compose/performance/bestpractices
- M3 type scale: https://m3.material.io/styles/typography/overview
- Material Theme Builder: https://material.io/material-theme-builder

The mental models above are stable. The exact spring constants, default durations per token, and "is this API experimental" answers are not.
