package com.example.shisuan

import com.example.shisuan.core.ocr.IngredientTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 配料表文本解析单元测试
 */
class IngredientTextParserTest {

    @Test
    fun `顿号分隔的基础配料表`() {
        // 用户示例：咖啡用植脂奶油配料
        val input = "配料：水、氢化植物油、白砂糖、乳化剂（酪氨酸钠、硬脂酰乳酸钠、吐温60）、酸度调节剂（柠檬酸钾、磷酸氢二钾、碳酸钠）、食用盐、食用香精、着色剂（β-胡萝卜素）"
        val result = IngredientTextParser.parse(input)

        assertEquals(
            listOf(
                "水",
                "氢化植物油",
                "白砂糖",
                "乳化剂（酪氨酸钠、硬脂酰乳酸钠、吐温60）",
                "酸度调节剂（柠檬酸钾、磷酸氢二钾、碳酸钠）",
                "食用盐",
                "食用香精",
                "着色剂（β-胡萝卜素）",
            ),
            result
        )
    }

    @Test
    fun `多行与混合分隔符`() {
        val input = "配料表\n水，白砂糖\n乳化剂（酪氨酸钠）\n食用盐"
        val result = IngredientTextParser.parse(input)
        assertEquals(listOf("水", "白砂糖", "乳化剂（酪氨酸钠）", "食用盐"), result)
    }

    @Test
    fun `过滤标题与噪声行`() {
        val input = "配料表\n生产日期：2026-01-01\n水\n\n白砂糖\n保质期 12 个月"
        val result = IngredientTextParser.parse(input)
        assertEquals(listOf("水", "白砂糖"), result)
    }

    @Test
    fun `过滤纯数字与符号`() {
        val input = "123\n水\n(500g)"
        val result = IngredientTextParser.parse(input)
        assertEquals(listOf("水"), result)
    }

    @Test
    fun `空输入返回空列表`() {
        assertTrue(IngredientTextParser.parse("").isEmpty())
        assertTrue(IngredientTextParser.parse("\n\n  \n").isEmpty())
    }
}
