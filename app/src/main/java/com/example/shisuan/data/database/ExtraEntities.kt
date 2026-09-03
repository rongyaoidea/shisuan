package com.example.shisuan.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredient")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val category: String,
    val unit: String,
    val unitPricePerGram: Double = 0.0,
    val origin: String? = null,
    val supplier: String? = null,
    val shelfLifeDays: Int? = null,
    val storageCondition: String? = null,
    val grade: String? = null,
    val color: String? = null,
    val odor: String? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "batch_material")
data class BatchMaterial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val batchId: Long,
    val ingredientId: Long,
    val ingredientName: String,
    val weightGram: Double,
    val unitPricePerGram: Double,
    val subtotal: Double
)

@Entity(tableName = "batch_result")
data class BatchResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
    val recordedAt: Long
)

enum class ProblemCategory { RAW, PROCESS, EQUIPMENT, PACKAGING, FLAVOR, COLOR, TEXTURE, MICROBIAL, OTHER }
enum class ProblemSeverity { LOW, MEDIUM, HIGH, CRITICAL }

@Entity(tableName = "batch_problem")
data class BatchProblem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val batchId: Long,
    val category: String,
    val severity: String,
    val description: String,
    val cause: String? = null,
    val solution: String? = null,
    val resolved: Boolean = false,
    val createdAt: Long,
    val resolvedAt: Long? = null
)

@Entity(tableName = "batch_snapshot")
data class BatchSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val batchId: Long,
    val snapshotNumber: Int,
    val productName: String,
    val batchName: String,
    val sampleWeightGram: Double,
    val materialCost: Double,
    val processingCost: Double,
    val note: String?,
    val tag: String? = null,
    val createdAt: Long,
    val unitCostPerGram: Double,
    val unitCostPerTon: Double,
    val costPerBox: Double,
    val costPerPackage: Double
)

@Entity(tableName = "operation_log")
data class OperationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val operationType: String,
    val entityType: String,
    val entityId: Long,
    val entityName: String,
    val before: String? = null,
    val after: String? = null,
    val reason: String? = null,
    val createdAt: Long
)
