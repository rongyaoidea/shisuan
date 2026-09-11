package com.example.shisuan

import android.app.Application
import com.example.shisuan.core.ShisuanCore
import dagger.hilt.android.HiltAndroidApp

/**
 * Application 类 - Hilt 入口点
 */
@HiltAndroidApp
class ShisuanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 后台预触发 Rust 引擎加载，避免首次计算时阻塞主线程
        Thread { ShisuanCore.preloadAsync() }.start()
    }
}
