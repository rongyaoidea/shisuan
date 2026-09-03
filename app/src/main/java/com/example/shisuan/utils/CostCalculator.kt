package com.example.shisuan.utils

import com.example.shisuan.core.ShisuanCore
import com.example.shisuan.domain.model.CostResult

/**
 * 成本计算核心逻辑
 *
 * 首选 Rust 引擎（libshisuan_core.so，通过 JNI 调用，源：shisuan-rs/）
 * 若原生库未加载（如单元测试环境），自动回退到等价的 Kotlin 实现。
 */
object CostCalculator {

    private val rustAvailable: Boolean
        get() = ShisuanCore.isLoaded

    /**
     * 批次差异数据
     */
    data class CostDifferential(
        val diffPercent: Double,      // 差异百分比（正=上涨，负=下降）
        val diffAmount: Double,       // 差异金额（元/吨）
        val isIncreased: Boolean      // 是否上涨
    )

    /**
     * 核心换算：
     * 克单价 = 总成本 / 样品重量
     * 吨价   = 克单价 × 1,000,000
     * 箱价   = 克单价 × 每箱克数
     * 包价   = 箱价 / 每箱包数
     */
    fun calculate(
        sampleWeightGram: Double,
        materialCost: Double,
        processingCost: Double = 0.0,
        weightPerBoxGram: Double,
        packagesPerBox: Int
    ): CostResult {
        if (rustAvailable) {
            val out = DoubleArray(5)
            val code = ShisuanCore.calculate(
                sampleWeightGram, materialCost, processingCost,
                weightPerBoxGram, packagesPerBox, out
            )
            if (code == 0) {
                return CostResult(out[0], out[1], out[2], out[3], out[4])
            }
            // Rust 返回无效参数时回退到 Kotlin 计算（保持行为一致）
        }
        return calculateKotlin(
            sampleWeightGram, materialCost, processingCost,
            weightPerBoxGram, packagesPerBox
        )
    }

    /**
     * 计算两个批次吨价差异
     */
    fun calcDifferential(currentTonCost: Double, previousTonCost: Double?): CostDifferential? {
        if (rustAvailable) {
            val out = DoubleArray(3)
            val code = ShisuanCore.calcDifferential(
                currentTonCost, previousTonCost ?: -1.0, out
            )
            if (code == 0) {
                return CostDifferential(
                    diffPercent = out[0],
                    diffAmount = out[1],
                    isIncreased = out[2] > 0.0
                )
            }
        }
        return calcDifferentialKotlin(currentTonCost, previousTonCost)
    }

    /**
     * 保留两位小数
     */
    fun round2(value: Double): Double {
        return if (rustAvailable) {
            ShisuanCore.round2(value)
        } else {
            kotlin.math.round(value * 100.0) / 100.0
        }
    }

    /**
     * 由用量(g)和单价换算原料成本（委托 Rust）
     * @param isPerGram true=单价为元/g，false=元/kg
     */
    fun unitPriceToTotal(weightGram: Double, unitPrice: Double, isPerGram: Boolean = false): Double {
        return if (rustAvailable) {
            ShisuanCore.unitPriceToTotal(weightGram, unitPrice, isPerGram)
        } else {
            val pricePerGram = if (isPerGram) unitPrice else unitPrice / 1000.0
            round2Kotlin(weightGram * pricePerGram)
        }
    }

    // ─────────── Kotlin 等价实现（回退） ───────────

    private fun calculateKotlin(
        sampleWeightGram: Double,
        materialCost: Double,
        processingCost: Double,
        weightPerBoxGram: Double,
        packagesPerBox: Int
    ): CostResult {
        if (sampleWeightGram <= 0.0 || weightPerBoxGram <= 0.0 || packagesPerBox <= 0) {
            return CostResult(0.0, 0.0, 0.0, 0.0, 0.0)
        }
        val totalCost = materialCost + processingCost
        val unitCostPerGram = totalCost / sampleWeightGram
        val unitCostPerTon = unitCostPerGram * 1_000_000.0
        val boxesPerTon = 1_000_000.0 / weightPerBoxGram
        val costPerBox = unitCostPerGram * weightPerBoxGram
        val costPerPackage = costPerBox / packagesPerBox.toDouble()
        return CostResult(
            round2Kotlin(unitCostPerGram), round2Kotlin(unitCostPerTon),
            round2Kotlin(boxesPerTon), round2Kotlin(costPerBox), round2Kotlin(costPerPackage)
        )
    }

    private fun calcDifferentialKotlin(currentTonCost: Double, previousTonCost: Double?): CostDifferential? {
        return previousTonCost?.let { prev ->
            if (prev <= 0.0) return null
            val diffAmount = currentTonCost - prev
            val diffPercent = diffAmount / prev * 100.0
            CostDifferential(
                diffPercent = round2Kotlin(diffPercent),
                diffAmount = round2Kotlin(diffAmount),
                isIncreased = diffAmount > 0
            )
        }
    }

    private fun round2Kotlin(value: Double): Double {
        return kotlin.math.round(value * 100.0) / 100.0
    }
}
