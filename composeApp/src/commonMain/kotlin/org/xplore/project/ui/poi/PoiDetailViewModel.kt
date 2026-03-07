package org.xplore.project.ui.poi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.xplore.project.data.local.MapPinLocalDataSource
import org.xplore.project.data.remote.MapPinRemoteDataSource
import org.xplore.project.data.local.TokenManager
import org.xplore.project.domain.model.MapPin
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.xplore.project.domain.repository.CommunityRepository
import xploreapp.composeapp.generated.resources.*

/**
 * ViewModel for the POI Detail screen.
 *
 * Loads a single POI from the local cache by ID and exposes it as UI state.
 */
class PoiDetailViewModel(
    private val localDataSource: MapPinLocalDataSource,
    private val remoteDataSource: MapPinRemoteDataSource,
    private val communityRepository: CommunityRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _pin = MutableStateFlow<MapPin?>(null)
    val pin: StateFlow<MapPin?> = _pin.asStateFlow()

    private val _ratingStatus = MutableStateFlow<RatingStatus>(RatingStatus.Idle)
    val ratingStatus: StateFlow<RatingStatus> = _ratingStatus.asStateFlow()

    private val _visitStatus = MutableStateFlow<VisitStatus>(VisitStatus.Idle)
    val visitStatus: StateFlow<VisitStatus> = _visitStatus.asStateFlow()

    val isGuest: Boolean
        get() = tokenManager.isGuest

    /**
     * Loads the POI with the given ID from the local cache.
     * Should be called once when the screen enters composition.
     */
    fun loadPin(poiId: String) {
        if (_pin.value != null) return // already loaded

        val cached = localDataSource.getCachedPins().firstOrNull { it.id == poiId }
        _pin.value = cached
    }

    /**
     * Submits a rating for the current POI. Requires the user to be logged in.
     */
    fun submitRating(poiId: String, score: Int) {
        val token = tokenManager.accessToken
        if (token == null) {
            viewModelScope.launch {
                _ratingStatus.value = RatingStatus.Error(getString(Res.string.error_login_required))
            }
            return
        }

        _ratingStatus.value = RatingStatus.Submitting
        
        viewModelScope.launch {
            try {
                remoteDataSource.ratePoi(poiId, score, token)
                
                // Optimistically update local state & cache
                val currentPin = _pin.value
                if (currentPin != null) {
                    val updatedCount = (currentPin.ratingsCount ?: 0) + 1
                    val newAvg = if (currentPin.rating == null || currentPin.rating == 0.0) 
                        score.toDouble()
                    else 
                        ((currentPin.rating!! * (updatedCount - 1)) + score) / updatedCount

                    val updatedPin = currentPin.copy(
                        rating = newAvg,
                        ratingsCount = updatedCount
                    )
                    _pin.value = updatedPin
                    localDataSource.cachePins(listOf(updatedPin))
                }
                
                _ratingStatus.value = RatingStatus.Success
            } catch (e: Exception) {
                val errorMsg = e.message ?: getString(Res.string.error_unknown)
                if (errorMsg.contains("400")) {
                    _ratingStatus.value = RatingStatus.Error(getString(Res.string.error_already_rated))
                } else {
                    _ratingStatus.value = RatingStatus.Error(errorMsg)
                }
            }
        }
    }
    
    fun resetRatingStatus() {
        _ratingStatus.value = RatingStatus.Idle
    }

    fun markAsVisited(poiId: String) {
        val token = tokenManager.accessToken
        if (token == null) {
            viewModelScope.launch {
                _visitStatus.value = VisitStatus.Error(getString(Res.string.error_login_required))
            }
            return
        }

        _visitStatus.value = VisitStatus.Loading
        viewModelScope.launch {
            try {
                communityRepository.visitPlace(poiId)
                _visitStatus.value = VisitStatus.Success
            } catch (e: Exception) {
                _visitStatus.value = VisitStatus.Error(e.message ?: getString(Res.string.error_unknown))
            }
        }
    }

    fun resetVisitStatus() {
        _visitStatus.value = VisitStatus.Idle
    }
}

sealed class VisitStatus {
    object Idle : VisitStatus()
    object Loading : VisitStatus()
    object Success : VisitStatus()
    data class Error(val message: String) : VisitStatus()
}

sealed class RatingStatus {
    object Idle : RatingStatus()
    object Submitting : RatingStatus()
    object Success : RatingStatus()
    data class Error(val message: String) : RatingStatus()
}
