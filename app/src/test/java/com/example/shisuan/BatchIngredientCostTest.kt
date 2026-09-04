package com.example.shisuan

import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.data.database.PRICE_UNIT_PER_GRAM
import com.example.shisuan.data.database.PRICE_UNIT_PER_KG
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BatchIngredient 小计较换算测试
 *
 * 回归重点：`weight` 单位是 **g**，`unitPrice` 默认单位是 **元/kg**，
 * 两者量纲不同，直接相乘会放大 1000 倍。
 * 工厂函数 [BatchIngredient.create] 必须按 priceUnit 正确换算，
 * 且不能依赖 Entity 的默认值（历史默认值 `weight * unitPrice` 就是错的）。
 *
 * 单测环境无法加载 libshisuan_core.so，自动走 Kotlin 等价实现。
 */
class BatchIngredientCostTest {

    private fun ingredient(
        price: Double,
        unit: String = PRICE_UNIT_PER_KG,
        name: String = "草莓",
        brand: String = "某品牌"
    ) = Ingredient(
        id = 1,
        name = name,
        supplier = brand,
        unitPrice = price,
        priceUnit = unit
    )

    @Test
    fun `元每kg 单价按克重正确换算`() {
        // 500g × 12元/kg = 6元；若误用 weight * unitPrice 会得到 6000
        val item = BatchIngredient.create(ingredient(12.0), weightGram = 500.0)
        assertEquals(6.0, item.totalCost, 0.001)
    }

    @Test
    fun `元每g 单价直接相乘`() {
        // 500g × 0.012元/g = 6元
        val item = BatchIngredient.create(
            ingredient(0.012, PRICE_UNIT_PER_GRAM),
            weightGram = 500.0
        )
        assertEquals(6.0, item.totalCost, 0.001)
    }

    @Test
    fun `两种单位换算结果一致`() {
        val perKg = BatchIngredient.create(ingredient(12.0, PRICE_UNIT_PER_KG), 250.0)
        val perGram = BatchIngredient.create(ingredient(0.012, PRICE_UNIT_PER_GRAM), 250.0)
        assertEquals(3.0, perKg.totalCost, 0.001)
        assertEquals(perKg.totalCost, perGram.totalCost, 0.001)
    }

    @Test
    fun `priceUnit 必须透传原料档案而非写死默认值`() {
        val item = BatchIngredient.create(ingredient(0.012, PRICE_UNIT_PER_GRAM), 1.0)
        assertEquals(PRICE_UNIT_PER_GRAM, item.priceUnit)

        val kgItem = BatchIngredient.create(ingredient(12.0, PRICE_UNIT_PER_KG), 1.0)
        assertEquals(PRICE_UNIT_PER_KG, kgItem.priceUnit)
    }

    @Test
    fun `保留原料档案的名称品牌与外键`() {
        val item = BatchIngredient.create(
            ingredient = ingredient(12.0, name = "草莓", brand = "A厂"),
            weightGram = 100.0
        )
        assertEquals("草莓", item.ingredientName)
        assertEquals("A厂", item.ingredientSupplier)
        assertEquals(1L, item.ingredientId)
        assertEquals(0L, item.batchId) // 保存批次时回填
    }

    @Test
    fun `比例输入保留原始比例值`() {
        // 样品 1000g、香精 0.05% → 0.5g，同时保留 0.05 供展示
        val item = BatchIngredient.create(
            ingredient = ingredient(12.0),
            weightGram = 0.5,
            ratioPercent = 0.05
        )
        assertEquals(0.05, item.ratioPercent!!, 0.0001)
        assertEquals(0.5, item.weight, 0.0001)
        assertEquals(0.01, item.totalCost, 0.001) // 0.5g × 12元/kg
    }

    @Test
    fun `克重输入时比例值为 null`() {
        val item = BatchIngredient.create(ingredient(12.0), weightGram = 100.0)
        assertEquals(null, item.ratioPercent)
        assertEquals(1.2, item.totalCost, 0.001)
    }

    @Test
    fun `边界值不崩溃且成本为零`() {
        assertEquals(0.0, BatchIngredient.create(ingredient(12.0), 0.0).totalCost, 0.0)
        assertEquals(0.0, BatchIngredient.create(ingredient(0.0), 100.0).totalCost, 0.0)
    }

    @Test
    fun `微量添加剂成本不会被量纲错误放大`() {
        // 香精 0.5g、单价 800元/kg → 0.4元
        // 若按 weight * unitPrice 会得到 400元，直接毁掉整个成本模型
        val item = BatchIngredient.create(ingredient(800.0), weightGram = 0.5)
        assertEquals(0.4, item.totalCost, 0.001)
    }
}
