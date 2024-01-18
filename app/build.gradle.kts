plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.halalscanner"
    compileSdk = 34

    viewBinding {

    }

    defaultConfig {
        applicationId = "com.example.halalscanner"
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

        dataBinding {
            enable
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Concurrent library for asynchronous coroutines
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")

    // CameraX core library
    implementation("androidx.camera:camera-core:1.3.1")

    // CameraX Camera2 extensions
    implementation("androidx.camera:camera-camera2:1.3.1")

    // CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:1.3.1")

    // CameraX View class
    implementation("androidx.camera:camera-view:1.3.1")

    // Barcode model
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // Text recognition
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Stuff for API requests
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")


    //Translation stuff for barcode (for some reason canadian barcodes tend to result in french)
    implementation("com.google.mlkit:language-id:17.0.4")
    implementation("com.google.mlkit:translate:17.0.2")

    // For the loading spinner
    //implementation("com.github.ybq:Android-SpinKit:1.4.0")

    // For DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // For Room database
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:2.6.1")

    // For Picasso
    implementation("com.squareup.picasso:picasso:2.8")

}