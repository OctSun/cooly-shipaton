package com.octsun.cooly.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octsun.cooly.config.AppConfig
import com.octsun.cooly.data.PurchaseOutcome
import com.octsun.cooly.data.RevenueCatEntitlements
import com.octsun.cooly.di.AppContainer
import com.octsun.cooly.domain.distanceMeters
import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.domain.model.EnvStatus
import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.domain.model.RiskLevel
import com.octsun.cooly.domain.model.SavedPlace
import com.octsun.cooly.domain.model.SpotLayer
import com.octsun.cooly.domain.model.TemperatureUnit
import com.octsun.cooly.domain.walkingMinutes
import com.octsun.cooly.platform.nowMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ViewMode { MAP, LIST }

/** Toilets are free safety info: shown by default whenever they aren't ad-gated. */
private fun defaultLayers(): Set<SpotLayer> = buildSet {
    add(SpotLayer.COOL_INDOOR)
    add(SpotLayer.WATER)
    add(SpotLayer.SHADE)
    if (!AppConfig.ADS_ENABLED) add(SpotLayer.TOILET)
}

enum class PermissionState { UNKNOWN, GRANTED, DENIED }

/** One-shot user message (snackbar) with a stable id so a queue can't drop/duplicate. */
data class UiMessage(val id: Long, val text: String)

data class CoolyUiState(
    val permission: PermissionState = PermissionState.UNKNOWN,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** True when the POI fetch failed (weather may still have loaded) — drives an inline
     *  retry state so the map/list is never silently blank. */
    val spotsFailed: Boolean = false,
    val userLocation: GeoPoint? = null,
    val env: EnvStatus? = null,
    val allSpots: List<CoolSpot> = emptyList(),
    val activeLayers: Set<SpotLayer> = defaultLayers(),
    val unlockedAllSpots: Boolean = false,
    val toiletUnlocked: Boolean = false,
    val viewMode: ViewMode = ViewMode.MAP,
    val selectedSpotId: String? = null,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    /** Background heat/air danger alerts (opt-in; enabling asks for notification permission). */
    val alertsEnabled: Boolean = false,
    val favorites: List<CoolSpot> = emptyList(),
    val everSavedFavorite: Boolean = false,
    val showFavoritesOnly: Boolean = false,
    val showSettings: Boolean = false,
    val showOnboarding: Boolean = false,
    val adInFlight: Boolean = false,
    val messages: List<UiMessage> = emptyList(),
    // Saved places (Cooly Plus feature 1)
    val isPlus: Boolean = false,
    val savedPlaces: List<SavedPlace> = emptyList(),
    val placeEnv: Map<String, EnvStatus> = emptyMap(),
    /** Max feels-like (°C) over the next 7 days per saved place — trip planning. */
    val placeWeeklyMax: Map<String, Double> = emptyMap(),
    val placeSearchQuery: String = "",
    val placeSearchResults: List<SavedPlace> = emptyList(),
    val placeSearchLoading: Boolean = false,
    val showPlaces: Boolean = false,
    val showPlusSheet: Boolean = false,
    /** False until a RevenueCat key is configured — all Plus surfaces hide (Apple 2.1). */
    val purchasesAvailable: Boolean = false,
    val purchaseInFlight: Boolean = false,
    val plusPriceLabel: String? = null,
    val plusError: String? = null,
) {
    val favoriteIds: Set<String> get() = favorites.map { it.id }.toSet()

    val isDanger: Boolean get() = env?.riskLevel == RiskLevel.DANGER

    private val effectiveLayers: Set<SpotLayer>
        get() = if (isDanger) activeLayers + SpotLayer.TOILET else activeLayers

    val filteredSpots: List<CoolSpot>
        get() = allSpots
            .filter { it.layer in effectiveLayers }
            .filter { !showFavoritesOnly || it.id in favoriteIds }

    val visibleSpots: List<CoolSpot>
        get() = when {
            showFavoritesOnly || unlockedAllSpots || isDanger -> filteredSpots
            else -> filteredSpots.take(AppConfig.FREE_SPOT_LIMIT)
        }

    val hasHiddenSpots: Boolean
        get() = !unlockedAllSpots && !showFavoritesOnly && !isDanger &&
            filteredSpots.size > AppConfig.FREE_SPOT_LIMIT

    val selectedSpot: CoolSpot?
        get() = allSpots.firstOrNull { it.id == selectedSpotId }
            ?: favorites.firstOrNull { it.id == selectedSpotId }

    val nearestRefuge: CoolSpot?
        get() = allSpots.firstOrNull { it.layer == SpotLayer.COOL_INDOOR }

    val favoriteSpots: List<CoolSpot>
        get() {
            val origin = userLocation ?: return favorites
            return favorites
                .map { spot ->
                    val d = distanceMeters(origin, spot.point)
                    spot.copy(distanceMeters = d, walkingMinutes = walkingMinutes(d))
                }
                .sortedBy { it.distanceMeters }
        }
}

class CoolyViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CoolyUiState(
            temperatureUnit = container.preferences.temperatureUnit(),
            alertsEnabled = container.preferences.alertsEnabled(),
            favorites = container.preferences.favorites(),
            everSavedFavorite = container.preferences.hasEverSavedFavorite(),
            // First-run onboarding is independent of monetization.
            showOnboarding = !container.preferences.onboardingComplete(),
            // With ads off there's no rewarded gating: everything is free, no ad CTAs.
            unlockedAllSpots = !AppConfig.ADS_ENABLED,
            toiletUnlocked = !AppConfig.ADS_ENABLED,
            purchasesAvailable = container.entitlements is RevenueCatEntitlements,
        ),
    )
    val uiState: StateFlow<CoolyUiState> = _uiState.asStateFlow()

    private val location get() = container.services.location
    private val ads get() = container.services.ads
    private var messageCounter = 0L

    private var placeSearchJob: Job? = null
    private var lastRefreshAt = 0L
    private val placeEnvFetchedAt = mutableMapOf<String, Long>()

    init {
        if (AppConfig.ADS_ENABLED) ads.preloadRewarded()
        _uiState.update { it.copy(savedPlaces = container.preferences.savedPlaces()) }
        viewModelScope.launch {
            container.entitlements.isPlus.collect { plus ->
                _uiState.update { it.copy(isPlus = plus) }
            }
        }
        viewModelScope.launch { container.entitlements.refresh() }
        if (location.hasPermission) {
            _uiState.update { it.copy(permission = PermissionState.GRANTED) }
            refresh()
        }
        refreshPlaceEnv()
    }

    fun requestPermissionAndLoad() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val granted = location.ensurePermission()
            _uiState.update {
                it.copy(permission = if (granted) PermissionState.GRANTED else PermissionState.DENIED)
            }
            if (granted) refresh()
        }
    }

    fun openLocationSettings() = container.services.links.openAppSettings()

    /**
     * Called on every app resume: the user may have granted location in system Settings
     * after a denial — without this the denied screen stays stuck until another tap.
     */
    fun recheckPermission() {
        if (_uiState.value.permission == PermissionState.GRANTED) return
        if (location.hasPermission) {
            _uiState.update { it.copy(permission = PermissionState.GRANTED, error = null) }
            refresh()
        }
    }

    /**
     * Cheap resume hook: refetch when the last refresh is older than a minute — the
     * repository caches make a fresh-enough resume cost zero network. A heat-safety app
     * must never present the morning's reading as current.
     */
    fun refreshIfStale() {
        if (_uiState.value.permission != PermissionState.GRANTED) return
        if (nowMillis() - lastRefreshAt < STALE_REFRESH_MS) return
        refresh()
    }

    fun refresh() {
        lastRefreshAt = nowMillis()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val point = location.currentLocation()
            if (point == null) {
                // Distinguish a revoked permission (recoverable) from a transient fix failure.
                if (!location.hasPermission) {
                    _uiState.update {
                        it.copy(isLoading = false, permission = PermissionState.DENIED)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = ERROR_LOCATION) }
                }
                return@launch
            }
            _uiState.update { it.copy(userLocation = point) }
            loadData(point)
        }
    }

    private suspend fun loadData(point: GeoPoint) = coroutineScope {
        // Weather and POIs are independent — fetch them in parallel.
        val envDeferred = async {
            runCatching { container.repository.loadEnv(point) }.getOrNull()
        }
        val spotsDeferred = async {
            runCatching { container.repository.loadSpots(point) }.getOrNull()
        }
        val env = envDeferred.await()
        val spots = spotsDeferred.await()

        // One call failed but we still have older data: say so instead of silently
        // presenting stale values as current. A degraded env (one weather
        // endpoint down → "—" on screen) counts as a partial failure too.
        val degradedEnv = env != null && (!env.hasTemp || !env.hasAqi)
        val partialFailure = (env == null) != (spots == null) || degradedEnv
        if (partialFailure) enqueueMessage(container.strings.partialUpdateFailed)

        _uiState.update { state ->
            val nextSpots = spots ?: state.allSpots
            // Drop a stale selection that no longer exists in either list.
            val validSelection = state.selectedSpotId?.takeIf { id ->
                nextSpots.any { it.id == id } || state.favorites.any { it.id == id }
            }
            state.copy(
                isLoading = false,
                env = env ?: state.env,
                allSpots = nextSpots,
                selectedSpotId = validSelection,
                error = if (env == null && spots == null) ERROR_NETWORK else null,
                // Flag a spots-only failure so the content area can show an inline retry
                // instead of an unexplained empty map/list.
                spotsFailed = spots == null && nextSpots.isEmpty(),
            )
        }

        // Snapshot the fresh reading + location for the widget and the alert worker.
        if (env != null) {
            container.preferences.setLastReading(env, nowMillis())
            container.preferences.setLastPoint(point)
            container.services.widgets?.refresh()
        }
    }

    fun toggleLayer(layer: SpotLayer) {
        _uiState.update {
            val next = it.activeLayers.toMutableSet()
            if (!next.add(layer)) next.remove(layer)
            it.copy(activeLayers = next)
        }
    }

    fun setViewMode(mode: ViewMode) = _uiState.update { it.copy(viewMode = mode) }

    fun selectSpot(id: String?) = _uiState.update { it.copy(selectedSpotId = id) }

    fun toggleFavoritesOnly() = _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }

    fun openDirections(spot: CoolSpot) {
        val label = spot.name.ifBlank { spot.type.name }
        container.services.directions.openWalkingDirections(spot.point, label)
    }

    fun toggleFavorite(spot: CoolSpot) {
        // Persist AFTER the CAS commit — update {} may retry its lambda.
        _uiState.update { state ->
            val next = if (spot.id in state.favoriteIds) {
                state.favorites.filterNot { it.id == spot.id }
            } else {
                state.favorites + spot
            }
            state.copy(favorites = next, everSavedFavorite = state.everSavedFavorite || next.isNotEmpty())
        }
        val committed = _uiState.value.favorites
        container.preferences.setFavorites(committed)
        if (committed.isNotEmpty()) container.preferences.markFavoriteSaved()
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        container.preferences.setTemperatureUnit(unit)
        _uiState.update { it.copy(temperatureUnit = unit) }
    }

    /** Toggle background danger alerts. The platform controller schedules/cancels the
     *  periodic check and, when enabling, may prompt for notification permission. */
    fun setAlertsEnabled(enabled: Boolean) {
        container.preferences.setAlertsEnabled(enabled)
        _uiState.update { it.copy(alertsEnabled = enabled) }
        container.services.alerts?.setEnabled(enabled)
    }

    fun openSettings() = _uiState.update { it.copy(showSettings = true) }
    fun closeSettings() = _uiState.update { it.copy(showSettings = false) }

    fun dismissOnboarding() {
        container.preferences.setOnboardingComplete()
        _uiState.update { it.copy(showOnboarding = false) }
    }

    fun openPrivacyPolicy() = container.services.links.openUrl(AppConfig.PRIVACY_POLICY_URL)

    /** Remove a shown message from the queue by id. */
    fun consumeMessage(id: Long) =
        _uiState.update { it.copy(messages = it.messages.filterNot { m -> m.id == id }) }

    private fun enqueueMessage(text: String) {
        // Increment outside the update lambda: MutableStateFlow.update may retry it.
        val id = ++messageCounter
        _uiState.update { it.copy(messages = it.messages + UiMessage(id, text)) }
    }

    fun unlockAllSpots() = runRewarded { unlocked ->
        if (unlocked) _uiState.update { it.copy(unlockedAllSpots = true) }
    }

    fun unlockToilets() = runRewarded { unlocked ->
        if (unlocked) {
            _uiState.update {
                it.copy(toiletUnlocked = true, activeLayers = it.activeLayers + SpotLayer.TOILET)
            }
        }
    }

    // ---- Saved places (Cooly Plus feature 1) ----

    fun openPlaces() {
        _uiState.update { it.copy(showPlaces = true) }
        refreshPlaceEnv()
    }

    fun closePlaces() = _uiState.update {
        it.copy(showPlaces = false, placeSearchQuery = "", placeSearchResults = emptyList())
    }

    fun openPlusSheet() {
        _uiState.update { it.copy(showPlusSheet = true, plusError = null) }
        loadPlusPrice()
    }

    /** Plus entry point from Settings — swaps the settings sheet for the paywall. */
    fun upgradeFromSettings() {
        _uiState.update { it.copy(showSettings = false, showPlusSheet = true, plusError = null) }
        loadPlusPrice()
    }

    fun closePlusSheet() = _uiState.update { it.copy(showPlusSheet = false, plusError = null) }

    /** Fetch the localized store price once, for the paywall (Apple 3.1.2: show the price). */
    private fun loadPlusPrice() {
        val rc = container.entitlements as? RevenueCatEntitlements ?: return
        if (_uiState.value.plusPriceLabel != null) return
        viewModelScope.launch {
            rc.plusPriceLabel()?.let { price ->
                _uiState.update { it.copy(plusPriceLabel = price) }
            }
        }
    }

    fun purchasePlus() {
        val rc = container.entitlements as? RevenueCatEntitlements
        if (rc == null) {
            // SDK key not set yet: acknowledge the tap so the CTA isn't a dead end.
            enqueueMessage(container.strings.purchasesComingSoon)
            return
        }
        // Atomically claim the in-flight slot (no double purchases on double-tap).
        val prev = _uiState.getAndUpdate {
            if (it.purchaseInFlight) it else it.copy(purchaseInFlight = true, plusError = null)
        }
        if (prev.purchaseInFlight) return

        viewModelScope.launch {
            // Never let a non-responding StoreKit/Billing call leave the button spinning
            // forever — time out and re-enable so the CTA can't become a dead end.
            val outcome = withTimeoutOrNull(60_000) { rc.purchasePlus() }
            _uiState.update { it.copy(purchaseInFlight = false) }
            when (outcome) {
                PurchaseOutcome.Success -> {
                    _uiState.update { it.copy(showPlusSheet = false) }
                    enqueueMessage(container.strings.purchaseSuccess)
                }
                PurchaseOutcome.Cancelled -> Unit
                // "No offering configured" = the store hasn't made the product purchasable on
                // this device yet — a distinct message beats a generic failure.
                is PurchaseOutcome.Failed ->
                    _uiState.update {
                        it.copy(
                            plusError = if (outcome.message?.contains("offering", true) == true)
                                container.strings.purchasesUnavailable
                            else container.strings.purchaseFailed,
                        )
                    }
                // Inline error: a snackbar would render behind the modal sheet.
                else -> _uiState.update { it.copy(plusError = container.strings.purchasesUnavailable) }
            }
        }
    }

    fun restorePlus() {
        val rc = container.entitlements as? RevenueCatEntitlements
        if (rc == null) {
            enqueueMessage(container.strings.purchasesComingSoon)
            return
        }
        val prev = _uiState.getAndUpdate {
            if (it.purchaseInFlight) it else it.copy(purchaseInFlight = true, plusError = null)
        }
        if (prev.purchaseInFlight) return

        viewModelScope.launch {
            val outcome = rc.restore()
            _uiState.update { it.copy(purchaseInFlight = false) }
            when (outcome) {
                PurchaseOutcome.Success -> {
                    _uiState.update { it.copy(showPlusSheet = false) }
                    enqueueMessage(container.strings.purchaseSuccess)
                }
                PurchaseOutcome.NothingToRestore ->
                    _uiState.update { it.copy(plusError = container.strings.nothingToRestore) }
                else -> _uiState.update { it.copy(plusError = container.strings.purchaseFailed) }
            }
        }
    }

    fun setPlaceSearchQuery(query: String) {
        _uiState.update { it.copy(placeSearchQuery = query) }
        placeSearchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(placeSearchResults = emptyList(), placeSearchLoading = false) }
            return
        }
        placeSearchJob = viewModelScope.launch {
            delay(350) // debounce typing
            _uiState.update { it.copy(placeSearchLoading = true) }
            val results = try {
                container.geocoding.search(query, container.services.languageCode)
            } catch (e: CancellationException) {
                // A newer query cancelled us — never clobber its state.
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            _uiState.update { it.copy(placeSearchResults = results, placeSearchLoading = false) }
        }
    }

    /** Adds a place; free users hit [AppConfig.FREE_PLACE_LIMIT] and see the Plus sheet. */
    fun addPlace(place: SavedPlace) {
        val state = _uiState.value
        if (state.savedPlaces.any { it.id == place.id }) return
        // Gate only when a purchase is actually possible — a paywall for an unbuyable
        // product is an Apple 2.1 rejection. Without purchases: no limit.
        if (state.purchasesAvailable && !state.isPlus &&
            state.savedPlaces.size >= AppConfig.FREE_PLACE_LIMIT
        ) {
            _uiState.update { it.copy(showPlusSheet = true, plusError = null) }
            loadPlusPrice()
            return
        }
        val next = state.savedPlaces + place
        container.preferences.setSavedPlaces(next)
        _uiState.update {
            it.copy(savedPlaces = next, placeSearchQuery = "", placeSearchResults = emptyList())
        }
        refreshPlaceEnv()
    }

    fun removePlace(id: String) {
        val next = _uiState.value.savedPlaces.filterNot { it.id == id }
        container.preferences.setSavedPlaces(next)
        placeEnvFetchedAt.remove(id)
        _uiState.update {
            it.copy(savedPlaces = next, placeEnv = it.placeEnv - id, placeWeeklyMax = it.placeWeeklyMax - id)
        }
    }

    /** Loads weather/AQI + 7-day max for each saved place (independent, partial results allowed). */
    fun refreshPlaceEnv() {
        val places = _uiState.value.savedPlaces
        if (places.isEmpty()) return
        val now = nowMillis()
        viewModelScope.launch {
            places.forEach { place ->
                // Skip places fetched recently — reopening the sheet shouldn't re-hit
                // two endpoints per place every time.
                val last = placeEnvFetchedAt[place.id] ?: 0L
                if (now - last < PLACE_ENV_TTL_MS) return@forEach
                placeEnvFetchedAt[place.id] = now
                launch {
                    val env = runCatching { container.placeWeather.fetch(place.point) }.getOrNull()
                    // Failed or degraded fetch: drop the TTL stamp so reopening the sheet
                    // retries instead of showing "—" for the full TTL.
                    if (env == null || !env.hasTemp || !env.hasAqi) {
                        placeEnvFetchedAt.remove(place.id)
                    }
                    if (env != null) {
                        _uiState.update { state ->
                            // Skip if the place was removed while this fetch was in flight.
                            if (state.savedPlaces.none { it.id == place.id }) state
                            else state.copy(placeEnv = state.placeEnv + (place.id to env))
                        }
                    }
                }
                launch {
                    container.placeWeather.fetchWeeklyMaxFeelsLike(place.point)?.let { max ->
                        _uiState.update { state ->
                            if (state.savedPlaces.none { it.id == place.id }) state
                            else state.copy(placeWeeklyMax = state.placeWeeklyMax + (place.id to max))
                        }
                    }
                }
            }
        }
    }

    /** Shared rewarded-ad flow with an atomic in-flight guard + ready/loading feedback. */
    private fun runRewarded(onResult: (Boolean) -> Unit) {
        if (!ads.isRewardedReady) {
            ads.preloadRewarded()
            enqueueMessage(container.strings.adLoading)
            return
        }
        // Atomically claim the in-flight slot; bail if another flow already owns it.
        val prev = _uiState.getAndUpdate { if (it.adInFlight) it else it.copy(adInFlight = true) }
        if (prev.adInFlight) return

        viewModelScope.launch {
            val rewarded = ads.showRewardedAd()
            onResult(rewarded)
            _uiState.update { it.copy(adInFlight = false) }
            enqueueMessage(if (rewarded) container.strings.unlocked else container.strings.adUnavailable)
            ads.preloadRewarded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        container.close()
    }

    companion object {
        const val ERROR_LOCATION = "error_location"
        const val ERROR_NETWORK = "error_network"
        private const val STALE_REFRESH_MS = 60_000L
        private const val PLACE_ENV_TTL_MS = 5 * 60_000L
    }
}
