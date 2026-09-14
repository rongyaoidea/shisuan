package com.example.shisuan.utils

/**
 * 批次按月分组（产品详情页折叠分组用）。
 *
 * 分组键取自批次名前缀 yyyy-MM（GenerateBatchNameUseCase 生成「日期-序号」格式）；
 * 前缀不合法的批次归入「未知月份」，不丢弃。
 * 输入顺序即输出顺序——调用方传入 createdAt DESC 列表，分组自然由新到旧。
 */
data class MonthGroup<T>(
    /** 分组键：yyyy-MM 或「未知月份」 */
    val key: String,
    /** 展示标签：如「2026年9月」 */
    val label: String,
    val items: List<T>
)

private val MONTH_KEY_PATTERN = Regex("^\\d{4}-\\d{2}")

/** 批次名 → 分组键 */
fun monthKeyOf(batchName: String): String =
    batchName.take(7).takeIf { MONTH_KEY_PATTERN.matches(it) } ?: "未知月份"

/** 分组键 → 展示标签 */
fun monthLabelOf(key: String): String {
    if (key == "未知月份") return key
    val month = key.substring(5, 7).trimStart('0').ifEmpty { "0" }
    return "${key.substring(0, 4)}年${month}月"
}

/** 保序分组：同月批次聚拢，组间保持首次出现顺序 */
fun <T> groupByBatchMonth(items: List<T>, nameOf: (T) -> String): List<MonthGroup<T>> {
    if (items.isEmpty()) return emptyList()
    val groups = LinkedHashMap<String, MutableList<T>>()
    for (item in items) {
        groups.getOrPut(monthKeyOf(nameOf(item))) { ArrayList() }.add(item)
    }
    return groups.map { (key, value) -> MonthGroup(key, monthLabelOf(key), value) }
}
