import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.marsa.smarttrackerhub"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.marsa.smarttrackerhub"
        minSdk = 29
        targetSdk = 35
        versionCode = 11
        versionName = "2.0"

        val localProperties = Properties()
        localProperties.load(FileInputStream(rootProject.file("local.properties")))
        buildConfigField("String", "api_key", "\"${localProperties["api_key"]}\"")
        buildConfigField("String", "project_id", "\"${localProperties["project_id"]}\"")
        buildConfigField("String", "storage_bucket", "\"${localProperties["storage_bucket"]}\"")
        buildConfigField("String", "api_key_account", "\"${localProperties["api_key_account"]}\"")
        buildConfigField("String", "project_id_account", "\"${localProperties["project_id_account"]}\"")
        buildConfigField("String", "storage_bucket_account", "\"${localProperties["storage_bucket_account"]}\"")

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
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
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.github.msarhan:ummalqura-calendar:2.0.2")
}