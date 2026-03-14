package org.xplore.project

/**
 * Returns the current FCM registration token, or null if unavailable.
 * - Android: fetches from FirebaseMessaging
 * - iOS: returns null (APNs token is handled natively by the OS)
 */
expect suspend fun getCurrentFcmToken(): String?
