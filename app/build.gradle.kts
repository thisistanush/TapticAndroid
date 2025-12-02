plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tapticapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tapticapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

}

dependencies {
    // Core Android
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    
    // ViewPager2 for tab navigation
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // Room database for history
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    // LiteRT for audio classification (supports 16KB page size)
    implementation("com.google.ai.edge.litert:litert:1.4.0")
    
    // JSON
    implementation("org.json:json:20230227")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}