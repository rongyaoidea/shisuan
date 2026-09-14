package com.example.shisuan.domain.model

import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.data.database.PRICE_UNIT_PER_GRAM
import com.example.shisuan.utils.CostCalculator

/**
 * 按现价重算预览：旧批次快照价 vs 原料库现价的吨价影响。
 *
 * 只算不存——历史批次保留旧价（存档真实成本），预览行帮用户决定
 * 「是否接受涨价 / 是否以新价建一个批次」。
 */
data class RecalculatedPreview(
    val recalcMaterialCost: Double,
    val recalcTonCost: Double,
    val diffAmountPerTon: Double,
    /** 相对当前吨价的涨跌幅(%)，当前吨价 <= 0 时为 null（只展示绝对价） */
    val diffPercentPerTon: Double?,
    /** 按现价替换的种数，与批次卡「历史价」横幅的项数一致 */
    val affectedCount: Int
) {
    val isIncrease: Boolean get() = diffAmountPerTon > 0
}

object RecalculatedCost {
    /**
     * 把批次配料按原料库现价重算原料成本（纯函数，不写库）。
     *
     * 替换规则与 [PriceDriftDetector] 同口径：库中有记录、现价 > 0、
     * 且归一到 元/kg 后与快照价差超容差的项才重算；其余保留快照小计
     * （避免把「同价」也计入影响种数，或因重复换算引入舍入漂移）。
     *
     * @return Pair(重算后原料成本, 被替换种数)
     */
    fun materialCostAtLatestPrices(
        ingredients: List<BatchIngredient>,
        latestByKey: Map<Pair<String, String>, Ingredient>
    ): Pair<Double, Int> {
        if (ingredients.isEmpty()) return 0.0 to 0
        if (latestByKey.isEmpty()) return ingredients.sumOf { it.totalCost } to 0
        var total = 0.0
        var affected = 0
        for (bi in ingredients) {
            val latest = latestByKey[bi.ingredientName to bi.ingredientSupplier]
            val latestPerKg = latest?.let { PriceDriftDetector.toPerKg(it.unitPrice, it.priceUnit) }
            val batchPerKg = PriceDriftDetector.toPerKg(bi.unitPrice, bi.priceUnit)
            if (latest != null && latestPerKg != null &&
                latestPerKg > 0.0 && latestPerKg.isFinite() &&
                batchPerKg.isFinite() &&
                kotlin.math.abs(batchPerKg - latestPerKg) > PriceDriftDetector.TOLERANCE_PER_KG
            ) {
                total += CostCalculator.unitPriceToTotal(
                    bi.weight,
                    latest.unitPrice,
                    isPerGram = latest.priceUnit == PRICE_UNIT_PER_GRAM
                )
                affected++
            } else {
                total += bi.totalCost
            }
        }
        return total to affected
    }
}
