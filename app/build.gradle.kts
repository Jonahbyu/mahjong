plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.homegame.mahjong"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.homegame.mahjong"
        minSdk = 26
        targetSdk = 36
        // CI passes the run number so every published build counts up; Android refuses
        // an update whose versionCode is not higher than the installed one.
        val ciVersion = System.getenv("VERSION_CODE")?.toIntOrNull()
        versionCode = ciVersion ?: 1
        versionName = if (ciVersion != null) "0.1.$ciVersion" else "0.1-dev"
    }

    // Release signing comes from environment variables set by the GitHub workflow
    // (.github/workflows/release.yml). Without them, assembleRelease builds unsigned.
    val releaseKeystore = System.getenv("RELEASE_KEYSTORE_PATH")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    // Music is streamed straight from the APK, which needs the files stored uncompressed.
    androidResources { noCompress += listOf("mp3", "m4a", "aac", "ogg", "wav", "flac") }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":engine"))

    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.core:core-ktx:1.17.0")
}
