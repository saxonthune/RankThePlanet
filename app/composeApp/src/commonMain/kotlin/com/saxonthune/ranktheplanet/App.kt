package com.saxonthune.ranktheplanet

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.saxonthune.ranktheplanet.data.CollectionPortIoService
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.DefaultCollectionPortIoService
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.LocationRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.fake.FakeRepositories
import com.saxonthune.ranktheplanet.data.location.DefaultLocationProviderRegistry
import com.saxonthune.ranktheplanet.data.location.LocationProviderRegistry
import com.saxonthune.ranktheplanet.data.projection.OverviewProjection
import com.saxonthune.ranktheplanet.data.secure.SecureStore
import com.saxonthune.ranktheplanet.data.secure.createSecureStore
import com.saxonthune.ranktheplanet.data.session.SessionStateStore
import com.saxonthune.ranktheplanet.data.sql.SqlRepositories
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.io.FilePicker
import com.saxonthune.ranktheplanet.io.NoOpFilePicker
import com.saxonthune.ranktheplanet.nav.About
import com.saxonthune.ranktheplanet.nav.Attributions
import com.saxonthune.ranktheplanet.nav.CollectionDetail
import com.saxonthune.ranktheplanet.nav.CollectionEntryDetail
import com.saxonthune.ranktheplanet.nav.ImportFlow
import com.saxonthune.ranktheplanet.nav.LicenseViewer
import com.saxonthune.ranktheplanet.nav.ManageProviders
import com.saxonthune.ranktheplanet.nav.MapMode
import com.saxonthune.ranktheplanet.nav.MapOverview
import com.saxonthune.ranktheplanet.nav.ProviderConfig
import com.saxonthune.ranktheplanet.nav.ReviewForm
import com.saxonthune.ranktheplanet.nav.CollectionEditor
import com.saxonthune.ranktheplanet.nav.DebugSettings
import com.saxonthune.ranktheplanet.nav.Settings
import com.saxonthune.ranktheplanet.ui.screens.CollectionDetailScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionEntryDetailScreen
import com.saxonthune.ranktheplanet.ui.screens.ImportFlowEvent
import com.saxonthune.ranktheplanet.ui.screens.ImportFlowViewModel
import com.saxonthune.ranktheplanet.ui.screens.ImportFlowScreen
import com.saxonthune.ranktheplanet.ui.screens.ManageProvidersScreen
import com.saxonthune.ranktheplanet.ui.screens.ManageProvidersViewModel
import com.saxonthune.ranktheplanet.ui.screens.MapOverviewScreen
import com.saxonthune.ranktheplanet.ui.screens.ProviderConfigScreen
import com.saxonthune.ranktheplanet.ui.screens.ProviderConfigViewModel
import com.saxonthune.ranktheplanet.ui.screens.CollectionEditorScreen
import com.saxonthune.ranktheplanet.ui.screens.CollectionEditorViewModel
import com.saxonthune.ranktheplanet.ui.screens.EditorMode
import com.saxonthune.ranktheplanet.ui.screens.ReviewFormEvent
import com.saxonthune.ranktheplanet.ui.screens.ReviewFormScreen
import com.saxonthune.ranktheplanet.ui.screens.ReviewFormViewModel
import com.saxonthune.ranktheplanet.ui.screens.AboutScreen
import com.saxonthune.ranktheplanet.ui.screens.AttributionsScreen
import com.saxonthune.ranktheplanet.ui.screens.DebugSettingsScreen
import com.saxonthune.ranktheplanet.ui.screens.LicenseViewerScreen
import com.saxonthune.ranktheplanet.ui.screens.SettingsScreen
import com.saxonthune.ranktheplanet.ui.theme.RtpTheme
import androidx.compose.ui.platform.LocalUriHandler

private const val GITHUB_URL = "https://github.com/saxonthune/RankThePlanet"

