package org.xplore.project.platform

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Provides the currently-active BCP-47 language tag (e.g. "en", "it", "fr")
 * throughout the composition tree.
 *
 * When this value changes via [CompositionLocalProvider], every composable
 * that reads it recomposes — which causes [stringResource] calls to re-read
 * from the updated system locale **without** destroying the NavHost state.
 *
 * Default value is "en" (English fallback).
 */
val LocalAppLanguage = staticCompositionLocalOf { "en" }
