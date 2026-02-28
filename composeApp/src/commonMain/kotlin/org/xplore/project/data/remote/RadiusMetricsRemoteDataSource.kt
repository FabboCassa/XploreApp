package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/**
 * Remote data source for radius loading metrics.
 * Talks to the backend's /api/metrics endpoints.
 */
class RadiusMetricsRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    /**
     * Submits a loading time measurement (fire-and-forget from the caller).
     */
    suspend fun postLoadingTime(radiusKm: Double, loadingTimeMs: Long) {
        try {
            httpClient.post("$baseUrl/api/metrics/loading") {
                contentType(ContentType.Application.Json)
                setBody(RadiusLoadingRequestDto(radiusKm, loadingTimeMs))
            }
        } catch (e: Exception) {
            println("📊 [Metrics] Failed to post metric: ${e.message}")
        }
    }

    /**
     * Retrieves average loading times per radius step (last 30 days).
     * Returns a map of radiusKm → avgMs.
     */
    suspend fun getLoadingAverages(): Map<Double, Long> {
        return try {
            val response = httpClient.get("$baseUrl/api/metrics/loading/averages")
            if (response.status.isSuccess()) {
                val list: List<RadiusLoadingAverageDto> = response.body()
                list.associate { it.radiusKm to it.avgMs }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            println("📊 [Metrics] Failed to fetch averages: ${e.message}")
            emptyMap()
        }
    }
}

@Serializable
data class RadiusLoadingRequestDto(
    val radiusKm: Double,
    val loadingTimeMs: Long,
)

@Serializable
data class RadiusLoadingAverageDto(
    val radiusKm: Double,
    val avgMs: Long,
)
