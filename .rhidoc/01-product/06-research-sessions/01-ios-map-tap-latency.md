---
title: iOS MapLibre tap arbitration latency
summary: Why a pin tap on iOS lags ~300ms before the sheet animates, how the gesture-recognizer cascade is diagnosed, and the runtime patch on MLNMapView that removes the delay
tags: [research, ios, maplibre, gesture, latency, debugging]
deps: [doc02.01, doc03.04]
---

# iOS MapLibre tap arbitration latency

## Symptom

A pin tap in `MapOverviewScreen` produces a perceptible gap before the `ModalBottomSheet` begins sliding up. The gap is between finger-up and the maplibre-compose layer `onClick` lambda firing — not in `queryRenderedFeatures`, not in recomposition, not in the sheet machinery.

## Root cause

`MaplibreMap` on iOS embeds an `MLNMapView`. That view ships with ~10 `UIGestureRecognizer`s (pan, pinch, rotation, double-tap-zoom, two-finger tap-zoom-out, two long-press recognizers, two single-tap recognizers).

When maplibre-compose attaches its own single-tap recognizer for the `onMapClick` / per-layer-`onClick` API, it sets `requireGestureRecognizerToFail:` against **every preexisting recognizer** on the view (`IosMapAdapter.kt::addGestures`, `isCooperative = true` for taps). Without this, a double-tap-to-zoom would *also* fire a single-tap, opening a sheet *and* zooming. The cost: every single tap waits for the slowest existing recognizer to fail. Discrete recognizers (multi-tap, long-press) have built-in timing windows of 300–500ms, so the maplibre tap is held up that long even on a clearly-single tap.

`GestureOptions` only exposes `isZoomEnabled` as a single switch — it disables pinch *and* taps together. No public knob for "disable double-tap only."

## Reusable techniques

### 1. End-to-end timing trace across composition boundaries

A `TimeSource.Monotonic`-backed singleton with `start()` and `log(label)` placed at every boundary the event crosses: input handler entry → ViewModel function entry → state-write line → downstream Composable's `LaunchedEffect(Unit)` first frame. The inter-mark deltas isolate which span owns the delay.

Pitfalls:

- Call `start()` **exactly once** per event. A second `start()` partway through silently resets `t0` and hides the gap that mattered.
- Composable-body marks fire on every recomposition. Gate with `LaunchedEffect(Unit) { trace.log(...) }` to mark "first time this Composable was on screen" instead of "any recompose."

### 2. Earliest-observable touch hook on iOS (Kotlin/Native)

K/N's `UIGestureRecognizer` binding does not expose `touchesBegan:withEvent:` as overridable. Workaround: attach a `UILongPressGestureRecognizer` with `minimumPressDuration = 0.0` and a target/action. `.Began` fires at touch-down, `.Ended` at touch-up. Configure as a passive observer:

```kotlin
r.cancelsTouchesInView = false
r.delaysTouchesBegan = false
r.delaysTouchesEnded = false
// delegate.shouldRecognizeSimultaneouslyWith returns true — never blocks others
```

Caveats:

- `.Began` fires one runloop tick after touch-down, not literally at touch-down. Fine for measuring 100ms+ gaps, useless for sub-frame work.
- `.Ended` is delivered through the same arbitration pipeline under investigation, so the recognizer's own `.Ended` timing can be inflated. The true finger-up moment is unobservable from user code. Assume "quick tap ≤ 100ms" and treat anything above that as arbitration delay.

### 3. Reaching into a UIView a library doesn't expose

