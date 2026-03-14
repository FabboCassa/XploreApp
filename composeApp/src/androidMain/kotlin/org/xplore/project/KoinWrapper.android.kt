package org.xplore.project

import androidx.compose.runtime.Composable
import org.koin.compose.KoinContext

@Composable
actual fun KoinWrapper(content: @Composable () -> Unit) {
    KoinContext(content = content)
}
