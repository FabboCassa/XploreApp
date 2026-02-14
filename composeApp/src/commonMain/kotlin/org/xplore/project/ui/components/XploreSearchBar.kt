package org.xplore.project.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.ic_close
import xploreapp.composeapp.generated.resources.ic_search
import xploreapp.composeapp.generated.resources.search_placeholder

/**
 * Floating search bar component.
 *
 * ## Design System
 * Implements a **Glassmorphism** aesthetic using transparency and blur effects (simulated via colors/shadows).
 *
 * ## Usage
 * Used as an overlay in [HomeScreen].State is hoisted to the parent (stateless component).
 *
 * @param query Current text in the field.
 * @param onQueryChange Callback for text updates.
 * @param onClear Callback for the clear 'X' button.
 */
@Composable
fun XploreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = colorScheme.primary.copy(alpha = 0.15f),
                spotColor = colorScheme.primary.copy(alpha = 0.1f),
            ),
        placeholder = {
            Text(
                text = stringResource(Res.string.search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_search),
                contentDescription = null,
                tint = colorScheme.primary,
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(onClick = onClear) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colorScheme.surface.copy(alpha = 0.95f),
            unfocusedContainerColor = colorScheme.surface.copy(alpha = 0.90f),
            focusedBorderColor = colorScheme.primary.copy(alpha = 0.4f),
            unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.2f),
            cursorColor = colorScheme.primary,
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = colorScheme.onSurface,
        ),
    )
}
