package org.xplore.project.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that carries deep-link payloads from platform entry points
 * (e.g. Activity.onNewIntent) into the Compose navigation layer.
 *
 * The consumer (HomeScreen) reads the event, acts on it, then clears it.
 */
object PendingDeepLink {
    private val _event = MutableStateFlow<String?>(null)
    val event: StateFlow<String?> = _event.asStateFlow()

    fun send(event: String) {
        _event.value = event
    }

    fun consume() {
        _event.value = null
    }
}
