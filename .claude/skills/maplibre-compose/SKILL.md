# maplibre-compose

Reference for the `org.maplibre.compose:maplibre-compose` library — a Compose-first wrapper around MapLibre Native. Use when adding or modifying the map, its style, ornaments, sources, or layers.

The mental model and architecture rules for the map live in `kotlin-cmp/SKILL.md` §11 ("Map rendering: maplibre-compose"). This skill is purely about the **API shapes** that bite if you guess — composable parameter names, expression DSL syntax, type mismatches. Verify against the canonical docs before pinning specifics: https://maplibre.org/maplibre-compose/api/

## When this triggers

- Editing `MaplibreMap(...)` or any `*Layer(...)` composable.
- Hiding/showing scale bar, compass, attribution, logo (ornaments).
- Driving layer properties from GeoJSON feature properties (the expression DSL).
- Changing the base style URL.
- Compile errors mentioning `Expression<...Value>`, `internal expect class SymbolLayer`, `Unresolved reference 'asString' / 'cast' / 'ornamentOptions'`.

## Key API shapes (v0.12.x)

### Map ornaments

`MaplibreMap` does **not** take an `ornamentOptions` parameter directly. Ornaments live inside `MapOptions`:

```kotlin
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.OrnamentOptions

MaplibreMap(
    cameraState = …,
    baseStyle = BaseStyle.Uri("…"),
    options = MapOptions(ornamentOptions = OrnamentOptions.OnlyLogo),
)
```

Common-code presets only: `AllEnabled` (default), `OnlyLogo` (hides scale/compass/attribution toggle), `AllDisabled` (hides everything including the logo). Per-ornament toggles require platform-specific code. `OnlyLogo` is the right default when the map is the whole screen — the MapLibre logo is small and the OSM/OpenFreeMap attribution requirements are still met by it.

### OpenFreeMap base styles

All under `https://tiles.openfreemap.org/styles/`:

- `positron` — light, muted, Google-Maps-like.
- `bright` — clean, slightly brighter.
- `liberty` — road-atlas feel.
- `dark` — dark counterpart of positron.
- `fiord` — desaturated, color-blind-friendly.
- `3d` — adds 3D building extrusions.

For an information-dense app (pins on top), `positron` keeps streets recessive so pins read. `liberty` competes with overlays.

### Layers come in pairs: class + composable, same name

`org.maplibre.compose.layers.SymbolLayer`, `CircleLayer`, `LineLayer`, `FillLayer` each ship as **both** an `internal expect class` and a public `@Composable fun` with the same name in the same package.

If a `SymbolLayer(...)` call errors with **"Cannot access 'class SymbolLayer : FeatureLayer': it is internal"**, the compiler resolved your call to the class because your argument list does not match any overload of the public function. Don't fight the import — fix the parameter types. Read the error as "your params are wrong."

### Driving a layer property from a GeoJSON feature property

The expression DSL has type-tagged `Expression<T>` (e.g., `Expression<StringValue>`, `Expression<FormattedValue>`, `Expression<DpValue>`). Layer params want a specific `Expression<X>` — you can't pass `Offset` where `Expression<TextUnitOffsetValue>` is expected.

Canonical pattern for a text label from feature property `"name"`:

```kotlin
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

SymbolLayer(
    id = "labels",
    source = source,
    minZoom = 12f,
    textField = format(span(feature["name"].asString())),
    textSize = const(12.sp),
    textOffset = offset(0f.em, 1.2f.em),
    textAnchor = const(SymbolAnchor.Top),
    textOptional = const(true),
    iconAllowOverlap = const(true),
    textHaloColor = const(Color.White),
    textHaloWidth = const(1.dp),
)
```

Things people get wrong:
- `textField` is `Expression<FormattedValue>`, **not** `Expression<StringValue>`. You must wrap the string in `format(span(...))`. There is no public `.cast()` helper.
- `textSize` is `Expression<TextUnitValue>` — `12.sp`, not `12.dp`.
- `textOffset` is `Expression<TextUnitOffsetValue>` — built with `offset(x.em, y.em)`, not `Offset(...)` from `compose.ui.geometry`.
- `textHaloWidth` is `Expression<DpValue>` — `1.dp` is right here.

### Collision-aware labels (the "labels hide when they'd overlap" effect)

MapLibre's symbol placement engine does this for free, but only when you let it:

- `textAllowOverlap = const(false)` (the default) — labels that would overlap drop out.
- `textIgnorePlacement = const(false)` (the default) — labels participate in the collision grid.
- `textOptional = const(true)` — if the label collides, drop only the label, keep the icon.
- `iconAllowOverlap = const(true)` — let icons always render even if labels can't.

Combined with `minZoom`, this gives "labels appear at street-level zoom and politely hide when crowded."

### Pins as a GeoJSON source, not N composables

Already covered in `kotlin-cmp` §11 but worth restating because it's the single most common perf mistake: render N pins as a `GeoJsonSource` + `CircleLayer`/`SymbolLayer`, not as N child composables. The source data is a JSON string of a `FeatureCollection`; each feature's `properties` is what the expression DSL reads via `feature["…"].asString()` (or `.asNumber()`, etc.).

When you add a property you want a layer to read, remember to JSON-escape it on the way in. There's no schema enforcement — a stray quote in a location name will break the whole source.

## Verifying before you commit

The library moves. Before changing API usage, sanity-check against:

- API reference: https://maplibre.org/maplibre-compose/api/
- Layers guide: https://maplibre.org/maplibre-compose/layers/
- The version pinned in `app/gradle/libs.versions.toml` under `maplibre-compose`.

If a compile error mentions an internal class, an unresolved DSL function, or a missing parameter — the API reference at the pinned version is the source of truth. Don't guess imports; look it up.
