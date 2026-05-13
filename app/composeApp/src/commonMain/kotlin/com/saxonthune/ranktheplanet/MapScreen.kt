package com.saxonthune.ranktheplanet

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private val PIN_COLOR = Color(0xFFE11D48)

// GeoJSON coordinates are [longitude, latitude]
private val PINS_GEOJSON = """
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": { "type": "Point", "coordinates": [-73.9857, 40.7484] },
      "properties": { "name": "Empire State Building" }
    },
    {
      "type": "Feature",
      "geometry": { "type": "Point", "coordinates": [-73.9855, 40.7580] },
      "properties": { "name": "Times Square" }
    },
    {
      "type": "Feature",
      "geometry": { "type": "Point", "coordinates": [-73.9812, 40.7669] },
      "properties": { "name": "Central Park South" }
    },
    {
      "type": "Feature",
      "geometry": { "type": "Point", "coordinates": [-73.9969, 40.7061] },
      "properties": { "name": "Brooklyn Bridge Manhattan Side" }
    },
    {
      "type": "Feature",
      "geometry": { "type": "Point", "coordinates": [-73.9973, 40.7308] },
      "properties": { "name": "Washington Square Park" }
    }
  ]
}
""".trimIndent()

@Composable
fun MapScreen() {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = -73.9855, latitude = 40.7580),
            zoom = 12.0
        )
    )

    val pinsSource = rememberGeoJsonSource(
        data = GeoJsonData.JsonString(PINS_GEOJSON)
    )

    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        cameraState = cameraState,
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
    ) {
        CircleLayer(
            id = "manhattan-pins",
            source = pinsSource,
            radius = const(8.dp),
            color = const(PIN_COLOR),
            strokeWidth = const(2.dp),
            strokeColor = const(Color.White),
        )
    }
}
