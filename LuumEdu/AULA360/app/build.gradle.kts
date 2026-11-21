
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // Firebase Google Services
    id("com.google.gms.google-services")
}

// =====================================================
// CONFIGURACION ANDROID
// =====================================================

android {
    namespace = "mx.aula360.app"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "mx.aula360.app"

        minSdk = 24
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    // =========================================
    // VERSION RELEASE
    // =========================================

    buildTypes {
        release {
            optimization {
                enable = true

                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    // =========================================
    // JAVA
    // =========================================

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // =========================================
    // JETPACK COMPOSE
    // =========================================

    buildFeatures {
        compose = true
    }
}

// =====================================================
// DEPENDENCIAS
// =====================================================

dependencies {

    // =========================================
    // FIREBASE BOM
    // =========================================

    implementation(
        platform("com.google.firebase:firebase-bom:34.19.0")
    )

    // =========================================
    // COMPOSE BOM
    // =========================================

    implementation(
        platform(libs.androidx.compose.bom)
    )

    // =========================================
    // GOOGLE PLAY SERVICES - UBICACION
    // =========================================

    implementation(
        "com.google.android.gms:play-services-location:21.4.0"
    )

    // =========================================
    // JETPACK COMPOSE
    // =========================================

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // =========================================
    // ANDROIDX
    // =========================================

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Ciclo de vida para Compose
    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.11.1"
    )

    // =========================================
    // MAPAS - MAPLIBRE
    // =========================================

    implementation(
        "org.maplibre.gl:android-sdk:13.7.1"
    )

    // =========================================
    // FIREBASE AUTHENTICATION Y FIRESTORE
    // =========================================

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // =========================================
    // FIREBASE AI LOGIC - GEMINI
    // =========================================

    implementation("com.google.firebase:firebase-ai")

    // =========================================
    // FIREBASE APP CHECK
    // =========================================

    // SDK principal de App Check
    implementation("com.google.firebase:firebase-appcheck")

    // Proveedor para pruebas locales
    debugImplementation(
        "com.google.firebase:firebase-appcheck-debug"
    )

    // Proveedor para versiones de producción
    releaseImplementation(
        "com.google.firebase:firebase-appcheck-playintegrity"
    )

    // =========================================
    // PRUEBAS UNITARIAS
    // =========================================

    testImplementation(libs.junit)

    // =========================================
    // PRUEBAS ANDROID
    // =========================================

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    // =========================================
    // DEPURACION
    // =========================================

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}
