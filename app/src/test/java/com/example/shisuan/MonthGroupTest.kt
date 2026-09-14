package com.example.shisuan

import com.example.shisuan.utils.groupByBatchMonth
import com.example.shisuan.utils.monthKeyOf
import com.example.shisuan.utils.monthLabelOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 批次按月分组测试（产品详情页折叠分组）。
 *
 * 约定：输入 createdAt DESC，分组由新到旧；非法批次名前缀不丢弃，
 * 归入「未知月份」。
 */
class MonthGroupTest {

    @Test
    fun `空列表返回空分组`() {
        assertTrue(groupByBatchMonth(emptyList<String>()) { it }.isEmpty())
    }

    @Test
    fun `同月聚拢组间保序`() {
        val groups = groupByBatchMonth(
            listOf("2026-09-03-01", "2026-09-01-02", "2026-08-20-01", "2026-09-05-01")
        ) { it }
        // 注意：第 4 条虽日期更新但仍属 9 月组——分组只看月份，且组序按首次出现
        assertEquals(listOf("2026-09", "2026-08"), groups.map { it.key })
        assertEquals(3, groups[0].items.size)
        assertEquals(1, groups[1].items.size)
        assertEquals("2026年9月", groups[0].label)
        assertEquals("2026年8月", groups[1].label)
    }

    @Test
    fun `月份标签去前导零`() {
        assertEquals("2026年9月", monthLabelOf("2026-09"))
        assertEquals("2026年11月", monthLabelOf("2026-11"))
        assertEquals("2026年1月", monthLabelOf("2026-01"))
    }

    @Test
    fun `非法批次名归入未知月份`() {
        assertEquals("未知月份", monthKeyOf("手工批次A"))
        assertEquals("未知月份", monthKeyOf(""))
        assertEquals("未知月份", monthLabelOf("未知月份"))
        val groups = groupByBatchMonth(listOf("2026-09-03-01", "手工批次A")) { it }
        assertEquals(listOf("2026-09", "未知月份"), groups.map { it.key })
    }
}
