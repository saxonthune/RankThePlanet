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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpRect
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
import com.saxonthune.ranktheplanet.util.PinTrace
import com.saxonthune.ranktheplanet.util.tuneMapForFastTaps
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraProjection
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

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = -73.9855, latitude = 40.7580),
            zoom = 12.0,
        )
    )

    val snackbarHostState = remember { SnackbarHostState() }

    // Strip iOS map gesture recognizers that delay single-tap recognition by ~300ms.
    // The MLNMapView is created lazily, so retry briefly until it shows up in the view tree.
    LaunchedEffect(Unit) {
        repeat(40) {
            if (tuneMapForFastTaps()) return@LaunchedEffect
            delay(50)
        }
    }

    // PinTrace: log camera-motion transitions and sample render coverage during pan.
    // Goal is to confirm or rule out stale tile-cache theory — see doc03.04 / doc03.05.
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.isCameraMoving }.collect { moving ->
            PinTrace.log(
                "cam/${if (moving) "start" else "stop"}",
                "zoom" to cameraState.position.zoom.toFixed2(),
            )
            // Experiment: re-push the source on pan start. If maplibre-native's
            // iOS GeoJsonSource invalidates cached tiles on setData, this should
            // heal pins stuck missing at a zoom level mid-session.
            if (moving) vm.pinController.forceRedraw()
            cameraState.projection?.let { probePinCoverage(it, vm.pinController.currentPins) }
        }
    }
    LaunchedEffect(cameraState, vm) {
        while (true) {
            if (cameraState.isCameraMoving) {
                cameraState.projection?.let { probePinCoverage(it, vm.pinController.currentPins) }
            }
            delay(150)
        }
    }

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
                    baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/bright"),
                    options = MapOptions(ornamentOptions = OrnamentOptions.OnlyLogo),
                    onMapLoadFinished = {
                        PinTrace.log("map/loadFinished")
                        vm.pinController.forceRedraw()
                    },
                    onMapLongClick = { position, _ ->
                        vm.startDraft(lat = position.latitude, lng = position.longitude)
                        ClickResult.Consume
                    },
                ) {
                    PinLayers(
                        controller = vm.pinController,
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

/**
 * For each in-memory pin whose lat/lng falls inside the visible bbox, probe
 * `queryRenderedFeatures` at the pin's screen position against the `pins-body`
 * layer. A pin that is geometrically in view but produces zero rendered features
 * is the smoking gun for the stale-tile-cache theory (doc03.04 / doc03.05).
 *
 * Cheap — O(visible pins) per call. queryRenderedFeatures is a sync delegate to
 * the native render thread; safe from a coroutine on the main dispatcher.
 */
private fun probePinCoverage(projection: CameraProjection, pins: List<PinUi>) {
    val bbox = projection.queryVisibleBoundingBox()
    val inView = pins.filter {
        it.lat in bbox.south..bbox.north && it.lng in bbox.west..bbox.east
    }
    val missing = mutableListOf<String>()
    var rendered = 0
    // Pin body icon is SymbolAnchor.Bottom, so the geometry point sits at the
    // icon's bottom edge. A point-offset query at that location is fragile —
    // it hits sub-pixel boundaries of the rendered icon and can false-negative.
    // Query a small rect covering the icon's screen footprint instead.
    inView.forEach { pin ->
        val anchor = projection.screenLocationFromPosition(Position(longitude = pin.lng, latitude = pin.lat))
        val rect = DpRect(
            left = anchor.x - 8.dp,
            top = anchor.y - 36.dp,
            right = anchor.x + 8.dp,
            bottom = anchor.y + 2.dp,
        )
        val hits = projection.queryRenderedFeatures(rect, setOf("pins-body"))
        if (hits.isEmpty()) missing += pin.locationName else rendered++
    }
    PinTrace.log(
        "probe",
        "expected" to inView.size,
        "rendered" to rendered,
        "missing" to (if (missing.isEmpty()) "-" else missing.joinToString(",")),
    )
}

private fun Double.toFixed2(): String {
    val scaled = (this * 100).toLong()
    val whole = scaled / 100
    val frac = (if (scaled < 0) -scaled else scaled) % 100
    return "$whole.${frac.toString().padStart(2, '0')}"
}
