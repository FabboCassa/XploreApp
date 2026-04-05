package org.xplore.project.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun getDeviceLanguageTag(): String {
    return NSLocale.currentLocale.languageCode
}

actual fun applyLocaleOverride(languageTag: String) {
    // On iOS, override the AppleLanguages user default so that
    // NSBundle picks up the correct .lproj on next resource access.
    NSUserDefaults.standardUserDefaults.setObject(
        listOf(languageTag),
        forKey = "AppleLanguages",
    )
    NSUserDefaults.standardUserDefaults.synchronize()
}
