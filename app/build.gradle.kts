plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.btlandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.btlandroid"
        minSdk = 24
        targetSdk = 35
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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


    
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.github.bumptech.glide:glide:4.15.1")
    
    // Thêm thư viện Firebase Auth và Google Auth
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Add Firebase Firestore and Storage dependencies
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    
    // Thêm thư viện CircleImageView để hiển thị ảnh đại diện dạng tròn
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.google.android.gms:play-services-safetynet:18.0.1")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.11.0")
    
    // Thêm thư viện MPAndroidChart cho chức năng thống kê
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
