package org.xplore.project.platform

import org.xplore.project.data.local.LanguageMode

/**
 * Returns the device's current BCP-47 language tag (e.g. "it", "en", "fr").
 * Used to resolve [LanguageMode.SYSTEM] into a concrete locale.
 */
expect fun getDeviceLanguageTag(): String

/**
 * Forces the app to use the given BCP-47 language tag for resource resolution.
 * On Android this sets `Locale.setDefault(...)`, on iOS it configures NSLocale.
 * Called at the root composable level when the user selects a language.
 */
expect fun applyLocaleOverride(languageTag: String)
