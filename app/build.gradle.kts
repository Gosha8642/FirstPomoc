plugins {
    alias(libs.plugins.android.application)
    id("com.onesignal.androidsdk.onesignal-gradle-plugin")
}

android {
    namespace = "com.example.sosapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sosapplication"
        minSdk = 26
        targetSdk = 36
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // Lifecycle
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    
    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    
    // OSMDroid Map
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    
    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // OneSignal Push Notifications
<<<<<<< HEAD
    implementation("com.onesignal:OneSignal:[5.1.0, 5.99.99]")
=======
    implementation("com.onesignal:OneSignal:[5.0.0, 5.99.99]")
>>>>>>> 552105aaafdee6c893057b00592ed0e3ca2a863a
    
    // OkHttp for API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
<<<<<<< HEAD
=======
    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
>>>>>>> 552105aaafdee6c893057b00592ed0e3ca2a863a
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}