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
