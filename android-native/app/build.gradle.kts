plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 签名信息一律从环境变量或 android-native/local.properties 读取，绝不入库
// （local.properties 已被 .gitignore 忽略；CI 走 FLOWTUNE_KEYSTORE_B64 等 secrets）
val signingProps = java.util.Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingProp(key: String): String? = System.getenv(key) ?: signingProps.getProperty(key)

val flowtuneStoreFile = signingProp("FLOWTUNE_STORE_FILE")
val hasReleaseKeystore = !flowtuneStoreFile.isNullOrBlank() && file(flowtuneStoreFile).exists()

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
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(flowtuneStoreFile!!)
                storePassword = signingProp("FLOWTUNE_STORE_PASSWORD")
                keyAlias = signingProp("FLOWTUNE_KEY_ALIAS")
                keyPassword = signingProp("FLOWTUNE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 未配置 keystore 时退回 debug 签名，保证 assembleRelease 不至于直接失败
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
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
