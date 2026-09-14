package com.example.shisuan.domain.model

import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.data.database.PRICE_UNIT_PER_GRAM

/**
 * 单种原料的价格偏离：批次快照价 vs 原料库现价。
 *
 * 批次配料是创建时的快照（单价拷贝存档），原料库改价后旧批次保留历史价。
 * 该模型用于在产品详情页标出「从哪一批起、哪种原料用了历史价」。
 *
 * 所有单价统一归一到 元/kg 后再比较，避免 元/g 与 元/kg 量纲误判。
 */
data class IngredientPriceDrift(
    val name: String,
    val brand: String,
    val batchPricePerKg: Double,
    val latestPricePerKg: Double,
    /** 相对批次价的涨跌幅(%)，批次价 <= 0 时为 null（无法算比例，只展示绝对价） */
    val diffPercent: Double? = null
) {
    val isIncrease: Boolean get() = latestPricePerKg > batchPricePerKg
}

object PriceDriftDetector {
    /** 归一后 元/kg 差值容差：过滤浮点噪音，小于 0.005 视为同价 */
    const val TOLERANCE_PER_KG = 0.005

    fun toPerKg(price: Double, priceUnit: String): Double =
        if (priceUnit == PRICE_UNIT_PER_GRAM) price * 1000.0 else price

    /**
     * 逐项比对批次快照与原料库现价，返回偏离项。
     *
     * 跳过规则（不提示）：
     * - 原料库已无该（名称+品牌）记录（删除/改名）：旧批次无从对比，保持安静
     * - 原料库现价 <= 0（未知价）：无法对比
     *
     * @param latestByKey 以 (名称, 品牌) 为键的原料库现价表，与使用频次统计口径一致
     */
    fun detect(
        batchIngredients: List<BatchIngredient>,
        latestByKey: Map<Pair<String, String>, Ingredient>
    ): List<IngredientPriceDrift> {
        if (batchIngredients.isEmpty() || latestByKey.isEmpty()) return emptyList()
        val drifts = ArrayList<IngredientPriceDrift>(2)
        for (bi in batchIngredients) {
            val latest = latestByKey[bi.ingredientName to bi.ingredientSupplier] ?: continue
            val latestPerKg = toPerKg(latest.unitPrice, latest.priceUnit)
            if (latestPerKg <= 0.0 || !latestPerKg.isFinite()) continue
            val batchPerKg = toPerKg(bi.unitPrice, bi.priceUnit)
            if (!batchPerKg.isFinite()) continue
            if (kotlin.math.abs(batchPerKg - latestPerKg) <= TOLERANCE_PER_KG) continue
            val diffPercent = if (batchPerKg > 0.0) {
                (latestPerKg - batchPerKg) / batchPerKg * 100.0
            } else null
            drifts.add(
                IngredientPriceDrift(
                    name = bi.ingredientName,
                    brand = bi.ingredientSupplier,
                    batchPricePerKg = batchPerKg,
                    latestPricePerKg = latestPerKg,
                    diffPercent = diffPercent
                )
            )
        }
        return drifts
    }
}
