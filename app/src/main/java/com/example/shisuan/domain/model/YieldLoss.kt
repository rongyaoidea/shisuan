package com.example.shisuan.domain.model

/**
 * 出品率损耗换算。
 *
 * 背景：熬煮会蒸发，投料 10 kg 常常只出 8.5 kg。吨价已经按**有效成品重量**
 * （投料 × 出品率）折算过，所以「不折算」与「折算」两个吨价的比值是个纯比例：
 *
 * ```
 * 有效重量 = 投料 × 出品率/100
 * 吨价     ∝ 1 / 有效重量 ∝ 100 / 出品率
 * 损耗影响 = 吨价(折算) / 吨价(不折算) − 1 = 100 / 出品率 − 1
 * ```
 *
 * 总成本不随出品率变化，所以这里不必重算成本，纯比例换算即可。
 *
 * ## 单位约定
 *
 * 本仓库所有 `*Percent` 字段都存**真百分数**（85 表示 85%，不是 0.85）：
 * `BatchRecord.yieldRatePercent` 是 85、`PriceDrift.diffPercent` 是 `(新−旧)/旧*100`、
 * Rust `CostDifferential.diff_percent` 是 16.67。本对象返回值同样遵守这一约定。
 *
 * 这一点曾出过一次事故：详情页的 `lossImpactPercent` 原本返回**分数**
 * （出品率 85 时是 0.176），而 UI 用 `%.1f%%` 格式化，于是显示
 * 「较不折算上升约 0.2%」—— 数字小了 100 倍，看起来像「基本没损耗」，
 * 与同卡里正确的「吨价可降约 ¥53,708」自相矛盾。
 * 所以换算收进本对象，让单位约定落在函数边界上，并由单测锁死。
 */
object YieldLoss {

    /**
     * 熬煮损耗使吨价上升的**百分数**。
     *
     * @param yieldRatePercent 出品率(%)，需 > 0
     * @return 百分数。出品率 85 时约 17.6（即 17.6%，不是 0.176）；
     *   [yieldRatePercent] <= 0 或非有限值时返回 0，表示无从折算。
     */
    fun lossImpactPercent(yieldRatePercent: Double): Double {
        if (yieldRatePercent <= 0.0 || !yieldRatePercent.isFinite()) return 0.0
        return (100.0 / yieldRatePercent - 1.0) * 100.0
    }

    /**
     * 最近批次若恢复到 [bestYieldPercent]，吨价可降金额（元/吨）。
     *
     * 同样因为吨价 ∝ 1/出品率，这是纯比例：
     * 节省 = 最近吨价 × (1 − 最近出品率 / 最佳出品率)。
     *
     * @param recentCostPerTon 最近批次的吨价（元/吨）
     * @param recentYieldPercent 最近批次的出品率(%)
     * @param bestYieldPercent 历史最佳出品率(%)；无记录时为 null
     * @return 元/吨，保留两位；[bestYieldPercent] 无效时为 null
     */
    fun potentialSavingPerTon(
        recentCostPerTon: Double,
        recentYieldPercent: Double,
        bestYieldPercent: Double?
    ): Double? {
        if (bestYieldPercent == null || bestYieldPercent <= 0.0) return null
        if (recentYieldPercent <= 0.0) return null
        return round2(recentCostPerTon * (1.0 - recentYieldPercent / bestYieldPercent))
    }

    /** 与 CostCalculator.round2 同语义：kotlin.math.round 是 half-up。 */
    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
}
