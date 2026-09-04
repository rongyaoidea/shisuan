package com.example.shisuan.utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 重量单位智能格式化
 *
 * 规则：
 * - < 1000g          → 显示克（保留 2 位小数，去尾零）
 * - 1000g ~ 1,000,000g → 显示千克（kg）
 * - ≥ 1,000,000g     → 显示吨（t）
 *
 * 例：950g → "950g"；1500g → "1.5kg"；2500g → "2.5kg"；1000000g → "1t"
 */
object WeightFormatter {

    private const val GRAM = 1.0
    private const val KILOGRAM = 1000.0
    private const val TON = 1_000_000.0

    fun format(weightGram: Double): String {
        if (weightGram <= 0) return "0g"
        return when {
            weightGram < KILOGRAM -> "${trim(weightGram)}g"
            weightGram < TON -> "${trim(weightGram / KILOGRAM)}kg"
            else -> "${trim(weightGram / TON)}t"
        }
    }

    /**
     * 保留最多 2 位小数并去掉尾部多余的 0（如 1.50 → 1.5、2.0 → 2）
     * 用 valueOf 走十进制转换，避免 BigDecimal(double) 的二进制近似导致 1.505 → 1.50
     */
    private fun trim(value: Double): String {
        val bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
        return bd.stripTrailingZeros().toPlainString()
    }
}
