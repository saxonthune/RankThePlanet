package com.saxonthune.ranktheplanet.nav

/**
 * The navigation views from doc02.02.01 (`01-navigation.statechart.json`).
 * One entry per statechart state; [title] is the on-screen heading.
 */
enum class Screen(val title: String) {
    MapOverview("Map Overview"),
    CollectionList("Collections"),
    CollectionDetail("Collection"),
    CollectionEntryDetail("Collection Entry"),
    ReviewForm("Review"),
    SchemaBuilder("Review Template"),
    LocationPicker("Pick a Location"),
    AddLocationToCollection("Add to Collection"),
    LocationDraft("Location Draft"),
    ImportFlow("Import"),
    Settings("Settings"),
}
