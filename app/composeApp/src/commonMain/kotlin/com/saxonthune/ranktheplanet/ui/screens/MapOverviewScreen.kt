package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.location.LocationProviderRegistry
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.nav.MapMode
import com.saxonthune.ranktheplanet.ui.RtpErrorState
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MapOverviewScreen(
    mode: MapMode,
    collections: CollectionRepository,
    entries: EntryRepository,
    templates: TemplateRepository,
    providerRegistry: LocationProviderRegistry,
    onCancelAdd: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onEditReview: (EntryId) -> Unit,
    onPickCollectionForDraft: (CollectionId) -> Unit,
    onNewCollectionForDraft: () -> Unit,
    onGoToReview: (EntryId) -> Unit,
    onPendingReviewDismissed: () -> Unit,
) {
    val vm = viewModel { MapOverviewViewModel(collections, entries, providerRegistry, templates) }
    val state by vm.uiState.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    var searchExpanded by remember { mutableStateOf(false) }

    // Capture surface color here — must not be read inside the MaplibreMap content lambda.
    val pinLabelHalo = MaterialTheme.colorScheme.surface

    var mapLoaded by remember { mutableStateOf(false) }

    // Workaround for maplibre-compose 0.12.1 iOS GeoJSON async setData
    // (https://github.com/maplibre/maplibre-compose/issues/738): pins added at the same zoom
    // they should render at don't appear until a real gesture triggers a render cycle. Start
    // slightly below the target zoom so pins are visible at frame 1, then animate up — by the
    // time the animation lands the source has been integrated into the render pipeline.
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = -73.9855, latitude = 40.7580),
            zoom = 11.7,
        )
    )

    LaunchedEffect(mapLoaded) {
        if (mapLoaded) {
            cameraState.animateTo(
                cameraState.position.copy(zoom = 12.0),
                duration = 150.milliseconds,
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val pending = state.pendingReview
    if (pending is PendingReviewPrompt.Pending) {
        LaunchedEffect(pending.entryId) {
            val result = snackbarHostState.showSnackbar(
                message = "Review ${pending.locationName}?",
                actionLabel = "Review",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> onGoToReview(pending.entryId)
                SnackbarResult.Dismissed -> Unit
            }
            vm.clearPendingReview()
            onPendingReviewDismissed()
        }
    }

    BackHandler(enabled = state.draft is LocationDraftSheet.Open || state.pinSheet !is PinSheet.None) {
        val draft = state.draft
        if (draft is LocationDraftSheet.Open) {
            if (draft.phase == DraftPhase.AddToCollection) vm.backToDraft()
            else vm.dismissDraft()
        } else vm.dismissSheet()
    }

    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        navigationIcon = {
                            when (mode) {
                                is MapMode.Browse -> IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open the app menu")
                                }
                                is MapMode.AddingToCollection -> IconButton(onClick = onCancelAdd) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel adding")
                                }
                            }
                        },
                        title = {},
                        actions = {
                            SearchBarField(
                                expanded = searchExpanded,
                                onFocus = { searchExpanded = true },
                                onSearch = { vm.search(it) },
                            )
                        },
                    )
                    if (searchExpanded && state.searchResults.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 8.dp,
                        ) {
                            Column {
                                state.searchResults.forEach { result ->
                                    ListItem(
                                        headlineContent = { Text(result.displayName) },
                                        modifier = Modifier.clickable {
                                            vm.startDraft(
                                                lat = result.lat,
                                                lng = result.lng,
                                                displayName = result.displayName,
                                            )
                                        },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                when (mode) {
                    is MapMode.Browse -> BottomAppBar(
                        actions = {
                            TextButton(onClick = onOpenCollections) {
                                Icon(Icons.Default.Layers, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Collections")
                            }
                        },
                    )
                    is MapMode.AddingToCollection -> Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Adding to ${mode.collectionName}")
                        }
                    }
                }
            },
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                MaplibreMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraState = cameraState,
                    baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                    options = MapOptions(ornamentOptions = OrnamentOptions.OnlyLogo),
                    onMapLoadFinished = { mapLoaded = true },
                    onMapLongClick = { position, _ ->
                        vm.startDraft(lat = position.latitude, lng = position.longitude)
                        ClickResult.Consume
                    },
                ) {
                    PinLayers(
                        pins = state.pins,
                        labelHaloColor = pinLabelHalo,
                        onPinClick = { vm.selectPin(it) },
                    )
                }
                if (state.error != null) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        RtpErrorState(
                            message = state.error!!,
                            onRetry = vm::retry,
                        )
                    }
                }
            }
        }
    }

    if (mode is MapMode.Browse) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppMenuDrawer(
                    state = state,
                    onToggleCollection = { vm.toggleCollection(it) },
                    onOpenSettings = onOpenSettings,
                )
            },
        ) {
            scaffoldContent()
        }
    } else {
        scaffoldContent()
    }

    PinSheetHost(
        sheet = state.pinSheet,
        sheetState = sheetState,
        onDismiss = { vm.dismissSheet() },
        onPickEntryFromPeek = { vm.openEntryFromPeek(it) },
        onAddEntryAtPeek = { vm.addEntryAtPeekLocation() },
        onTapEntryLocation = { vm.peekLocationFromEntry() },
        onViewCollection = { collectionId ->
            vm.dismissSheet()
            onViewCollection(collectionId)
        },
        onEditReview = { entryId ->
            vm.dismissSheet()
            onEditReview(entryId)
        },
    )

    val currentDraft = state.draft
    if (currentDraft is LocationDraftSheet.Open) {
        LocationDraftSheetHost(
            draft = currentDraft,
            mode = mode,
            nearbyCandidates = state.nearbyCandidates,
            isResolvingNearby = state.isResolvingNearby,
            collectionPicks = state.collectionPicks,
            onDismiss = { vm.dismissDraft() },
            onCancelAdd = {
                vm.dismissDraft()
                onCancelAdd()
            },
            onOpenAddToCollection = { vm.openAddToCollection() },
            onFindNearby = { vm.findNearby() },
            onAdoptCandidate = { vm.adoptCandidate(it) },
            onKeepCoordinates = { vm.keepCoordinates() },
            onBackToDraft = { vm.backToDraft() },
            onNewCollection = {
                vm.dismissDraft()
                onNewCollectionForDraft()
            },
            onPickCollection = { vm.pickCollectionForDraft(it) },
        )
    }
}
