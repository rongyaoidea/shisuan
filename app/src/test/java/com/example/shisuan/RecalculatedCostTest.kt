package com.example.shisuan

import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.data.database.PRICE_UNIT_PER_GRAM
import com.example.shisuan.data.database.PRICE_UNIT_PER_KG
import com.example.shisuan.domain.model.RecalculatedCost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 按现价重算预览测试：只算不存，口径与历史价横幅一致。
 */
class RecalculatedCostTest {

    private fun batch(
        name: String = "白糖",
        brand: String = "",
        weight: Double = 100.0,
        unitPrice: Double = 12.5,
        priceUnit: String = PRICE_UNIT_PER_KG,
        totalCost: Double = 1.25
    ) = BatchIngredient(
        batchId = 1, ingredientName = name, ingredientSupplier = brand,
        weight = weight, unitPrice = unitPrice, priceUnit = priceUnit, totalCost = totalCost
    )

    private fun lib(
        name: String = "白糖",
        brand: String = "",
        unitPrice: Double = 14.0,
        priceUnit: String = PRICE_UNIT_PER_KG
    ) = Ingredient(name = name, supplier = brand, unitPrice = unitPrice, priceUnit = priceUnit)

    @Test
    fun `涨价配料按现价重算并计数`() {
        // 100g 白糖快照 12.5/kg（小计 1.25），现价 15/kg → 重算 1.5，计 1 种
        val (total, affected) = RecalculatedCost.materialCostAtLatestPrices(
            listOf(batch()),
            mapOf(("白糖" to "") to lib(unitPrice = 15.0))
        )
        assertEquals(1.5, total, 1e-9)
        assertEquals(1, affected)
    }

    @Test
    fun `同价不计入影响种数且保留快照小计`() {
        val (total, affected) = RecalculatedCost.materialCostAtLatestPrices(
            listOf(batch(totalCost = 1.25)),
            mapOf(("白糖" to "") to lib(unitPrice = 12.5))
        )
        assertEquals(1.25, total, 1e-9)
        assertEquals(0, affected)
    }

    @Test
    fun `库无记录或现价未知时保留快照值`() {
        // 原料库删了该原料
        val (t1, a1) = RecalculatedCost.materialCostAtLatestPrices(listOf(batch()), emptyMap())
        assertEquals(1.25, t1, 1e-9)
        assertEquals(0, a1)
        // 现价为 0（未知）
        val (t2, a2) = RecalculatedCost.materialCostAtLatestPrices(
            listOf(batch()),
            mapOf(("白糖" to "") to lib(unitPrice = 0.0))
        )
        assertEquals(1.25, t2, 1e-9)
        assertEquals(0, a2)
    }

    @Test
    fun `元g现价正确换算`() {
        // 100g 用量，现价 0.02 元/g → 100 × 0.02 = 2.0
        val (total, affected) = RecalculatedCost.materialCostAtLatestPrices(
            listOf(batch(unitPrice = 12.5)),
            mapOf(("白糖" to "") to lib(unitPrice = 0.02, priceUnit = PRICE_UNIT_PER_GRAM))
        )
        assertEquals(2.0, total, 1e-9)
        assertEquals(1, affected)
    }

    @Test
    fun `混合配料只重算偏离项`() {
        val ings = listOf(
            batch(name = "白糖", totalCost = 1.25),
            batch(name = "面粉", unitPrice = 5.0, totalCost = 0.5)
        )
        val libMap = mapOf(
            ("白糖" to "") to lib(name = "白糖", unitPrice = 15.0),
            ("面粉" to "") to lib(name = "面粉", unitPrice = 5.0)
        )
        val (total, affected) = RecalculatedCost.materialCostAtLatestPrices(ings, libMap)
        assertEquals(1.5 + 0.5, total, 1e-9)
        assertEquals(1, affected)
    }
}
