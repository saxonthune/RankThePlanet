package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.compose.util.MaplibreComposable
import ranktheplanet.composeapp.generated.resources.Res
import ranktheplanet.composeapp.generated.resources.pin_body

/**
 * Pins rendered for an active search-results-mode candidate set. Styled distinctly from
 * Collection Entry pins (orange vs. collection colors) so users can tell "result to adopt"
 * from "already saved." Disappears when [candidates] is null or empty.
 */
@Composable
@MaplibreComposable
internal fun SearchCandidatePinLayer(
    candidates: List<SearchHitUi.Candidate>?,
    onCandidateTap: (SearchHitUi.Candidate) -> Unit,
) {
    val list = candidates.orEmpty()
    val json = remember(list) {
        if (list.isEmpty()) {
            """{"type":"FeatureCollection","features":[]}"""
        } else {
            buildJsonObject {
                put("type", JsonPrimitive("FeatureCollection"))
                put("features", buildJsonArray {
                    list.forEachIndexed { idx, c ->
                        add(buildJsonObject {
                            put("type", JsonPrimitive("Feature"))
                            put("geometry", buildJsonObject {
                                put("type", JsonPrimitive("Point"))
                                put("coordinates", buildJsonArray {
                                    add(JsonPrimitive(c.lng))
                                    add(JsonPrimitive(c.lat))
                                })
                            })
                            put("properties", buildJsonObject {
                                put("idx", JsonPrimitive(idx.toString()))
                            })
                        })
                    }
                })
            }.toString()
        }
    }
    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(json))
    val bodyImage = image(painterResource(Res.drawable.pin_body), drawAsSdf = true)
    SymbolLayer(
        id = "search-candidate-pins",
        source = source,
        iconImage = bodyImage,
        iconColor = const(Color(0xFFE65100)),
        iconSize = const(1.0f),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        iconAnchor = const(SymbolAnchor.Bottom),
        onClick = { features ->
            val idxStr = features.firstOrNull()
                ?.properties
                ?.get("idx")
                ?.jsonPrimitive
                ?.contentOrNull
            val idx = idxStr?.toIntOrNull()
            val hit = if (idx != null) list.getOrNull(idx) else null
            if (hit != null) {
                onCandidateTap(hit)
                ClickResult.Consume
            } else {
                ClickResult.Pass
            }
        },
    )
}
