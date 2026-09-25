package com.octsun.cooly.data.remote

import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.domain.model.SavedPlace
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open-Meteo geocoding (free, no key, global) — resolves a typed city/place name to
 * coordinates for the saved-places feature. Same provider family as the weather API.
 */
class GeocodingApi(private val client: HttpClient) {

    suspend fun search(query: String, languageCode: String): List<SavedPlace> {
        if (query.isBlank()) return emptyList()
        val response: GeocodingResponse = client.get("https://geocoding-api.open-meteo.com/v1/search") {
            parameter("name", query.trim())
            parameter("count", 8)
            parameter("language", languageCode)
            parameter("format", "json")
        }.body()
        return response.results.map { r ->
            SavedPlace(
                id = "geo/${r.id}",
                name = r.name,
                region = listOfNotNull(r.admin1, r.country)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { null },
                point = GeoPoint(r.latitude, r.longitude),
            )
        }
    }
}

@Serializable
private data class GeocodingResponse(val results: List<GeocodingResult> = emptyList())

@Serializable
private data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("admin1") val admin1: String? = null,
)
