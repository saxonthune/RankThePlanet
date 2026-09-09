package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import ranktheplanet.composeapp.generated.resources.Res
import ranktheplanet.composeapp.generated.resources.pin_body

/**
 * Transient marker for a search-picked provider candidate that hasn't been committed
 * as an Entry yet. Renders only while the candidate-bearing LocationSheet peek is open;
 * disappears once the user dismisses the sheet or commits an Entry (which then shows
 * up as a normal pin via [PinLayers]).
 */
@Composable
@MaplibreComposable
internal fun CandidateMarkerLayer(sheet: PinSheet) {
    val candidate = (sheet as? PinSheet.Peek)?.candidateLocation
    val json = remember(candidate) {
        if (candidate == null) {
            """{"type":"FeatureCollection","features":[]}"""
        } else {
            buildJsonObject {
                put("type", JsonPrimitive("FeatureCollection"))
                put("features", buildJsonArray {
                    add(buildJsonObject {
                        put("type", JsonPrimitive("Feature"))
                        put("geometry", buildJsonObject {
                            put("type", JsonPrimitive("Point"))
                            put("coordinates", buildJsonArray {
                                add(JsonPrimitive(candidate.coordinates.lng))
                                add(JsonPrimitive(candidate.coordinates.lat))
                            })
                        })
                        put("properties", buildJsonObject {})
                    })
                })
            }.toString()
        }
    }
    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(json))
    val bodyImage = image(painterResource(Res.drawable.pin_body), drawAsSdf = true)
    SymbolLayer(
        id = "candidate-marker",
        source = source,
        iconImage = bodyImage,
        iconColor = const(Color(0xFF1976D2)),
        iconSize = const(1.1f),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        iconAnchor = const(SymbolAnchor.Bottom),
    )
}
