package com.example.shisuan

import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.data.database.PRICE_UNIT_PER_GRAM
import com.example.shisuan.data.database.PRICE_UNIT_PER_KG
import com.example.shisuan.domain.model.PriceDriftDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 原料改价断点检测测试：批次快照价 vs 原料库现价。
 *
 * 约定：历史批次保留旧价，UI 以「历史价」横幅指出从哪一批起、哪种原料偏离。
 */
class PriceDriftTest {

    private fun batch(
        name: String = "白糖",
        brand: String = "",
        unitPrice: Double = 12.5,
        priceUnit: String = PRICE_UNIT_PER_KG
    ) = BatchIngredient(
        batchId = 1, ingredientName = name, ingredientSupplier = brand,
        weight = 100.0, unitPrice = unitPrice, priceUnit = priceUnit, totalCost = 1.25
    )

    private fun lib(
        name: String = "白糖",
        brand: String = "",
        unitPrice: Double = 14.0,
        priceUnit: String = PRICE_UNIT_PER_KG
    ) = Ingredient(name = name, supplier = brand, unitPrice = unitPrice, priceUnit = priceUnit)

    @Test
    fun `同价不提示`() {
        val drifts = PriceDriftDetector.detect(
            listOf(batch(unitPrice = 12.5)),
            mapOf(("白糖" to "") to lib(unitPrice = 12.5))
        )
        assertTrue(drifts.isEmpty())
    }

    @Test
    fun `涨价被检出并算出涨幅`() {
        val drifts = PriceDriftDetector.detect(
            listOf(batch(unitPrice = 10.0)),
            mapOf(("白糖" to "") to lib(unitPrice = 12.0))
        )
        assertEquals(1, drifts.size)
        assertEquals("白糖", drifts[0].name)
        assertEquals(10.0, drifts[0].batchPricePerKg, 1e-9)
        assertEquals(12.0, drifts[0].latestPricePerKg, 1e-9)
        assertEquals(20.0, drifts[0].diffPercent!!, 1e-9)
        assertTrue(drifts[0].isIncrease)
    }

    @Test
    fun `元g与元kg量纲归一后同价不误报`() {
        // 批次 0.012 元/g == 12 元/kg，库价 12 元/kg → 无偏离
        val drifts = PriceDriftDetector.detect(
            listOf(batch(unitPrice = 0.012, priceUnit = PRICE_UNIT_PER_GRAM)),
            mapOf(("白糖" to "") to lib(unitPrice = 12.0, priceUnit = PRICE_UNIT_PER_KG))
        )
        assertTrue(drifts.isEmpty())
    }

    @Test
    fun `库中无记录或现价未知时不提示`() {
        // 删除/改名：无从对比，保持安静
        assertTrue(
            PriceDriftDetector.detect(listOf(batch()), emptyMap()).isEmpty()
        )
        // 现价为 0（未知）：不提示
        assertTrue(
            PriceDriftDetector.detect(
                listOf(batch(unitPrice = 12.5)),
                mapOf(("白糖" to "") to lib(unitPrice = 0.0))
            ).isEmpty()
        )
    }

    @Test
    fun `按名称加品牌匹配不同品牌互不干扰`() {
        val drifts = PriceDriftDetector.detect(
            listOf(batch(brand = "A", unitPrice = 10.0)),
            mapOf(("白糖" to "B") to lib(brand = "B", unitPrice = 99.0))
        )
        assertTrue(drifts.isEmpty())
    }
}
