package com.example.shisuan

import com.example.shisuan.utils.CostCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 加工费 / 出品率 / 毛利报价 计算测试
 *
 * 公式（v1.5.0 引入）：
 *   总成本   = 原料成本 + 加工费（包材/人工/水电折旧）
 *   有效产量 = 投料重量 × 出品率
 *   克单价   = 总成本 / 有效产量
 *   建议售价 = 吨价 ÷ (1 - 毛利率)
 */
class CostExtensionTest {

    // ─────────── 加工费 ───────────

    @Test
    fun `加工费计入总成本抬高吨价`() {
        // 投料 1000g，原料 50 元，加工 15 元 → 总成本 65 元
        val r = CostCalculator.calculate(
            sampleWeightGram = 1000.0,
            materialCost = 50.0,
            processingCost = 15.0,
            weightPerBoxGram = 5000.0,
            packagesPerBox = 20
        )
        // 注意：克单价字段被引擎舍入到两位小数（0.065 → 0.07），
        // 精度验证必须用吨价/箱价/包价 —— 它们由未舍入的克单价参与运算后才舍入
        assertEquals(65_000.0, r.unitCostPerTon, 0.01)
        assertEquals(325.0, r.costPerBox, 0.01) // 0.065 × 5000g
        assertEquals(16.25, r.costPerPackage, 0.01)
    }

    @Test
    fun `无加工费时与旧行为一致`() {
        val withDefault = CostCalculator.calculate(
            10.0, 5.0, weightPerBoxGram = 5000.0, packagesPerBox = 20
        )
        val withZero = CostCalculator.calculate(
            10.0, 5.0, 0.0, weightPerBoxGram = 5000.0, packagesPerBox = 20
        )
        assertEquals(withDefault, withZero)
    }

    // ─────────── 出品率 ───────────

    @Test
    fun `出品率按成品重量折算成本`() {
        // 投料 1000g 出 850g：50 元成本摊到 850g 上，吨价高于不折算的 50_000
        val r = CostCalculator.calculate(
            sampleWeightGram = 1000.0,
            materialCost = 50.0,
            weightPerBoxGram = 5000.0,
            packagesPerBox = 20,
            yieldRatePercent = 85.0
        )
        // 50 / 0.85 = 58.8235… 元/kg → 吨价 58_823.53（保留全精度后再舍入）
        assertEquals(58_823.53, r.unitCostPerTon, 0.01)
    }

    @Test
    fun `出品率与加工费叠加`() {
        // 总成本 60 元，有效产量 800g → 75 元/kg
        val r = CostCalculator.calculate(
            sampleWeightGram = 1000.0,
            materialCost = 50.0,
            processingCost = 10.0,
            weightPerBoxGram = 5000.0,
            packagesPerBox = 20,
            yieldRatePercent = 80.0
        )
        assertEquals(75_000.0, r.unitCostPerTon, 0.01)
        assertEquals(375.0, r.costPerBox, 0.01) // 0.075 × 5000g
    }

    @Test
    fun `出品率非法值按不折算处理`() {
        val baseline = CostCalculator.calculate(
            1000.0, 50.0, weightPerBoxGram = 5000.0, packagesPerBox = 20
        )
        // null / 0 / 负数 / >100 均按 100% 计，避免把成本摊到不存在的产量上
        listOf(null, 0.0, -5.0, 120.0).forEach { invalid ->
            val r = CostCalculator.calculate(
                1000.0, 50.0,
                weightPerBoxGram = 5000.0, packagesPerBox = 20,
                yieldRatePercent = invalid
            )
            assertEquals(baseline, r)
        }
    }

    // ─────────── 毛利报价 ───────────

    @Test
    fun `由吨价与毛利率推导建议出厂价`() {
        // 吨价 100_000，毛利率 30% → 100_000 / 0.7 ≈ 142_857.14
        assertEquals(
            142_857.14,
            CostCalculator.suggestedTonPrice(100_000.0, 30.0)!!,
            0.01
        )
    }

    @Test
    fun `毛利率为零或无效时不给报价`() {
        assertNull(CostCalculator.suggestedTonPrice(100_000.0, 0.0))
        assertNull(CostCalculator.suggestedTonPrice(100_000.0, -10.0))
        assertNull(CostCalculator.suggestedTonPrice(100_000.0, 100.0)) // 除零保护
        assertNull(CostCalculator.suggestedTonPrice(100_000.0, 150.0))
    }

    @Test
    fun `成本非正时不给报价`() {
        assertNull(CostCalculator.suggestedTonPrice(0.0, 30.0))
        assertNull(CostCalculator.suggestedTonPrice(-1.0, 30.0))
    }
}
