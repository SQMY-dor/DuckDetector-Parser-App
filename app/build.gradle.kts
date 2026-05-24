plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Auto-increment version code from git commit count
fun gitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().use { it.readText().trim().toInt() }
    } catch (_: Exception) {
        // Fallback: timestamp-based version
        ((System.currentTimeMillis() / 1000) - 1700000000).toInt()
    }
}

fun gitShortHash(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short=7", "HEAD")
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().use { it.readText().trim() }
    } catch (_: Exception) {
        "unknown"
    }
}

android {
    namespace = "com.eltavine.duckparse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eltavine.duckparse"
        minSdk = 26
        targetSdk = 35
        versionCode = gitCommitCount()
        versionName = "1.0.${gitCommitCount()}-${gitShortHash()}"

        // Only 64-bit ARM — most modern devices; halves native lib size
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // Signing: reads from env vars set by CI (keystore never committed to repo)
    signingConfigs {
        val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
        if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }

    lint {
        disable += "NullSafeMutableLiveData"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    // ML Kit – on-device barcode scanning (QR codes)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // ML Kit – on-device text recognition (watermark OCR)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // CameraX – live QR scanning and photo capture
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
