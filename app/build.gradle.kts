plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.perenniasystems.lascito"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.perenniasystems.lascito"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material.icons)
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    // ── Biometric Authentication ─────────────────────────────────────────────
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // ── Media3 (ExoPlayer) ──────────────────────────────────────────────────
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // ── Firebase ──────────────────────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.functions)
    
    // ── Hilt ──────────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ── DataStore ─────────────────────────────────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ── Security / BCrypt ───────────────────────────────────────────────────
    implementation("at.favre.lib:bcrypt:0.10.2")

    debugImplementation(libs.androidx.ui.tooling)
}

