package org.xplore.project

import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication
import org.xplore.project.di.appModule
import org.xplore.project.di.platformModule

@Composable
actual fun KoinWrapper(content: @Composable () -> Unit) {
    KoinApplication(application = {
        modules(platformModule(), appModule)
    }, content = content)
}
