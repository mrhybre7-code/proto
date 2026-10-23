plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // 👇 Necesario desde Kotlin 2.x
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.m.adacolorytrazo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.m.adacolorytrazo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "11"
    }

    // ✅ Jetpack Compose activado
    buildFeatures {
        compose = true
    }

    // ⛔ Con el plugin de Kotlin 2.x, NO necesitas kotlinCompilerExtensionVersion
    // composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // ✅ BOM: gestiona versiones de Compose (ui, material3, foundation, etc.)
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)

    // Core Compose
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")



    // (Opcional) Window size classes de Material 3 — versión la maneja el BOM
    implementation("androidx.compose.material3:material3-window-size-class")

    // ViewModel para Compose (si lo usas)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")


    // Compat / soporte XML (si lo necesitas en otras pantallas)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material:material-icons-extended")


    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
