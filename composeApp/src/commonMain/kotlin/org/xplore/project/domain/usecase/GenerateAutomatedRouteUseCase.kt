package org.xplore.project.domain.usecase

import org.xplore.project.domain.model.ItineraryStop
import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.PinType
import kotlin.math.*

/**
 * Client-side heuristic that generates an automated itinerary from available POIs.
 *
 * ## Algorithm
 * 1. Filter pins by the selected categories.
 * 2. Greedily pick the closest unvisited POI (nearest-neighbor) while the
 *    chosen constraint is not exhausted.
 *
 * Two constraint modes are supported:
 * - **MaxTravelTime**: accumulate only travel time between stops (visit time excluded).
 * - **MaxStops**: simply pick up to N nearest-neighbor stops.
 *
 * Walking speed default ≈ 5 km/h, cycling ≈ 15 km/h, driving ≈ 30 km/h (urban).
 */
class GenerateAutomatedRouteUseCase {

    /**
     * Sealed interface representing the route constraint chosen by the user.
     */
    sealed interface RouteConstraint {
        /** Limit by total travel time (in minutes) between stops, excluding visit time. */
        data class MaxTravelTime(val minutes: Int) : RouteConstraint
        /** Limit by maximum number of stops to visit. */
        data class MaxStops(val count: Int) : RouteConstraint
    }

    data class Params(
        val allPins: List<MapPin>,
        val startLat: Double,
        val startLng: Double,
        val constraint: RouteConstraint,
        val travelMode: TravelMode = TravelMode.WALKING,
        val selectedCategories: Set<PinType> = emptySet(),
    )

    /**
     * Result wrapper that carries the generated stops and an optional info message
     * (e.g. when fewer stops were found than requested).
     */
    data class Result(
        val stops: List<ItineraryStop>,
        val infoMessage: String? = null,
    )

    enum class TravelMode(val speedKmh: Double) {
        WALKING(5.0),
        BICYCLING(15.0),
        DRIVING(30.0),
    }

    /**
     * Returns an ordered list of [ItineraryStop] that satisfy the chosen constraint.
     */
    fun execute(params: Params): Result {
        val candidates = params.allPins
            .let { pins ->
                if (params.selectedCategories.isEmpty()) pins
                else pins.filter { it.type in params.selectedCategories }
            }
            .filter { it.latitude != 0.0 && it.longitude != 0.0 }
            .toMutableList()

        return when (params.constraint) {
            is RouteConstraint.MaxTravelTime -> executeByTime(params, candidates)
            is RouteConstraint.MaxStops -> executeByStops(params, candidates)
        }
    }

    // ── MaxTravelTime strategy ────────────────────────────────────

    private fun executeByTime(
        params: Params,
        candidates: MutableList<MapPin>,
    ): Result {
        val maxMinutes = (params.constraint as RouteConstraint.MaxTravelTime).minutes
        val result = mutableListOf<ItineraryStop>()
        var remainingMinutes = maxMinutes.toDouble()
        var currentLat = params.startLat
        var currentLng = params.startLng

        while (candidates.isNotEmpty() && remainingMinutes > 0) {
            val best = candidates
                .map { pin ->
                    val distKm = haversineKm(currentLat, currentLng, pin.latitude, pin.longitude)
                    val travelMin = (distKm / params.travelMode.speedKmh) * 60.0
                    Triple(pin, travelMin, distKm)
                }
                .filter { (_, travelMin, _) -> travelMin <= remainingMinutes }
                .minByOrNull { (pin, _, distKm) ->
                    distKm - (pin.rating ?: 0.0) * 0.05
                }

            if (best == null) break

            val (pin, travelMin, _) = best
            result.add(ItineraryStop(pin = pin))
            remainingMinutes -= travelMin
            currentLat = pin.latitude
            currentLng = pin.longitude
            candidates.remove(pin)
        }

        return Result(stops = result)
    }

    // ── MaxStops strategy ─────────────────────────────────────────

    private fun executeByStops(
        params: Params,
        candidates: MutableList<MapPin>,
    ): Result {
        val requested = (params.constraint as RouteConstraint.MaxStops).count
        val result = mutableListOf<ItineraryStop>()
        var currentLat = params.startLat
        var currentLng = params.startLng

        while (candidates.isNotEmpty() && result.size < requested) {
            val best = candidates
                .minByOrNull { pin ->
                    val distKm = haversineKm(currentLat, currentLng, pin.latitude, pin.longitude)
                    distKm - (pin.rating ?: 0.0) * 0.05
                }

            if (best == null) break

            result.add(ItineraryStop(pin = best))
            currentLat = best.latitude
            currentLng = best.longitude
            candidates.remove(best)
        }

        val infoMessage = if (result.size < requested) {
            "Percorso generato con ${result.size} tappe (meno delle $requested richieste a causa dei POI disponibili)."
        } else null

        return Result(stops = result, infoMessage = infoMessage)
    }

    companion object {
        /**
         * Haversine formula for great-circle distance between two lat/lng points.
         */
        fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6371.0
            val dLat = (lat2 - lat1) * PI / 180.0
            val dLng = (lng2 - lng1) * PI / 180.0
            val lat1Rad = lat1 * PI / 180.0
            val lat2Rad = lat2 * PI / 180.0
            val a = sin(dLat / 2).pow(2) +
                    cos(lat1Rad) * cos(lat2Rad) *
                    sin(dLng / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
