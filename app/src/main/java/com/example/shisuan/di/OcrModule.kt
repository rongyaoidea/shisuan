package com.example.shisuan.di

import android.content.Context
import com.example.shisuan.core.ocr.OcrAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 模块 - OCR 识别器
 *
 * OcrAnalyzer 为 Singleton 常驻应用进程（ML Kit client 创建成本高，不宜逐次新建）。
 * 释放：进程退出时由 Application 调用 OcrAnalyzer.close()；
 * Hilt SingletonComponent 无自动 @PreDestroy 回调，此处不加释放注解以保证编译通过。
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideOcrAnalyzer(@ApplicationContext context: Context): OcrAnalyzer {
        return OcrAnalyzer(context)
    }
}
