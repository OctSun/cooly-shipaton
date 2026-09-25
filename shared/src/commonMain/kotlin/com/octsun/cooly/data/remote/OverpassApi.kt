package com.octsun.cooly.data.remote

import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.domain.model.SpotSource
import com.octsun.cooly.domain.model.SpotType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable

/** Raw POI as returned from Overpass, before distance is computed against the user. */
data class RawSpot(
    val id: String,
    val name: String,
    val type: SpotType,
    val point: GeoPoint,
    val openingHours: String?,
    val address: String?,
    val source: SpotSource = SpotSource.OSM,
)

/**
 * OpenStreetMap Overpass API. Free, global, tag-based. Covers cool indoor
 * spaces, drinking water, parks and toilets in a single request.
 */
class OverpassApi(private val client: HttpClient) {

    // Public Overpass mirrors rate-limit and go down often; a heat-safety app must not
    // silently show "no places" on one mirror's hiccup, so we fan out across several
    // and retry the whole set once before giving up.
    private val endpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.osm.ch/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    )

    suspend fun fetchSpots(center: GeoPoint, radiusMeters: Int): List<RawSpot> {
        val query = buildQuery(center, radiusMeters)
        var lastError: Throwable? = null
        // Two passes over the mirror list: a transient 429/timeout on the first sweep
        // often clears on a second attempt against the same (now-cooler) mirrors.
        repeat(2) { attempt ->
            for (url in endpoints) {
                try {
                    val response: OverpassResponse = client.post(url) {
                        contentType(ContentType.Application.FormUrlEncoded)
                        setBody("data=" + query.encodeUrlComponent())
                        // Public mirrors are slow under load; the query itself allows 25s —
                        // don't cut successful-but-slow responses at the 15s default.
                        // The socket timeout must be raised too: Overpass sends no bytes while
                        // computing server-side, which reads as socket inactivity.
                        timeout {
                            requestTimeoutMillis = 30_000
                            socketTimeoutMillis = 30_000
                        }
                    }.body()
                    return response.elements.mapNotNull { it.toRawSpot() }
                } catch (t: SerializationException) {
                    // A malformed body won't be fixed by another mirror — fail fast.
                    throw t
                } catch (t: Throwable) {
                    // Network/timeout/5xx/429 — try the next mirror.
                    lastError = t
                }
            }
            if (attempt == 0) delay(800) // brief backoff before the second sweep
        }
        throw lastError ?: IllegalStateException("Overpass request failed")
    }

    private fun buildQuery(center: GeoPoint, radius: Int): String {
        val a = "around:$radius,${center.lat},${center.lng}"
        // nwr = node/way/relation, so mall/park buildings mapped as areas are included.
        return """
            [out:json][timeout:25];
            (
              nwr["amenity"="library"]($a);
              nwr["shop"="mall"]($a);
              nwr["shop"="department_store"]($a);
              nwr["shop"="supermarket"]($a);
              nwr["amenity"="cafe"]($a);
              node["railway"="station"]["station"="subway"]($a);
              node["railway"="subway_entrance"]($a);
              nwr["leisure"="park"]($a);
              nwr["amenity"="drinking_water"]($a);
              nwr["amenity"="toilets"]($a);
              nwr["amenity"="community_centre"]($a);
              nwr["amenity"="social_facility"]["social_facility"~"shelter|day_centre|outreach"]($a);
            );
            out center 500;
        """.trimIndent()
    }
}

private fun OverpassElement.toRawSpot(): RawSpot? {
    val lat = lat ?: center?.lat ?: return null
    val lon = lon ?: center?.lon ?: return null
    val t = tags ?: return null
    val type = classify(t) ?: return null

    // Named POIs are preferred; unnamed water/toilet/park keep a generic label.
    val name = t["name"]
    val resolvedName = name ?: when (type) {
        SpotType.WATER_FOUNTAIN, SpotType.TOILET, SpotType.PARK, SpotType.SUBWAY,
        SpotType.COOLING_CENTER -> null
        else -> return null // skip unnamed malls/libraries/cafes to avoid noise
    }

    return RawSpot(
        id = "$osmType/$id",
        name = (resolvedName ?: "").take(120), // UI fills a type label when blank
        type = type,
        point = GeoPoint(lat, lon),
        openingHours = t["opening_hours"]?.take(200),
        address = buildAddress(t)?.take(500),
    )
}

private fun classify(t: Map<String, String>): SpotType? = when {
    // Public refuges first: community centres, and only the genuinely public kinds of
    // social_facility (shelter / day_centre / outreach). Bare social_facility covers private
    // group homes, nursing homes, food banks etc. — never advertise those as a cooling refuge.
    t["amenity"] == "community_centre" ||
        (t["amenity"] == "social_facility" &&
            t["social_facility"] in setOf("shelter", "day_centre", "outreach")) ->
        SpotType.COOLING_CENTER
    t["amenity"] == "library" -> SpotType.LIBRARY
    t["shop"] == "mall" || t["shop"] == "department_store" -> SpotType.MALL
    t["shop"] == "supermarket" -> SpotType.SUPERMARKET
    t["railway"] == "station" || t["railway"] == "subway_entrance" -> SpotType.SUBWAY
    t["amenity"] == "cafe" -> SpotType.CAFE
    t["leisure"] == "park" -> SpotType.PARK
    t["amenity"] == "drinking_water" -> SpotType.WATER_FOUNTAIN
    t["amenity"] == "toilets" -> SpotType.TOILET
    else -> null
}

private fun buildAddress(t: Map<String, String>): String? {
    val street = t["addr:street"]
    val number = t["addr:housenumber"]
    val city = t["addr:city"]
    val parts = listOfNotNull(
        listOfNotNull(number, street).joinToString(" ").ifBlank { null },
        city,
    )
    return parts.joinToString(", ").ifBlank { null }
}

@Serializable
private data class OverpassResponse(val elements: List<OverpassElement> = emptyList())

@Serializable
private data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null,
) {
    val osmType: String get() = type
}

@Serializable
private data class OverpassCenter(val lat: Double, val lon: Double)

/** Minimal percent-encoding for the Overpass QL body (no external URL helper needed). */
private fun String.encodeUrlComponent(): String = buildString {
    for (c in this@encodeUrlComponent) {
        when (c) {
            in 'A'..'Z', in 'a'..'z', in '0'..'9', '-', '_', '.', '~' -> append(c)
            else -> {
                for (b in c.toString().encodeToByteArray()) {
                    append('%')
                    append(((b.toInt() and 0xFF) shr 4).toHexDigit())
                    append((b.toInt() and 0x0F).toHexDigit())
                }
            }
        }
    }
}

private fun Int.toHexDigit(): Char = if (this < 10) ('0' + this) else ('A' + (this - 10))