@Composable
fun App(graph: RtpAppGraph? = null, enableDebugTools: Boolean = false) {
    RtpTheme {
        val navController = rememberNavController()
        val fake = if (graph == null) remember { FakeRepositories() } else null
        val collections: CollectionRepository = graph?.repos?.collections ?: fake!!.collections
        val entries: EntryRepository = graph?.repos?.entries ?: fake!!.entries
        val templates: TemplateRepository = graph?.repos?.templates ?: fake!!.templates
        val locations: LocationRepository = graph?.repos?.locations ?: fake!!.locations
        val projection: OverviewProjection = graph?.projection ?: fake!!.overviewProjection
        val session: SessionStateStore = graph?.session ?: fake!!.sessionStateStore
        val portIo: CollectionPortIoService = graph?.portIo
            ?: remember { DefaultCollectionPortIoService(collections, entries, templates, locations) }
        val filePicker: FilePicker = graph?.filePicker ?: remember { NoOpFilePicker() }

        val secureStore = remember { createSecureStore() }
        val registry by produceState<LocationProviderRegistry?>(initialValue = null, secureStore) {
            value = DefaultLocationProviderRegistry.create(secureStore)
        }
        var pendingNewCollectionForDraft by remember { mutableStateOf<String?>(null) }
        NavHost(
            navController = navController,
            startDestination = MapOverview(),
            // One motion for every destination: the entering surface rises from the bottom
            // over the screen below, which holds perfectly still (ExitTransition.None) so it
            // stays opaque behind the rising panel — fading it would expose the black NavHost
            // background and flash before the panel covers. Pop drops the top surface back
            // down. A consistent, sheet-like push with no horizontal cross-slide. See G014.
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { slideOutVertically(targetOffsetY = { it }) },
        ) {
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
                    locations = locations,
                    providerRegistry = r,
                    projection = projection,
                    session = session,
                    onCancelAdd = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Settings) },
                    onNewCollection = { navController.navigate(CollectionEditor()) },
                    onImport = { navController.navigate(ImportFlow) },
                    onViewCollection = { collectionId -> navController.navigate(CollectionDetail(collectionId.value)) },
                    onEditReview = { entryId -> navController.navigate(ReviewForm(entryId.value)) },
                    onPickCollectionForDraft = { },
                    onNewCollectionForDraft = {
                        navController.navigate(CollectionEditor(addToDraft = true))
                    },
                    onGoToReview = { entryId -> navController.navigate(ReviewForm(entryId.value)) },
                    onPendingReviewDismissed = { },
                    initialFilterCollectionId = route.filterCollectionId,
                    pendingNewCollectionForDraft = pendingNewCollectionForDraft,
                    onPendingNewCollectionForDraftConsumed = { pendingNewCollectionForDraft = null },
                )
            }
            composable<CollectionDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<CollectionDetail>()
                CollectionDetailScreen(
                    collectionId = CollectionId(route.collectionId),
                    collections = collections,
                    entries = entries,
                    templates = templates,
                    portIo = portIo,
                    filePicker = filePicker,
                    onAddEntry = { navController.navigate(MapOverview(addToCollectionId = route.collectionId)) },
                    onEditCollection = { navController.navigate(CollectionEditor(route.collectionId)) },
                    onViewOnMap = { navController.navigate(MapOverview(filterCollectionId = route.collectionId)) },
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
            composable<CollectionEditor> { entry ->
                val route = entry.toRoute<CollectionEditor>()
                val mode: EditorMode = route.collectionId
                    ?.let { EditorMode.Edit(CollectionId(it)) }
                    ?: EditorMode.Create
                val vm = viewModel { CollectionEditorViewModel(mode, collections, templates) }
                CollectionEditorScreen(
                    viewModel = vm,
                    onSaved = { id ->
                        if (route.addToDraft) {
                            pendingNewCollectionForDraft = id.value
                            navController.popBackStack()
                        } else if (mode is EditorMode.Create) {
                            navController.navigate(CollectionDetail(id.value)) {
                                popUpTo<CollectionEditor> { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<ImportFlow> {
                val vm: ImportFlowViewModel = viewModel { ImportFlowViewModel(portIo, filePicker) }
                val state by vm.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(vm) {
                    vm.events.collect { e ->
                        when (e) {
                            is ImportFlowEvent.Imported -> {
                                navController.navigate(CollectionDetail(e.collectionId.value)) {
                                    popUpTo<ImportFlow> { inclusive = true }
                                }
                            }
                            ImportFlowEvent.Cancelled -> navController.popBackStack()
                        }
                    }
                }
                ImportFlowScreen(state = state, onPick = vm::onPickFile, onCancel = vm::onCancel)
            }
            composable<Settings> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onManageProviders = { navController.navigate(ManageProviders) },
                    onOpenAbout = { navController.navigate(About) },
                    onOpenDebug = if (enableDebugTools) {
                        { navController.navigate(DebugSettings) }
                    } else null,
                )
            }
            composable<About> {
                val uriHandler = LocalUriHandler.current
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onViewLicense = { navController.navigate(LicenseViewer) },
                    onViewAttributions = { navController.navigate(Attributions) },
                    onViewSource = { uriHandler.openUri(GITHUB_URL) },
                )
            }
            composable<LicenseViewer> {
                LicenseViewerScreen(onBack = { navController.popBackStack() })
            }
            composable<Attributions> {
                AttributionsScreen(onBack = { navController.popBackStack() })
            }
            composable<DebugSettings> {
                DebugSettingsScreen(
                    onBack = { navController.popBackStack() },
                    collections = collections,
                    templates = templates,
                    locations = locations,
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
