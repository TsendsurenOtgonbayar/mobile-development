plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // Энэ app module дээр Room-ийн код үүсгэгчийг ажиллуулна.
    alias(libs.plugins.google.ksp)
}
android {
    namespace = "num.edu.mn.flashcard"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "num.edu.mn.flashcard"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation(libs.androidx.room.runtime)
    // ViewModel болон viewModelScope ашиглах.
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
// Compose дотроос lifecycle-д тохируулан ViewModel авах.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
// Compose дээр Flow-г lifecycle-д тохируулан ажиглах.
    implementation(libs.androidx.lifecycle.runtime.compose)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}