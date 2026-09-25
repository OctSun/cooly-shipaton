package com.octsun.cooly.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.octsun.cooly.config.AppConfig
import com.octsun.cooly.di.AppContainer
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.i18n.stringsFor
import com.octsun.cooly.platform.AppServices
import com.octsun.cooly.platform.BannerAd
import com.octsun.cooly.presentation.CoolyViewModel
import com.octsun.cooly.presentation.PermissionState
import com.octsun.cooly.presentation.ViewMode
import com.octsun.cooly.ui.components.LayerFilterRow
import com.octsun.cooly.ui.components.PlusSheet
import com.octsun.cooly.ui.components.SavedPlacesSheet
import com.octsun.cooly.ui.components.SettingsSheet
import com.octsun.cooly.ui.components.SpotDetailSheet
import com.octsun.cooly.ui.components.SpotList
import com.octsun.cooly.ui.components.SpotMap
import com.octsun.cooly.ui.components.StatusBar
import com.octsun.cooly.ui.theme.CoolyPalette
import com.octsun.cooly.ui.theme.CoolyTheme

/** Height reserved by the floating toggle+chips overlay; the list starts below it. */
private val CONTROLS_OVERLAY_HEIGHT = 112.dp

/** App entry point. The [AppContainer] is created once inside the ViewModel so its HttpClient
 *  is released in onCleared. */
