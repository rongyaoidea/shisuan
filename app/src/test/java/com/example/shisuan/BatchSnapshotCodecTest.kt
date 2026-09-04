package com.example.shisuan

import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.BatchRecord
import com.example.shisuan.utils.BatchSnapshotCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 批次快照编解码 + 内容指纹测试（git 式版本链）
 *
 * 快照是「可恢复」承诺的最后防线：编码必须无损往返，
 * 指纹必须内容寻址（同内容同编号、改一字即变），否则恢复链不可信。
 */
class BatchSnapshotCodecTest {

    private fun batch(note: String = "常规批次") = BatchRecord(
        id = 7,
        productId = 1,
        batchName = "2026-09-03-01",
        sampleWeightGram = 1000.0,
        packagingCost = 12.5,
        laborCost = 8.0,
        overheadCost = 0.0,
        yieldRatePercent = 85.0,
        note = note
    )

    private fun ingredients() = listOf(
        BatchIngredient(
            batchId = 7,
            ingredientName = "草莓酱原料",
            ingredientSupplier = "A厂",
            ingredientId = 3,
            weight = 500.0,
            ratioPercent = null,
            unitPrice = 12.0,
            totalCost = 6.0,
            note = "含\"引号\"与\\反斜杠"
        ),
        BatchIngredient(
            batchId = 7,
            ingredientName = "香精",
            ingredientSupplier = "",
            ingredientId = null,       // 未关联原料库
            weight = 0.5,
            ratioPercent = 0.05,       // 比例输入
            unitPrice = 800.0,
            totalCost = 0.4,
            note = "多行\n备注\t含制表符"
        )
    )

    @Test
    fun `编码解码无损往返`() {
        val original = batch()
        val ings = ingredients()
        val data = BatchSnapshotCodec.decode(BatchSnapshotCodec.encode(original, ings))!!

        assertEquals(original.sampleWeightGram, data.sampleWeightGram, 0.0)
        assertEquals(original.packagingCost, data.packagingCost, 0.0)
        assertEquals(original.laborCost, data.laborCost, 0.0)
        assertEquals(original.overheadCost, data.overheadCost, 0.0)
        assertEquals(original.yieldRatePercent!!, data.yieldRatePercent!!, 0.0)
        assertEquals(original.note, data.note)

        assertEquals(2, data.ingredients.size)
        val first = data.ingredients[0]
        assertEquals("草莓酱原料", first.ingredientName)
        assertEquals("A厂", first.ingredientSupplier)
        assertEquals(3L, first.ingredientId)
        assertEquals(500.0, first.weight, 0.0)
        assertEquals(null, first.ratioPercent)
        assertEquals(6.0, first.totalCost, 0.0)
        assertEquals("含\"引号\"与\\反斜杠", first.note)

        val second = data.ingredients[1]
        assertEquals(null, second.ingredientId)
        assertEquals(0.05, second.ratioPercent!!, 0.0)
        // 多行 / 制表符等受控字符在往返后保持原样
        assertEquals("多行\n备注\t含制表符", second.note)
    }

    @Test
    fun `可空字段往返一致`() {
        // 出品率未设置 / 配料未关联原料库 / 按克重输入 —— 全部走 null 哨兵
        val b = batch().copy(yieldRatePercent = null)
        val data = BatchSnapshotCodec.decode(
            BatchSnapshotCodec.encode(b, ingredients())
        )!!
        assertEquals(null, data.yieldRatePercent)
    }

    @Test
    fun `同内容指纹相同 内容变更指纹即变`() {
        val d1 = BatchSnapshotCodec.digest(batch(), ingredients())
        val d2 = BatchSnapshotCodec.digest(batch(), ingredients())
        assertEquals(d1, d2)

        // 改一个字段编号即变 —— 内容寻址语义
        val changed = BatchSnapshotCodec.digest(batch(note = "改了备注"), ingredients())
        assertNotEquals(d1, changed)

        val changedIngs = BatchSnapshotCodec.digest(
            batch(), ingredients() + ingredients().first().copy(weight = 501.0, batchId = 0)
        )
        assertNotEquals(d1, changedIngs)
    }

    @Test
    fun `指纹为8位十六进制`() {
        val digest = BatchSnapshotCodec.digest(batch(), ingredients())
        assertEquals(8, digest.length)
        assertTrue(digest.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `损坏或异版本快照解码返回null`() {
        assertNull(BatchSnapshotCodec.decode(""))
        assertNull(BatchSnapshotCodec.decode("不是快照内容"))
        assertNull(BatchSnapshotCodec.decode("SHISUAN_SNAPSHOT v999\n1\t2\t3\t4\t5\t6"))
        assertNull(BatchSnapshotCodec.decode("SHISUAN_SNAPSHOT v1\nabc\tdef"))
        // 配料行字段缺失
        assertNull(
            BatchSnapshotCodec.decode(
                BatchSnapshotCodec.encode(batch(), ingredients())
                    .lineSequence().take(2).joinToString("\n") + "\nING\t只有名字"
            )
        )
    }
}
