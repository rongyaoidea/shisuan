package com.example.shisuan.domain.model

import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.utils.BatchSnapshotCodec
import com.example.shisuan.utils.WeightFormatter
import java.util.Locale

/**
 * 快照 Diff：两个版本的内容对比（git diff 语义）。
 *
 * 版本链只存完整内容、只能整体恢复；Diff 回答「两个版本之间到底变了什么」：
 * 批次字段变化 + 配料增删改（用量/单价/小计），供试产复盘与排障。
 *
 * 配料按键（名称, 品牌, 同名同品牌下的序号）配对，同一批次里配料名重复
 * （允许按索引删除，说明重复是合法输入）时也能逐条对齐，不串行。
 */
data class BatchFieldChange(
    val label: String,
    val oldText: String,
    val newText: String
)

enum class IngredientDiffKind { ADDED, REMOVED, CHANGED }

data class IngredientDiff(
    val kind: IngredientDiffKind,
    val name: String,
    val brand: String,
    val detail: String
) {
    val label: String get() = if (brand.isNotEmpty()) "$name（$brand）" else name
}

data class SnapshotDiffResult(
    val fieldChanges: List<BatchFieldChange>,
    val ingredientDiffs: List<IngredientDiff>
) {
    val hasChanges: Boolean get() = fieldChanges.isNotEmpty() || ingredientDiffs.isNotEmpty()
}

object SnapshotDiffer {

    fun diff(
        old: BatchSnapshotCodec.SnapshotData,
        new: BatchSnapshotCodec.SnapshotData
    ): SnapshotDiffResult {
        val fields = ArrayList<BatchFieldChange>(6)
        if (old.sampleWeightGram != new.sampleWeightGram) {
            fields += BatchFieldChange(
                "投料重量",
                WeightFormatter.format(old.sampleWeightGram),
                WeightFormatter.format(new.sampleWeightGram)
            )
        }
        if (old.packagingCost != new.packagingCost) {
            fields += BatchFieldChange("包材费", yuan(old.packagingCost), yuan(new.packagingCost))
        }
        if (old.laborCost != new.laborCost) {
            fields += BatchFieldChange("人工费", yuan(old.laborCost), yuan(new.laborCost))
        }
        if (old.overheadCost != new.overheadCost) {
            fields += BatchFieldChange("水电折旧", yuan(old.overheadCost), yuan(new.overheadCost))
        }
        if (old.yieldRatePercent != new.yieldRatePercent) {
            fields += BatchFieldChange(
                "出品率", yieldText(old.yieldRatePercent), yieldText(new.yieldRatePercent)
            )
        }
        if (old.note != new.note) {
            fields += BatchFieldChange("备注", old.note.ifEmpty { "（空）" }, new.note.ifEmpty { "（空）" })
        }

        val oldKeyed = keyed(old.ingredients)
        val newKeyed = keyed(new.ingredients)
        val diffs = ArrayList<IngredientDiff>(oldKeyed.size + newKeyed.size)

        // 删除：按旧版顺序；新增/变更：按新版顺序（阅读顺序与配料表一致）
        for ((key, o) in oldKeyed) {
            if (!newKeyed.containsKey(key)) {
                diffs += IngredientDiff(
                    IngredientDiffKind.REMOVED, o.ingredientName, o.ingredientSupplier,
                    removalDetail(o)
                )
            }
        }
        for ((key, n) in newKeyed) {
            val o = oldKeyed[key] ?: run {
                diffs += IngredientDiff(
                    IngredientDiffKind.ADDED, n.ingredientName, n.ingredientSupplier,
                    additionDetail(n)
                )
                return@run null
            } ?: continue
            changedDetail(o, n)?.let { detail ->
                diffs += IngredientDiff(IngredientDiffKind.CHANGED, n.ingredientName, n.ingredientSupplier, detail)
            }
        }
        // 删除行置前：先看到配料结构变化，再看存量配料的参数调整
        diffs.sortWith(
            compareBy(
                {
                    when (it.kind) {
                        IngredientDiffKind.REMOVED -> 0
                        IngredientDiffKind.ADDED -> 1
                        IngredientDiffKind.CHANGED -> 2
                    }
                }
            )
        )
        return SnapshotDiffResult(fields, diffs)
    }

    // ─────────── 配对键 ───────────

    private fun keyed(list: List<BatchIngredient>): LinkedHashMap<Triple<String, String, Int>, BatchIngredient> {
        val counts = mutableMapOf<Pair<String, String>, Int>()
        val map = LinkedHashMap<Triple<String, String, Int>, BatchIngredient>(list.size)
        for (bi in list) {
            val base = bi.ingredientName to bi.ingredientSupplier
            val n = (counts[base] ?: 0) + 1
            counts[base] = n
            map[Triple(base.first, base.second, n)] = bi
        }
        return map
    }

    // ─────────── 明细文案 ───────────

    private fun additionDetail(n: BatchIngredient): String =
        "${WeightFormatter.format(n.weight)} × ${priceText(n.unitPrice, n.priceUnit)} = ${yuan(n.totalCost)}"

    private fun removalDetail(o: BatchIngredient): String =
        "${WeightFormatter.format(o.weight)} × ${priceText(o.unitPrice, o.priceUnit)} = ${yuan(o.totalCost)}"

    /** 用量/单价/小计任一变化即返回明细；完全一致返回 null（无差异行） */
    private fun changedDetail(o: BatchIngredient, n: BatchIngredient): String? {
        val parts = ArrayList<String>(3)
        if (o.weight != n.weight) {
            parts += "用量 ${WeightFormatter.format(o.weight)} → ${WeightFormatter.format(n.weight)}"
        }
        if (o.unitPrice != n.unitPrice || o.priceUnit != n.priceUnit) {
            parts += "单价 ${priceText(o.unitPrice, o.priceUnit)} → ${priceText(n.unitPrice, n.priceUnit)}"
        }
        if (o.totalCost != n.totalCost) {
            parts += "小计 ${yuan(o.totalCost)} → ${yuan(n.totalCost)}"
        }
        // 备注变化不单独成行：配料备注多为操作记录，计入会淹没有成本意义的差异
        return parts.takeIf { it.isNotEmpty() }?.joinToString("；")
    }

    private fun yuan(v: Double): String = "¥${"%.2f".format(Locale.CHINA, v)}"

    private fun priceText(price: Double, unit: String): String =
        "¥${"%.2f".format(Locale.CHINA, price)}/${unit.removePrefix("元/")}"

    private fun yieldText(v: Double?): String =
        v?.let { "%.1f%%".format(Locale.CHINA, it) } ?: "未设置"
}
