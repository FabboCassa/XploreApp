import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spmForKmp)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.googleServices)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.compilations {
            val main by getting {
                cinterops.create("spmMaplibre")
            }
        }
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            
            // Google Sign-In (Credential Manager)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)

            // Firebase Cloud Messaging
            implementation(libs.firebase.messaging)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Navigation
            implementation(libs.navigation.compose)

            // QR Code (qrose)
            implementation(libs.qrose)

            // Multiplatform Settings (token storage)
            implementation(libs.multiplatform.settings.no.arg)

            // MapLibre Compose (OSM interactive map with offline support)
            implementation(libs.maplibre.compose)
            implementation(libs.maplibre.compose.material3)

            // Moko Geo & Permissions (GPS location + permission handling)
            implementation(libs.moko.geo)
            implementation(libs.moko.geo.compose)
            implementation(libs.moko.permissions)
            implementation(libs.moko.permissions.compose)

            // SQLDelight (local POI caching)
            implementation(libs.sqldelight.coroutines)

            // Coil (Image loading)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.xplore.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.xplore.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

swiftPackageConfig {
    create("spmMaplibre") {
        dependency {
            remotePackageVersion(
                url = URI("https://github.com/maplibre/maplibre-gl-native-distribution.git"),
                products = { add("MapLibre") },
                version = "6.17.1",
            )
        }
    }
}

sqldelight {
    databases {
        create("XploreDatabase") {
            packageName.set("org.xplore.project.data.local.db")
        }
    }
}
