package com.example.shisuan.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

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
    val batchName: String, // 批次编号
    val sampleWeightGram: Double, // 样品重量(g)
    val processingCost: Double = 0.0, // 加工费
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

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
    val ingredientId: Long? = null, // 可选：关联到原料库
    val weight: Double, // 用量(g)
    val unitPrice: Double, // 单价(元/g 或 元/kg)
    val priceUnit: String = "元/kg", // 单价单位
    val totalCost: Double = weight * unitPrice, // 小计
    val note: String = ""
)

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
    val priceUnit: String = "元/kg",
    val shelfLifeDays: Int? = null,
    val storageCondition: String = "",
    val note: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 换算配置表 - 保持不变
 */
@Entity(tableName = "unit_config")
data class UnitConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightPerBoxGram: Double,
    val packagesPerBox: Int,
    val weightPerPackageGram: Double = weightPerBoxGram / packagesPerBox,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 批次原料关联表（旧表，用于迁移兼容）
 * 新设计中由 BatchIngredient 替代
 */
@Entity(
    tableName = "batch_material",
    foreignKeys = [
        ForeignKey(entity = BatchRecord::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Ingredient::class, parentColumns = ["id"], childColumns = ["ingredientId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["batchId"]), Index(value = ["ingredientId"])]
)
data class BatchMaterial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val ingredientId: Long,
    val weight: Double,
    val cost: Double
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
 * 批次快照表 - 保持不变
 */
@Entity(
    tableName = "batch_snapshot",
    foreignKeys = [ForeignKey(entity = BatchRecord::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["batchId"])]
)
data class BatchSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val snapshotData: String,
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
