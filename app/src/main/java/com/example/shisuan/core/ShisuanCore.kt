package com.example.shisuan.core

import android.util.Log

/**
 * Rust 核心计算引擎的 JNI 桥接层
 *
 * 对应 Rust crate: shisuan-core（libshisuan_core.so）
 * 源文件: shisuan-rs/src/jni_bridge.rs
 *
 * 所有成本计算最终由 Rust 引擎执行，Kotlin 仅做参数传递。
 */
object ShisuanCore {

    private const val TAG = "ShisuanCore"
    private const val LIB_NAME = "shisuan_core"

    /** 引擎是否成功加载 */
    @Volatile
    var isLoaded: Boolean = false
        private set

    init {
        try {
            System.loadLibrary(LIB_NAME)
            isLoaded = true
            log("Rust 引擎加载成功: ${engineInfo()}")
        } catch (e: UnsatisfiedLinkError) {
            isLoaded = false
            log("Rust 引擎加载失败，将回退到 Kotlin 实现", e)
        }
    }

    /**
     * 测试环境安全日志：单元测试中 android.util.Log 为 stub 实现，
     * 直接调用会抛 RuntimeException，这里吞掉避免阻塞引擎降级路径。
     */
    private fun log(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) Log.e(TAG, message, throwable) else Log.i(TAG, message)
        } catch (_: Throwable) {
            // 单元测试环境无 Log 实现，忽略
        }
    }

    // ─────────── 原生方法（与 jni_bridge.rs 一一对应） ───────────

    /** 引擎版本号 */
    external fun version(): String

    /**
     * 核心成本换算。
     * @param out 长度 5 的数组：[克单价, 吨价, 每吨箱数, 箱价, 包价]
     * @return 0=成功, -1=参数无效
     */
    external fun calculate(
        sampleWeightGram: Double,
        materialCost: Double,
        processingCost: Double,
        weightPerBoxGram: Double,
        packagesPerBox: Int,
        out: DoubleArray
    ): Int

    /**
     * 批次吨价差异对比。
     * @param previousTonCost <= 0 表示无上一批次
     * @param out 长度 3 的数组：[差异百分比, 差异金额, 是否上涨]
     * @return 0=成功计算, 1=无上一批次
     */
    external fun calcDifferential(
        currentTonCost: Double,
        previousTonCost: Double,
        out: DoubleArray
    ): Int

    /** 保留两位小数 */
    external fun round2(value: Double): Double

    /**
     * 由用量(g)和单价换算原料成本。
     * @param isPerGram true=元/g, false=元/kg
     */
    external fun unitPriceToTotal(weightGram: Double, unitPrice: Double, isPerGram: Boolean): Double

    /** 引擎描述（诊断用） */
    external fun engineInfo(): String
}
