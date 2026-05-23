package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.saxonthune.ranktheplanet.domain.EntryId
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.expressions.dsl.any
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.rememberComputedSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import ranktheplanet.composeapp.generated.resources.Res
import ranktheplanet.composeapp.generated.resources.pin_body
import ranktheplanet.composeapp.generated.resources.pin_mark_dot
import ranktheplanet.composeapp.generated.resources.pin_mark_plus

@Composable
@MaplibreComposable
internal fun PinLayers(
    pins: List<PinUi>,
    labelHaloColor: Color,
    onPinClick: (EntryId) -> Unit,
) {
    // ComputedSource is pull-based: maplibre calls our lambda per visible tile, so features
    // appear in newly-panned tiles without the async setData race that affects GeoJsonSource
    // on iOS (maplibre-compose#738).
    val pinsState = rememberUpdatedState(pins)
    val getFeatures = remember {
        { bounds: BoundingBox, _: Int ->
            val visible = pinsState.value.filter {
                it.lng in bounds.west..bounds.east && it.lat in bounds.south..bounds.north
            }
            FeatureCollection(
                visible.map { pin ->
                    Feature(
                        geometry = Point(Position(longitude = pin.lng, latitude = pin.lat)),
                        properties = buildJsonObject {
                            put("entryId", JsonPrimitive(pin.entryId.value))
                            put("name", JsonPrimitive(pin.locationName))
                            put("color", JsonPrimitive(pin.colorHex))
                            put("kind", JsonPrimitive(pin.kind.token()))
                        },
                    )
                }
            )
        }
    }
    val source = rememberComputedSource(getFeatures = getFeatures)
    LaunchedEffect(pins) {
        source.invalidateBounds(BoundingBox(west = -180.0, south = -90.0, east = 180.0, north = 90.0))
    }

    val bodyImage = image(painterResource(Res.drawable.pin_body), drawAsSdf = true)
    val dotImage = image(painterResource(Res.drawable.pin_mark_dot), drawAsSdf = true)
    val plusImage = image(painterResource(Res.drawable.pin_mark_plus), drawAsSdf = true)
    val grey = Color(0xFF9AA0A6)
    val offWhite = Color(0xFFF5F0E8)
    val kindExpr = feature["kind"].asString()
    val colorExpr = feature["color"].convertToColor(const(grey))

    SymbolLayer(
        id = "pins-body",
        source = source,
        iconImage = bodyImage,
        iconColor = switch(
            kindExpr,
            case("multi", const(offWhite)),
            case("multi-unvisited", const(grey)),
            fallback = colorExpr,
        ),
        iconSize = const(1.0f),
        iconAllowOverlap = const(true),
        iconAnchor = const(SymbolAnchor.Bottom),
        onClick = { features ->
            val entryId = features.firstOrNull()
                ?.properties
                ?.get("entryId")
                ?.jsonPrimitive
                ?.contentOrNull
            if (entryId != null) {
                onPinClick(EntryId(entryId))
                ClickResult.Consume
            } else {
                ClickResult.Pass
            }
        },
    )
    SymbolLayer(
        id = "pins-mark",
        source = source,
        filter = any(
            kindExpr eq const("reviewed"),
            kindExpr eq const("multi"),
            kindExpr eq const("multi-unvisited"),
        ),
        iconImage = switch(
            kindExpr,
            case("reviewed", dotImage),
            case("multi", plusImage),
            case("multi-unvisited", plusImage),
            fallback = dotImage,
        ),
        iconColor = const(Color.Black),
        iconSize = const(1.0f),
        iconAllowOverlap = const(true),
        iconAnchor = const(SymbolAnchor.Bottom),
    )
    SymbolLayer(
        id = "pin-labels",
        source = source,
        minZoom = 12f,
        textField = format(span(feature["name"].asString())),
        textSize = const(12.sp),
        textOffset = offset(0f.em, 1.2f.em),
        textAnchor = const(SymbolAnchor.Top),
        textOptional = const(true),
        iconAllowOverlap = const(true),
        textHaloColor = const(labelHaloColor),
        textHaloWidth = const(1.dp),
    )
}

private fun PinKind.token(): String = when (this) {
    PinKind.Unvisited -> "unvisited"
    PinKind.Reviewed -> "reviewed"
    PinKind.Multi -> "multi"
    PinKind.MultiUnvisited -> "multi-unvisited"
}
