package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
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
    onOpenSettings: () -> Unit,
    onNewCollection: () -> Unit,
    onImport: () -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onEditReview: (EntryId) -> Unit,
    onPickCollectionForDraft: (CollectionId) -> Unit,
    onNewCollectionForDraft: () -> Unit,
    onGoToReview: (EntryId) -> Unit,
    onPendingReviewDismissed: () -> Unit,
    initialFilterCollectionId: String? = null,
    pendingNewCollectionForDraft: String? = null,
    onPendingNewCollectionForDraftConsumed: () -> Unit = {},
) {
    val vm = viewModel { MapOverviewViewModel(collections, entries, providerRegistry, templates, projection, session, locations) }

    LaunchedEffect(initialFilterCollectionId) {
        val id = initialFilterCollectionId ?: return@LaunchedEffect
        vm.applyFilter(setOf(CollectionId(id)))
    }

    LaunchedEffect(pendingNewCollectionForDraft) {
        val id = pendingNewCollectionForDraft ?: return@LaunchedEffect
        vm.pickCollectionForDraft(CollectionId(id))
        onPendingNewCollectionForDraftConsumed()
    }
    val state by vm.uiState.collectAsState()

    val sheetState = rememberModalBottomSheetState()

    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sheetHeightDp by remember { mutableStateOf(0.dp) }

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
                cameraState.projection?.queryVisibleBoundingBox()?.let { bbox ->
                    vm.onVisibleBoundsChange(
                        south = bbox.southwest.latitude,
                        west = bbox.southwest.longitude,
                        north = bbox.northeast.latitude,
                        east = bbox.northeast.longitude,
                    )
                }
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

    // Center the camera on a search-picked provider candidate, offsetting the target
    // upward by the sheet height so the marker sits in the visible map area above the
    // peek. Bottom padding shifts where the camera's "center" lands on screen.
    val pinSheet = state.pinSheet
    LaunchedEffect(pinSheet, sheetHeightDp) {
        val peek = pinSheet as? PinSheet.Peek ?: return@LaunchedEffect
        val candidate = peek.candidateLocation ?: return@LaunchedEffect
        cameraState.animateTo(
            finalPosition = cameraState.position.copy(
                target = Position(
                    longitude = candidate.coordinates.lng,
                    latitude = candidate.coordinates.lat,
                ),
                padding = PaddingValues(bottom = sheetHeightDp),
            ),
        )
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
                                mode is MapMode.Browse -> IconButton(onClick = onOpenSettings) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                                trailingIcon = if (state.isSearchResultsMode) {
                                    {
                                        IconButton(onClick = {
                                            vm.clearSearch()
                                            searchQuery = ""
                                            vm.onQueryChange("")
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search results")
                                        }
                                    }
                                } else null,
                            )
                        },
                    )
                    if (searchExpanded && searchQuery.isNotBlank()) {
                        val density = LocalDensity.current
                        val windowInfo = LocalWindowInfo.current
                        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                        val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
                        // TopAppBar standard height is 64.dp; leave the rest for the dropdown so
                        // the keyboard never clips the list — the IME inset is the load-bearing constraint.
                        val maxDropdownHeight = (screenHeight - 64.dp - imeBottom).coerceAtLeast(0.dp)
                        val hasCandidate = state.searchHits.any { it is SearchHitUi.Candidate }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxDropdownHeight),
                            tonalElevation = 8.dp,
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TextButton(
                                        onClick = { vm.submitSearch() },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                        Text("Search")
                                    }
                                    if (hasCandidate) {
                                        TextButton(
                                            onClick = {
                                                vm.commitSearchToMap()
                                                searchExpanded = false
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 8.dp),
                                            )
                                            Text("Search on map")
                                        }
                                    }
                                }
                                HorizontalDivider()
                                state.searchHits.forEach { hit ->
                                    SearchHitRow(
                                        hit = hit,
                                        onClick = {
                                            closeSearch()
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
                        onClick = { vm.openCollectionList() },
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
                    CandidateMarkerLayer(sheet = state.pinSheet)
                    SearchCandidatePinLayer(
                        candidates = state.searchContext?.candidates,
                        onCandidateTap = { hit ->
                            vm.pickSearchCandidate(
                                hit = hit,
                                inAddMode = mode is MapMode.AddingToCollection,
                                collectionId = (mode as? MapMode.AddingToCollection)?.collectionId,
                            )
                        },
                    )
                }
                if (state.showSearchThisAreaChip || state.isSearchingArea) {
                    val searching = state.isSearchingArea
                    AssistChip(
                        onClick = { vm.searchThisArea() },
                        enabled = !searching,
                        label = {
                            Text(if (searching) "Searching…" else "Search this area")
                        },
                        leadingIcon = {
                            if (searching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                )
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = AssistChipDefaults.assistChipBorder(enabled = !searching),
                        elevation = AssistChipDefaults.assistChipElevation(elevation = 3.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 72.dp),
                    )
                }
                if (state.filterContext != null) {
                    val filterCtx: Set<CollectionId> = state.filterContext!!
                    val singleRow = filterCtx.takeIf { it.size == 1 }?.first()
                        ?.let { id -> state.collectionRows.find { it.id == id } }
                    val chipLabel = singleRow?.name ?: "Filtered: ${filterCtx.size} Collections"
                    InputChip(
                        selected = true,
                        onClick = { vm.openCollectionList(preselection = filterCtx) },
                        label = { Text(chipLabel) },
                        leadingIcon = {
                            if (singleRow != null) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(singleRow.color, CircleShape),
                                )
                            } else {
                                Box(modifier = Modifier.size(16.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.CenterStart)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.CenterEnd)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear filter",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { vm.clearFilter() },
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 120.dp),
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

    scaffoldContent()

    val sheet = state.collectionListSheet
    if (sheet is CollectionListSheet.Open) {
        CollectionListSheet(
            collections = collections,
            entries = entries,
            preselection = sheet.preselection,
            onDismiss = vm::closeCollectionList,
            onTapCollection = { id ->
                vm.closeCollectionList()
                onViewCollection(id)
            },
            onNewCollection = {
                vm.closeCollectionList()
                onNewCollection()
            },
            onImport = {
                vm.closeCollectionList()
                onImport()
            },
            onApplyFilter = vm::applyFilter,
        )
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
        onJumpToCollection = { collectionId ->
            vm.dismissSheet()
            vm.applyFilter(setOf(collectionId))
        },
        onEditReview = { entryId ->
            vm.dismissSheet()
            onEditReview(entryId)
        },
        onContentHeightChange = { sheetHeightDp = it },
    )

    LaunchedEffect(Unit) {
        vm.jumpToViewport.collect { vp ->
            cameraState.animateTo(
                finalPosition = cameraState.position.copy(
                    target = Position(longitude = vp.centerLng, latitude = vp.centerLat),
                    zoom = vp.zoom,
                    bearing = vp.bearing,
                ),
            )
        }
    }

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
            onManualNameChange = { vm.setManualName(it) },
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

