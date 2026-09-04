package com.example.shisuan.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 批次日期换算工具
 *
 * Material3 的 DatePicker 以「UTC 午夜时间戳」表示日期，
 * 而批次名前缀是本地日期字符串（yyyy-MM-dd-NN），
 * 这里统一两种表示法之间的换算，供 UI 与 ViewModel 共用。
 */

/** UTC 毫秒 → yyyy-MM-dd（DatePicker 使用 UTC 午夜） */
fun formatDateMillis(utcMillis: Long): String =
    Instant.ofEpochMilli(utcMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()

/** 从批次名解析日期（yyyy-MM-dd-NN），失败返回 null */
fun parseBatchDateMillis(batchName: String): Long? {
    if (batchName.length < 10) return null
    val datePart = batchName.take(10)
    return try {
        LocalDate.parse(datePart)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

/** epoch 毫秒（本地时区）→ 该日期 UTC 午夜的毫秒 */
fun toUtcDateMillis(epochMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
