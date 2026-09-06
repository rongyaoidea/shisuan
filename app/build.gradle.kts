plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.shisuan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shisuan"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.8.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 发布签名：由环境变量驱动，未配置时保持未签名（不影响日常构建）
    //   SHISUAN_KEYSTORE=keystore 路径，SHISUAN_KEYSTORE_PASSWORD=keystore 口令
    //   SHISUAN_KEY_ALIAS 别名（默认 shisuan）、SHISUAN_KEY_PASSWORD 密钥口令（默认同 keystore）
    val ksPath = System.getenv("SHISUAN_KEYSTORE")
    signingConfigs {
        if (ksPath != null) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("SHISUAN_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SHISUAN_KEY_ALIAS") ?: "shisuan"
                keyPassword = System.getenv("SHISUAN_KEY_PASSWORD")
                    ?: System.getenv("SHISUAN_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // 启用 R8 代码收缩与资源压缩；keep 规则见 proguard-rules.pro
            // （JNI native 方法、Room 实体与生成类不可混淆）
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (ksPath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    // Room schema 导出：供 MigrationTestHelper 校验迁移前后的表结构
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    // schema JSON 作为 assets 打进 androidTest 与 debug variant：
    //   - androidTest：真机 connectedAndroidTest 时 MigrationTestHelper 读取
    //   - debug：Robolectric 迁移测试（testDebugUnitTest）继承 debug variant 的 assets
    //   - release 不包含，生产 APK 不携带测试数据
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
        getByName("debug").assets.srcDirs("$projectDir/schemas")
    }

    testOptions {
        unitTests {
            // Robolectric 需要访问打包资源
            isIncludeAndroidResources = true
            all {
                // android-all 运行时 jar 改走阿里云镜像（默认 maven central 在部分网络不可达）
                it.systemProperty("robolectric.dependency.repo.url", "https://maven.aliyun.com/repository/public")
                it.systemProperty("robolectric.dependency.repo.id", "aliyun")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // OCR - 配料表中文识别（ML Kit 离线模型）
    implementation(libs.mlkit.text.recognition.chinese)
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    // Robolectric：JVM 上模拟 Android 运行时，迁移测试无需真机即可执行
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.room:room-testing:2.6.1")

}
