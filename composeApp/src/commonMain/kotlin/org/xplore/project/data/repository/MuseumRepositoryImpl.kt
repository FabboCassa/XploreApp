package org.xplore.project.data.repository

import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.Museum
import org.xplore.project.domain.model.PinType
import org.xplore.project.domain.repository.MuseumRepository

/**
 * Concrete implementation of the [MuseumRepository].
 *
 * ## Data Layer
 * This class handles data retrieval. Currently, it uses **Mock Data** (`sampleMuseums`) to simulate a backend.
 *
 * ## Future Implementation
 * When the .NET backend is ready, this class will be updated to:
 * 1. Inject a `HttpClient` (Ktor).
 * 2. Make network requests to endpoints (e.g., `GET /api/museums`).
 * 3. Map API DTOs to Domain [Museum] entities.
 */
class MuseumRepositoryImpl : MuseumRepository {

    override suspend fun getMuseums(): List<Museum> = sampleMuseums

    override suspend fun getMapPins(): List<MapPin> =
        sampleMuseums.map { museum ->
            MapPin(
                id = museum.id,
                label = museum.name,
                latitude = museum.latitude,
                longitude = museum.longitude,
                type = PinType.MUSEUM,
            )
        } + sampleArtworkPins + sampleEventPins

    companion object {
        private val sampleMuseums = listOf(
            Museum(
                id = "1",
                name = "Galleria degli Uffizi",
                description = "Uno dei musei d'arte più famosi al mondo.",
                latitude = 43.7687,
                longitude = 11.2558,
                imageUrl = null,
                address = "Piazzale degli Uffizi, 6, Firenze",
                rating = 4.8,
            ),
            Museum(
                id = "2",
                name = "Museo Archeologico Nazionale",
                description = "Il più importante museo archeologico d'Italia.",
                latitude = 40.8538,
                longitude = 14.2508,
                imageUrl = null,
                address = "Piazza Museo, 19, Napoli",
                rating = 4.6,
            ),
            Museum(
                id = "3",
                name = "Musei Vaticani",
                description = "Una delle raccolte d'arte più grandi del mondo.",
                latitude = 41.9065,
                longitude = 12.4536,
                imageUrl = null,
                address = "Viale Vaticano, Roma",
                rating = 4.7,
            ),
            Museum(
                id = "4",
                name = "Pinacoteca di Brera",
                description = "Galleria nazionale d'arte antica e moderna.",
                latitude = 45.4720,
                longitude = 9.1880,
                imageUrl = null,
                address = "Via Brera, 28, Milano",
                rating = 4.5,
            ),
        )

        private val sampleArtworkPins = listOf(
            MapPin(
                id = "art_1",
                label = "La Nascita di Venere",
                latitude = 43.7697,
                longitude = 11.2548,
                type = PinType.ARTWORK,
            ),
            MapPin(
                id = "art_2",
                label = "La Scuola di Atene",
                latitude = 41.9035,
                longitude = 12.4546,
                type = PinType.ARTWORK,
            ),
        )

        private val sampleEventPins = listOf(
            MapPin(
                id = "evt_1",
                label = "Notte al Museo",
                latitude = 45.4730,
                longitude = 9.1870,
                type = PinType.EVENT,
            ),
        )
    }
}
