// 根构建脚本
// 在插件 classpath 上显式声明 JavaPoet 1.13，修复 Hilt Gradle 插件
// AggregateDepsTask 在 AGP 8.x 下找不到 ClassName.canonicalName() 的问题
buildscript {
    dependencies {
        classpath("com.squareup:javapoet:1.13.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
