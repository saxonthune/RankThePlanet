package com.saxonthune.ranktheplanet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.saxonthune.ranktheplanet.data.fake.FakeRepositories
import com.saxonthune.ranktheplanet.data.fake.Fixtures
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.ui.theme.RtpTheme
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.nav.rememberNavState
import com.saxonthune.ranktheplanet.ui.screens.AddLocationToCollectionScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionDetailScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionEntryDetailScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionListScreen
import com.saxonthune.ranktheplanet.ui.screens.ImportFlowScreen
import com.saxonthune.ranktheplanet.ui.screens.LocationDraftScreen
import com.saxonthune.ranktheplanet.ui.screens.LocationPickerScreen
import com.saxonthune.ranktheplanet.ui.screens.MapOverviewScreen
import com.saxonthune.ranktheplanet.ui.screens.ReviewFormScreen
import com.saxonthune.ranktheplanet.ui.screens.SchemaBuilderScreen
import com.saxonthune.ranktheplanet.ui.screens.SettingsScreen

@Composable
fun App() {
    RtpTheme {
        val nav = rememberNavState()
        val go: (Screen) -> Unit = nav::go
        val repos = remember { FakeRepositories() }
        when (nav.current) {
            Screen.MapOverview -> MapOverviewScreen(go)
            Screen.CollectionList -> CollectionListScreen(
                collections = repos.collections,
                entries = repos.entries,
                onNavigate = go,
                onOpenCollection = { id -> nav.go(Screen.CollectionDetail, id) },
            )
            Screen.CollectionDetail -> CollectionDetailScreen(
                collectionId = nav.selectedCollectionId ?: Fixtures.dripCoffeeId,
                collections = repos.collections,
                entries = repos.entries,
                templates = repos.templates,
                onNavigate = go,
                onOpenEntry = { id -> nav.go(Screen.CollectionEntryDetail, entryId = id) },
            )
            Screen.CollectionEntryDetail -> CollectionEntryDetailScreen(
                entryId = nav.selectedEntryId ?: EntryId("ent-bluebottle"),
                entries = repos.entries,
                templates = repos.templates,
                onNavigate = go,
            )
            Screen.ReviewForm -> ReviewFormScreen(go)
            Screen.SchemaBuilder -> SchemaBuilderScreen(go)
            Screen.LocationPicker -> LocationPickerScreen(go)
            Screen.AddLocationToCollection -> AddLocationToCollectionScreen(go)
            Screen.LocationDraft -> LocationDraftScreen(go)
            Screen.ImportFlow -> ImportFlowScreen(go)
            Screen.Settings -> SettingsScreen(go)
        }
    }
}
