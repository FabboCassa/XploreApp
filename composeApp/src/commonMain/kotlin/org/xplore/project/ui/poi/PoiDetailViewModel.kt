package org.xplore.project.ui.poi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.xplore.project.data.local.MapPinLocalDataSource
import org.xplore.project.domain.model.MapPin

/**
 * ViewModel for the POI Detail screen.
 *
 * Loads a single POI from the local cache by ID and exposes it as UI state.
 */
class PoiDetailViewModel(
    private val localDataSource: MapPinLocalDataSource,
) : ViewModel() {

    private val _pin = MutableStateFlow<MapPin?>(null)
    val pin: StateFlow<MapPin?> = _pin.asStateFlow()

    /**
     * Loads the POI with the given ID from the local cache.
     * Should be called once when the screen enters composition.
     */
    fun loadPin(poiId: String) {
        if (_pin.value != null) return // already loaded

        val cached = localDataSource.getCachedPins().firstOrNull { it.id == poiId }
        _pin.value = cached
    }
}
