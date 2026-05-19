package com.saxonthune.ranktheplanet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.saxonthune.ranktheplanet.data.fake.FakeRepositories
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.nav.AddLocationToCollection
import com.saxonthune.ranktheplanet.nav.CollectionDetail
import com.saxonthune.ranktheplanet.nav.CollectionEntryDetail
import com.saxonthune.ranktheplanet.nav.CollectionList
import com.saxonthune.ranktheplanet.nav.ImportFlow
import com.saxonthune.ranktheplanet.nav.LocationDraft
import com.saxonthune.ranktheplanet.nav.LocationPicker
import com.saxonthune.ranktheplanet.nav.MapOverview
import com.saxonthune.ranktheplanet.nav.ReviewForm
import com.saxonthune.ranktheplanet.nav.CollectionEditor
import com.saxonthune.ranktheplanet.nav.Settings
import com.saxonthune.ranktheplanet.ui.screens.AddLocationToCollectionScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionDetailScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionEntryDetailScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionListScreen
import com.saxonthune.ranktheplanet.ui.screens.ImportFlowScreen
import com.saxonthune.ranktheplanet.ui.screens.LocationDraftScreen
import com.saxonthune.ranktheplanet.ui.screens.LocationPickerScreen
import com.saxonthune.ranktheplanet.ui.screens.MapOverviewScreen
import com.saxonthune.ranktheplanet.ui.screens.ReviewFormScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionEditorScreen
import com.saxonthune.ranktheplanet.ui.screens.SettingsScreen
import com.saxonthune.ranktheplanet.ui.theme.RtpTheme

@Composable
fun App() {
    RtpTheme {
        val navController = rememberNavController()
        val repos = remember { FakeRepositories() }
        NavHost(navController = navController, startDestination = MapOverview) {
            composable<MapOverview> {
                MapOverviewScreen(
                    onOpenCollections = { navController.navigate(CollectionList) },
                    onOpenSettings = { navController.navigate(Settings) },
                    onInspectPin = { navController.navigate(CollectionEntryDetail("ent-bluebottle")) },
                    onDropPin = { navController.navigate(LocationDraft) },
                )
            }
            composable<CollectionList> {
                CollectionListScreen(
                    collections = repos.collections,
                    entries = repos.entries,
                    onNewCollection = { navController.navigate(CollectionEditor) },
                    onImport = { navController.navigate(ImportFlow) },
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { id -> navController.navigate(CollectionDetail(id.value)) },
                )
            }
            composable<CollectionDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<CollectionDetail>()
                CollectionDetailScreen(
                    collectionId = CollectionId(route.collectionId),
                    collections = repos.collections,
                    entries = repos.entries,
                    templates = repos.templates,
                    onAddEntry = { navController.navigate(LocationPicker) },
                    onEditCollection = { navController.navigate(CollectionEditor) },
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { id -> navController.navigate(CollectionEntryDetail(id.value)) },
                )
            }
            composable<CollectionEntryDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<CollectionEntryDetail>()
                CollectionEntryDetailScreen(
                    entryId = EntryId(route.entryId),
                    entries = repos.entries,
                    templates = repos.templates,
                    onEditReview = { navController.navigate(ReviewForm) },
                    onRemoveEntry = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<ReviewForm> {
                ReviewFormScreen(
                    onSave = { navController.navigate(CollectionEntryDetail("ent-bluebottle")) },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<CollectionEditor> {
                CollectionEditorScreen(
                    onFinish = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<LocationPicker> {
                LocationPickerScreen(
                    onPickLocation = { navController.navigate(ReviewForm) },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<AddLocationToCollection> {
                AddLocationToCollectionScreen(
                    onPickCollection = { navController.navigate(ReviewForm) },
                    onNewCollection = { navController.navigate(CollectionEditor) },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<LocationDraft> {
                LocationDraftScreen(
                    onClose = { navController.popBackStack() },
                    onAddToCollection = { navController.navigate(AddLocationToCollection) },
                )
            }
            composable<ImportFlow> {
                ImportFlowScreen(
                    onFinish = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<Settings> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
