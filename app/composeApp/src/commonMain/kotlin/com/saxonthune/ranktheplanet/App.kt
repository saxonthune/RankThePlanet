package com.saxonthune.ranktheplanet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.fake.FakeRepositories
import com.saxonthune.ranktheplanet.data.location.DefaultLocationProviderRegistry
import com.saxonthune.ranktheplanet.data.location.LocationProviderRegistry
import com.saxonthune.ranktheplanet.data.secure.SecureStore
import com.saxonthune.ranktheplanet.data.secure.createSecureStore
import com.saxonthune.ranktheplanet.data.sql.SqlRepositories
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.nav.CollectionDetail
import com.saxonthune.ranktheplanet.nav.CollectionEntryDetail
import com.saxonthune.ranktheplanet.nav.CollectionList
import com.saxonthune.ranktheplanet.nav.ImportFlow
import com.saxonthune.ranktheplanet.nav.ManageProviders
import com.saxonthune.ranktheplanet.nav.MapMode
import com.saxonthune.ranktheplanet.nav.MapOverview
import com.saxonthune.ranktheplanet.nav.ProviderConfig
import com.saxonthune.ranktheplanet.nav.ReviewForm
import com.saxonthune.ranktheplanet.nav.CollectionEditor
import com.saxonthune.ranktheplanet.nav.Settings
import com.saxonthune.ranktheplanet.ui.screens.CollectionDetailScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionEntryDetailScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionListScreen
import com.saxonthune.ranktheplanet.ui.screens.ImportFlowScreen
import com.saxonthune.ranktheplanet.ui.screens.ManageProvidersScreen
import com.saxonthune.ranktheplanet.ui.screens.ManageProvidersViewModel
import com.saxonthune.ranktheplanet.ui.screens.MapOverviewScreen
import com.saxonthune.ranktheplanet.ui.screens.ProviderConfigScreen
import com.saxonthune.ranktheplanet.ui.screens.ProviderConfigViewModel
import com.saxonthune.ranktheplanet.ui.screens.ReviewFormEvent
import com.saxonthune.ranktheplanet.ui.screens.ReviewFormScreen
import com.saxonthune.ranktheplanet.ui.screens.ReviewFormViewModel
import com.saxonthune.ranktheplanet.ui.screens.CollectionEditorScreen
import com.saxonthune.ranktheplanet.ui.screens.SettingsScreen
import com.saxonthune.ranktheplanet.ui.theme.RtpTheme

@Composable
fun App(repos: SqlRepositories? = null) {
    RtpTheme {
        val navController = rememberNavController()
        val fake = if (repos == null) remember { FakeRepositories() } else null
        val collections: CollectionRepository = repos?.collections ?: fake!!.collections
        val entries: EntryRepository = repos?.entries ?: fake!!.entries
        val templates: TemplateRepository = repos?.templates ?: fake!!.templates

        val secureStore = remember { createSecureStore() }
        val registry by produceState<LocationProviderRegistry?>(initialValue = null, secureStore) {
            value = DefaultLocationProviderRegistry.create(secureStore)
        }
        NavHost(navController = navController, startDestination = MapOverview()) {
            composable<MapOverview> { backStackEntry ->
                val r = registry ?: return@composable
                val route = backStackEntry.toRoute<MapOverview>()
                val collectionId = route.addToCollectionId
                val collectionName by produceState<String?>(initialValue = collectionId, collectionId) {
                    if (collectionId != null) {
                        collections.observe(CollectionId(collectionId)).collect { collection ->
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
                    collections = collections,
                    entries = entries,
                    templates = templates,
                    providerRegistry = r,
                    onCancelAdd = { navController.popBackStack() },
                    onOpenCollections = { navController.navigate(CollectionList) },
                    onOpenSettings = { navController.navigate(Settings) },
                    onViewCollection = { collectionId -> navController.navigate(CollectionDetail(collectionId.value)) },
                    onEditReview = { entryId -> navController.navigate(ReviewForm(entryId.value)) },
                    onPickCollectionForDraft = { },
                    onNewCollectionForDraft = { navController.navigate(CollectionEditor) },
                    onGoToReview = { entryId -> navController.navigate(ReviewForm(entryId.value)) },
                    onPendingReviewDismissed = { },
                )
            }
            composable<CollectionList> {
                CollectionListScreen(
                    collections = collections,
                    entries = entries,
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
                    collections = collections,
                    entries = entries,
                    templates = templates,
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
                    entries = entries,
                    collections = collections,
                    templates = templates,
                    onEditReview = { entryId -> navController.navigate(ReviewForm(entryId.value)) },
                    onRemoveEntry = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onViewCollection = { collectionId -> navController.navigate(CollectionDetail(collectionId.value)) },
                    onTapLocation = { navController.navigate(MapOverview()) },
                )
            }
            composable<ReviewForm> { backStackEntry ->
                val route = backStackEntry.toRoute<ReviewForm>()
                val vm: ReviewFormViewModel = viewModel {
                    ReviewFormViewModel(EntryId(route.entryId), entries, collections, templates)
                }
                val state by vm.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(vm) {
                    vm.events.collect { event ->
                        when (event) {
                            ReviewFormEvent.Saved -> navController.popBackStack()
                        }
                    }
                }
                ReviewFormScreen(
                    state = state,
                    onEdit = vm::onEdit,
                    onClear = vm::onClear,
                    onSave = { vm.save() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<CollectionEditor> {
                CollectionEditorScreen(
                    onFinish = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<ImportFlow> {
                ImportFlowScreen(
                    onFinish = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<Settings> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onManageProviders = { navController.navigate(ManageProviders) },
                )
            }
            composable<ManageProviders> {
                val r = registry ?: return@composable
                ManageProvidersScreen(
                    viewModel = remember(r, secureStore) { ManageProvidersViewModel(r, secureStore) },
                    onBack = { navController.popBackStack() },
                    onOpenProvider = { type -> navController.navigate(ProviderConfig(type.name)) },
                )
            }
            composable<ProviderConfig> { backStackEntry ->
                val route = backStackEntry.toRoute<ProviderConfig>()
                val type = SourceType.valueOf(route.provider)
                val r = registry ?: return@composable
                ProviderConfigScreen(
                    viewModel = remember(type, r, secureStore) {
                        ProviderConfigViewModel(type, r, secureStore)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
