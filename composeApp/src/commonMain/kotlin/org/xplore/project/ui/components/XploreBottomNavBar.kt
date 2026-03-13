package org.xplore.project.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.ic_community
import xploreapp.composeapp.generated.resources.ic_map
import xploreapp.composeapp.generated.resources.ic_person
import xploreapp.composeapp.generated.resources.nav_map
import xploreapp.composeapp.generated.resources.nav_profile
import xploreapp.composeapp.generated.resources.nav_community

/**
 * Primary navigation component.
 *
 * ## Navigation Structure
 * - **Map** (Index 0): The main homepage.
 * - **Community** (Index 1): Groups and Leaderboard.
 * - **Profile** (Index 2): User settings and account.
 *
 * Uses [painterResource] for cross-platform icon support.
 */
@Composable
fun XploreBottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    val items = listOf(
        NavItem(Res.string.nav_map, Res.drawable.ic_map),
        NavItem(Res.string.nav_community, Res.drawable.ic_community),
        NavItem(Res.string.nav_profile, Res.drawable.ic_person),
    )

    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(64.dp),
        containerColor = colorScheme.surface.copy(alpha = 0.95f),
        contentColor = colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = stringResource(item.labelRes),
                    )
                },
                label = null,
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colorScheme.primary,
                    unselectedIconColor = colorScheme.onSurfaceVariant,
                    indicatorColor = colorScheme.primaryContainer.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/**
 * Internal data for a navigation item.
 */
private data class NavItem(
    val labelRes: StringResource,
    val iconRes: DrawableResource,
)
