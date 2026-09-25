package com.octsun.cooly.data

import com.octsun.cooly.config.AppConfig
import com.octsun.cooly.data.remote.OverpassApi
import com.octsun.cooly.data.remote.RawSpot
import com.octsun.cooly.data.remote.WeatherApi
import com.octsun.cooly.domain.distanceMeters
import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.domain.model.EnvStatus
import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.domain.walkingMinutes
import com.octsun.cooly.platform.nowMillis
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for env status + POIs. Caches results by location and dedupes
 * concurrent fetches so a rapid refresh/pan reuses one in-flight request.
 */
class CoolyRepository(
    private val weatherApi: WeatherApi,
    private val overpassApi: OverpassApi,
) {
    private data class SpotCache(
        val center: GeoPoint,
        val radius: Int,
        val fetchedAt: Long,
        val raw: List<RawSpot>,
    )

    private data class EnvCache(val center: GeoPoint, val fetchedAt: Long, val env: EnvStatus)

    private class InFlight(
        val deferred: Deferred<List<RawSpot>>,
        val center: GeoPoint,
        val radius: Int,
    )

    private val spotMutex = Mutex()
    private val envMutex = Mutex()
    private var spotCache: SpotCache? = null
    private var envCache: EnvCache? = null
    private var inFlightSpots: InFlight? = null

    /** Weather + AQI with a short-TTL cache keyed by location. */
    suspend fun loadEnv(point: GeoPoint): EnvStatus = withContext(Dispatchers.Default) {
        loadEnvLocked(point)
    }

    private suspend fun loadEnvLocked(point: GeoPoint): EnvStatus = envMutex.withLock {
        val cache = envCache
        val now = nowMillis()
        if (cache != null &&
            now - cache.fetchedAt < WEATHER_TTL_MS &&
            distanceMeters(cache.center, point) < WEATHER_CACHE_RADIUS_M
        ) {
            return cache.env
        }
        val env = weatherApi.fetch(point)
        // Cache only complete results — a degraded fetch (one endpoint down) must stay
        // refreshable instead of pinning "—" on screen for the full TTL.
        if (env.hasTemp && env.hasAqi) {
            envCache = EnvCache(point, now, env)
        }
        env
    }

    /** Distance-sorted spots around [point]. Uses cached raw POIs when still fresh & nearby. */
    suspend fun loadSpots(
        point: GeoPoint,
        radius: Int = AppConfig.POI_SEARCH_RADIUS_M,
    ): List<CoolSpot> = withContext(Dispatchers.Default) {
        // Parse/map/sort off the main thread.
        cachedOrFetch(point, radius)
            .map { it.withDistanceFrom(point) }
            .sortedBy { it.distanceMeters }
    }

    private suspend fun cachedOrFetch(point: GeoPoint, radius: Int): List<RawSpot> = coroutineScope {
        val existing = spotMutex.withLock {
            val cache = spotCache
            val now = nowMillis()
            val fresh = cache != null &&
                cache.radius == radius &&
                now - cache.fetchedAt < AppConfig.POI_CACHE_TTL_MS &&
                distanceMeters(cache.center, point) < radius / 4
            if (fresh) return@withLock CompletedResult(cache.raw)

            // Reuse an in-flight fetch only when it was issued for (roughly) the same spot —
            // otherwise its results would be cached under the wrong center.
            inFlightSpots?.let { current ->
                if (current.radius == radius &&
                    distanceMeters(current.center, point) < radius / 4
                ) {
                    return@withLock PendingResult(current)
                }
            }

            val flight = InFlight(async { overpassApi.fetchSpots(point, radius) }, point, radius)
            inFlightSpots = flight
            PendingResult(flight)
        }
        when (existing) {
            is CompletedResult -> existing.value
            is PendingResult -> {
                val flight = existing.flight
                try {
                    val result = flight.deferred.await()
                    spotMutex.withLock {
                        // Cache under the fetch's own center, not the awaiting caller's point.
                        spotCache = SpotCache(flight.center, flight.radius, nowMillis(), result)
                    }
                    result
                } finally {
                    // Always release the in-flight slot — a failed fetch must not poison
                    // every future retry with the same stale exception.
                    spotMutex.withLock {
                        if (inFlightSpots === flight) inFlightSpots = null
                    }
                }
            }
        }
    }

    private sealed interface FetchResult
    private class CompletedResult(val value: List<RawSpot>) : FetchResult
    private class PendingResult(val flight: InFlight) : FetchResult

    private fun RawSpot.withDistanceFrom(origin: GeoPoint): CoolSpot {
        val dist = distanceMeters(origin, point)
        return CoolSpot(
            id = id,
            name = name,
            type = type,
            point = point,
            distanceMeters = dist,
            walkingMinutes = walkingMinutes(dist),
            openingHours = openingHours,
            address = address,
            source = source,
        )
    }

    private companion object {
        const val WEATHER_TTL_MS = 5 * 60 * 1000L
        const val WEATHER_CACHE_RADIUS_M = 2000
    }
}
