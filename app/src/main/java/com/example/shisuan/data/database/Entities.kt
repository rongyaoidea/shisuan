package com.example.shisuan.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.shisuan.utils.CostCalculator

/** 单价单位：元/千克（原料档案与批次配料的默认值） */
const val PRICE_UNIT_PER_KG = "元/kg"

/** 单价单位：元/克 */
const val PRICE_UNIT_PER_GRAM = "元/g"

/**
 * 产品表 - 主实体
 * 一个产品（如草莓酱）可以有多个批次
 */
@Entity(tableName = "product")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "", // 分类：果酱/酱料/调味品等
    val description: String = "",
    val isActive: Boolean = true, // 是否在产
    // 包装规格（产品级绑定，不同产品可有不同规格）
    val weightPerBoxGram: Double = 5000.0,    // 每箱克数
    val packagesPerBox: Int = 20,             // 每箱包数
    val weightPerPackageGram: Double = 250.0, // 每包克数
    /** 目标毛利率(%)，用于由成本推导建议出厂价；0 = 未设置，不展示报价 */
    val targetMarginRate: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 批次记录表 - 关联到产品
 * 移除 productName，改为 productId 外键
 * 移除 materialCost，改由 BatchIngredient 明细计算
 */
@Entity(
    tableName = "batch_record",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productId"]), Index(value = ["createdAt"])]
)
data class BatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long, // 外键：关联到 Product
    val batchName: String, // 批次编号（日期+序号，如 2026-09-03-01）
    val sampleWeightGram: Double, // 投料重量(g)

    // ─────────── 加工费（制造费用）分项 ───────────
    // 工厂真实成本远不止原料：包材、人工、水电折旧常占总成本 30%~50%，
    // 只算原料得出的吨价无法用于真实报价。
    val packagingCost: Double = 0.0, // 包材：瓶/盖/标签/外箱
    val laborCost: Double = 0.0,     // 人工
    val overheadCost: Double = 0.0,  // 水电蒸汽/设备折旧/其他

    /**
     * 出品率(%)：成品重量 ÷ 投料重量 × 100。
     *
     * 熬煮蒸发会使成品少于投料（如投 1000g 出 850g），
     * 不折算会把成本摊薄到并不存在的产量上，显著低估吨价。
     * null 或 <= 0 表示不折算（按 100% 计）。
     */
    val yieldRatePercent: Double? = null,

    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 加工费合计（元）。
     * 由分项求和得出而非单独存储，避免出现「分项改了、合计没改」的不一致。
     */
    val processingCost: Double
        get() = packagingCost + laborCost + overheadCost
}

/**
 * 批次原料明细表 - 记录每个批次使用的原料配比
 * 替代原来的单一 materialCost 字段
 */
@Entity(
    tableName = "batch_ingredient",
    foreignKeys = [
        ForeignKey(
            entity = BatchRecord::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["batchId"])]
)
data class BatchIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long, // 外键：关联到 BatchRecord
    val ingredientName: String, // 原料名称（冗余字段，便于显示）
    val ingredientSupplier: String = "", // 品牌（冗余字段，便于区分同款不同品牌）
    val ingredientId: Long? = null, // 可选：关联到原料库
    val weight: Double, // 用量(g)（百分比输入时已换算为克重）
    val ratioPercent: Double? = null, // 用户输入比例(%)，null=按克重输入；用于展示原值
    val unitPrice: Double, // 单价(元/g 或 元/kg)
    val priceUnit: String = PRICE_UNIT_PER_KG, // 单价单位，决定如何换算小计
    val totalCost: Double, // 小计(元)：必须经 CostCalculator.unitPriceToTotal 换算，见 [create]
    val note: String = ""
) {
    companion object {
        /**
         * 构造批次配料并正确换算小计。
         *
         * 注意：`weight` 单位是 g，而 `unitPrice` 默认单位是 元/kg，
         * 直接 `weight * unitPrice` 会因量纲不匹配算出放大 1000 倍的错误成本。
         * 换算统一委托给 [CostCalculator.unitPriceToTotal]（Rust 引擎或其 Kotlin 等价实现）。
         *
         * @param ingredient 原料库档案，提供单价与品牌
         * @param weightGram 用量（g）；比例输入时请先经 CostCalculator.ratioPercentToGram 换算
         * @param ratioPercent 用户输入的原始比例(%)，null=按克重输入
         */
        fun create(
            ingredient: Ingredient,
            weightGram: Double,
            ratioPercent: Double? = null
        ): BatchIngredient = BatchIngredient(
            batchId = 0, // 保存批次时再回填
            ingredientName = ingredient.name,
            ingredientSupplier = ingredient.supplier,
            ingredientId = ingredient.id,
            weight = weightGram,
            ratioPercent = ratioPercent,
            unitPrice = ingredient.unitPrice,
            priceUnit = ingredient.priceUnit,
            totalCost = CostCalculator.unitPriceToTotal(
                weightGram,
                ingredient.unitPrice,
                isPerGram = ingredient.priceUnit == PRICE_UNIT_PER_GRAM
            )
        )
    }
}

/**
 * 原料库表 - 保持原有设计，略作调整
 */
@Entity(tableName = "ingredient")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "", // 分类：水果/糖类/添加剂
    val supplier: String = "",
    val origin: String = "",
    val unitPrice: Double = 0.0,
    val priceUnit: String = PRICE_UNIT_PER_KG,
    val shelfLifeDays: Int? = null,
    val storageCondition: String = "",
    val note: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 批次成果记录表 - 保持不变
 */
@Entity(
    tableName = "batch_result",
    foreignKeys = [ForeignKey(entity = BatchRecord::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["batchId"])]
)
data class BatchResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val texture: String? = null,
    val color: String? = null,
    val aroma: String? = null,
    val taste: String? = null,
    val appearance: String? = null,
    val pHValue: Double? = null,
    val brixDegree: Double? = null,
    val yieldRate: Double? = null,
    val packagingResult: String? = null,
    val overallRating: Int? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

/**
 * 批次问题记录表 - 保持不变
 */
@Entity(
    tableName = "batch_problem",
    foreignKeys = [ForeignKey(entity = BatchRecord::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["batchId"])]
)
data class BatchProblem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val category: String,
    val description: String,
    val cause: String? = null,
    val solution: String? = null,
    val resolved: Boolean = false,
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 批次快照表 - git 式版本链
 *
 * 每次保存/更新批次时把完整内容（批次字段 + 配料明细）编码为文本存档：
 * - [digest] 内容指纹（SHA-256 前 8 位）即「唯一变更编号」，内容相同则不重复入库
 * - [version] 递增序号，用于时间线排序与「当前版本」定位
 * - 恢复 = 解码快照并原子写回批次与配料，回溯本身不产生新版本
 */
@Entity(
    tableName = "batch_snapshot",
    foreignKeys = [ForeignKey(entity = BatchRecord::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["batchId"]), Index(value = ["batchId", "digest"], unique = true)]
)
data class BatchSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val snapshotData: String,
    /** 内容指纹（唯一变更编号），如 "a1b2c3d4" */
    val digest: String = "",
    val version: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 操作日志表 - 保持不变
 */
@Entity(tableName = "operation_log")
data class OperationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationType: String,
    val targetType: String,
    val targetId: Long,
    val details: String,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
