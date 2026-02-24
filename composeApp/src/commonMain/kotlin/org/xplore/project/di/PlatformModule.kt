package org.xplore.project.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin module.
 *
 * Provides dependencies that require platform context, such as:
 * - [DatabaseDriverFactory] (Android needs Context, iOS does not).
 */
expect fun platformModule(): Module
