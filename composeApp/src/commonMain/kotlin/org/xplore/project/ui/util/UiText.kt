package org.xplore.project.ui.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A wrapper to represent text that can either be a hardcoded string
 * or a Compose string resource with optional formatting arguments.
 * This allows ViewModels to pass strings without needing a Context/Composer.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    
    class Resource(
        val res: StringResource,
        vararg val args: Any
    ) : UiText()
    
    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is Resource -> stringResource(res, *args)
        }
    }
}
