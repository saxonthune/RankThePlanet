# kotlin-cmp

Mental models for Kotlin + Compose Multiplatform. Use this when shaping architecture, naming layers, choosing state-holder patterns, or writing CMP code in headless sessions. The goal is **convert user intent into a sturdy, refactorable architecture** — not produce a cheatsheet.

## When This Triggers
- Architecture conversations about screens, modules, layers, or where logic should live.
- "How should I structure X" / "where should this state live" / "is this a ViewModel".
- Writing or reviewing `@Composable` functions, state holders, navigation, side effects.
- Adding a new platform (`expect`/`actual`), a new source set, or a new platform-specific dependency.
- Refactoring code that re-renders too often, leaks coroutines, or is hard to test off-Compose.
- Slash command: `/kotlin-cmp`.

## Read Order
This file is dense. For an architecture conversation, read top-to-bottom once. For a coding task, jump to the section whose heading matches the question — the cross-references in `[brackets]` point to the section you also need.

For visual-design questions (theming, color, typography, spacing, animation, "looks bad"), read the sibling file `LOOK-AND-FEEL.md` instead — this file is architecture-only.

---

## 1. Two pillars

Almost every design question in this project reduces to one of two pillars. State them out loud when the conversation drifts:

1. **Unidirectional Data Flow (UDF).** State flows down (immutable, into composables). Events flow up (lambdas, into the holder). The UI is a pure function of state.
2. **Composition over inheritance, hoisting over ownership.** A composable should not own state it does not need to own. Push state up to the lowest common ancestor that needs it; pass it down as a value + a callback.

If a proposal violates either pillar, name the pillar it violates before debating the proposal. Most "should this be a ViewModel" / "where does X live" arguments dissolve once you ask: *what's the state, who owns it, and who needs to react to it?*

---

## 2. The composition mental model

A `@Composable` function is **not a render call**. It is a description of what should be on screen *given its inputs*. The Compose runtime decides when to call it, how often, and in what order.

You must treat composables as if they:
- Run frequently (every state change in their scope).
- Run in **any order** with siblings.
- Run in **parallel** with siblings (on some platforms / configurations).
- Can be **skipped** if inputs are equal to the previous call.
- Can be **restarted mid-flight** if a read invalidates them.
- Run **off the UI thread** for layout/measure phases — but the composition phase runs on the UI thread.

Therefore composables must be:
- **Fast** — no I/O, no heavy compute. Push that into a holder, expose result as state.
- **Idempotent** — same inputs → same UI description.
- **Side-effect free** in the body — all side effects go through the side-effect APIs [§7].

If a composable body contains a `mutableStateOf` without `remember`, an `if` that mutates a captured `var`, or a direct call to a suspend function — it's wrong, regardless of whether it appears to work.

### Recomposition is not re-rendering
Recomposition re-runs the composable function, builds a new slot table diff, and the runtime applies only the changes. There is no "render". This is why **stability** matters [§9]: if inputs are reference-equal-stable, the runtime skips the call entirely.

---

## 3. State: where it lives, what shape it takes

There are exactly two kinds of state in a CMP app, and they have different homes:

### UI state — owned by composables
- Scroll position, expanded/collapsed, hover, text-field cursor, focus.
- Lives in `remember { mutableStateOf(...) }` or hoisted to the nearest common parent.
- Use `rememberSaveable` if it must survive configuration changes / process death and is trivially serializable.

### Screen / domain state — owned by a state holder (usually `ViewModel`)
- Anything derived from data, network, persistence, or domain logic.
- Exposed as a single `StateFlow<UiState>` where `UiState` is an **immutable data class** (or a sealed hierarchy if the screen has fundamentally different shapes — Loading / Content / Error).
- Mutated only through methods on the holder (the "events flow up" half of UDF).

**Rule:** the holder must never expose `mutableStateOf` to the UI. Expose `StateFlow`. The reason is testability — `StateFlow` is a plain Kotlin construct testable off-Compose; `mutableStateOf` requires a Compose test harness.

### UiState shape
```kotlin
data class ListUiState(
    val items: ImmutableList<Place> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
```
- One screen, one `UiState`. Resist proliferation.
- Prefer **flat** over nested. Two booleans `isLoading` + `error != null` are usually clearer than a `sealed Status`.
- Use a sealed hierarchy only when shapes are *mutually exclusive and the UI tree truly differs* (e.g., a list screen vs. an empty-state illustration vs. a permission-denied screen).

