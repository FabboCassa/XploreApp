package org.xplore.project.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.xplore.project.data.repository.MuseumRepositoryImpl
import org.xplore.project.domain.repository.MuseumRepository
import org.xplore.project.ui.home.HomeViewModel

/**
 * Main Koin application module.
 * Binds repository implementations and provides ViewModels.
 */
val appModule = module {
    // Data layer
    singleOf(::MuseumRepositoryImpl) bind MuseumRepository::class

    // Presentation layer
    viewModelOf(::HomeViewModel)
}
