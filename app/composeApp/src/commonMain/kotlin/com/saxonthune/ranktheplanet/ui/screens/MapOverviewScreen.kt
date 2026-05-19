package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.nav.Screen
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

private val MOCK_COLLECTIONS = listOf("Drip Coffee", "NYT Top 100", "Geo Diary")

@Composable
fun MapOverviewScreen(onNavigate: (Screen) -> Unit) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = -73.9855, latitude = 40.7580),
            zoom = 12.0,
        )
    )

    Box(Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        ) {
            // Sources and layers must be created inside the MaplibreMap content
            // lambda — they consume LocalStyleNode, which only exists here.
            val pinsSource = rememberGeoJsonSource(
                data = GeoJsonData.JsonString(PINS_GEOJSON)
            )
            CircleLayer(
                id = "manhattan-pins",
                source = pinsSource,
                radius = const(8.dp),
                color = const(PIN_COLOR),
                strokeWidth = const(2.dp),
                strokeColor = const(Color.White),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.45f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(onClick = { onNavigate(Screen.CollectionList) }) {
                        Text("Collections")
                    }
                    Button(onClick = { onNavigate(Screen.Settings) }) {
                        Text("Settings")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val chipSelected = remember {
                    MOCK_COLLECTIONS.associateWith { mutableStateOf(false) }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MOCK_COLLECTIONS.forEach { label ->
                        val selected by chipSelected.getValue(label)
                        FilterChip(
                            selected = selected,
                            onClick = { chipSelected.getValue(label).value = !selected },
                            label = { Text(label) },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.CollectionEntryDetail) },
                    ) {
                        Text("Inspect a pin")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.AddLocationToCollection) },
                    ) {
                        Text("Add here")
                    }
                }
            }
        }
    }
}
