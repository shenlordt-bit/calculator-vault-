plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.calculatorvault"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.calculatorvault"
        // Change this via first-run setup dialog; hardcoded PINs are never stored.
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-V1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Local encryption: Android Keystore-backed master key, EncryptedSharedPreferences, EncryptedFile
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Needed for ProcessLifecycleOwner-based auto-lock (app-wide foreground/background detection)
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