@Composable
fun CoolyApp(services: AppServices) {
    CoolyTheme {
        val vm: CoolyViewModel = viewModel { CoolyViewModel(AppContainer(services)) }
        // Root surface: without it the platform window background (often light) shows
        // through gaps and during transitions in dark mode.
        val state by vm.uiState.collectAsStateWithLifecycle()
        val strings = remember(services.languageCode) { stringsFor(services.languageCode) }

        // The user may grant location in system Settings and come back — re-check on
        // resume so the denied screen unsticks without another tap.
        LifecycleResumeEffect(Unit) {
            vm.recheckPermission()
            onPauseOrDispose { }
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            when (state.permission) {
                PermissionState.GRANTED -> MainScreen(vm, services, strings)
                else -> PermissionScreen(
                    strings = strings,
                    denied = state.permission == PermissionState.DENIED,
                    onRequest = vm::requestPermissionAndLoad,
                    onOpenSettings = vm::openLocationSettings,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun MainScreen(vm: CoolyViewModel, services: AppServices, strings: Strings) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val spots = if (state.showFavoritesOnly) state.favoriteSpots else state.visibleSpots

    val snackbarHostState = remember { SnackbarHostState() }
    val nextMessage = state.messages.firstOrNull()
    LaunchedEffect(nextMessage?.id) {
        nextMessage?.let {
            snackbarHostState.showSnackbar(it.text)
            vm.consumeMessage(it.id)
        }
    }

    var bannerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // Refetch on resume so hours-old readings are never presented as current.
    // Repository TTLs make a quick re-open cost zero network.
    LifecycleResumeEffect(Unit) {
        vm.refreshIfStale()
        onPauseOrDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = strings.appName,
                onRefresh = vm::refresh,
                onSettings = vm::openSettings,
                onPlaces = vm::openPlaces,
                refreshLabel = strings.refresh,
                settingsLabel = strings.settings,
                placesLabel = strings.savedPlaces,
            )
            StatusBar(
                env = state.env,
                strings = strings,
                temperatureUnit = state.temperatureUnit,
                nearestRefuge = state.nearestRefuge,
                isLoading = state.isLoading,
                onNearestClick = { vm.selectSpot(it.id) },

            )
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.error?.let { err ->
                ErrorBanner(
                    isLocationError = err == CoolyViewModel.ERROR_LOCATION,
                    strings = strings,
                    onRetry = vm::refresh,
                    onOpenSettings = vm::openLocationSettings,
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // The map stays composed while the list is shown, so switching back
                // preserves the user's viewport instead of resetting the camera.
                SpotMap(
                    userLocation = state.userLocation,
                    spots = spots,
                    strings = strings,
                    onSpotClick = { vm.selectSpot(it.id) },
                    modifier = Modifier.fillMaxSize(),
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.viewMode == ViewMode.LIST,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        SpotList(
                            spots = spots,
                            hasHiddenSpots = state.hasHiddenSpots,
                            strings = strings,
                            emptyText = if (state.showFavoritesOnly) strings.noFavorites else strings.noSpotsFound,
                            onSpotClick = { vm.selectSpot(it.id) },
                            onUnlockClick = vm::unlockAllSpots,
                            adInFlight = state.adInFlight,
                            topPadding = CONTROLS_OVERLAY_HEIGHT,
                        )
                    }
                }

                // Floating controls OVER the map/list: gives the hero map ~120dp more
                // vertical space vs the old stacked-bands layout.
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // No wrapper Surface: the segmented row draws its own capsule — a second
                    // white capsule behind it read as a rendering glitch. Opaque
                    // button containers are set inside ViewModeToggle instead.
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ViewModeToggle(
                            mode = state.viewMode,
                            onSelect = vm::setViewMode,
                            listLabel = strings.listView,
                            mapLabel = strings.mapView,
                        )
                    }
                    LayerFilterRow(
                        activeLayers = state.activeLayers,
                        onToggle = vm::toggleLayer,
                        strings = strings,
                        showFavoritesOnly = state.showFavoritesOnly,
                        onToggleFavorites = vm::toggleFavoritesOnly,
                        toiletUnlocked = state.toiletUnlocked,
                        isDanger = state.isDanger,
                        onFindToilets = vm::unlockToilets,
                    )
                }

                // Ad-unlock notice as a floating pill anchored to the map's bottom edge.
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.hasHiddenSpots,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                ) {
                    HiddenSpotsNotice(
                        text = strings.showingCount(state.visibleSpots.size, state.filteredSpots.size),
                        enabled = !state.adInFlight,
                        onClick = vm::unlockAllSpots,
                    )
                }
                // Favorites filter with nothing saved would otherwise blank the map with
                // no explanation — surface the same hint the list shows.
                if (state.viewMode == ViewMode.MAP && state.showFavoritesOnly && spots.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    ) {
                        Text(
                            strings.noFavorites,
                            fontSize = 14.sp,
                            color = CoolyPalette.Sub,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                // POI fetch failed and we have nothing to show: an explicit retry card so
                // the map/list is never silently blank (a heat-safety app must not look like
                // "no places exist here" when the network just hiccuped).
                if (!state.isLoading && state.spotsFailed && state.allSpots.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                strings.spotsLoadFailed,
                                fontSize = 14.sp,
                                color = CoolyPalette.Sub,
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = vm::refresh, modifier = Modifier.padding(top = 8.dp)) {
                                Text(strings.retry)
                            }
                        }
                    }
                }
                // Prominent loading state on the first load.
                if (state.isLoading && state.allSpots.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(strings.loading, color = CoolyPalette.Sub, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }

            if (AppConfig.ADS_ENABLED) {
                BannerAd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .onSizeChanged { bannerHeightPx = it.height },
                )
            } else {
                // Ads hidden: still inset the bottom content above the nav bar.
                Spacer(Modifier.fillMaxWidth().navigationBarsPadding())
            }
        }

        // Snackbar floats just above the banner ad, using its measured height.
        // navigationBarsPadding keeps it out of the gesture-nav area when ads are off,
        // and stacks correctly with the banner (whose measured height excludes the inset).
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = with(density) { bannerHeightPx.toDp() } + 8.dp),
        )
    }

    state.selectedSpot?.let { spot ->
        SpotDetailSheet(
            spot = spot,
            strings = strings,
            isFavorite = spot.id in state.favoriteIds,
            onDismiss = { vm.selectSpot(null) },
            onDirections = { vm.openDirections(spot) },
            onToggleFavorite = { vm.toggleFavorite(spot) },
        )
    }

    if (state.showPlaces) {
        SavedPlacesSheet(
            strings = strings,
            temperatureUnit = state.temperatureUnit,
            savedPlaces = state.savedPlaces,
            placeEnv = state.placeEnv,
            placeWeeklyMax = state.placeWeeklyMax,
            searchQuery = state.placeSearchQuery,
            searchResults = state.placeSearchResults,
            searchLoading = state.placeSearchLoading,
            onQueryChange = vm::setPlaceSearchQuery,
            onAdd = vm::addPlace,
            onRemove = vm::removePlace,
            onDismiss = vm::closePlaces,
        )
    }

    if (state.showPlusSheet) {
        PlusSheet(
            strings = strings,
            priceLabel = state.plusPriceLabel,
            purchaseInFlight = state.purchaseInFlight,
            error = state.plusError,
            onPurchase = vm::purchasePlus,
            onRestore = vm::restorePlus,
            onPrivacyPolicy = vm::openPrivacyPolicy,
            onDismiss = vm::closePlusSheet,
        )
    }

    if (state.showSettings) {
        SettingsSheet(
            strings = strings,
            currentUnit = state.temperatureUnit,
            languageLabel = services.languageCode.uppercase(),
            showPlus = state.purchasesAvailable,
            isPlus = state.isPlus,
            showAlerts = services.alerts != null,
            alertsEnabled = state.alertsEnabled,
            onAlertsChange = vm::setAlertsEnabled,
            onUpgrade = vm::upgradeFromSettings,
            onUnitChange = vm::setTemperatureUnit,
            onPrivacyPolicy = vm::openPrivacyPolicy,
            onDismiss = vm::closeSettings,
        )
    }

    if (state.showOnboarding && !state.isLoading) {
        OnboardingDialog(strings = strings, onDismiss = vm::dismissOnboarding)
    }
}

