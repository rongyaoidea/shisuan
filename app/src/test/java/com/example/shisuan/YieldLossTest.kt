package com.example.shisuan

import com.example.shisuan.domain.model.YieldLoss
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * 出品率损耗换算测试。
 *
 * 最重要的一条是「损耗影响返回百分数而不是分数」：这里曾出过一次事故，
 * `lossImpactPercent` 返回分数（出品率 85 时 0.176）而 UI 用 `%.1f%%` 格式化，
 * 详情页显示「较不折算上升约 0.2%」，数字小了 100 倍。
 * 所以既断言数值，也断言**经过 UI 实际使用的格式化函数之后**的输出。
 */
class YieldLossTest {

    // ── 损耗影响 ──

    @Test
    fun `损耗影响返回百分数而不是分数`() {
        // 出品率 85：100/85 − 1 = 0.1765（分数）→ ×100 = 17.65（百分数）
        assertEquals(17.647, YieldLoss.lossImpactPercent(85.0), 1e-3)
    }

    @Test
    fun `经过UI格式化后显示为17点6而不是0点2`() {
        // 复现详情页 ProductDetailScreen 的真实格式化路径
        val shown = String.format(
            Locale.CHINA, "较不折算上升约 %.1f%%", YieldLoss.lossImpactPercent(85.0)
        )
        assertEquals("较不折算上升约 17.6%", shown)
    }

    @Test
    fun `无损耗时影响为零`() {
        // 出品率 100：100/100 − 1 = 0
        assertEquals(0.0, YieldLoss.lossImpactPercent(100.0), 1e-9)
    }

    @Test
    fun `出品率越低损耗影响越大`() {
        val at92 = YieldLoss.lossImpactPercent(92.0)
        val at85 = YieldLoss.lossImpactPercent(85.0)
        val at60 = YieldLoss.lossImpactPercent(60.0)
        assertEquals(8.696, at92, 1e-3)
        assertEquals(17.647, at85, 1e-3)
        assertEquals(66.667, at60, 1e-3)
        assertTrue("出品率下降应使损耗影响单调上升：$at92 < $at85 < $at60", at92 < at85 && at85 < at60)
    }

    @Test
    fun `损耗影响与吨价比值互为印证`() {
        // 投料 10 g、总成本 6 元。吨价 ∝ 100/出品率。
        val costPerTonAt100 = 6.0 / 10.0 * 1_000_000.0
        val costPerTonAt85 = 6.0 / 8.5 * 1_000_000.0
        val ratio = costPerTonAt85 / costPerTonAt100
        assertEquals(
            1.0 + YieldLoss.lossImpactPercent(85.0) / 100.0,
            ratio,
            1e-9
        )
    }

    @Test
    fun `非正或非有限的出品率返回零`() {
        assertEquals(0.0, YieldLoss.lossImpactPercent(0.0), 1e-9)
        assertEquals(0.0, YieldLoss.lossImpactPercent(-5.0), 1e-9)
        assertEquals(0.0, YieldLoss.lossImpactPercent(Double.NaN), 1e-9)
        assertEquals(0.0, YieldLoss.lossImpactPercent(Double.POSITIVE_INFINITY), 1e-9)
    }

    // ── 恢复到最佳出品率的节省 ──

    @Test
    fun `恢复到最佳出品率的吨价节省`() {
        // 演示场景：最近 85%、吨价 705882.35；历史最佳 92%
        val saving = YieldLoss.potentialSavingPerTon(
            recentCostPerTon = 705882.35,
            recentYieldPercent = 85.0,
            bestYieldPercent = 92.0
        )
        // 705882.35 × (1 − 85/92) = 705882.35 × 7/92 = 53708.44
        assertEquals(53708.44, saving!!, 0.01)
    }

    @Test
    fun `节省比例等于出品率缺口`() {
        val savingRatio = YieldLoss.potentialSavingPerTon(
            1_000_000.0, 85.0, 92.0
        )!! / 1_000_000.0
        // 1 − 85/92 = 7.609%
        assertEquals(7.609, savingRatio * 100.0, 1e-3)
    }

    @Test
    fun `最近批次与最佳出品率相同时节省为零`() {
        // 两个批次并列最佳：ViewModel 会因 id 相同而不显示；这里只锁数学行为
        val saving = YieldLoss.potentialSavingPerTon(705882.35, 92.0, 92.0)
        assertEquals(0.0, saving!!, 1e-9)
    }

    @Test
    fun `缺少最佳出品率或参数非法时无节省`() {
        assertNull(YieldLoss.potentialSavingPerTon(705882.35, 85.0, null))
        assertNull(YieldLoss.potentialSavingPerTon(705882.35, 85.0, 0.0))
        assertNull(YieldLoss.potentialSavingPerTon(705882.35, 0.0, 92.0))
    }
}
