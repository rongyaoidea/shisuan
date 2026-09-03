package com.example.shisuan.core.ocr

/**
 * 配料表文本解析器（纯逻辑，可单元测试）
 *
 * 中文配料表特征：
 * - 顿号（、）、逗号（，/ ,）、换行分隔各配料
 * - 复合配料带括号：乳化剂（酪氨酸钠、硬脂酰乳酸钠、吐温60）
 *   → 保留整个复合项作为单一配料（工厂通常按整包复合物采购计价）
 * - 单字配料合法：水、盐、糖
 * - 首行常见标题：配料/配料表/成分表
 *
 * 示例输入：
 * "配料：水、氢化植物油、白砂糖、乳化剂（酪氨酸钠、硬脂酰乳酸钠、吐温60）、食用盐"
 * → [水, 氢化植物油, 白砂糖, 乳化剂（酪氨酸钠、硬脂酰乳酸钠、吐温60）, 食用盐]
 */
object IngredientTextParser {

    /** 需要忽略的标题/噪声关键词（精确到完整词，避免误伤配料名） */
    private val ignoreKeywords = listOf(
        "配料表", "配料", "成分", "成分表", "生产日期", "保质期", "产品标准号",
        "生产许可证", "储存方法", "贮存", "食用方法", "产地", "净含量",
        "生产商", "制造商", "地址", "电话", "食品添加剂", "营养成分表",
        "能量", "蛋白质", "脂肪", "碳水化合物", "每100", "个月", "产品名称",
        "品牌", "规格", "委托方", "受托方",
    )

    /** 分隔符：顿号、中文逗号、英文逗号、换行、分号、冒号 */
    private val separators = setOf('、', '，', ',', '；', ';', '\n', '\r', '\t', ' ', ':', '：')

    /**
     * 解析配料表文本为配料名称列表。
     * 逐字符切分，保留括号内容（复合配料整体保留）。
     */
    fun parse(text: String): List<String> {
        val result = LinkedHashSet<String>()
        var current = StringBuilder()

        fun flush() {
            val token = current.toString().trim()
            current = StringBuilder()
            if (token.isEmpty()) return
            // 必须含至少一个汉字（滤除纯数字/符号/英文噪声，同时保留单字配料如 水/盐）
            if (!token.any { it in '\u4e00'..'\u9fff' }) return
            // 标题/噪声行忽略
            if (ignoreKeywords.any { token.contains(it) }) return
            result.add(token)
        }

        var inParen = false
        text.forEach { ch ->
            when {
                ch == '（' || ch == '(' -> { inParen = true; current.append(ch) }
                ch == '）' || ch == ')' -> { inParen = false; current.append(ch) }
                inParen -> current.append(ch)
                ch in separators -> flush()
                else -> current.append(ch)
            }
        }
        flush()
        return result.toList()
    }
}
