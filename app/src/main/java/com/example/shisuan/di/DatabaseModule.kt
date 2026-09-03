package com.example.shisuan.di

import android.content.Context
import com.example.shisuan.data.database.CostCalDatabase
import com.example.shisuan.data.repository.CostRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CostCalDatabase {
        return CostCalDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideRepository(database: CostCalDatabase): CostRepository {
        return CostRepository(database)
    }
}
