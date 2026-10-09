plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.faisal.routine"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.faisal.routine"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so you can install the release APK directly on your phone.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // Personal app, not for the Play Store: don't block release builds on Play Store lint rules.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

// No external libraries: only the Android platform APIs are used.