When a CMP interop library wraps a UIView but hides the handle (true of maplibre-compose's `MLNMapView`), walk `UIApplication.sharedApplication.windows` recursively and `is`-check for the target class. The wrapped view lives in the same window as the rest of the app.

Use `windows` (the array), not `keyWindow` (the singleton) — `keyWindow` is deprecated in iOS 13+ and can be `nil` for some seconds after launch in multi-scene apps.

The wrapped view is created lazily by the Compose UIKit interop machinery. Drive the walk from `LaunchedEffect(Unit)` with ~50ms retries, capped at a few seconds. There is no observable creation event to hook.

### 4. Inspecting iOS gesture recognizer class names from Kotlin

Class names disambiguate sibling recognizers. Read them with:

```kotlin
import platform.Foundation.NSStringFromClass
import platform.objc.object_getClass
val cls = object_getClass(recognizer)?.let { NSStringFromClass(it) } ?: "?"
```

For `UITapGestureRecognizer`, also log `numberOfTapsRequired` and `numberOfTouchesRequired` — single-tap vs. double-tap vs. two-finger-tap recognizers all share the same class and look identical otherwise.

### 5. Kotlin/Native foreign-API ergonomics

- A top-level `@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)` is the cheapest way to silence opt-in errors for iOS UIKit interop across a whole file.
- Subclass `NSObject` with `@ExportObjCClass` to make a Kotlin class addressable by ObjC selectors. Use `@kotlinx.cinterop.ObjCAction fun handle(sender: …)` for the target/action target.
- `UIGestureRecognizer` holds its `target` and `delegate` **weakly**. Anything passed in must be retained elsewhere (e.g., a top-level `val`) or it dies and the recognizer silently stops firing.

## Diagnosis trail

In order of cost:

1. **Bracket the lag with marks.** Confirm whether the gap sits before the gesture detector, before recomposition, before the sheet's first frame, or during the sheet's slide animation. Each domain has a different fix.
2. **If the gap is before the gesture detector**, install the touch-down probe (technique 2). The gap between touch-down and the detector callback is iOS gesture arbitration.
3. **Dump the recognizer list** on the underlying UIView (techniques 3 + 4). Tally every `UITapGestureRecognizer` with `taps>=2`, every `UILongPressGestureRecognizer`, every extra 1-tap-1-finger tap — these are the common arbitration delays.
4. **Disable suspects** one at a time until the delay drops. Re-enable any whose loss breaks a user-facing feature.

Signature of an arbitration delay: a flat ~300ms gap that does not scale with map content, pin count, or zoom level. Gaps of 600ms+ usually mean *two* recognizers in series, each with its own timing window — disabling one reveals the next.

## Fix

`util/MapTapTuner.kt` exposes `expect fun tuneMapForFastTaps(): Boolean`. The iOS actual walks the window tree, finds every `MLNMapView`, and on each one **removes** (via `removeGestureRecognizer:`):

- every `UITapGestureRecognizer` except the last 1-tap-1-finger one — kills double-tap-zoom, two-finger tap-zoom-out, and the MLN annotation-select tap, while preserving the maplibre-compose tap (added last);
- every `UILongPressGestureRecognizer` except the last — kills MLN's quick-zoom hold while preserving maplibre-compose's `onMapLongClick` (added last).

Android and JVM actuals are no-ops. `MapOverviewScreen` invokes the tuner from a `LaunchedEffect(Unit)` that retries until the lazily-created `MLNMapView` appears in the view tree.

### Why removal, not `setEnabled(false)`

The natural-looking move — disable each unwanted recognizer — does not survive past the tuner's call. MLN re-enables its own recognizers later (style-load callback, options reconciliation), so a tap that comes after the first frame finds the cascade restored and pays the full arbitration delay again. `removeGestureRecognizer:` detaches the recognizer from the view entirely; the maplibre-compose tap's `requireGestureRecognizerToFail:` relationship still references it, but a detached recognizer never participates in arbitration, so the requirement resolves immediately.

The "last recognizer of each kind is maplibre-compose's" heuristic depends on the library's `addGestures` order. A maplibre-compose version bump that reorders recognizer attachment would silently break the tuner — re-run technique 4 to confirm the assumption still holds.

### User-visible tradeoff

| Gesture                         | Status        |
|---------------------------------|---------------|
| Pinch zoom                      | works         |
| Pan / rotate / tilt             | works         |
| Long-press to drop a pin        | works         |
| Tap on a pin                    | works, snappy |
| Double-tap to zoom in           | **disabled**  |
| Two-finger tap to zoom out      | **disabled**  |

Most map apps make the same call. If iOS users build muscle memory around a system-wide single-tap zoom gesture, revisit.

### Upstream alternative

The runtime patch exists because maplibre-compose has no public knob for "disable double-tap arbitration." A `GestureOptions.isDoubleTapZoomEnabled` (and friends) upstream would let `MapTapTuner.ios.kt` disappear. A PR against `maplibre/maplibre-compose` would supersede this file.

## Libraries

- **maplibre-compose** (`org.maplibre.compose`) — Compose Multiplatform wrapper for MapLibre Native. Source set vendored under `.temp/maplibre-compose-sources/` per [[reference_temp_dir_unzipped_deps]] for read access.
- **MapLibre Native iOS SDK** (`MapLibre.MLNMapView`) — the underlying UIKit view that owns the preexisting recognizers.
- **Kotlin/Native UIKit interop** — `platform.UIKit.*`, `platform.Foundation.*`, `platform.objc.*` bindings used to reach the view and manipulate recognizers.

## Related

- [[doc02.01]] — tech stack and why maplibre-compose
- [[doc03.04]] — how pins reach the map (the `ComputedSource` layer split)
