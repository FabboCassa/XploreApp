package org.xplore.project.platform

import java.util.Locale

actual fun getDeviceLanguageTag(): String {
    return Locale.getDefault().language
}

actual fun applyLocaleOverride(languageTag: String) {
    val locale = Locale(languageTag)
    Locale.setDefault(locale)
}
