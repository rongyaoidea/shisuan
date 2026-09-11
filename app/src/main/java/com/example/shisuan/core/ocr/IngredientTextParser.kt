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
    // 白名单说明：必须「相等或去括号后相等」才忽略；
    // 旧 contains 会误伤如「高蛋白质粉」（含“蛋白质”）等合法配料，故收紧为精确匹配。
    private val ignoreKeywords = listOf(
        "配料表", "配料", "成分", "成分表", "生产日期", "保质期", "产品标准号",
        "生产许可证", "储存方法", "贮存", "食用方法", "产地", "净含量",
        "生产商", "制造商", "地址", "电话", "食品添加剂", "营养成分表",
        "能量", "蛋白质", "脂肪", "碳水化合物", "每100", "个月", "产品名称",
        "品牌", "规格", "委托方", "受托方",
    )

    /** 去括号后比对用：去掉「（…）/(…)」及其内容，前后 trim */
    private fun stripParen(token: String): String =
        token.replace(Regex("[（(][^）)]*[）)]"), "").trim()

    private fun isIgnored(token: String): Boolean {
        val t = token.trim()
        return ignoreKeywords.any { kw -> t == kw || stripParen(t) == kw }
    }

    /** 分隔符：顿号、中文逗号、英文逗号、换行、分号、冒号 */
    private val separators = setOf('、', '，', ',', '；', ';', '\n', '\r', '\t', ' ', ':', '：')

    /**
     * 解析配料表文本为配料名称列表。
     * 逐字符切分，保留括号内容（复合配料整体保留）。
     */
    fun parse(text: String): List<String> {
        val result = LinkedHashSet<String>()
        var current = StringBuilder()
        var parenDepth = 0

        fun flush() {
            val raw = current.toString().trim()
            current = StringBuilder()
            if (raw.isEmpty()) return
            // 未闭合括号：孤立左括号与其包住的分隔符一律按分隔处理，
            // 避免整段吞掉后续配料（如 "乳化剂（酪氨酸钠、白砂糖" → 乳化剂 / 酪氨酸钠 / 白砂糖）
            val candidates = if (parenDepth > 0) {
                raw.split('（', '(', '）', ')', '、', '，', ',', '；', ';', '\n', '\r', '\t', ' ', ':', '：')
            } else {
                listOf(raw)
            }
            for (part in candidates) {
                var token = part.trim().take(30)
                if (token.isEmpty()) continue
                // 必须含至少一个汉字（滤除纯数字/符号/英文噪声，同时保留单字配料如 水/盐）
                if (!token.any { it in '\u4e00'..'\u9fff' }) continue
                // 标题/噪声行忽略（相等或去括号后相等）
                if (isIgnored(token)) continue
                result.add(token)
            }
        }

        text.forEach { ch ->
            when {
                ch == '（' || ch == '(' -> { parenDepth++; current.append(ch) }
                ch == '）' || ch == ')' -> {
                    if (parenDepth > 0) parenDepth--
                    current.append(ch)
                }
                parenDepth > 0 -> current.append(ch)
                ch in separators -> flush()
                else -> current.append(ch)
            }
        }
        flush()
        return result.toList()
    }
}