### Collecting state in composables
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```
`collectAsStateWithLifecycle` pauses collection when the host is not at least STARTED, which avoids work for off-screen tabs. Use it as the default. `collectAsState` is the fallback when lifecycle-aware collection is not needed.

---

## 4. The state holder: ViewModel vs. plain class

`androidx.lifecycle.ViewModel` is multiplatform. Use it whenever the state needs to **survive across the lifetime of its owner** (a screen, a nav destination) and you want a `viewModelScope` that auto-cancels.

Use a **plain state holder** (a regular class) only for UI-only state with no async work and no need to outlive a single composition (e.g., a complex form state object that's just bookkeeping).

### Canonical holder shape
```kotlin
class ListViewModel(
    private val repo: PlaceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repo.list() }
                .onSuccess { items -> _uiState.update { it.copy(isLoading = false, items = items.toImmutableList()) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```
Notice:
- `_uiState.update { it.copy(...) }` — atomic, lock-free, the only safe mutation pattern.
- `runCatching` — local error capture, not a try/catch ladder.
- One method per event. The UI never reaches into internals.

### Obtaining a ViewModel in CMP
On non-JVM platforms, the no-arg `viewModel()` will not find your class. **Always pass an initializer lambda:**
```kotlin
val vm: ListViewModel = viewModel { ListViewModel(repo = appGraph.placeRepo) }
```
A DI factory works too. The point is: assume reflection-based instantiation is not available.

### Events the holder cannot model as state
One-shot events (navigate, show snackbar, share intent) do not belong in `UiState` — they would replay on every state read. Use a `Channel<UiEvent>` exposed as `receiveAsFlow()`, or a `SharedFlow` with `replay = 0`. Collect once in the screen and route to a navigator/snackbar host.

---

## 5. Architecture layers

Three layers, three responsibilities. Don't add a fourth until the third becomes painful.

```
ui  ─── composables, screens, theme, nav graph         (depends on: state)
state  ─ ViewModels / state holders, UiState models     (depends on: domain)
domain ─ repositories, use cases (if needed), DTO→model (depends on: data)
data ─── DB, network, files, platform APIs              (depends on: nothing)
```

Rules of thumb:
- **Data flows up, dependencies flow down.** Higher layers know about lower layers; lower layers know nothing of higher.
- **Domain models are pure Kotlin in `commonMain`.** No Compose imports, no platform types.
- A "use case" is only worth introducing when **multiple ViewModels share the same orchestration** — otherwise it's a method on the repository and the layer is wasted ceremony.
- Repositories return domain types and `Result`/`Flow`, never `Response<T>` from the network layer.

### Where Compose stops
Compose is a UI library, not an architecture. Nothing below the `ui` layer should import from `androidx.compose.*`. The instant a repository imports `State` or `mutableStateOf`, the architecture has broken.

### Module boundaries
Start with a single module. Split a module out only when you need to enforce a boundary the compiler cannot otherwise enforce (e.g., to prevent `data` from importing `ui`). Premature modularization buys compile-time pain for no architectural benefit on a small app.

---

## 6. Source set hierarchy & `expect`/`actual`

Targets: `androidTarget`, `iosX64`, `iosArm64`, `iosSimulatorArm64`. The Kotlin **default hierarchy template** then auto-creates:

```
commonMain
  └── mobileMain        (Android + iOS shared; auto)
        ├── androidMain
        └── appleMain
              └── iosMain
                    ├── iosX64Main
                    ├── iosArm64Main
                    └── iosSimulatorArm64Main
```

Do **not** hand-roll intermediate source sets. Lean on the template.

### Where code goes
- **`commonMain`** is the default home. Push everything here first; move it down only when forced.
- **`mobileMain`** — code shared between Android and iOS but not desktop/web (e.g., when you add desktop later, mobile-only code stays here instead of polluting `commonMain`).
- **`androidMain` / `iosMain`** — only the parts that *cannot* be common: platform APIs, file paths, native interop.

### `expect`/`actual` discipline
- `expect` declares a contract: the signature, nothing else.
- The `actual` implementation lives at the most specific source set that needs to diverge — often `androidMain` and `iosMain`, but if both platforms can share the implementation, put a single `actual` in `mobileMain`.
- **Prefer interfaces + DI over `expect`/`actual` for anything non-trivial.** `expect`/`actual` is great for tiny platform shims (current time, file path, secure storage) and painful for everything else. For a feature with branching logic, declare an `interface PlatformFoo` in common, instantiate the right impl per platform at the app entry point, inject it.

---

## 7. Side effects: which API for which job

These are the only legitimate ways for a composable to cause something to happen outside the slot table.

| API | Use when | Restart on |
|---|---|---|
| `LaunchedEffect(keyN…)` | Run suspend work tied to the composition | Any key changes |
| `rememberCoroutineScope()` | Launch from event handlers (`onClick`) | Never (scope tied to composition) |
| `DisposableEffect(keyN…)` | Subscribe + must clean up (`onDispose`) | Any key changes |
| `rememberUpdatedState(v)` | Capture the latest value inside a long-lived effect without restarting it | Reference does, effect doesn't |
| `produceState(initial, keyN…)` | Convert an async source into `State<T>` | Any key changes |
| `derivedStateOf { … }` | Cache a derived value; recompose only when output changes | Reads inside |
| `snapshotFlow { … }` | Bridge Compose state → cold Flow | Reads inside |
| `SideEffect { … }` | Publish Compose state to a non-Compose API after every successful composition | Every composition |

### Mental model
- **Keys are the dependency array.** Too few → stale closures. Too many → thrashing restarts. If you'd use `Unit`, ask whether `rememberUpdatedState` is what you actually want.
- **Effects belong to the composition lifecycle.** They start when the composable enters, cancel/dispose when it leaves. They are not free — don't spawn them in tight loops.
- **One-shot navigation/snackbar is not a `LaunchedEffect(Unit)` pattern.** Collect a `Flow` of events from the holder in a `LaunchedEffect` keyed on the holder, and route each event. Never put navigation in `UiState`.

### `derivedStateOf` rule
Only worth its overhead when **inputs change more often than the derived output**. If `screenWidthDp.value > 600` flips on rotation but the inputs change every frame, it's a win. If inputs change once per minute, it's wasted indirection — just compute it.

---

## 8. Navigation

Use **JetBrains `navigation-compose` multiplatform** (the official port of AndroidX Navigation Compose). Not Decompose, not Voyager — neither is JB-endorsed for CMP and switching later is expensive.

### Type-safe routes (modern style)
```kotlin
@Serializable data object ListRoute
@Serializable data class DetailRoute(val placeId: String)

NavHost(navController, startDestination = ListRoute) {
    composable<ListRoute> { ListScreen(onPlaceClick = { id -> navController.navigate(DetailRoute(id)) }) }
    composable<DetailRoute> { entry ->
        val route: DetailRoute = entry.toRoute()
        DetailScreen(placeId = route.placeId)
    }
}
```
- Routes are `@Serializable` Kotlin objects/classes.
- Args are typed, not stringly-typed.
- Add `kotlinx.serialization` to `commonMain`.

### NavController is not a state holder
Never inject `navController` into a ViewModel. Expose navigation events from the holder; have the screen translate events into `navController.navigate(...)`. This keeps holders pure Kotlin and testable.

---

## 9. Stability & recomposition performance

The Compose compiler tags each composable as **skippable** (skipped if all params are equal to the prior call) and/or **restartable**. To be skippable, every parameter must be **stable**.

### What's stable
- Primitives, `String`.
- `data class` with `val`s of stable types.
- Compose snapshot types (`MutableState`, `SnapshotStateList`, `SnapshotStateMap`).
- Types annotated `@Stable` or `@Immutable`.
- Lambdas capturing only stable values.

### What's unstable (kills skipping)
- `var` properties (the compiler must assume they change).
- Standard `List`, `Set`, `Map` interfaces — **even if backed by an immutable impl**. The compiler can't prove it.
- Types from modules without the Compose compiler plugin.

### Fixes
- For collections in UI params, use `kotlinx.collections.immutable` (`ImmutableList`, `PersistentList`). This is the canonical fix for the most common cause of unnecessary recomposition.
- Annotate wrappers around third-party data classes with `@Immutable` when you know their fields don't change.
- Hoist `var` into `MutableState`.

### Strong skipping
Modern Compose compiler has **strong skipping**: it skips composables with unstable params if the params are **reference-equal** to the prior call. This forgives a lot of sins. Don't rely on it — write stable params anyway — but know that a single unstable param doesn't necessarily destroy performance.

### Diagnose
Enable the Compose compiler stability report. If a composable is hot in the profiler and not skipping, the report will name the culprit param.

---

## 10. Kotlin idioms that matter

Just the ones that materially shape architecture.

### Coroutines & structured concurrency
- A coroutine **always runs in a scope**. The scope dictates lifetime. In a ViewModel: `viewModelScope`. In a composable: `rememberCoroutineScope()` or a `LaunchedEffect`. In a platform service: an app-scoped `CoroutineScope(SupervisorJob() + Dispatchers.Default)` injected via DI.
- **Cancellation is cooperative.** Long-running CPU work must check `isActive` or call `yield()`. Suspend functions handle cancellation automatically.
- **`Dispatchers.Main.immediate` vs `Dispatchers.Main`.** Use `.immediate` when already on Main to avoid re-dispatch. CMP Desktop requires `kotlinx-coroutines-swing` for either to exist.
- Never `runBlocking` outside tests or the very top of `main()`. Never `GlobalScope.launch` — it is a leak by design.

### Flow taxonomy
- **`Flow<T>`** — cold. Re-runs the producer for every collector. Use for streams of values from data sources.
- **`StateFlow<T>`** — hot, conflated, has a current value. Use for UI state.
- **`SharedFlow<T>`** — hot, multicast, replay configurable. Use for events with no current value (one-shot, fan-out).
- The bridge: `flow.stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)`. The 5-second timeout is the standard "survive a config change" idiom — keeps upstream alive briefly after the last collector leaves so we don't restart on rotation.

### Sealed hierarchies for modeling
- `sealed interface` is preferred over `sealed class` unless you need state on the base.
- Use it for **closed sets** that the compiler should enforce exhaustively: results (`Loading | Success | Error`), events, navigation actions.
- When you `when` over a sealed type, **don't add an `else` branch** — that's the whole point. Let the compiler tell you when you add a new case.

### Result modeling
- Use `kotlin.Result<T>` or a custom `sealed interface Outcome<out T> { data class Ok<T>(val value: T) : Outcome<T>; data class Err(val cause: Throwable) : Outcome<Nothing> }` at layer boundaries.
- **Do not throw across architectural boundaries.** Repositories return `Result`; ViewModels translate to `UiState`.
- Inside a function, exceptions are fine and idiomatic. Across boundaries, model the failure.

### Immutability
- Default to `val`. A `var` is a code smell to be justified.
- Default to `data class` with `val` fields. Mutate via `.copy(...)`.
- `data object` for singletons that participate in `when`.

### DI
- A simple manual DI graph (one `class AppGraph(...)` constructed at the platform entry point) is sufficient for an app this size. Koin is fine but it is **runtime DI** — errors surface at first access, not at compile time. If you adopt Koin, do it once and globally; don't half-adopt.

---

## 11. Project context: RankThePlanet

### Map rendering: maplibre-compose
This is the load-bearing dependency. It is a Compose-first wrapper around MapLibre Native.

- The map is a **composable** (`MaplibreMap { ... }` or similar — confirm exact composable name from the current API reference at https://maplibre.org/maplibre-compose/ before writing snippets).
- Pins are a **GeoJSON source + symbol/circle layer**, not per-pin views. This is the single most important performance rule for the map: never spawn N composables for N pins.
- Camera is hoisted via a `CameraState` (`rememberCameraState`-style) — treat it as you would `rememberLazyListState`: state lives in the screen / holder, the map reads it.
- Platform parity: Android and iOS are well-supported; Desktop and Web are not. Do not depend on map functionality in `commonMain` for the desktop/web split until the wrapper catches up.

### Architectural decisions already made
- Compose Multiplatform is the framework. (`.carta/02-design/01-architecture.md` → doc02.01)
- Android + iOS first; desktop/web later, gated on `maplibre-compose` parity.
- Tiles: Protomaps PMTiles, bundled low-zoom basemap.
- Persistence: **undecided** (SQLCipher vs. zipped bundle). Don't assume a DB layer exists when shaping new code; design the repository interface against the domain, let the persistence choice plug in behind it.

### Conventions for this repo
- All design docs live under `.carta/`. Read `.carta/MANIFEST.md` before architecture work.
- Domain models in `commonMain`, no Compose imports.
- One `UiState` per screen, exposed as `StateFlow` from a `ViewModel`.
- `expect`/`actual` reserved for tiny shims; everything bigger goes through an `interface` + DI.

### Verifying Kotlin changes off macOS
The iOS Kotlin/Native targets are disabled when not on macOS, because MapLibre cinterop needs macOS. `:composeApp:compileKotlinJvm` is **not** a sufficient check there — it only sees the jvm source set. Run `make verify` (= `:composeApp:compileDebugKotlinAndroid` + `:composeApp:compileCommonMainKotlinMetadata`); together they enforce commonMain strictly and catch the failures that would otherwise blow up at iOS compile time. The remaining iOS-only risk (`iosMain`, cinterop, cocoapods) can only be checked on macOS.

---

## 12. Heuristics for architecture conversations

When the user proposes a design, run the checklist:

1. **Where does state live?** Name the holder. If you can't, the design is incomplete.
2. **Is state immutable on the way down?** If a composable receives a `MutableState` parameter, you've leaked the holder.
3. **Are events lambdas going up?** If a composable calls a ViewModel method directly through a captured reference, the screen and the holder are coupled in the wrong direction.
4. **Is there exactly one `UiState` per screen?** Multiple parallel `StateFlow`s on a holder are a code smell — they desync.
5. **Is anything that imports Compose below the `ui` layer?** That's the architectural smoke alarm.
6. **Does a ViewModel know about `navController` or `Context`?** Both are wrong. Hoist them.
7. **Is `expect`/`actual` being used for branching logic?** Replace with an interface + per-platform impl injected at the entry point.
8. **Is a list of N things rendered as N composables, when N can be large?** Use `LazyColumn`/`LazyRow` with a stable `key`. For map markers, use a GeoJSON source.

### "Sturdy & refactorable" — what that means concretely
- **Sturdy:** the domain model has no Compose, no platform, no I/O. It is a pure Kotlin description of the problem. The compiler enforces this by source-set placement.
- **Refactorable:** because state holders take dependencies through constructors, because UI is a function of `UiState`, because events are explicit method calls — you can rewrite any one layer without touching the others. The test of refactorability is: *could I swap the persistence backend without changing a single composable?* If yes, the architecture is doing its job.

---

## 13. Anti-patterns — name them, refuse them

- **Passing a `ViewModel` down as a parameter.** Pass `uiState` and event lambdas instead. The ViewModel stops at the screen-level composable.
- **`mutableStateOf` exposed from a ViewModel.** Always `StateFlow`.
- **`LaunchedEffect(Unit) { while (true) { ... } }` for polling.** Use a `Flow` from the holder.
- **Navigation events stored in `UiState`.** They replay. Use a `Channel`/`SharedFlow`.
- **`Context` reached from a ViewModel via a static `appContext` field.** Inject the dependency you actually need (a `Resources`-like abstraction, a file path, etc.), not the Context.
- **`runBlocking` in production code.** Always a bug outside `main()` or tests.
- **Standard `List<T>` in composable params for collections that change.** Use `ImmutableList`.
- **A "Manager" or "Helper" class.** Name the responsibility. If you can't, the class shouldn't exist.
- **Stringly-typed nav routes when type-safe routes work.** Always prefer `@Serializable` route objects.
- **An "else" branch on a sealed `when`.** It defeats the safety net.

---

## 14. Verifying claims before using them

Versions and stability statuses move. Before pinning a version or claiming a platform is stable:

- CMP platform stability matrix → https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform.html
- Lifecycle / ViewModel multiplatform artifact → https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-viewmodel.html
- Navigation multiplatform → https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html
- maplibre-compose API → https://maplibre.org/maplibre-compose/

The mental models in this document are stable. The artifact coordinates, version numbers, and "is X stable yet" answers are not — verify with the source before writing them into a spec.
