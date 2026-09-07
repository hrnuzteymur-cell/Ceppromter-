plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cepprompter.wear"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.cepprompter.app.wear"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.0-rc01"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
}
