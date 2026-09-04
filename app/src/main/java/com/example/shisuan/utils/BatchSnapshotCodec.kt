package com.example.shisuan.utils

import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.BatchRecord
import java.security.MessageDigest

/**
 * 批次快照编解码 + 内容指纹（git 式版本链的核心）
 *
 * 设计要点：
 * - [encode] 产出格式化文本存入 BatchSnapshot.snapshotData，纯 Kotlin 实现、
 *   零平台依赖，可在 JVM 单元测试中完整验证
 * - [digest] 对编码文本做 SHA-256，取前 8 位十六进制作为「唯一变更编号」：
 *   内容相同 → 编号相同 → 入库前查重即可跳过，与 git 的提交去重语义一致
 * - 批次名（batchName）不进入快照：批次号可能因改日期而重新生成，
 *   恢复历史版本时保留当前批次号
 *
 * 编码格式（行式，字段以 \t 分隔，受控转义）：
 * ```
 * SHISUAN_SNAPSHOT v1
 * <投料克重>\t<包材费>\t<人工费>\t<水电折旧>\t<出品率,-1=未设置>\t<备注>
 * ING\t<名称>\t<品牌>\t<原料库id,-1=未关联>\t<用量g>\t<比例,-1=按克重>\t<单价>\t<单价单位>\t<小计>\t<备注>
 * ...
 * ```
 */
object BatchSnapshotCodec {

    /** 编码格式版本，未来字段变更时升版并保持向后兼容读取 */
    const val FORMAT_VERSION = 1

    private const val HEADER = "SHISUAN_SNAPSHOT v$FORMAT_VERSION"
    private const val ING_TAG = "ING"
    private const val NULL_SENTINEL = "-1"

    /** 快照恢复所需的批次内容（不含批次号/时间戳等元数据） */
    data class SnapshotData(
        val sampleWeightGram: Double,
        val packagingCost: Double,
        val laborCost: Double,
        val overheadCost: Double,
        val yieldRatePercent: Double?,
        val note: String,
        val ingredients: List<BatchIngredient>
    )

    // ─────────── 编码 ───────────

    fun encode(batch: BatchRecord, ingredients: List<BatchIngredient>): String = buildString {
        appendLine(HEADER)
        appendLine(
            listOf(
                batch.sampleWeightGram,
                batch.packagingCost,
                batch.laborCost,
                batch.overheadCost,
                batch.yieldRatePercent ?: NULL_SENTINEL.toDouble(),
                batch.note.escape()
            ).joinToString("\t")
        )
        ingredients.forEach { ing ->
            appendLine(
                listOf(
                    ING_TAG,
                    ing.ingredientName.escape(),
                    ing.ingredientSupplier.escape(),
                    ing.ingredientId ?: NULL_SENTINEL.toLong(),
                    ing.weight,
                    ing.ratioPercent ?: NULL_SENTINEL.toDouble(),
                    ing.unitPrice,
                    ing.priceUnit.escape(),
                    ing.totalCost,
                    ing.note.escape()
                ).joinToString("\t")
            )
        }
    }

    // ─────────── 解码 ───────────

    /** 解码失败（损坏/异版本格式）返回 null，调用方应视为不可恢复的快照 */
    fun decode(text: String): SnapshotData? {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty() || lines[0] != HEADER) return null

        val body = lines.getOrNull(1)?.split("\t") ?: return null
        if (body.size < 6) return null

        val sampleWeight = body[0].toDoubleOrNull() ?: return null
        val packaging = body[1].toDoubleOrNull() ?: return null
        val labor = body[2].toDoubleOrNull() ?: return null
        val overhead = body[3].toDoubleOrNull() ?: return null
        val yieldRate = body[4].toDoubleOrNull()?.takeIf { it != NULL_SENTINEL.toDouble() }
        val note = body[5].unescape()

        val ingredients = lines.drop(2).map { line ->
            val f = line.split("\t")
            if (f.size < 10 || f[0] != ING_TAG) return null
            BatchIngredient(
                batchId = 0, // 恢复时按目标批次回填
                ingredientName = f[1].unescape(),
                ingredientSupplier = f[2].unescape(),
                ingredientId = f[3].toLongOrNull()?.takeIf { it != NULL_SENTINEL.toLong() },
                weight = f[4].toDoubleOrNull() ?: return null,
                ratioPercent = f[5].toDoubleOrNull()?.takeIf { it != NULL_SENTINEL.toDouble() },
                unitPrice = f[6].toDoubleOrNull() ?: return null,
                priceUnit = f[7].unescape(),
                totalCost = f[8].toDoubleOrNull() ?: return null,
                note = f[9].unescape()
            )
        }

        return SnapshotData(
            sampleWeightGram = sampleWeight,
            packagingCost = packaging,
            laborCost = labor,
            overheadCost = overhead,
            yieldRatePercent = yieldRate,
            note = note,
            ingredients = ingredients
        )
    }

    // ─────────── 内容指纹 ───────────

    /**
     * 内容指纹（唯一变更编号）：SHA-256 前 8 位十六进制，展示为 #xxxxxxxx。
     * 与 git 短哈希同思路：内容寻址，改一个字符编号即变。
     */
    fun digest(batch: BatchRecord, ingredients: List<BatchIngredient>): String =
        digestOf(encode(batch, ingredients))

    fun digestOf(encoded: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(encoded.toByteArray(Charsets.UTF_8))
        return bytes.take(4).joinToString("") { "%02x".format(it) }
    }

    // ─────────── 转义（\t / \n / \\ 受控字符） ───────────

    private fun String.escape(): String =
        replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")

    private fun String.unescape(): String {
        val sb = StringBuilder(length)
        var i = 0
        while (i < length) {
            val c = this[i]
            if (c == '\\' && i + 1 < length) {
                when (val next = this[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    '\\' -> sb.append('\\')
                    else -> {
                        sb.append(c).append(next)
                    }
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
