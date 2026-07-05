plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cyberlens.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cyberlens.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "IPINFO_BASE_URL", "\"https://ipinfo.io/\"")
        buildConfigField("String", "VIRUSTOTAL_BASE_URL", "\"https://www.virustotal.com/vtapi/v2/\"")
        buildConfigField("String", "HACKERTARGET_BASE_URL", "\"https://api.hackertarget.com/\"")
        buildConfigField("String", "IPAPI_BASE_URL", "\"https://ipapi.co/\"")
        buildConfigField("String", "CVEDB_BASE_URL", "\"https://cve.circl.lu/api/\"")
        buildConfigField("String", "SHODAN_BASE_URL", "\"https://internetdb.shodan.io/\"")
        buildConfigField("String", "SAUCENAO_BASE_URL", "\"https://saucenao.com/\"")
        buildConfigField("String", "VT_API_KEY", "\"0f534fc80b261f5e993f8a82bc9d91b31eae344fcf7abd74f1b81896c62cc89f\"")
        buildConfigField("String", "SAUCENAO_API_KEY", "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + OkHttp
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.coroutines.android)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)

    // Coil for images
    implementation(libs.coil.compose)

    // Gson
    implementation(libs.gson)

    // Retrofit Scalars converter (for plain text responses)
    implementation(libs.retrofit.scalars)

    // AppCompat (for theme)
    implementation(libs.appcompat)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // DataStore
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
