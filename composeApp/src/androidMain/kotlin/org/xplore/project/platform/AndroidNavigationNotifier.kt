package org.xplore.project.platform

import android.content.Intent
import android.os.Build
import org.xplore.project.data.local.db.XploreApplication
import org.xplore.project.domain.notification.NavigationNotifier

/**
 * Android implementation of [NavigationNotifier].
 *
 * Delegates to [NavigationService] (a foreground service) which keeps an
 * ongoing notification visible in the status bar while the user navigates.
 */
class AndroidNavigationNotifier : NavigationNotifier {

    private val context get() = XploreApplication.appContext

    override fun startNavigation(
        poiName: String,
        distanceMeters: Int,
        stopIndex: Int,
        totalStops: Int,
        instruction: String?,
    ) {
        val intent = buildIntent(NavigationService.ACTION_START, poiName, distanceMeters, stopIndex, totalStops, instruction)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun updateNavigation(
        poiName: String,
        distanceMeters: Int,
        stopIndex: Int,
        totalStops: Int,
        instruction: String?,
    ) {
        val intent = buildIntent(NavigationService.ACTION_UPDATE, poiName, distanceMeters, stopIndex, totalStops, instruction)
        context.startService(intent)
    }

    override fun stopNavigation() {
        val intent = Intent(context, NavigationService::class.java).apply {
            action = NavigationService.ACTION_STOP
        }
        context.startService(intent)
    }

    private fun buildIntent(
        action: String,
        poiName: String,
        distanceMeters: Int,
        stopIndex: Int,
        totalStops: Int,
        instruction: String?,
    ): Intent = Intent(context, NavigationService::class.java).apply {
        this.action = action
        putExtra(NavigationService.EXTRA_POI_NAME, poiName)
        putExtra(NavigationService.EXTRA_DISTANCE, distanceMeters)
        putExtra(NavigationService.EXTRA_STOP_INDEX, stopIndex)
        putExtra(NavigationService.EXTRA_TOTAL_STOPS, totalStops)
        putExtra(NavigationService.EXTRA_INSTRUCTION, instruction)
    }
}
