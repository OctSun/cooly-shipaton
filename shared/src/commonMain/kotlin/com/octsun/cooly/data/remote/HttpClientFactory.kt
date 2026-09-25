package com.octsun.cooly.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Shared Ktor client. The engine is resolved per-platform (OkHttp on Android, Darwin on iOS). */
fun createHttpClient(): HttpClient = HttpClient {
    // Public Overpass/Open-Meteo instances ask API clients to identify themselves;
    // an anonymous default UA risks being rate-limited or blocked (usage policy).
    defaultRequest {
        header(HttpHeaders.UserAgent, "Cooly/1.2 (heat-safety app; contact: hsapps89@gmail.com)")
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            }
        )
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
}
