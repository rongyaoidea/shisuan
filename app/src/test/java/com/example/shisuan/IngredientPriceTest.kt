package com.example.shisuan

import com.example.shisuan.data.repository.CostRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 原料单价 upsert 保留策略测试。
 *
 * 回归背景：saveIngredientByNameAndBrand 曾无条件用新价覆盖旧价，
 * 而 OCR 批量入库传价恒为 0（未识别价格），导致重复识别会把
 * 库中已有原料的单价静默清零，成本随之算错。修复后 0 价不覆盖。
 */
class IngredientPriceTest {

    @Test
    fun `零价不覆盖已有单价 - OCR 重复识别场景`() {
        assertEquals(12.0, CostRepository.preservedUnitPrice(incomingPrice = 0.0, existingPrice = 12.0), 1e-9)
    }

    @Test
    fun `正数新价覆盖旧价 - 手动更新价格场景`() {
        assertEquals(14.0, CostRepository.preservedUnitPrice(incomingPrice = 14.0, existingPrice = 12.0), 1e-9)
    }

    @Test
    fun `新旧价格均为零时保持为零`() {
        assertEquals(0.0, CostRepository.preservedUnitPrice(incomingPrice = 0.0, existingPrice = 0.0), 1e-9)
    }
}
