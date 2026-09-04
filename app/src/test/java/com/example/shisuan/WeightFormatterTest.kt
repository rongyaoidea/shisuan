package com.example.shisuan

import com.example.shisuan.utils.WeightFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * WeightFormatter 单元测试
 * 边界：<1000g 显示克；≥1000g 显示 kg；≥1,000,000g 显示 t
 */
class WeightFormatterTest {

    @Test
    fun `克级别`() {
        assertEquals("0g", WeightFormatter.format(0.0))
        assertEquals("950g", WeightFormatter.format(950.0))
        assertEquals("999.99g", WeightFormatter.format(999.99))
    }

    @Test
    fun `千克级别`() {
        assertEquals("1kg", WeightFormatter.format(1000.0))
        assertEquals("1.5kg", WeightFormatter.format(1500.0))
        assertEquals("2.5kg", WeightFormatter.format(2500.0))
        assertEquals("5kg", WeightFormatter.format(5000.0))
        assertEquals("999.99kg", WeightFormatter.format(999_990.0))
    }

    @Test
    fun `吨级别`() {
        assertEquals("1t", WeightFormatter.format(1_000_000.0))
        assertEquals("1.5t", WeightFormatter.format(1_500_000.0))
        assertEquals("12.34t", WeightFormatter.format(12_340_000.0))
    }

    @Test
    fun `四舍五入与去尾零`() {
        assertEquals("1.51kg", WeightFormatter.format(1505.0))   // 1505/1000=1.505 → 1.51（HALF_UP）
        assertEquals("1kg", WeightFormatter.format(1000.0))      // 去尾零：1.00 → 1
        assertEquals("999.5g", WeightFormatter.format(999.5))    // 边界下保持克级，不越级换算
    }
}