@Composable
private fun TopBar(
    title: String,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onPlaces: () -> Unit,
    refreshLabel: String,
    settingsLabel: String,
    placesLabel: String,
) {
    Surface(color = CoolyPalette.TopBar, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "❄️ $title",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onPlaces, modifier = Modifier.semantics { contentDescription = placesLabel }) {
                Text("🌍", color = Color.White, fontSize = 20.sp)
            }
            IconButton(onClick = onRefresh, modifier = Modifier.semantics { contentDescription = refreshLabel }) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = settingsLabel }) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun HiddenSpotsNotice(text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = CoolyPalette.InfoTint,
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .heightIn(min = 48.dp)
            .semantics { role = Role.Button; contentDescription = text },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, color = CoolyPalette.BrandDeep, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("›", color = CoolyPalette.BrandDeep, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorBanner(
    isLocationError: Boolean,
    strings: Strings,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(color = CoolyPalette.ErrorBg, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (isLocationError) strings.locationUnavailable else strings.networkError,
                color = CoolyPalette.ErrorInk,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            if (isLocationError) {
                TextButton(onClick = onOpenSettings) { Text(strings.openSettings) }
            }
            TextButton(onClick = onRetry) { Text(strings.retry) }
        }
    }
}

@Composable
private fun OnboardingDialog(strings: Strings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
        title = { Text(strings.appName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Only what the permission screen has NOT already taught: the 🌍 watched
                // places feature — repeating the value bullets got the dialog skimmed.
                Text(strings.onboardingPlacesHint, fontSize = 15.sp)
                if (AppConfig.ADS_ENABLED) {
                    Text(strings.unlockAllSpotsDesc, color = CoolyPalette.Sub, fontSize = 13.sp)
                }
            }
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeToggle(
    mode: ViewMode,
    onSelect: (ViewMode) -> Unit,
    listLabel: String,
    mapLabel: String,
) {
    // Opaque containers (the row floats over the live map, so transparency would be
    // unreadable) — and exactly ONE capsule: the row's own outline.
    val segColors = SegmentedButtonDefaults.colors(
        inactiveContainerColor = MaterialTheme.colorScheme.surface,
    )
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = mode == ViewMode.MAP,
            onClick = { onSelect(ViewMode.MAP) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = segColors,
        ) { Text(mapLabel) }
        SegmentedButton(
            selected = mode == ViewMode.LIST,
            onClick = { onSelect(ViewMode.LIST) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = segColors,
        ) { Text(listLabel) }
    }
}
