package org.xplore.project.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.ui.components.XploreBottomNavBar
import org.xplore.project.ui.components.XploreFilterChips
import org.xplore.project.ui.components.XploreMap
import org.xplore.project.ui.components.XploreSearchBar

/**
 * The main Homepage / "Mappa" screen composable.
 *
 * ## UI Structure
 * Uses a [Scaffold] to organize the layout:
 * - **Background**: Full-screen [XploreMap] (z-index 0).
 * - **Top Overlay**: [XploreSearchBar] and [XploreFilterChips] (z-index 1).
 * - **Bottom Bar**: [XploreBottomNavBar] for main navigation.
 *
 * ## State Management
 * - Observes [HomeViewModel.uiState] via `collectAsState()`.
 * - Passes strictly necessary data (simple types/lambdas) to child components.
 * - Does NOT handle business logic; delegates events to [HomeViewModel].
 *
 * @param viewModel Injected via [koinViewModel]. Default value allows for easy preview/testing.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            XploreBottomNavBar(
                selectedIndex = uiState.selectedNavIndex,
                onItemSelected = viewModel::onNavItemSelected,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Layer 1: Map (full-bleed behind everything) ──
            XploreMap(
                pins = uiState.pins,
                onPinClick = { /* TODO: Navigate to museum detail */ },
                modifier = Modifier.fillMaxSize(),
            )

            // ── Layer 2: Top overlay (search + filters) ──
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(top = 12.dp),
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
                )
            }

            // ── Layer 3: Loading indicator ──
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
