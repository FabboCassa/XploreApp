package org.xplore.project.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.xplore.project.data.local.db.DatabaseDriverFactory

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
}
