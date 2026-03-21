package org.xplore.project.di

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.xplore.project.data.local.db.DatabaseDriverFactory
import org.xplore.project.domain.notification.NavigationNotifier
import org.xplore.project.platform.IosNavigationNotifier

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
    single { IosNavigationNotifier() } bind NavigationNotifier::class
}
