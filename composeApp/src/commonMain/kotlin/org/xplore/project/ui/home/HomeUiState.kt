package org.xplore.project.ui.home

import org.jetbrains.compose.resources.StringResource
import org.xplore.project.domain.model.MapPin

/**
 * Immutable UI state for the Home / Map screen.
 *
 * ## State Management
 * Follows **Unidirectional Data Flow (UDF)**. This data class represents the single source of truth for the [HomeScreen].
 * Any user interaction (search, filter) results in a new instance of this class being emitted by the ViewModel.
 *
 * @param searchQuery Current text in the search bar.
 * @param filters List of filter chips with their selection state.
 * @param pins List of map pins to render on the map.
 * @param isLoading Whether data is currently being fetched.
 * @param selectedNavIndex Index of the currently active bottom nav tab.
 * @param errorMessage User-facing error message, if any.
 */
data class HomeUiState(
    val searchQuery: String = "",
    val filters: List<FilterChipData> = emptyList(),
    val pins: List<MapPin> = emptyList(),
    val isLoading: Boolean = true,
    val selectedNavIndex: Int = 0,
    val errorMessage: String? = null,
)

/**
 * Data for a single filter chip.
 */
data class FilterChipData(
    val id: String,
    val labelRes: StringResource,
    val selected: Boolean = false,
)
