package com.example.shisuan

import com.example.shisuan.ui.components.donutSweepAngles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 环形图扇区角度测试：占比归一（总和 360°）与空输入。
 *
 * 动画进度由 UI 层相乘（sweep * progress），此处只锁定角度分配。
 */
class DonutChartTest {

    @Test
    fun `占比按成本归一总和360`() {
        val sweeps = donutSweepAngles(listOf("白糖" to 1.25, "面粉" to 0.5, "香精" to 0.25))
        assertEquals(3, sweeps.size)
        assertEquals(360f, sweeps.sum(), 0.01f)
        // 白糖占 1.25/2.0 = 62.5% → 225°
        assertEquals(225f, sweeps[0], 0.01f)
    }

    @Test
    fun `单项占满整圆`() {
        val sweeps = donutSweepAngles(listOf("白糖" to 2.0))
        assertEquals(listOf(360f), sweeps)
    }

    @Test
    fun `空列表或非正总成本返回空`() {
        assertTrue(donutSweepAngles(emptyList()).isEmpty())
        assertTrue(donutSweepAngles(listOf("白糖" to 0.0)).isEmpty())
    }
}
