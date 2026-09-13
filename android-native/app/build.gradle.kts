plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.flowtune.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.flowtune.tv"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "2.2.4"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("flowtune.jks")
            storePassword = "flowtune2026"
            keyAlias = "flowtune"
            keyPassword = "flowtune2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // 媒体
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-datasource:1.5.0")
    implementation("androidx.media3:media3-common:1.5.0")
    implementation("androidx.media:media:1.7.0")

    // 图片
    implementation("io.coil-kt:coil-compose:2.7.0")

    // 标签
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.google.zxing:core:3.5.3")

    // 网络（在线平台）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // LX 音源脚本引擎
    implementation("io.github.dokar3:quickjs-kt:1.0.15")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
