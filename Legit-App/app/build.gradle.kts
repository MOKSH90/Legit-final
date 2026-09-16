plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.vaultkey"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.vaultkey"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    @Suppress("UnstableApiUsage")
    androidResources {
        noCompress += "tflite"
    }

    signingConfigs {
        // For local testing only: allows installing a "release" APK without a real keystore.
        // Do NOT ship this to Play Store.
        getByName("debug")
    }

    buildTypes {
        debug {
            buildConfigField("String", "LEGIT_BASE_URL", "\"http://10.0.2.2:8080\"")
            buildConfigField("Boolean", "ENABLE_HTTP_LOGS", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "LEGIT_BASE_URL", "\"https://api.legit.example.com\"")
            buildConfigField("Boolean", "ENABLE_HTTP_LOGS", "false")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    val fragment_version = "1.8.2" // Current stable version for 2026
    val activity_version = "1.9.1"

    implementation ("androidx.fragment:fragment-ktx:$fragment_version")
    implementation ("androidx.activity:activity-ktx:$activity_version")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation ("androidx.activity:activity-compose:$activity_version")
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation(libs.androidx.core.ktx)
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp) // The engine for Android
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation("io.ktor:ktor-client-logging:3.4.1")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha15")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.ui)

    // TFLite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation("org.tensorflow:tensorflow-lite-gpu:${libs.versions.tensorflowLite.get()}")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
