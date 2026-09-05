package com.example.shisuan.utils

import com.example.shisuan.core.ShisuanCore
import com.example.shisuan.domain.model.CostResult

/**
 * 成本计算核心逻辑
 *
 * 首选 Rust 引擎（libshisuan_core.so，通过 JNI 调用，源：shisuan-rs/）
 * 若原生库未加载（如单元测试环境），自动回退到等价的 Kotlin 实现。
 *
 * 出品率折算在本层统一完成：先把投料重量换算为有效成品重量，
 * 再进入引擎计算。这样引擎的 JNI 签名保持不变，
 * 且 Rust 路径与 Kotlin 回退路径的公式天然一致，不会出现双实现漂移。
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
     * 总成本   = 原料成本 + 加工费（包材/人工/水电折旧）
     * 有效产量 = 投料重量 × 出品率
     * 克单价   = 总成本 / 有效产量
     * 吨价     = 克单价 × 1,000,000
     * 箱价     = 克单价 × 每箱克数
     * 包价     = 箱价 / 每箱包数
     *
     * @param yieldRatePercent 出品率(%)，null 表示不折算（按 100% 计）
     */
    fun calculate(
        sampleWeightGram: Double,
        materialCost: Double,
        processingCost: Double = 0.0,
        weightPerBoxGram: Double,
        packagesPerBox: Int,
        yieldRatePercent: Double? = null
    ): CostResult {
        val effectiveWeight = effectiveWeightGram(sampleWeightGram, yieldRatePercent)
        if (rustAvailable) {
            val out = DoubleArray(5)
            val code = ShisuanCore.calculate(
                effectiveWeight, materialCost, processingCost,
                weightPerBoxGram, packagesPerBox, out
            )
            if (code == 0) {
                return CostResult(out[0], out[1], out[2], out[3], out[4])
            }
            // Rust 返回无效参数时回退到 Kotlin 计算（保持行为一致）
        }
        return calculateKotlin(
            effectiveWeight, materialCost, processingCost,
            weightPerBoxGram, packagesPerBox
        )
    }

    /**
     * 出品率 → 有效成品重量。
     *
     * 合法区间为 (0, 100]：
     * - null / <=0：按 100% 计（不折算），兼容未录入出品率的历史批次
     * - >100：视为无效录入 —— 折算出大于投料的「产量」会低估成本，
     *   与其放大产量不如按 100% 保守处理
     */
    fun effectiveWeightGram(sampleWeightGram: Double, yieldRatePercent: Double?): Double {
        if (sampleWeightGram <= 0.0) return sampleWeightGram
        if (yieldRatePercent == null || yieldRatePercent <= 0.0 || yieldRatePercent > 100.0) {
            return sampleWeightGram
        }
        return sampleWeightGram * yieldRatePercent / 100.0
    }

    /**
     * 由吨价与目标毛利率推导建议出厂价（元/吨）。
     *
     * 建议售价 = 吨价 ÷ (1 - 毛利率)
     *
     * @return 毛利率不在 (0, 100) 区间或成本非正时返回 null（UI 不展示报价）
     */
    fun suggestedTonPrice(costPerTon: Double, marginRatePercent: Double): Double? {
        if (costPerTon <= 0.0) return null
        if (marginRatePercent <= 0.0 || marginRatePercent >= 100.0) return null
        return round2(costPerTon / (1.0 - marginRatePercent / 100.0))
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
            // 防御与 Rust 版（shisuan-rs calc.rs）对齐：
            // 无效重量/负单价一律返回 0，避免两条路径对同一输入给出不同结果
            if (weightGram <= 0.0 || unitPrice < 0.0 || !weightGram.isFinite() || !unitPrice.isFinite()) {
                0.0
            } else {
                val pricePerGram = if (isPerGram) unitPrice else unitPrice / 1000.0
                round2Kotlin(weightGram * pricePerGram)
            }
        }
    }

    /**
     * 比例(%) → 克重(g)：按样品重量换算
     * 香精/添加剂微量场景，如样品 1000g、比例 0.05% → 0.5g
     */
    fun ratioPercentToGram(sampleWeightGram: Double, ratioPercent: Double): Double {
        return sampleWeightGram * ratioPercent / 100.0
    }

    // ─────────── Kotlin 等价实现（回退） ───────────

    /**
     * @param sampleWeightGram 有效成品重量（调用前已完成出品率折算）
     */
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
        // 与 Rust 版对齐：当前值非有限时按「无对比数据」处理，避免 NaN/Inf 进入 UI
        if (!currentTonCost.isFinite()) return null
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
