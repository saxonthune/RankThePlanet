package com.saxonthune.ranktheplanet.domain.io

enum class PortFormat(val mimeType: String, val extension: String) {
    Kml("application/vnd.google-earth.kml+xml", "kml"),
    GeoJson("application/geo+json", "geojson"),
}
