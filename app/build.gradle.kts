@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    compileSdk = 33
    buildToolsVersion = "30.0.3"
    defaultConfig {
        applicationId = "bogomolov.aa.anochat"
        minSdk = 21
        targetSdk = 30
        versionCode = 58
        versionName = "2023.6.27"
        multiDexEnabled = true
        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.expandProjection", "true")
            }
        }
    }

    signingConfigs {
        create("release"){
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.exifinterface:exifinterface:1.3.2")
    kapt("androidx.room:room-compiler:2.3.0")

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.paging:paging-runtime-ktx:3.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.5.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.room:room-runtime:2.3.0")
    implementation("com.android.support:multidex:1.0.3")
    implementation("androidx.emoji:emoji-appcompat:1.1.0")
    implementation("androidx.emoji:emoji-bundled:1.1.0")
    implementation("com.google.android.material:material:1.4.0-rc01")
    implementation("com.google.firebase:firebase-auth:19.2.0")
    implementation("com.google.firebase:firebase-database-ktx:19.2.1")
    implementation("com.google.firebase:firebase-messaging:20.1.0")
    implementation("com.google.firebase:firebase-analytics:17.2.2")
    implementation("com.google.firebase:firebase-storage-ktx:19.1.1")
    implementation("androidx.annotation:annotation:1.2.0")
    implementation("com.vanniktech:emoji-ios:0.6.0")
    implementation("com.arthenica:mobile-ffmpeg-min-gpl:4.4.LTS")

    implementation("androidx.hilt:hilt-navigation-fragment:1.0.0")
    implementation("androidx.hilt:hilt-work:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-compiler:2.44")

    implementation("com.google.android.exoplayer:exoplayer-core:2.14.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.14.1")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")

    implementation("androidx.compose.ui:ui:1.0.0")
    // Tooling support (Previews, etc.)
    implementation("androidx.compose.ui:ui-tooling:1.0.0")
    // Foundation (Border, Background, Box, Image, Scroll, shapes, animations, etc.)
    implementation("androidx.compose.foundation:foundation:1.0.0")
    // Material Design
    implementation("androidx.compose.material:material:1.0.0")
    // Material design icons
    implementation("androidx.compose.material:material-icons-core:1.0.0")
    implementation("androidx.compose.material:material-icons-extended:1.0.0")
    // Integration with activities
    implementation("androidx.activity:activity-compose:1.3.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha07")
    implementation("androidx.paging:paging-compose:1.0.0-alpha13")
    implementation("androidx.navigation:navigation-compose:2.5.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0-alpha03")


    testImplementation("junit:junit:4.13")
    testImplementation("org.mockito:mockito-core:3.7.7")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1") {
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-debug")
    }
    testImplementation("org.robolectric:robolectric:4.9.2")
    testImplementation("com.google.dagger:hilt-android-testing:2.44")
    kaptTest("com.google.dagger:hilt-android-compiler:2.44")
}