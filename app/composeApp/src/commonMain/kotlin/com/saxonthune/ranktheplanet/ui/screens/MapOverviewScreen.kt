package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.LocationRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.location.LocationProviderRegistry
import com.saxonthune.ranktheplanet.data.projection.OverviewProjection
import com.saxonthune.ranktheplanet.data.session.SessionStateStore
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.Viewport
import com.saxonthune.ranktheplanet.nav.MapMode
import com.saxonthune.ranktheplanet.ui.RtpErrorState
import com.saxonthune.ranktheplanet.util.tuneMapForFastTaps
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
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
    locations: LocationRepository,
    providerRegistry: LocationProviderRegistry,
    projection: OverviewProjection,
    session: SessionStateStore,
    onCancelAdd: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onEditReview: (EntryId) -> Unit,
    onPickCollectionForDraft: (CollectionId) -> Unit,
    onNewCollectionForDraft: () -> Unit,
    onGoToReview: (EntryId) -> Unit,
    onPendingReviewDismissed: () -> Unit,
    pendingNewCollectionForDraft: String? = null,
    onPendingNewCollectionForDraftConsumed: () -> Unit = {},
) {
    val vm = viewModel { MapOverviewViewModel(collections, entries, providerRegistry, templates, projection, session, locations) }

    LaunchedEffect(pendingNewCollectionForDraft) {
        val id = pendingNewCollectionForDraft ?: return@LaunchedEffect
        vm.pickCollectionForDraft(CollectionId(id))
        onPendingNewCollectionForDraftConsumed()
    }
    val state by vm.uiState.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val closeSearch = {
        searchExpanded = false
        searchQuery = ""
        vm.onQueryChange("")
    }

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

    // Restore the saved viewport once on cold start.
    var viewportApplied by remember { mutableStateOf(false) }
    LaunchedEffect(state.viewport) {
        val vp = state.viewport
        if (!viewportApplied && vp != null) {
            viewportApplied = true
            cameraState.position = CameraPosition(
                target = Position(longitude = vp.centerLng, latitude = vp.centerLat),
                zoom = vp.zoom,
                bearing = vp.bearing,
            )
        }
    }

    // Save viewport whenever the camera moves (debounce handled inside the store).
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.position }
            .distinctUntilChanged()
            .collect { pos ->
                vm.onViewportChange(
                    Viewport(
                        centerLat = pos.target.latitude,
                        centerLng = pos.target.longitude,
                        zoom = pos.zoom,
                        bearing = pos.bearing,
                    )
                )
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

    BackHandler(enabled = searchExpanded) {
        closeSearch()
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
                            when {
                                searchExpanded -> IconButton(onClick = { closeSearch() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Close search",
                                    )
                                }
                                mode is MapMode.Browse -> IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open the app menu")
                                }
                                mode is MapMode.AddingToCollection -> IconButton(onClick = onCancelAdd) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel adding")
                                }
                            }
                        },
                        title = {},
                        actions = {
                            SearchBarField(
                                expanded = searchExpanded,
                                query = searchQuery,
                                onFocus = { searchExpanded = true },
                                onSearch = { vm.onSubmitSearch(it) },
                                onQueryChange = {
                                    searchQuery = it
                                    vm.onQueryChange(it)
                                },
                            )
                        },
                    )
                    if (searchExpanded && state.searchHits.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 8.dp,
                        ) {
                            Column {
                                state.searchHits.forEach { hit ->
                                    SearchHitRow(
                                        hit = hit,
                                        onClick = {
                                            when (hit) {
                                                is SearchHitUi.ExistingEntry -> vm.pickExistingHit(hit.locationId)
                                                is SearchHitUi.Candidate -> vm.pickSearchCandidate(
                                                    hit = hit,
                                                    inAddMode = mode is MapMode.AddingToCollection,
                                                    collectionId = (mode as? MapMode.AddingToCollection)?.collectionId,
                                                )
                                            }
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
                if (mode is MapMode.AddingToCollection) {
                    Surface(
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
            floatingActionButton = {
                if (mode is MapMode.Browse) {
                    ExtendedFloatingActionButton(
                        onClick = onOpenCollections,
                        icon = { Icon(Icons.Default.Layers, contentDescription = null) },
                        text = { Text("Collections") },
                    )
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
                        vm.pinController.forceRedraw()
                    },
                    onMapLongClick = { position, _ ->
                        vm.startDraft(lat = position.latitude, lng = position.longitude)
                        ClickResult.Consume
                    },
                ) {
                    PinLayers(
                        controller = vm.pinController,
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
        onConfirmAddAtPeek = { vm.confirmAddCandidateAtPeek() },
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
            onNewCollection = { onNewCollectionForDraft() },
            onPickCollection = { vm.pickCollectionForDraft(it) },
        )
    }
}

@Composable
private fun SearchHitRow(
    hit: SearchHitUi,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (hit) {
                is SearchHitUi.ExistingEntry -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        hit.dots.take(3).forEach { dotColor ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(dotColor, CircleShape)
                            )
                        }
                    }
                }
                is SearchHitUi.Candidate -> {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hit.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            val detail = (hit as? SearchHitUi.Candidate)?.detail
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

