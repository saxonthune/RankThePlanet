package com.saxonthune.ranktheplanet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.saxonthune.ranktheplanet.data.fake.FakeLocationProvider
import com.saxonthune.ranktheplanet.data.fake.FakeRepositories
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.nav.AddLocationToCollection
import com.saxonthune.ranktheplanet.nav.CollectionDetail
import com.saxonthune.ranktheplanet.nav.CollectionEntryDetail
import com.saxonthune.ranktheplanet.nav.CollectionList
import com.saxonthune.ranktheplanet.nav.ImportFlow
import com.saxonthune.ranktheplanet.nav.LocationDraft
import com.saxonthune.ranktheplanet.nav.MapMode
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
        val locationProvider = remember { FakeLocationProvider() }
        NavHost(navController = navController, startDestination = MapOverview()) {
            composable<MapOverview> { backStackEntry ->
                val route = backStackEntry.toRoute<MapOverview>()
                val collectionId = route.addToCollectionId
                val collectionName by produceState<String?>(initialValue = collectionId, collectionId) {
                    if (collectionId != null) {
                        repos.collections.observe(CollectionId(collectionId)).collect { collection ->
                            value = collection?.name ?: collectionId
                        }
                    }
                }
                val mode: MapMode = if (collectionId == null) {
                    MapMode.Browse
                } else {
                    MapMode.AddingToCollection(
                        collectionId = CollectionId(collectionId),
                        collectionName = collectionName ?: collectionId,
                    )
                }
                MapOverviewScreen(
                    mode = mode,
                    collections = repos.collections,
                    entries = repos.entries,
                    locationProvider = locationProvider,
                    onCancelAdd = { navController.popBackStack() },
                    onOpenCollections = { navController.navigate(CollectionList) },
                    onOpenSettings = { navController.navigate(Settings) },
                    onOpenFullDetail = { entryId -> navController.navigate(CollectionEntryDetail(entryId.value)) },
                    onViewCollection = { collectionId -> navController.navigate(CollectionDetail(collectionId.value)) },
                    onEditReview = { navController.navigate(ReviewForm) },
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
                    onAddEntry = { navController.navigate(MapOverview(route.collectionId)) },
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
                    onViewCollection = { collectionId -> navController.navigate(CollectionDetail(collectionId.value)) },
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
