package com.example.shisuan

import com.example.shisuan.domain.model.CostResult
import com.example.shisuan.utils.CostCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CostCalculator 单元测试
 *
 * 注意：单元测试环境无法加载 libshisuan_core.so，
 * 自动走 Kotlin 等价实现；Rust 引擎行为由 shisuan-rs 的 cargo test 保证。
 */
class CostCalculatorTest {

    @Test
    fun `基础成本换算`() {
        // 样品10g，原料成本5元（纯物料），每箱5000g，每箱20包
        val r = CostCalculator.calculate(
            sampleWeightGram = 10.0,
            materialCost = 5.0,
            weightPerBoxGram = 5000.0,
            packagesPerBox = 20
        )
        assertEquals(0.5, r.unitCostPerGram, 0.001)   // 5/10
        assertEquals(500_000.0, r.unitCostPerTon, 0.01)
        assertEquals(200.0, r.boxesPerTon, 0.01)
        assertEquals(2500.0, r.costPerBox, 0.01)
        assertEquals(125.0, r.costPerPackage, 0.01)
    }

    @Test
    fun `非法输入返回全零`() {
        val r = CostCalculator.calculate(0.0, 5.0, 5000.0, 20)
        assertEquals(CostResult(0.0, 0.0, 0.0, 0.0, 0.0), r)

        val r2 = CostCalculator.calculate(10.0, 5.0, 0.0, 20)
        assertEquals(0.0, r2.costPerBox, 0.0)
    }

    @Test
    fun `批次差异对比`() {
        val diff = CostCalculator.calcDifferential(700_000.0, 600_000.0)!!
        assertEquals(16.67, diff.diffPercent, 0.01)
        assertEquals(100_000.0, diff.diffAmount, 0.01)
        assertTrue(diff.isIncreased)

        val down = CostCalculator.calcDifferential(500_000.0, 600_000.0)!!
        assertEquals(-16.67, down.diffPercent, 0.01)
        assertTrue(!down.isIncreased)
    }

    @Test
    fun `无上一批次时不返回差异`() {
        assertEquals(null, CostCalculator.calcDifferential(100.0, null))
        assertEquals(null, CostCalculator.calcDifferential(100.0, 0.0))
    }

    @Test
    fun `四舍五入两位小数`() {
        assertEquals(1.23, CostCalculator.round2(1.23456), 0.0)
        assertEquals(1.24, CostCalculator.round2(1.235), 0.0)
    }

    @Test
    fun `元每kg单价换算原料成本`() {
        // 5g × 12元/kg = 0.06元 —— 回归测试：曾出现 1000 倍计算错误
        val cost = CostCalculator.unitPriceToTotal(
            weightGram = 5.0,
            unitPrice = 12.0,
            isPerGram = false
        )
        assertEquals(0.06, cost, 0.001)

        // 元/g 直接相乘
        val perGram = CostCalculator.unitPriceToTotal(5.0, 0.012, isPerGram = true)
        assertEquals(0.06, perGram, 0.001)
    }
}
