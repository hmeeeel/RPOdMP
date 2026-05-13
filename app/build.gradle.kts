plugins {
    alias(libs.plugins.android.application)
    // УДАЛИТЬ: id("com.google.gms.google-services")
}

//val mapkitApiKey = rootProject.extra["mapkitApiKey"] as String

android {
    namespace = "com.example.myapplication"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPKIT_API_KEY", "\"7efa5239-470e-406e-adf5-4edc2d567c88\"")
        buildConfigField("String", "SUPABASE_URL", "\"https://cvheyhcknzfpgjpbhvnz.supabase.co/rest/v1/\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImN2aGV5aGNrbnpmcGdqcGJodm56Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgyMDY1ODQsImV4cCI6MjA5Mzc4MjU4NH0.rcE7ndulcSPVaZndCb9CPyUK2QYVhTmM45-Y4Un3kqw\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // SplashScreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Yandex MapKit
    implementation("com.yandex.android:maps.mobile:4.33.1-full")

    // WorkManager
    implementation("androidx.work:work-runtime:2.9.1")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // OkHttp для Supabase REST + WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("de.hdodenhof:circleimageview:3.1.0")
}