package com.example.shisuan

import com.example.shisuan.utils.formatDateMillis
import com.example.shisuan.utils.parseBatchDateMillis
import com.example.shisuan.utils.toUtcDateMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * 批次日期换算工具测试
 *
 * DatePicker 使用「UTC 午夜时间戳」表示日期，批次名前缀是本地日期字符串（yyyy-MM-dd-NN）。
 * 两者往返换算必须稳定：编辑批次时日期是从批次名反解的，
 * 一旦换算漂移就会在保存后跳到相邻日期，进而生成错误的批次序号。
 */
class BatchDateUtilsTest {

    private fun utcMidnightOf(isoDate: String): Long =
        LocalDate.parse(isoDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `UTC 毫秒格式化为日期字符串`() {
        assertEquals("2026-09-03", formatDateMillis(utcMidnightOf("2026-09-03")))
        assertEquals("2026-12-31", formatDateMillis(utcMidnightOf("2026-12-31")))
    }

    @Test
    fun `从批次名解析日期`() {
        assertEquals(
            utcMidnightOf("2026-09-03"),
            parseBatchDateMillis("2026-09-03-01")
        )
        // 序号部分不参与解析
        assertEquals(
            parseBatchDateMillis("2026-09-03-01"),
            parseBatchDateMillis("2026-09-03-12")
        )
    }

    @Test
    fun `批次名格式非法时返回 null`() {
        assertNull(parseBatchDateMillis(""))
        assertNull(parseBatchDateMillis("2026-9"))
        assertNull(parseBatchDateMillis("非日期-01"))
        assertNull(parseBatchDateMillis("2026-13-45-01")) // 非法月/日
    }

    @Test
    fun `日期与批次名往返一致`() {
        val millis = parseBatchDateMillis("2026-12-31-02")!!
        assertEquals("2026-12-31", formatDateMillis(millis))
        // 编辑回填链路：毫秒 → 日期字符串 → 新批次名 → 再解析回同一天
        assertEquals(millis, parseBatchDateMillis(formatDateMillis(millis) + "-01"))
    }

    @Test
    fun `本地时间戳归一到当天 UTC 午夜`() {
        // 下午 15:30（本地时区）应归一到当天，而不是因为时区偏移跳到前一天/后一天
        val afternoon = ZonedDateTime.of(2026, 9, 3, 15, 30, 0, 0, ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        assertEquals("2026-09-03", formatDateMillis(toUtcDateMillis(afternoon)))

        val midnight = ZonedDateTime.of(2026, 9, 3, 0, 0, 0, 0, ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        assertEquals("2026-09-03", formatDateMillis(toUtcDateMillis(midnight)))
    }
}
