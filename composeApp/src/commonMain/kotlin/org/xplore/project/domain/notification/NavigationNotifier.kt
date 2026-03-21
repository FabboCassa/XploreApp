package org.xplore.project.domain.notification

/**
 * Abstraction for showing a persistent notification during navigation.
 *
 * Platform implementations:
 * - Android: Foreground Service with an ongoing notification.
 * - iOS: UNUserNotificationCenter with silent replacement updates.
 */
interface NavigationNotifier {

    /**
     * Called when in-app navigation starts. Shows the initial notification.
     *
     * @param poiName      Name of the next POI (e.g. "Colosseo").
     * @param distanceMeters Distance in meters to the next POI.
     * @param stopIndex     1-based index of the current stop.
     * @param totalStops    Total number of stops in the itinerary.
     * @param instruction   Turn-by-turn instruction (e.g. "Prosegui per Via Roma per 50 m poi gira a destra su Via Emilia").
     */
    fun startNavigation(
        poiName: String,
        distanceMeters: Int,
        stopIndex: Int,
        totalStops: Int,
        instruction: String? = null,
    )

    /**
     * Called on every location update to refresh the notification content.
     * Parameters mirror [startNavigation].
     */
    fun updateNavigation(
        poiName: String,
        distanceMeters: Int,
        stopIndex: Int,
        totalStops: Int,
        instruction: String? = null,
    )

    /**
     * Called when navigation ends. Removes the notification.
     */
    fun stopNavigation()
}
