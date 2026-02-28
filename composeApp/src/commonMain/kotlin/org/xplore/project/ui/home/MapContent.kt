package org.xplore.project.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.xplore.project.ui.components.XploreFilterChips
import org.xplore.project.ui.components.XploreMap
import org.xplore.project.ui.components.XploreSearchBar

/**
 * Map tab content — extracted for clarity.
 */
@Composable
fun MapContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        XploreMap(
            pins = uiState.pins,
            onPinClick = { pin -> viewModel.onPinSelected(pin) },
            modifier = Modifier.fillMaxSize(),
            userLatitude = uiState.userLatitude,
            userLongitude = uiState.userLongitude,
            selectedPin = uiState.selectedPin,
            onDismissCallout = { viewModel.onDismissCallout() },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 4.dp),
        ) {
            XploreSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onClear = viewModel::onClearSearch,
            )

            Spacer(Modifier.height(12.dp))

            XploreFilterChips(
                filters = uiState.filters,
                onFilterClick = viewModel::onFilterSelected,
                onSettingsClick = viewModel::openSettings,
                onMoreFiltersClick = viewModel::openFilterDialog,
            )

            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(4.dp),
                        color = Color(0xFF4A90D9),
                        trackColor = Color(0xFF4A90D9).copy(alpha = 0.15f),
                    )

                    if (uiState.loadingStatusText != null) {
                        Text(
                            text = uiState.loadingStatusText.asString(),
                            fontSize = 12.sp,
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
