package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.saxonthune.ranktheplanet.domain.EntryId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
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
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.compose.util.MaplibreComposable
import ranktheplanet.composeapp.generated.resources.Res
import ranktheplanet.composeapp.generated.resources.pin_body
import ranktheplanet.composeapp.generated.resources.pin_mark_dot
import ranktheplanet.composeapp.generated.resources.pin_mark_plus

@Composable
@MaplibreComposable
internal fun PinLayers(
    controller: PinRenderController,
    onPinClick: (EntryId) -> Unit,
) {
    // Push-based GeoJsonSource: every emission from `controller.frames` rebuilds the
    // FeatureCollection and the library pushes it to native via setData atomically.
    // Pulling forceRedraw on the controller (e.g. from onMapLoadFinished, or after a
    // detected render glitch) bumps the frame revision so a content-identical pin set
    // still triggers a fresh native push — the recovery hook for iOS render races.
    val frame by controller.frames.collectAsState(initial = PinFrame(persistentListOf(), 0L))
    // Use JsonString rather than GeoJsonData.Features because spatialk-geojson's polymorphic
    // serializer crashes on empty FeatureCollections (firstNotNullOf on an empty list throws
    // NoSuchElementException). JsonString hands the raw GeoJSON to MapLibre's native parser
    // and skips the Kotlin-side polymorphic dispatch entirely.
    val data = remember(frame) { GeoJsonData.JsonString(frame.toGeoJsonString()) }
    val source = rememberGeoJsonSource(data = data, options = GeoJsonOptions(buffer = 512))

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
        iconIgnorePlacement = const(true),
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
        iconIgnorePlacement = const(true),
        iconAnchor = const(SymbolAnchor.Bottom),
    )
}

private fun PinFrame.toGeoJsonString(): String =
    buildJsonObject {
        put("type", JsonPrimitive("FeatureCollection"))
        put("features", buildJsonArray {
            pins.forEach { pin ->
                add(buildJsonObject {
                    put("type", JsonPrimitive("Feature"))
                    put("geometry", buildJsonObject {
                        put("type", JsonPrimitive("Point"))
                        put("coordinates", buildJsonArray {
                            add(JsonPrimitive(pin.lng))
                            add(JsonPrimitive(pin.lat))
                        })
                    })
                    put("properties", buildJsonObject {
                        put("entryId", JsonPrimitive(pin.entryId.value))
                        put("name", JsonPrimitive(pin.locationName))
                        put("color", JsonPrimitive(pin.colorHex))
                        put("kind", JsonPrimitive(pin.kind.token()))
                    })
                })
            }
        })
    }.toString()

private fun PinKind.token(): String = when (this) {
    PinKind.Unvisited -> "unvisited"
    PinKind.Reviewed -> "reviewed"
    PinKind.Multi -> "multi"
    PinKind.MultiUnvisited -> "multi-unvisited"
}
