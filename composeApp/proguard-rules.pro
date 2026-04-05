# ── Xplore App – ProGuard / R8 rules ────────────────────────────

# ── Kotlin Serialization ──────────────────────────────────────
# Keep @Serializable classes and their generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class org.xplore.project.**$$serializer { *; }
-keepclassmembers class org.xplore.project.** {
    *** Companion;
}
-keepclasseswithmembers class org.xplore.project.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Ktor ──────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class io.ktor.client.engine.** { *; }
-dontwarn io.ktor.**

# ── OkHttp (Ktor Android engine) ─────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ── Koin ──────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ── Compose / AndroidX ───────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Moko ──────────────────────────────────────────────────────
-keep class dev.icerock.moko.** { *; }
-dontwarn dev.icerock.moko.**

# ── MapLibre ──────────────────────────────────────────────────
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# ── SQLDelight ────────────────────────────────────────────────
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# ── Coil ──────────────────────────────────────────────────────
-keep class coil3.** { *; }
-dontwarn coil3.**

# ── Google Credential Manager ─────────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-dontwarn com.google.android.libraries.identity.**

# ── Firebase ──────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── General ───────────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable
