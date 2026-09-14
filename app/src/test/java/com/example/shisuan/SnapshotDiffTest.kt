package com.example.shisuan

import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.BatchRecord
import com.example.shisuan.domain.model.IngredientDiffKind
import com.example.shisuan.domain.model.SnapshotDiffer
import com.example.shisuan.utils.BatchSnapshotCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 快照 Diff 测试：两版本字段变化 + 配料增删改。
 *
 * 用 BatchSnapshotCodec.encode/decode 走真实往返，保证 Diff 吃的是
 * 与线上一致的解码结构，而不是手拼的 SnapshotData。
 */
class SnapshotDiffTest {

    private fun batch(
        sampleWeight: Double = 1000.0,
        packaging: Double = 5.0,
        labor: Double = 3.0,
        overhead: Double = 0.0,
        yield: Double? = 85.0,
        note: String = ""
    ) = BatchRecord(
        productId = 1, batchName = "2026-09-03-01",
        sampleWeightGram = sampleWeight,
        packagingCost = packaging, laborCost = labor, overheadCost = overhead,
        yieldRatePercent = yield, note = note
    )

    private fun ing(
        name: String,
        brand: String = "",
        weight: Double = 100.0,
        unitPrice: Double = 12.5,
        totalCost: Double = 1.25
    ) = BatchIngredient(
        batchId = 1, ingredientName = name, ingredientSupplier = brand,
        weight = weight, unitPrice = unitPrice, totalCost = totalCost
    )

    private fun decode(batch: BatchRecord, ings: List<BatchIngredient>) =
        BatchSnapshotCodec.decode(BatchSnapshotCodec.encode(batch, ings))!!

    @Test
    fun `内容一致时无差异`() {
        val a = decode(batch(), listOf(ing("白糖")))
        val b = decode(batch(), listOf(ing("白糖")))
        val result = SnapshotDiffer.diff(a, b)
        assertFalse(result.hasChanges)
        assertTrue(result.fieldChanges.isEmpty())
        assertTrue(result.ingredientDiffs.isEmpty())
    }

    @Test
    fun `批次字段变化被检出`() {
        val a = decode(batch(packaging = 5.0, yield = 85.0), emptyList())
        val b = decode(batch(packaging = 6.0, yield = 90.0), emptyList())
        val result = SnapshotDiffer.diff(a, b)
        assertEquals(2, result.fieldChanges.size)
        assertEquals("包材费", result.fieldChanges[0].label)
        assertTrue(result.ingredientDiffs.isEmpty())
    }

    @Test
    fun `配料新增与删除`() {
        val a = decode(batch(), listOf(ing("白糖"), ing("香精", weight = 0.5, unitPrice = 800.0, totalCost = 0.4)))
        val b = decode(batch(), listOf(ing("白糖"), ing("蜂蜜", weight = 50.0, unitPrice = 60.0, totalCost = 3.0)))
        val result = SnapshotDiffer.diff(a, b)
        val kinds = result.ingredientDiffs.map { it.kind }
        assertTrue(kinds.contains(IngredientDiffKind.REMOVED))
        assertTrue(kinds.contains(IngredientDiffKind.ADDED))
        val removed = result.ingredientDiffs.first { it.kind == IngredientDiffKind.REMOVED }
        val added = result.ingredientDiffs.first { it.kind == IngredientDiffKind.ADDED }
        assertEquals("香精", removed.name)
        assertEquals("蜂蜜", added.name)
        // 删除行排在新增行之前
        assertTrue(result.ingredientDiffs.indexOf(removed) < result.ingredientDiffs.indexOf(added))
    }

    @Test
    fun `同种配料用量与单价变化合并为一行`() {
        val a = decode(batch(), listOf(ing("白糖", weight = 100.0, unitPrice = 12.5, totalCost = 1.25)))
        val b = decode(batch(), listOf(ing("白糖", weight = 120.0, unitPrice = 14.0, totalCost = 1.68)))
        val result = SnapshotDiffer.diff(a, b)
        assertEquals(1, result.ingredientDiffs.size)
        val d = result.ingredientDiffs[0]
        assertEquals(IngredientDiffKind.CHANGED, d.kind)
        assertTrue(d.detail.contains("用量"))
        assertTrue(d.detail.contains("单价"))
        assertTrue(d.detail.contains("小计"))
    }

    @Test
    fun `同名不同品牌视为不同配料`() {
        val a = decode(batch(), listOf(ing("白糖", brand = "A")))
        val b = decode(batch(), listOf(ing("白糖", brand = "B")))
        val result = SnapshotDiffer.diff(a, b)
        assertEquals(2, result.ingredientDiffs.size)
        assertEquals(
            setOf(IngredientDiffKind.REMOVED, IngredientDiffKind.ADDED),
            result.ingredientDiffs.map { it.kind }.toSet()
        )
    }
}
