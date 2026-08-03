plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.music.vivi.wear"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vivi.vivimusic.wear"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":innertube"))

    // Wear OS UI
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity)

    // Playback
    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    // Horologist Media Toolkit
    implementation(libs.horologist.media.ui)
    implementation(libs.horologist.media.data)
    implementation(libs.horologist.media3.backend)
    implementation(libs.horologist.audio.ui)
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.network.awareness)
    implementation(libs.horologist.tiles)
    implementation(libs.horologist.networks.ui)

    // Phone↔watch sync
    implementation(libs.play.services.wearable)

    // Bluetooth / audio device state, local storage, coroutines
    implementation(libs.androidx.core.ktx)
    implementation(libs.security.crypto)
    implementation(libs.guava)
    implementation(libs.work.runtime.ktx)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    // Networking (reuse from innertube)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Image loading
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Logging
    implementation(libs.timber)

    // Hilt
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    // Core library desugaring
    coreLibraryDesugaring(libs.desugaring)
}